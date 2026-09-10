/**
 * Espelho dos DTOs do backend.
 *
 * Duas regras que valem para o arquivo inteiro:
 *
 * 1. **Campo que o Java pode devolver nulo é opcional aqui.** A API roda com
 *    `spring.jackson.default-property-inclusion: non_null`, então um campo
 *    nulo é *removido* do JSON em vez de vir como `null`. Escrever
 *    `limiteMinimo: number | null` seria errado — `=== null` nunca daria certo.
 *
 * 2. **Enums são union de string literal**, não `enum` do TypeScript. Assim
 *    `Record<Gravidade, string>` em dominio/rotulos.ts faz o compilador cobrar
 *    o rótulo de qualquer valor novo que o backend passe a devolver.
 */

/** ISO-8601 UTC, vindo de um java.time.Instant. Alias para documentar. */
export type Instante = string

export type TipoFonte =
  | 'POCO_ARTESIANO'
  | 'MANANCIAL_SUPERFICIAL'
  | 'NASCENTE'
  | 'CISTERNA'
  | 'REDE_PUBLICA'
  | 'CAMINHAO_PIPA'

/** O prazo de resposta de cada nível vem no campo `prazoHoras` da ocorrência. */
export type Gravidade = 'CRITICA' | 'ALTA' | 'MEDIA'

/** RESOLVIDA é terminal: não existe caminho de volta no backend. */
export type StatusOcorrencia = 'ABERTA' | 'EM_TRATATIVA' | 'RESOLVIDA'

export type RiscoSanitario =
  | 'MICROBIOLOGICO'
  | 'DESINFECCAO'
  | 'FISICO_QUIMICO'
  | 'ORGANOLEPTICO'

/* Pontos de monitoramento --------------------------------------------------- */

export interface Localizacao {
  municipio: string
  /** Sempre duas letras: o backend valida com @Size(min=2, max=2). */
  uf: string
  latitude?: number | undefined
  longitude?: number | undefined
}

export interface Responsavel {
  nome: string
  /** Registro profissional (CREA, CRQ). Ausente no PMA-003 da carga demo. */
  registro?: string | undefined
  contato: string
}

export interface PontoRequest {
  /**
   * Obrigatório sempre, mas **ignorado no PUT**: o código é imutável. O campo
   * segue sendo enviado na edição só para satisfazer o @NotBlank do backend.
   */
  codigo: string
  nome: string
  tipoFonte: TipoFonte
  populacaoAtendida: number
  localizacao: Localizacao
  responsavel: Responsavel
}

export interface PontoResponse extends PontoRequest {
  id: string
  /** Rótulo do servidor, sem acento ("Poco artesiano"). Não exibir cru. */
  descricaoFonte: string
  ativo: boolean
  criadoEm: Instante
}

/* Análises ------------------------------------------------------------------ */

export interface LeituraRequest {
  codigo: string
  valor: number
}

export interface AnaliseRequest {
  codigoPonto: string
  coletor: string
  coletadoEm: Instante
  leituras: LeituraRequest[]
}

export interface ParametroAvaliado {
  codigo: string
  nome: string
  valor: number
  /** Pode ser string vazia — o pH não tem unidade. */
  unidade: string
  limiteMinimo?: number | undefined
  limiteMaximo?: number | undefined
  conforme: boolean
  /** Texto pronto do servidor, específico da violação. */
  mensagem: string
  risco: RiscoSanitario
  referenciaLegal: string
}

export interface AnaliseResponse {
  id: string
  pontoId: string
  pontoCodigo: string
  coletadoEm: Instante
  coletor: string
  conforme: boolean
  qtdParametros: number
  qtdNaoConformidades: number
  /** Não arredondado pelo servidor, ao contrário do percentual do painel. */
  percentualConformidade: number
  parametros: ParametroAvaliado[]
  registradoEm: Instante
}

/* Ocorrências --------------------------------------------------------------- */

export interface ParametroVioladoResponse {
  codigo: string
  nome: string
  valorMedido: number
  unidade: string
  /** Texto pronto: "ausencia (UFC/100mL)", "entre 0.2 e 2.0 mg/L". */
  limiteAplicado: string
  risco: RiscoSanitario
  mensagem: string
}

export interface TratativaResponse {
  acao: string
  por: string
  em: Instante
  statusResultante: StatusOcorrencia
}

export interface TratativaRequest {
  acao: string
  por: string
}

export interface OcorrenciaResponse {
  id: string
  analiseId: string
  pontoId: string
  pontoCodigo: string
  gravidade: Gravidade
  prazoHoras: number
  /** Qual política classificou: "risco-sanitario" ou "quantidade-de-desvios". */
  politicaClassificacao: string
  status: StatusOcorrencia
  abertaEm: Instante
  prazoLimite: Instante
  /**
   * Calculado pelo servidor no instante da requisição, então envelhece entre
   * dois fetches. dominio/prazos.ts recalcula localmente sem nunca desmarcar
   * o que o servidor já marcou como vencido.
   */
  vencida: boolean
  parametrosViolados: ParametroVioladoResponse[]
  tratativas: TratativaResponse[]
}

/* Painel -------------------------------------------------------------------- */

export interface IndicadoresConformidade {
  /** "geral" ou o código do ponto. */
  escopo: string
  totalAnalises: number
  analisesConformes: number
  analisesNaoConformes: number
  /** Arredondado em duas casas pelo servidor; 0.0 quando não há análises. */
  percentualConformidade: number
  ocorrenciasPendentes: number
  ocorrenciasVencidas: number
}

export interface ParametroCatalogado {
  codigo: string
  nome: string
  unidade: string
  /** Texto pronto do limite: "ate 5.0 uT", "ausencia (UFC/100mL)". */
  limite: string
  /**
   * ATENÇÃO: o catálogo não diz de que *forma* é o limite. Parâmetro de
   * ausência (ECOLI, CTOT) chega com `limiteMaximo: 0` e sem `limiteMinimo`.
   * Nunca derivar o atributo HTML `max` daqui: com `max={0}` seria impossível
   * digitar 14 UFC/100mL, que é justamente o valor que abre uma ocorrência
   * crítica. Para comparar, use dominio/limites.ts.
   */
  limiteMinimo?: number | undefined
  limiteMaximo?: number | undefined
  risco: RiscoSanitario
  referenciaLegal: string
}
