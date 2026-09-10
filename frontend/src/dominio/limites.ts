import type { ParametroCatalogado } from '../api/tipos'

export type Previsao = 'conforme' | 'fora' | 'indefinida'

/**
 * Prévia local de conformidade de um valor digitado.
 *
 * Espelha as três formas de limite do backend usando só os números que o
 * catálogo publica:
 *
 * | Subclasse no backend | limiteMinimo | limiteMaximo |
 * |----------------------|--------------|--------------|
 * | `ParametroFaixa`     | presente     | presente     |
 * | `ParametroMaximo`    | ausente      | presente     |
 * | `ParametroAusencia`  | ausente      | **0**        |
 *
 * As duas comparações abaixo cobrem as três formas sem precisar saber qual é:
 * para ausência, qualquer contagem acima de zero cai em `fora`.
 *
 * É **prévia**: o veredito que vale é o do 201 do `POST /api/analises`, porque
 * é o backend que aplica a norma. Serve para o técnico perceber o desvio
 * enquanto digita, não para decidir.
 */
export function preverConformidade(
  parametro: ParametroCatalogado,
  valor: number | null,
): Previsao {
  if (valor === null || !Number.isFinite(valor) || valor < 0) {
    return 'indefinida'
  }

  const { limiteMinimo, limiteMaximo } = parametro

  if (limiteMinimo !== undefined && valor < limiteMinimo) return 'fora'
  if (limiteMaximo !== undefined && valor > limiteMaximo) return 'fora'

  return 'conforme'
}
