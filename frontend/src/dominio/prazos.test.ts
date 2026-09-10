import { describe, expect, it } from 'vitest'
import type { OcorrenciaResponse } from '../api/tipos'
import { estadoDoPrazo } from './prazos'
import { ROTULO_GRAVIDADE, ROTULO_STATUS, rotular } from './rotulos'

const ABERTA_EM = '2026-01-15T09:00:00.000Z'
const PRAZO_LIMITE = '2026-01-16T09:00:00.000Z' // 24 h depois: critica

function ocorrencia(
  ajustes: Partial<OcorrenciaResponse> = {},
): OcorrenciaResponse {
  return {
    id: 'oc-1',
    analiseId: 'analise-2',
    pontoId: 'ponto-1',
    pontoCodigo: 'PMA-001',
    gravidade: 'CRITICA',
    prazoHoras: 24,
    politicaClassificacao: 'risco-sanitario',
    status: 'ABERTA',
    abertaEm: ABERTA_EM,
    prazoLimite: PRAZO_LIMITE,
    vencida: false,
    parametrosViolados: [],
    tratativas: [],
    ...ajustes,
  }
}

const emHoras = (horasDepoisDaAbertura: number) =>
  Date.parse(ABERTA_EM) + horasDepoisDaAbertura * 3_600_000

describe('estadoDoPrazo', () => {
  it('mede quanto do prazo foi consumido', () => {
    expect(estadoDoPrazo(ocorrencia(), emHoras(0)).consumido).toBe(0)
    expect(estadoDoPrazo(ocorrencia(), emHoras(6)).consumido).toBe(25)
    expect(estadoDoPrazo(ocorrencia(), emHoras(12)).consumido).toBe(50)
    expect(estadoDoPrazo(ocorrencia(), emHoras(24)).consumido).toBe(100)
    // Nao passa de 100 depois de vencer.
    expect(estadoDoPrazo(ocorrencia(), emHoras(72)).consumido).toBe(100)
  })

  it('recalcula o vencimento em vez de confiar no campo do servidor', () => {
    // `vencida` e calculado no instante da requisicao e envelhece no cache.
    const dentro = estadoDoPrazo(ocorrencia(), emHoras(6))
    const depois = estadoDoPrazo(ocorrencia(), emHoras(30))

    expect(dentro.vencida).toBe(false)
    expect(depois.vencida).toBe(true)
  })

  it('nunca desmarca o que o servidor marcou como vencido', () => {
    // Se o relogio da maquina estiver atrasado, o erro vai para o lado seguro.
    const estado = estadoDoPrazo(ocorrencia({ vencida: true }), emHoras(1))

    expect(estado.vencida).toBe(true)
  })

  it('descreve o tempo restante em hora ou dia, conforme a escala', () => {
    expect(estadoDoPrazo(ocorrencia(), emHoras(18)).texto).toBe('em 6 horas')
    expect(estadoDoPrazo(ocorrencia(), emHoras(25)).texto).toBe('há 1 hora')

    // Prazo de 168 h (gravidade media) em horas ficaria ilegivel.
    const media = ocorrencia({
      gravidade: 'MEDIA',
      prazoHoras: 168,
      prazoLimite: '2026-01-22T09:00:00.000Z',
    })
    expect(estadoDoPrazo(media, emHoras(0)).texto).toBe('em 7 dias')
  })
})

describe('rotular', () => {
  it('traduz os enums para portugues acentuado', () => {
    expect(rotular(ROTULO_GRAVIDADE, 'CRITICA')).toBe('Crítica')
    expect(rotular(ROTULO_STATUS, 'EM_TRATATIVA')).toBe('Em tratativa')
  })

  it('devolve o valor cru quando o mapa nao conhece, em vez de undefined', () => {
    // Protege contra backend atualizado com bundle antigo em cache: a tela
    // mostraria "undefined" na coluna de gravidade durante a gravacao.
    expect(rotular(ROTULO_GRAVIDADE, 'CATASTROFICA')).toBe('CATASTROFICA')
  })
})
