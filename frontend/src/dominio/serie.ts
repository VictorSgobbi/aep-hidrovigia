import type { SerieHistorica } from '../api/tipos'

export interface Moldura {
  largura: number
  altura: number
  /** Espaço livre em volta da área de desenho, para rótulos e pontos. */
  margem: number
}

export interface PontoDoGrafico {
  x: number
  y: number
  dia: string
  media: number
  fora: boolean
}

export interface GraficoDaSerie {
  pontos: PontoDoGrafico[]
  /** `y` de cada limite da norma; ausente quando o parâmetro não tem aquele lado. */
  yLimiteMinimo?: number | undefined
  yLimiteMaximo?: number | undefined
  /** Valores nas bordas de baixo e de cima da área de desenho. */
  escala: { minimo: number; maximo: number }
}

const DIA_EM_MS = 86_400_000

/**
 * Converte a série em coordenadas de SVG.
 *
 * O eixo x é o **tempo**, e não a posição na lista: três coletas espalhadas em
 * 60 dias e três em 3 dias não podem virar o mesmo desenho, ou o gráfico
 * esconde justamente o intervalo entre coletas.
 *
 * O eixo y cobre os valores medidos **e** os limites, para que a faixa da norma
 * apareça mesmo quando todas as leituras estão longe dela.
 */
export function escalarSerie(
  serie: SerieHistorica,
  { largura, altura, margem }: Moldura,
): GraficoDaSerie {
  const valores = serie.dias.flatMap((dia) => [dia.minimo, dia.maximo])
  for (const limite of [serie.limiteMinimo, serie.limiteMaximo]) {
    if (limite !== undefined) valores.push(limite)
  }

  let minimo = valores.length > 0 ? Math.min(...valores) : 0
  let maximo = valores.length > 0 ? Math.max(...valores) : 1
  if (minimo === maximo) {
    minimo -= 1
    maximo += 1
  }
  const folga = (maximo - minimo) * 0.1
  minimo = Math.max(0, minimo - folga)
  maximo += folga

  const inicio = Date.parse(serie.inicio)
  const duracao = Math.max(Date.parse(serie.fim) - inicio, DIA_EM_MS)
  const larguraUtil = largura - 2 * margem
  const alturaUtil = altura - 2 * margem

  const xDe = (dia: string) => {
    // O dia chega em UTC; o meio-dia evita que um dia caia em cima da borda.
    const instante = Date.parse(`${dia}T12:00:00Z`)
    const fracao = Math.min(Math.max((instante - inicio) / duracao, 0), 1)
    return margem + fracao * larguraUtil
  }
  const yDe = (valor: number) =>
    margem + (1 - (valor - minimo) / (maximo - minimo)) * alturaUtil

  return {
    pontos: serie.dias.map((dia) => ({
      x: xDe(dia.dia),
      y: yDe(dia.media),
      dia: dia.dia,
      media: dia.media,
      fora: dia.foraDoLimite > 0,
    })),
    yLimiteMinimo:
      serie.limiteMinimo === undefined ? undefined : yDe(serie.limiteMinimo),
    yLimiteMaximo:
      serie.limiteMaximo === undefined ? undefined : yDe(serie.limiteMaximo),
    escala: { minimo, maximo },
  }
}
