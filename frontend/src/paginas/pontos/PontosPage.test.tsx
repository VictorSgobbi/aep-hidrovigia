import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { MemoryRouter } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { criarQueryClient } from '../../api/queryClient'
import type { PontoRequest, PontoResponse } from '../../api/tipos'
import { PontosPage } from './PontosPage'

const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => servidor.resetHandlers())
afterAll(() => servidor.close())

function ponto(ajustes: Partial<PontoResponse> = {}): PontoResponse {
  return {
    id: 'ponto-1',
    codigo: 'PMA-001',
    nome: 'Poco da Escola Rural Sao Jose',
    tipoFonte: 'POCO_ARTESIANO',
    descricaoFonte: 'Poco artesiano',
    populacaoAtendida: 320,
    localizacao: { municipio: 'Maringa', uf: 'PR' },
    responsavel: { nome: 'Ana Souza', contato: 'ana.souza@exemplo.gov.br' },
    ativo: true,
    criadoEm: '2026-01-15T09:00:00Z',
    ...ajustes,
  }
}

function renderizar() {
  render(
    <QueryClientProvider client={criarQueryClient()}>
      <MemoryRouter>
        <PontosPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PontosPage', () => {
  it('mostra o tipo de fonte com acento, e nao o rotulo cru do servidor', async () => {
    // O servidor manda descricaoFonte: "Poco artesiano". Exibir isso cru
    // deixaria a interface sem acento no meio do portugues.
    servidor.use(http.get('/api/pontos', () => HttpResponse.json([ponto()])))
    renderizar()

    expect(await screen.findByText('Poço artesiano')).toBeInTheDocument()
    expect(screen.queryByText('Poco artesiano')).not.toBeInTheDocument()
  })

  it('desabilita "apenas ativos" ao filtrar por municipio, que a API ignora', async () => {
    // Com `municipio` preenchido o backend ignora apenasAtivos e devolve os
    // dois. Manter a caixa ativa fingiria um filtro que nao acontece.
    servidor.use(http.get('/api/pontos', () => HttpResponse.json([ponto()])))
    renderizar()

    const apenasAtivos = await screen.findByRole('checkbox', {
      name: /Apenas ativos/,
    })
    expect(apenasAtivos).toBeEnabled()

    await userEvent.type(screen.getByRole('searchbox'), 'Maringa')

    expect(apenasAtivos).toBeDisabled()
    expect(
      screen.getByText('O filtro por município retorna ativos e inativos.'),
    ).toBeInTheDocument()
  })

  it('oferece apenas a acao de situacao possivel', async () => {
    // O 409 "ja esta desativado" fica inalcancavel pela interface.
    servidor.use(
      http.get('/api/pontos', () =>
        HttpResponse.json([
          ponto(),
          ponto({ id: 'ponto-2', codigo: 'PMA-002', ativo: false }),
        ]),
      ),
    )
    renderizar()

    const linhas = await screen.findAllByRole('row')
    const ativo = linhas.find((linha) => linha.textContent?.includes('PMA-001'))
    const inativo = linhas.find((linha) =>
      linha.textContent?.includes('PMA-002'),
    )

    expect(
      within(ativo as HTMLElement).getByRole('button', { name: 'Desativar' }),
    ).toBeInTheDocument()
    expect(
      within(ativo as HTMLElement).queryByRole('button', { name: 'Reativar' }),
    ).not.toBeInTheDocument()

    expect(
      within(inativo as HTMLElement).getByRole('button', { name: 'Reativar' }),
    ).toBeInTheDocument()
  })

  it('mantem o codigo somente leitura na edicao, mas continua enviando', async () => {
    // O backend exige o campo (@NotBlank) e o ignora, porque o codigo e
    // imutavel. Remover do corpo "porque nao e usado" causaria 400.
    let corpo: PontoRequest | null = null

    servidor.use(
      http.get('/api/pontos', () => HttpResponse.json([ponto()])),
      http.put('/api/pontos/ponto-1', async ({ request }) => {
        corpo = (await request.json()) as PontoRequest
        return HttpResponse.json(ponto({ nome: 'Poco Reformado' }))
      }),
    )
    renderizar()

    await userEvent.click(
      await screen.findByRole('button', { name: 'Editar' }),
    )

    const codigo = screen.getByLabelText(/Código/)
    expect(codigo).toHaveAttribute('readonly')
    expect(codigo).toHaveValue('PMA-001')
    expect(
      screen.getByText('O código identifica o ponto e não pode mudar.'),
    ).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    const enviado = corpo as unknown as PontoRequest
    expect(enviado.codigo).toBe('PMA-001')
  })

  it('aponta o 409 de codigo duplicado no proprio campo do codigo', async () => {
    // O 409 nao vem com `detalhes`, entao a mensagem e copiada para o campo a
    // que ela se refere, em vez de ficar so num aviso solto no topo.
    servidor.use(
      http.get('/api/pontos', () => HttpResponse.json([])),
      http.post('/api/pontos', () =>
        HttpResponse.json(
          {
            instante: '2026-09-09T12:00:00Z',
            status: 409,
            erro: 'Regra de negocio violada',
            mensagem: 'Ja existe um ponto de monitoramento com o codigo PMA-001',
            detalhes: [],
          },
          { status: 409 },
        ),
      ),
    )
    renderizar()

    await userEvent.click(
      await screen.findByRole('button', { name: 'Cadastrar o primeiro ponto' }),
    )

    // As buscas ficam dentro do dialogo: o filtro da pagina tem um campo
    // "Município" com o mesmo rotulo do formulario.
    const dialogo = within(screen.getByRole('dialog'))

    // Todos os obrigatorios: o jsdom aplica a validacao nativa do HTML, entao
    // com um campo required vazio o submit nem sai — como no navegador.
    await userEvent.type(dialogo.getByLabelText(/Código/), 'PMA-001')
    await userEvent.type(dialogo.getByLabelText(/Nome do ponto/), 'Outro poco')
    await userEvent.type(dialogo.getByLabelText(/População/), '10')
    await userEvent.type(dialogo.getByLabelText(/Município/), 'Maringa')
    await userEvent.type(dialogo.getByLabelText(/^UF/), 'PR')
    await userEvent.type(
      dialogo.getByLabelText(/Nome do responsável/),
      'Ana Souza',
    )
    await userEvent.type(
      dialogo.getByLabelText(/Contato/),
      'ana.souza@exemplo.gov.br',
    )
    await userEvent.click(dialogo.getByRole('button', { name: 'Salvar' }))

    // Espera a resposta antes de olhar a descricao do campo.
    await screen.findByText(/Ja existe um ponto de monitoramento/)

    const codigo = dialogo.getByLabelText(/Código/)
    expect(codigo).toHaveAccessibleDescription(
      /Ja existe um ponto de monitoramento com o codigo PMA-001/,
    )
    expect(codigo).toBeInvalid()
  })
})
