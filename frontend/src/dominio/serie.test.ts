import { describe, expect, it } from 'vitest'
import type { SerieHistorica } from '../api/tipos'
import { escalarSerie } from './serie'

const MOLDURA = { largura: 120, altura: 120, margem: 10 }

function serie(parcial: Partial<SerieHistorica>): SerieHistorica {
  return {
    codigoPonto: 'PMA-001',
    codigoParametro: 'CRL',
    nomeParametro: 'Cloro residual livre',
    unidade: 'mg/L',
    limiteMinimo: 0.2,
    limiteMaximo: 2,
    inicio: '2026-01-01T00:00:00Z',
    fim: '2026-01-11T00:00:00Z',
    dias: [],
    ...parcial,
  }
}

const dia = (data: string, media: number, foraDoLimite = 0) => ({
  dia: data,
  minimo: media,
  media,
  maximo: media,
  leituras: 1,
  foraDoLimite,
})

describe('escalarSerie', () => {
  it('posiciona cada dia pelo tempo decorrido, e nao pela ordem na lista', () => {
    const { pontos } = escalarSerie(
      serie({ dias: [dia('2026-01-01', 1), dia('2026-01-02', 1), dia('2026-01-10', 1)] }),
      MOLDURA,
    )

    const [primeiro, segundo, terceiro] = pontos.map((ponto) => ponto.x)
    // 100 de largura util em 10 dias: o intervalo de 8 dias e oito vezes maior.
    expect(segundo! - primeiro!).toBeCloseTo(10)
    expect(terceiro! - segundo!).toBeCloseTo(80)
  })

  it('inclui os limites da norma na escala e desenha a faixa', () => {
    const grafico = escalarSerie(serie({ dias: [dia('2026-01-05', 1)] }), MOLDURA)

    // Valor maior fica mais acima: y menor.
    expect(grafico.yLimiteMaximo!).toBeLessThan(grafico.pontos[0]!.y)
    expect(grafico.yLimiteMinimo!).toBeGreaterThan(grafico.pontos[0]!.y)
    expect(grafico.escala.maximo).toBeGreaterThan(2)
  })

  it('sem limite minimo, nao inventa a linha de baixo', () => {
    const grafico = escalarSerie(
      serie({ limiteMinimo: undefined, limiteMaximo: 5, dias: [dia('2026-01-05', 9, 1)] }),
      MOLDURA,
    )

    expect(grafico.yLimiteMinimo).toBeUndefined()
    expect(grafico.pontos[0]!.fora).toBe(true)
    expect(grafico.pontos[0]!.y).toBeLessThan(grafico.yLimiteMaximo!)
  })

  it('nunca desce a escala abaixo de zero, que nao existe em concentracao', () => {
    const grafico = escalarSerie(
      serie({ limiteMinimo: undefined, limiteMaximo: 0, dias: [dia('2026-01-05', 14, 1)] }),
      MOLDURA,
    )

    expect(grafico.escala.minimo).toBe(0)
  })

  it('valores todos iguais e sem limite ainda produzem uma escala valida', () => {
    const grafico = escalarSerie(
      serie({ limiteMinimo: undefined, limiteMaximo: undefined, dias: [dia('2026-01-05', 7)] }),
      MOLDURA,
    )

    expect(grafico.escala.maximo).toBeGreaterThan(grafico.escala.minimo)
    expect(Number.isFinite(grafico.pontos[0]!.y)).toBe(true)
  })

  it('serie vazia e periodo de um instante nao quebram o desenho', () => {
    const grafico = escalarSerie(
      serie({
        limiteMinimo: undefined,
        limiteMaximo: undefined,
        inicio: '2026-01-05T00:00:00Z',
        fim: '2026-01-05T00:00:00Z',
      }),
      MOLDURA,
    )

    expect(grafico.pontos).toEqual([])
    expect(grafico.escala).toEqual({ minimo: 0, maximo: 1.1 })
  })
})
