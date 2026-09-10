import type {
  Gravidade,
  RiscoSanitario,
  StatusOcorrencia,
  TipoFonte,
} from '../api/tipos'

/**
 * Rótulos em português para os enums da API.
 *
 * O backend serializa o *nome* do enum (`EM_TRATATIVA`), nunca a descrição —
 * e as descrições que ele tem internamente vêm sem acento. Então os rótulos
 * acentuados são responsabilidade daqui.
 *
 * Como os enums são union de string literal, `Record<...>` faz o compilador
 * cobrar o rótulo de qualquer valor novo que o backend passe a devolver.
 */

export const ROTULO_TIPO_FONTE: Record<TipoFonte, string> = {
  POCO_ARTESIANO: 'Poço artesiano',
  MANANCIAL_SUPERFICIAL: 'Manancial superficial',
  NASCENTE: 'Nascente',
  CISTERNA: 'Cisterna',
  REDE_PUBLICA: 'Rede pública',
  CAMINHAO_PIPA: 'Caminhão-pipa',
}

export const ROTULO_GRAVIDADE: Record<Gravidade, string> = {
  CRITICA: 'Crítica',
  ALTA: 'Alta',
  MEDIA: 'Média',
}

export const ROTULO_STATUS: Record<StatusOcorrencia, string> = {
  ABERTA: 'Aberta',
  EM_TRATATIVA: 'Em tratativa',
  RESOLVIDA: 'Resolvida',
}

export const ROTULO_RISCO: Record<RiscoSanitario, string> = {
  MICROBIOLOGICO: 'Microbiológico',
  DESINFECCAO: 'Desinfecção',
  FISICO_QUIMICO: 'Físico-químico',
  ORGANOLEPTICO: 'Organoléptico',
}

/**
 * Busca o rótulo e, se o valor não estiver no mapa, devolve o próprio valor.
 *
 * Existe para o caso de o backend ser atualizado sem o bundle do frontend: sem
 * isto a tela mostraria "undefined" no lugar da gravidade de uma ocorrência,
 * que é o tipo de coisa que só aparece na gravação do vídeo.
 */
export function rotular<T extends string>(
  mapa: Record<T, string>,
  valor: T | string,
): string {
  return (mapa as Record<string, string | undefined>)[valor] ?? valor
}
