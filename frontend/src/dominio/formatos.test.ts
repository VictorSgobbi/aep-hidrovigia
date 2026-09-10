import { describe, expect, it } from 'vitest'
import {
  formatarData,
  formatarDataHora,
  formatarMedida,
  formatarNumero,
  formatarPercentual,
  paraDateLocal,
  paraDatetimeLocal,
  paraDecimal,
  paraInstante,
} from './formatos'

// O fuso destes testes esta fixado em America/Sao_Paulo (UTC-3) pelo
// test.env.TZ do vite.config.ts. Sem isso, as conversoes abaixo passariam no
// notebook e falhariam no runner da CI, que roda em UTC.

describe('paraInstante', () => {
  it('acrescenta o offset que o input datetime-local nao manda', () => {
    // Sem esta conversao o Jackson recusa o corpo e o erro sai no formato
    // padrao do Spring, sem detalhes por campo.
    expect(paraInstante('2026-09-09T14:30')).toBe('2026-09-09T17:30:00.000Z')
  })

  it('devolve null para texto vazio ou data invalida', () => {
    expect(paraInstante('')).toBeNull()
    expect(paraInstante('   ')).toBeNull()
    expect(paraInstante('09/09/2026')).toBeNull()
    expect(paraInstante('2026-13-45T99:99')).toBeNull()
  })

  it('faz a volta completa sem perder a hora local', () => {
    const local = '2026-03-01T08:05'
    const instante = paraInstante(local)

    expect(instante).not.toBeNull()
    expect(paraDatetimeLocal(instante as string)).toBe(local)
  })
})

describe('paraDecimal', () => {
  it('aceita virgula, que e o que o teclado brasileiro escreve', () => {
    // Number("0,8") e NaN, e JSON.stringify(NaN) e null: daria um 400 sem
    // relacao aparente com o que foi digitado.
    expect(paraDecimal('0,8')).toBe(0.8)
    expect(paraDecimal('14')).toBe(14)
    expect(paraDecimal('0')).toBe(0)
    expect(paraDecimal(' 2.5 ')).toBe(2.5)
  })

  it('recusa o que nao e um decimal nao negativo', () => {
    expect(paraDecimal('')).toBeNull()
    expect(paraDecimal('1.2.3')).toBeNull()
    expect(paraDecimal('abc')).toBeNull()
    expect(paraDecimal('1e5')).toBeNull()
    // A API responde 409 para valor negativo: melhor barrar antes de enviar.
    expect(paraDecimal('-1')).toBeNull()
  })
})

describe('formatacao para exibicao', () => {
  it('mostra percentual com uma casa, venha arredondado ou cru', () => {
    // 40.0 vem do painel (arredondado pelo servidor);
    // 66.66666666666667 vem da analise (double cru).
    expect(formatarPercentual(40)).toBe('40,0%')
    expect(formatarPercentual(66.66666666666667)).toBe('66,7%')
    expect(formatarPercentual(0)).toBe('0,0%')
  })

  it('omite a unidade quando ela e vazia, como no pH', () => {
    expect(formatarMedida(7.2, '')).toBe('7,2')
    expect(formatarMedida(0.05, 'mg/L')).toBe('0,05 mg/L')
    expect(formatarMedida(14, 'UFC/100mL')).toBe('14 UFC/100mL')
  })

  it('usa o formato brasileiro de data e de numero', () => {
    // Locale errado mostraria 1/15/2026, que e o tipo de detalhe que passa
    // desapercebido em revisao e aparece na tela do video.
    expect(formatarData('2026-01-15T09:00:00Z')).toBe('15/01/2026')
    expect(formatarDataHora('2026-01-15T09:00:00Z')).toBe('15/01/2026, 06:00')
    expect(formatarNumero(2400)).toBe('2.400')
  })

  it('converte um instante para o formato do input date', () => {
    expect(paraDateLocal('2026-01-15T09:00:00Z')).toBe('2026-01-15')
  })
})
