import type { Instante } from '../api/tipos'

/**
 * Conversões entre o que o navegador digita e o que a API aceita.
 *
 * As duas primeiras funções existem por causa de falhas que a API reporta de
 * um jeito difícil de entender:
 *
 * - `<input type="datetime-local">` produz `"2026-09-09T14:30"`, sem offset. O
 *   Jackson recusa isso, e o erro sai no corpo padrão do Spring, **sem**
 *   `detalhes` — então o mapeamento por campo não tem o que exibir.
 * - Teclado brasileiro escreve `0,8`. `Number("0,8")` é `NaN`, e
 *   `JSON.stringify(NaN)` é `null`, o que gera um 400 sem relação aparente
 *   com o que foi digitado.
 */

/**
 * Converte o valor de um `<input type="datetime-local">` (hora local, sem
 * offset) para um Instante ISO-8601 em UTC.
 *
 * @returns o Instante, ou `null` se o texto não for uma data válida
 */
export function paraInstante(local: string): Instante | null {
  const texto = local.trim()

  // A forma e validada antes de entregar ao Date porque o construtor aceita
  // demais: `new Date("09/09/2026")` nao e invalido, e sim interpretado no
  // formato americano. Sem este teste, uma data digitada a mao em formato
  // brasileiro viraria silenciosamente o instante errado.
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(texto)) return null

  const data = new Date(texto)
  if (Number.isNaN(data.getTime())) return null

  return data.toISOString()
}

/** Formato aceito por `<input type="datetime-local">`, no fuso local. */
export function paraDatetimeLocal(instante: Instante | Date): string {
  const data = instante instanceof Date ? instante : new Date(instante)

  const doisDigitos = (n: number) => String(n).padStart(2, '0')

  return (
    `${data.getFullYear()}-${doisDigitos(data.getMonth() + 1)}-` +
    `${doisDigitos(data.getDate())}T${doisDigitos(data.getHours())}:` +
    `${doisDigitos(data.getMinutes())}`
  )
}

/** Só a data, para `<input type="date">`. */
export function paraDateLocal(instante: Instante | Date): string {
  return paraDatetimeLocal(instante).slice(0, 10)
}

/**
 * Converte o texto digitado num número decimal, aceitando vírgula.
 *
 * @returns o número, ou `null` se não for um decimal válido e não negativo
 */
export function paraDecimal(texto: string): number | null {
  const limpo = texto.trim().replace(',', '.')
  if (limpo === '') return null

  // Recusa "1.2.3", "1e5", "abc" e sinal negativo: a API responde 409 para
  // valor negativo, e é melhor barrar antes de enviar.
  if (!/^\d+(\.\d+)?$/.test(limpo)) return null

  const valor = Number(limpo)
  return Number.isFinite(valor) ? valor : null
}

/* Exibição ------------------------------------------------------------------ */

const NUMERO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 })
const PERCENTUAL = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})
const DECIMAL = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 })
const DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
})
const DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })

export const formatarNumero = (valor: number) => NUMERO.format(valor)

/**
 * O percentual do painel já vem arredondado em duas casas, e o da análise vem
 * cru (um `double`, como 66.66666666666667). Uma casa decimal para os dois.
 */
export const formatarPercentual = (valor: number) =>
  `${PERCENTUAL.format(valor)}%`

export const formatarDecimal = (valor: number) => DECIMAL.format(valor)

export const formatarDataHora = (instante: Instante) =>
  DATA_HORA.format(new Date(instante))

export const formatarData = (instante: Instante) =>
  DATA.format(new Date(instante))

/** Junta valor e unidade, respeitando o pH, que chega com unidade vazia. */
export function formatarMedida(valor: number, unidade: string): string {
  return unidade === ''
    ? formatarDecimal(valor)
    : `${formatarDecimal(valor)} ${unidade}`
}
