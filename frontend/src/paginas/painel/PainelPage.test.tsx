import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { MemoryRouter } from 'react-router-dom'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { criarQueryClient } from '../../api/queryClient'
import type {
  IndicadoresConformidade,
  OcorrenciaResponse,
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
  totalAnalises: 5,
  analisesConformes: 2,
  analisesNaoConformes: 3,
  percentualConformidade: 40,
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
    )

    renderizar()

    // Percentual em formato brasileiro, e nunca sozinho: o texto de apoio diz
    // de quantas analises ele saiu.
    expect(await screen.findByText('40,0%')).toBeInTheDocument()
    expect(
      screen.getByText('2 de 5 análises dentro do padrão'),
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
