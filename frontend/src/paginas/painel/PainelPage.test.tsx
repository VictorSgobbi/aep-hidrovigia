import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { MemoryRouter } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { criarQueryClient } from '../../api/queryClient'
import type {
  IndicadoresConformidade,
  OcorrenciaResponse,
  SerieHistorica,
  SituacaoColeta,
} from '../../api/tipos'
import { PainelPage } from './PainelPage'

/**
 * Dois cenários com lógica de verdade: o estado vazio, que decide entre
 * indicadores e chamada para ação, e a leitura dos números do servidor.
 *
 * O layout não é testado aqui de propósito — ele é verificado abrindo a tela.
 * Mas "o painel abre em 0% e parece quebrado" é um defeito de comportamento, e
 * é o primeiro quadro do vídeo.
 */
const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => servidor.resetHandlers())
afterAll(() => servidor.close())

// Exatamente o que GET /api/painel/conformidade devolve com a carga de
// demonstracao, conferido contra a API rodando.
const INDICADORES_DO_SEED: IndicadoresConformidade = {
  escopo: 'geral',
  totalAnalises: 9,
  analisesConformes: 6,
  analisesNaoConformes: 3,
  percentualConformidade: 66.67,
  ocorrenciasPendentes: 3,
  ocorrenciasVencidas: 0,
}

const OCORRENCIA_CRITICA: OcorrenciaResponse = {
  id: 'oc-1',
  analiseId: 'analise-4',
  pontoId: 'ponto-3',
  pontoCodigo: 'PMA-003',
  gravidade: 'CRITICA',
  prazoHoras: 24,
  politicaClassificacao: 'risco-sanitario',
  status: 'ABERTA',
  abertaEm: new Date(Date.now() - 3_600_000).toISOString(),
  prazoLimite: new Date(Date.now() + 23 * 3_600_000).toISOString(),
  vencida: false,
  parametrosViolados: [
    {
      codigo: 'ECOLI',
      nome: 'Escherichia coli',
      valorMedido: 14,
      unidade: 'UFC/100mL',
      limiteAplicado: 'ausencia (UFC/100mL)',
      risco: 'MICROBIOLOGICO',
      mensagem: 'Escherichia coli detectado na amostra: 14 UFC/100mL',
    },
  ],
  tratativas: [],
}

function renderizar() {
  return render(
    <QueryClientProvider client={criarQueryClient()}>
      <MemoryRouter>
        <PainelPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PainelPage', () => {
  it('mostra os indicadores e a fila ordenada pelo servidor', async () => {
    servidor.use(
      http.get('/api/painel/conformidade', () =>
        HttpResponse.json(INDICADORES_DO_SEED),
      ),
      http.get('/api/ocorrencias', () =>
        HttpResponse.json([OCORRENCIA_CRITICA]),
      ),
      http.get('/api/pontos', () => HttpResponse.json([])),
      http.get('/api/painel/coletas', () => HttpResponse.json([])),
    )

    renderizar()

    // Percentual em formato brasileiro, e nunca sozinho: o texto de apoio diz
    // de quantas analises ele saiu.
    expect(await screen.findByText('66,7%')).toBeInTheDocument()
    expect(
      screen.getByText('6 de 9 análises dentro do padrão'),
    ).toBeInTheDocument()

    // A pendencia aparece com gravidade, prazo e o parametro violado.
    expect(await screen.findByText(/Crítica/)).toBeInTheDocument()
    expect(screen.getByText('· 24 h')).toBeInTheDocument()
    expect(screen.getByText('Escherichia coli')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'PMA-003' })).toHaveAttribute(
      'href',
      '/ocorrencias?ocorrencia=oc-1',
    )
  })

  it('lista so as coletas vencidas, distinguindo atrasada de nunca coletada', async () => {
    const situacoes: SituacaoColeta[] = [
      {
        codigoPonto: 'PMA-004',
        nomePonto: 'Cisterna Nova',
        tipoFonte: 'CISTERNA',
        frequenciaMinimaDias: 30,
        vencida: true,
      },
      {
        codigoPonto: 'PMA-002',
        nomePonto: 'Manancial do Distrito',
        tipoFonte: 'MANANCIAL_SUPERFICIAL',
        frequenciaMinimaDias: 7,
        ultimaColeta: '2026-01-01T12:00:00Z',
        proximaColetaAte: '2026-01-08T12:00:00Z',
        vencida: true,
      },
      {
        codigoPonto: 'PMA-001',
        nomePonto: 'Poco da Escola',
        tipoFonte: 'POCO_ARTESIANO',
        frequenciaMinimaDias: 30,
        ultimaColeta: '2026-01-10T12:00:00Z',
        proximaColetaAte: '2026-02-09T12:00:00Z',
        vencida: false,
      },
    ]
    servidor.use(
      http.get('/api/painel/conformidade', () =>
        HttpResponse.json(INDICADORES_DO_SEED),
      ),
      http.get('/api/ocorrencias', () => HttpResponse.json([])),
      http.get('/api/pontos', () => HttpResponse.json([])),
      http.get('/api/painel/coletas', () => HttpResponse.json(situacoes)),
    )

    renderizar()

    expect(await screen.findByText('Nunca coletado')).toBeInTheDocument()
    expect(screen.getByText('Coleta atrasada')).toBeInTheDocument()
    expect(screen.getByText(/a próxima devia ter sido até 08\/01\/2026/)).toBeInTheDocument()
    // O ponto em dia nao aparece: o bloco e um alerta, nao um inventario.
    expect(screen.queryByText('PMA-001')).not.toBeInTheDocument()
  })

  it('ao escolher um ponto, desenha a serie e lista os dias fora do limite', async () => {
    const serie: SerieHistorica = {
      codigoPonto: 'PMA-001',
      codigoParametro: 'CRL',
      nomeParametro: 'Cloro residual livre',
      unidade: 'mg/L',
      limiteMinimo: 0.2,
      limiteMaximo: 2,
      inicio: '2026-01-01T00:00:00Z',
      fim: '2026-03-31T00:00:00Z',
      dias: [
        { dia: '2026-01-10', minimo: 1.4, media: 1.4, maximo: 1.4, leituras: 1, foraDoLimite: 0 },
        { dia: '2026-02-10', minimo: 0.1, media: 0.1, maximo: 0.1, leituras: 1, foraDoLimite: 1 },
      ],
    }
    const pedidos: string[] = []
    servidor.use(
      http.get('/api/painel/conformidade/PMA-001', () =>
        HttpResponse.json({ ...INDICADORES_DO_SEED, escopo: 'PMA-001' }),
      ),
      http.get('/api/ocorrencias', () => HttpResponse.json([])),
      http.get('/api/pontos', () =>
        HttpResponse.json([{ id: 'ponto-1', codigo: 'PMA-001', nome: 'Poco da Escola' }]),
      ),
      http.get('/api/painel/coletas', () => HttpResponse.json([])),
      http.get('/api/painel/parametros', () => HttpResponse.json([])),
      http.get('/api/painel/serie/PMA-001', ({ request }) => {
        pedidos.push(new URL(request.url).search)
        return HttpResponse.json(serie)
      }),
    )

    renderizar()
    await userEvent.selectOptions(
      await screen.findByLabelText('Escopo'),
      await screen.findByRole('option', { name: /PMA-001/ }),
    )

    expect(
      await screen.findByRole('img', { name: /Cloro residual livre em 2 dias/ }),
    ).toBeInTheDocument()
    expect(screen.getByText(/fora do limite em 10\/02\/2026\./)).toBeInTheDocument()
    // Cloro residual e o parametro inicial: e onde a tendencia antecede o desvio.
    expect(pedidos).toEqual(['?parametro=CRL'])
  })

  it('sem analise nenhuma, oferece registrar a primeira coleta', async () => {
    // E como o sistema abre numa base limpa. Sem isso, a tela mostraria 0,0%
    // e quatro zeros, o que parece defeito.
    servidor.use(
      http.get('/api/painel/conformidade', () =>
        HttpResponse.json({
          ...INDICADORES_DO_SEED,
          totalAnalises: 0,
          analisesConformes: 0,
          analisesNaoConformes: 0,
          percentualConformidade: 0,
          ocorrenciasPendentes: 0,
        }),
      ),
      http.get('/api/ocorrencias', () => HttpResponse.json([])),
      http.get('/api/pontos', () => HttpResponse.json([])),
      http.get('/api/painel/coletas', () => HttpResponse.json([])),
    )

    renderizar()

    expect(
      await screen.findByRole('link', { name: 'Registrar a primeira coleta' }),
    ).toHaveAttribute('href', '/coletas')
    expect(screen.queryByText('0,0%')).not.toBeInTheDocument()
  })

  it('avisa que a API pode estar fora quando a requisicao nao sai', async () => {
    // Erro numero um de quem clona o repositorio e abre a interface sem subir
    // o backend.
    servidor.use(
      http.get('/api/painel/conformidade', () => HttpResponse.error()),
      http.get('/api/ocorrencias', () => HttpResponse.json([])),
      http.get('/api/pontos', () => HttpResponse.json([])),
      http.get('/api/painel/coletas', () => HttpResponse.json([])),
    )

    renderizar()

    // A espera longa e a politica de retry trabalhando: falha de rede e
    // reenviada duas vezes com recuo exponencial antes de virar erro na tela.
    // O teste usa o QueryClient real de proposito — desligar o retry aqui
    // testaria uma configuracao que nao existe em producao.
    const aviso = await screen.findByRole('alert', {}, { timeout: 6000 })
    expect(aviso).toHaveTextContent('localhost:8080')
  }, 10_000)
})
