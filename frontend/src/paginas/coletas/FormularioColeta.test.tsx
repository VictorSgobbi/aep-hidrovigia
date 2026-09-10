import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { MemoryRouter } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { criarQueryClient } from '../../api/queryClient'
import type {
  AnaliseRequest,
  AnaliseResponse,
  ParametroCatalogado,
  PontoResponse,
} from '../../api/tipos'
import { FormularioColeta } from './FormularioColeta'

const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => servidor.resetHandlers())
afterAll(() => servidor.close())

/** Recorte fiel de GET /api/painel/parametros, na ordem que a API devolve. */
const CATALOGO: ParametroCatalogado[] = [
  {
    codigo: 'ECOLI',
    nome: 'Escherichia coli',
    unidade: 'UFC/100mL',
    limite: 'ausencia (UFC/100mL)',
    limiteMaximo: 0,
    risco: 'MICROBIOLOGICO',
    referenciaLegal: 'Portaria GM/MS n. 888/2021',
  },
  {
    codigo: 'CRL',
    nome: 'Cloro residual livre',
    unidade: 'mg/L',
    limite: 'entre 0.2 e 2.0 mg/L',
    limiteMinimo: 0.2,
    limiteMaximo: 2,
    risco: 'DESINFECCAO',
    referenciaLegal: 'Portaria GM/MS n. 888/2021',
  },
  {
    codigo: 'PH',
    nome: 'pH',
    unidade: '',
    limite: 'entre 6.0 e 9.0',
    limiteMinimo: 6,
    limiteMaximo: 9,
    risco: 'FISICO_QUIMICO',
    referenciaLegal: 'Portaria GM/MS n. 888/2021',
  },
]

const PONTO: PontoResponse = {
  id: 'ponto-3',
  codigo: 'PMA-003',
  nome: 'Cisterna da Associacao Agua Viva',
  tipoFonte: 'CISTERNA',
  descricaoFonte: 'Cisterna',
  populacaoAtendida: 85,
  localizacao: { municipio: 'Sarandi', uf: 'PR' },
  responsavel: { nome: 'Marta Reis', contato: 'marta.reis@exemplo.org.br' },
  ativo: true,
  criadoEm: '2026-01-15T09:00:00Z',
}

function renderizar(aoRegistrar = vi.fn()) {
  render(
    <QueryClientProvider client={criarQueryClient()}>
      <MemoryRouter>
        <FormularioColeta aoRegistrar={aoRegistrar} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return aoRegistrar
}

function padrao() {
  servidor.use(
    http.get('/api/painel/parametros', () => HttpResponse.json(CATALOGO)),
    http.get('/api/pontos', () => HttpResponse.json([PONTO])),
  )
}

describe('FormularioColeta', () => {
  it('monta uma linha por parametro do catalogo, na ordem da norma', async () => {
    padrao()
    renderizar()

    const linhas = await screen.findAllByRole('listitem')

    // Microbiologicos primeiro: a ordem vem do LinkedHashMap do catalogo e nao
    // e reordenada aqui.
    expect(linhas).toHaveLength(3)
    expect(linhas[0]).toHaveTextContent('Escherichia coli')
    expect(linhas[1]).toHaveTextContent('Cloro residual livre')
    expect(linhas[2]).toHaveTextContent('pH')

    // O limite vem pronto do servidor, como dica ligada ao campo.
    expect(linhas[0]).toHaveTextContent('Limite: ausencia (UFC/100mL)')
  })

  it('aceita digitar 14 no parametro de ausencia, cujo limiteMaximo e 0', async () => {
    // A armadilha: um <input max={parametro.limiteMaximo}> daria max="0" aqui,
    // e seria impossivel registrar a contaminacao que abre a ocorrencia
    // critica. O limite e verificado pela previa, nao pelo atributo HTML.
    padrao()
    renderizar()

    const linhaEcoli = (await screen.findAllByRole('listitem'))[0] as HTMLElement
    await userEvent.click(within(linhaEcoli).getByRole('checkbox'))

    const valor = within(linhaEcoli).getByLabelText(
      /Valor medido de Escherichia coli/,
    )
    await userEvent.type(valor, '14')

    expect(valor).toHaveValue('14')
    expect(valor).not.toHaveAttribute('max')
    expect(linhaEcoli).toHaveTextContent('Prévia: fora do limite')
  })

  it('mostra a previa dentro do limite e aceita virgula decimal', async () => {
    padrao()
    renderizar()

    const linhaCloro = (await screen.findAllByRole('listitem'))[1] as HTMLElement
    await userEvent.click(within(linhaCloro).getByRole('checkbox'))
    await userEvent.type(
      within(linhaCloro).getByLabelText(/Valor medido de Cloro/),
      '0,8',
    )

    expect(linhaCloro).toHaveTextContent('Prévia: dentro do limite')
  })

  it('envia so os marcados, com o decimal convertido, e devolve o veredito', async () => {
    let corpo: AnaliseRequest | null = null

    const resposta: AnaliseResponse = {
      id: 'analise-9',
      pontoId: 'ponto-3',
      pontoCodigo: 'PMA-003',
      coletadoEm: '2026-09-09T10:00:00Z',
      coletor: 'Tecnico Bruno',
      conforme: false,
      qtdParametros: 2,
      qtdNaoConformidades: 1,
      percentualConformidade: 50,
      parametros: [],
      registradoEm: '2026-09-09T11:00:00Z',
    }

    padrao()
    servidor.use(
      http.post('/api/analises', async ({ request }) => {
        corpo = (await request.json()) as AnaliseRequest
        return HttpResponse.json(resposta, { status: 201 })
      }),
    )

    const aoRegistrar = renderizar()

    await userEvent.selectOptions(
      await screen.findByLabelText(/Ponto de coleta/),
      'PMA-003',
    )
    await userEvent.type(screen.getByLabelText(/Quem coletou/), 'Tecnico Bruno')

    const linhas = await screen.findAllByRole('listitem')
    // Marca ECOLI e pH, deixando o cloro de fora.
    await userEvent.click(within(linhas[0] as HTMLElement).getByRole('checkbox'))
    await userEvent.type(
      within(linhas[0] as HTMLElement).getByLabelText(/Valor medido/),
      '14',
    )
    await userEvent.click(within(linhas[2] as HTMLElement).getByRole('checkbox'))
    await userEvent.type(
      within(linhas[2] as HTMLElement).getByLabelText(/Valor medido/),
      '7,1',
    )

    await userEvent.click(
      screen.getByRole('button', { name: 'Registrar coleta' }),
    )

    await vi.waitFor(() => expect(corpo).not.toBeNull())

    const enviado = corpo as unknown as AnaliseRequest
    expect(enviado.codigoPonto).toBe('PMA-003')
    // Só os dois marcados, na ordem do catálogo, com a vírgula convertida.
    expect(enviado.leituras).toEqual([
      { codigo: 'ECOLI', valor: 14 },
      { codigo: 'PH', valor: 7.1 },
    ])
    // Instante com offset, e nao o "2026-09-09T10:00" que o input produz.
    expect(enviado.coletadoEm).toMatch(/Z$/)

    expect(aoRegistrar).toHaveBeenCalledWith(resposta)
  })

  it('nao monta o formulario se o catalogo da norma nao carregar', async () => {
    // Registrar as cegas produziria 422 em cada codigo.
    servidor.use(
      http.get('/api/painel/parametros', () =>
        HttpResponse.json({ status: 500 }, { status: 500 }),
      ),
      http.get('/api/pontos', () => HttpResponse.json([PONTO])),
    )

    renderizar()

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.queryByLabelText(/Ponto de coleta/)).not.toBeInTheDocument()
  })
})
