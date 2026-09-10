import type { OcorrenciaResponse } from '../api/tipos'

const RELATIVO = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' })

export interface EstadoDoPrazo {
  vencida: boolean
  /** Quanto do prazo já foi consumido, de 0 a 100. */
  consumido: number
  restanteMs: number
  /** "vence em 6 horas", "venceu há 2 dias". */
  texto: string
}

/**
 * Estado do prazo de uma ocorrência em relação a um instante de referência.
 *
 * O campo `vencida` que a API devolve é calculado no servidor no momento da
 * requisição, então envelhece entre dois fetches: uma ocorrência buscada às
 * 10h com prazo para as 11h continuaria chegando com `vencida: false` no cache.
 *
 * A regra aqui é `vencida do servidor OU já passou do prazo`: recalcula para
 * frente, mas **nunca desmarca** o que o servidor marcou — se o relógio da
 * máquina estiver errado, o erro é para o lado seguro.
 */
export function estadoDoPrazo(
  ocorrencia: OcorrenciaResponse,
  agora: number,
): EstadoDoPrazo {
  const abertura = Date.parse(ocorrencia.abertaEm)
  const limite = Date.parse(ocorrencia.prazoLimite)

  const vencida = ocorrencia.vencida || agora >= limite
  const restanteMs = limite - agora

  const total = limite - abertura
  const consumido =
    total > 0
      ? Math.min(100, Math.max(0, ((agora - abertura) / total) * 100))
      : 100

  return { vencida, consumido, restanteMs, texto: descrever(restanteMs) }
}

function descrever(restanteMs: number): string {
  const horas = Math.round(restanteMs / 3_600_000)

  // Acima de 48 h em horas fica ilegivel ("vence em 168 horas").
  if (Math.abs(horas) >= 48) {
    return RELATIVO.format(Math.round(horas / 24), 'day')
  }
  if (Math.abs(horas) >= 1) {
    return RELATIVO.format(horas, 'hour')
  }

  return RELATIVO.format(Math.round(restanteMs / 60_000), 'minute')
}
