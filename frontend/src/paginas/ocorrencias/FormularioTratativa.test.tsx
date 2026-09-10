import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { criarQueryClient } from '../../api/queryClient'
import type { OcorrenciaResponse } from '../../api/tipos'
import { FormularioTratativa } from './FormularioTratativa'

const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledRequest: 'bypass' }))
afterEach(() => {
  servidor.resetHandlers()
  localStorage.clear()
})
afterAll(() => servidor.close())

function ocorrencia(
  ajustes: Partial<OcorrenciaResponse> = {},
): OcorrenciaResponse {
  return {
    id: 'oc-1',
    analiseId: 'analise-4',
    pontoId: 'ponto-3',
    pontoCodigo: 'PMA-003',
    gravidade: 'CRITICA',
    prazoHoras: 24,
    politicaClassificacao: 'risco-sanitario',
    status: 'ABERTA',
    abertaEm: '2026-01-15T09:00:00Z',
    prazoLimite: '2026-01-16T09:00:00Z',
    vencida: false,
    parametrosViolados: [],
    tratativas: [],
    ...ajustes,
  }
}

function renderizar(dados: OcorrenciaResponse) {
  return render(
    <QueryClientProvider client={criarQueryClient()}>
      <FormularioTratativa ocorrencia={dados} />
    </QueryClientProvider>,
  )
}

describe('FormularioTratativa', () => {
  it('aceita tratativas sucessivas sem travar o formulario', async () => {
    // O backend permite varias acoes na mesma ocorrencia (o status fica em
    // EM_TRATATIVA). Se o formulario se desabilitasse depois da primeira, a
    // interface seria mais restritiva que a regra — e era justamente esse o
    // defeito que existia no dominio antes.
    let chamadas = 0

    servidor.use(
      http.post('/api/ocorrencias/oc-1/tratativas', async ({ request }) => {
        chamadas += 1
        const corpo = (await request.json()) as { acao: string; por: string }
        return HttpResponse.json(
          ocorrencia({
            status: 'EM_TRATATIVA',
            tratativas: [
              {
                acao: corpo.acao,
                por: corpo.por,
                em: '2026-01-15T10:00:00Z',
                statusResultante: 'EM_TRATATIVA',
              },
            ],
          }),
        )
      }),
    )

    renderizar(ocorrencia())

    await userEvent.type(
      screen.getByLabelText(/O que foi feito/),
      'Ponto isolado da rede',
    )
    await userEvent.type(screen.getByLabelText(/Responsável/), 'Leonardo')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar ação' }))

    // O campo de acao e limpo e o botao continua disponivel para a proxima.
    const acao = await screen.findByLabelText(/O que foi feito/)
    expect(acao).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Registrar ação' })).toBeEnabled()

    await userEvent.type(acao, 'Recloracao do reservatorio')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar ação' }))

    expect(chamadas).toBe(2)
    // O responsavel e lembrado entre registros, para nao redigitar na demo.
    expect(screen.getByLabelText(/Responsável/)).toHaveValue('Leonardo')
  })

  it('exige confirmacao para encerrar, porque nao existe reabertura', async () => {
    let encerrou = false

    servidor.use(
      http.post('/api/ocorrencias/oc-1/resolucao', () => {
        encerrou = true
        return HttpResponse.json(ocorrencia({ status: 'RESOLVIDA' }))
      }),
    )

    renderizar(ocorrencia({ status: 'EM_TRATATIVA' }))

    await userEvent.type(screen.getByLabelText(/O que foi feito/), 'Contraprova')
    await userEvent.type(screen.getByLabelText(/Responsável/), 'Victor')

    // O primeiro clique apenas pede confirmacao.
    await userEvent.click(
      screen.getByRole('button', { name: 'Encerrar ocorrência' }),
    )
    expect(encerrou).toBe(false)
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'não pode ser reaberta',
    )

    await userEvent.click(
      screen.getByRole('button', { name: 'Confirmar encerramento' }),
    )
    expect(encerrou).toBe(true)
  })

  it('numa ocorrencia resolvida nao oferece formulario', () => {
    // Evita o 409 "transicao invalida" por construcao: a acao que a API
    // recusaria nem aparece na tela.
    renderizar(
      ocorrencia({
        status: 'RESOLVIDA',
        tratativas: [
          {
            acao: 'Contraprova conforme',
            por: 'Victor',
            em: '2026-01-15T12:00:00Z',
            statusResultante: 'RESOLVIDA',
          },
        ],
      }),
    )

    expect(screen.getByText(/Ocorrência encerrada por Victor/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/O que foi feito/)).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Encerrar ocorrência' }),
    ).not.toBeInTheDocument()
  })
})
