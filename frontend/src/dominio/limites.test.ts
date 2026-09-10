import { describe, expect, it } from 'vitest'
import type { ParametroCatalogado } from '../api/tipos'
import { preverConformidade } from './limites'

/*
  Os tres itens abaixo sao copias fieis do que GET /api/painel/parametros
  devolve — conferidos contra a API rodando. Note que limiteMinimo nao aparece
  em ECOLI nem em TURB: o backend roda com default-property-inclusion non_null,
  entao o campo e removido do JSON em vez de vir como null.
*/

const ECOLI: ParametroCatalogado = {
  codigo: 'ECOLI',
  nome: 'Escherichia coli',
  unidade: 'UFC/100mL',
  limite: 'ausencia (UFC/100mL)',
  limiteMaximo: 0,
  risco: 'MICROBIOLOGICO',
  referenciaLegal: 'Portaria GM/MS n. 888/2021',
}

const CRL: ParametroCatalogado = {
  codigo: 'CRL',
  nome: 'Cloro residual livre',
  unidade: 'mg/L',
  limite: 'entre 0.2 e 2.0 mg/L',
  limiteMinimo: 0.2,
  limiteMaximo: 2,
  risco: 'DESINFECCAO',
  referenciaLegal: 'Portaria GM/MS n. 888/2021',
}

const TURB: ParametroCatalogado = {
  codigo: 'TURB',
  nome: 'Turbidez',
  unidade: 'uT',
  limite: 'ate 5.0 uT',
  limiteMaximo: 5,
  risco: 'FISICO_QUIMICO',
  referenciaLegal: 'Portaria GM/MS n. 888/2021',
}

describe('preverConformidade', () => {
  it('ausencia: so zero passa, e qualquer deteccao reprova', () => {
    expect(preverConformidade(ECOLI, 0)).toBe('conforme')
    // 14 e o valor da demonstracao, o que abre a ocorrencia critica.
    expect(preverConformidade(ECOLI, 14)).toBe('fora')
    expect(preverConformidade(ECOLI, 0.5)).toBe('fora')
  })

  it('faixa: reprova dos dois lados, e aceita os extremos', () => {
    expect(preverConformidade(CRL, 0.2)).toBe('conforme')
    expect(preverConformidade(CRL, 2)).toBe('conforme')
    expect(preverConformidade(CRL, 0.8)).toBe('conforme')
    expect(preverConformidade(CRL, 0.05)).toBe('fora')
    expect(preverConformidade(CRL, 5)).toBe('fora')
  })

  it('maximo: nao tem piso, e o teto entra como conforme', () => {
    expect(preverConformidade(TURB, 0)).toBe('conforme')
    expect(preverConformidade(TURB, 5)).toBe('conforme')
    expect(preverConformidade(TURB, 5.01)).toBe('fora')
  })

  it('nao arrisca palpite sem valor utilizavel', () => {
    expect(preverConformidade(CRL, null)).toBe('indefinida')
    expect(preverConformidade(CRL, Number.NaN)).toBe('indefinida')
    expect(preverConformidade(CRL, -1)).toBe('indefinida')
  })

  it('o limiteMaximo 0 da ausencia nunca deve virar o atributo max do input', () => {
    // Este teste existe como aviso: com <input max={0}> seria impossivel
    // digitar 14 UFC/100mL, justamente o valor que produz a ocorrencia
    // critica da demonstracao. A comparacao numerica funciona; o atributo
    // HTML e que nao pode sair daqui.
    expect(ECOLI.limiteMaximo).toBe(0)
    expect(ECOLI.limiteMinimo).toBeUndefined()
    expect(preverConformidade(ECOLI, 14)).toBe('fora')
  })
})
