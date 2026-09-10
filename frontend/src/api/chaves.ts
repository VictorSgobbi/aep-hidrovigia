import type { StatusOcorrencia } from './tipos'

/**
 * Chaves de cache do React Query, num só lugar.
 *
 * O prefixo importa: `invalidateQueries({ queryKey: ['ocorrencias'] })`
 * alcança todas as variações de filtro de uma vez, que é o que precisa
 * acontecer depois de registrar uma coleta — a ocorrência é aberta pelo
 * backend e não vem na resposta.
 */
export const chaves = {
  pontos: (filtros: Record<string, unknown> = {}) =>
    ['pontos', filtros] as const,
  ponto: (id: string) => ['pontos', 'id', id] as const,
  pontoPorCodigo: (codigo: string) => ['pontos', 'codigo', codigo] as const,

  analise: (id: string) => ['analises', 'id', id] as const,
  analisesDoPonto: (codigo: string) => ['analises', 'ponto', codigo] as const,
  analisesPorPeriodo: (inicio: string, fim: string) =>
    ['analises', 'periodo', inicio, fim] as const,

  /** Sem status = a fila de pendentes, que é a consulta padrão da tela. */
  ocorrencias: (status?: StatusOcorrencia) =>
    ['ocorrencias', status ?? 'pendentes'] as const,
  ocorrencia: (id: string) => ['ocorrencias', 'id', id] as const,

  painel: (escopo = 'geral') => ['painel', escopo] as const,
  catalogo: () => ['catalogo'] as const,
}
