import { query, requisitar } from './cliente'
import type {
  AnaliseRequest,
  AnaliseResponse,
  IndicadoresConformidade,
  Instante,
  OcorrenciaResponse,
  ParametroCatalogado,
  PontoRequest,
  PontoResponse,
  StatusOcorrencia,
  TratativaRequest,
} from './tipos'

/* Pontos de monitoramento --------------------------------------------------- */

/**
 * @param filtros quando `municipio` vem preenchido, o backend **ignora**
 *                `apenasAtivos` e devolve ativos e inativos do município.
 */
export const listarPontos = (filtros: {
  apenasAtivos?: boolean
  municipio?: string
}) => requisitar<PontoResponse[]>(`/pontos${query(filtros)}`)

export const buscarPonto = (id: string) =>
  requisitar<PontoResponse>(`/pontos/${encodeURIComponent(id)}`)

export const buscarPontoPorCodigo = (codigo: string) =>
  requisitar<PontoResponse>(`/pontos/codigo/${encodeURIComponent(codigo)}`)

export const cadastrarPonto = (corpo: PontoRequest) =>
  requisitar<PontoResponse>('/pontos', { metodo: 'POST', corpo })

/** O `codigo` do corpo é exigido pela validação e ignorado pelo serviço. */
export const atualizarPonto = (id: string, corpo: PontoRequest) =>
  requisitar<PontoResponse>(`/pontos/${encodeURIComponent(id)}`, {
    metodo: 'PUT',
    corpo,
  })

/** Responde 200 com o ponto já desativado no corpo — não é 204. */
export const desativarPonto = (id: string) =>
  requisitar<PontoResponse>(`/pontos/${encodeURIComponent(id)}`, {
    metodo: 'DELETE',
  })

/** Sem corpo de requisição, e idempotente. */
export const reativarPonto = (id: string) =>
  requisitar<PontoResponse>(`/pontos/${encodeURIComponent(id)}/reativacao`, {
    metodo: 'POST',
  })

/* Análises ------------------------------------------------------------------ */

/**
 * Registra a coleta. Se algum parâmetro reprovar, o backend abre uma ocorrência
 * como efeito colateral — e ela **não** vem nesta resposta. Quem chama precisa
 * invalidar as consultas de ocorrências e do painel.
 */
export const registrarAnalise = (corpo: AnaliseRequest) =>
  requisitar<AnaliseResponse>('/analises', { metodo: 'POST', corpo })

export const buscarAnalise = (id: string) =>
  requisitar<AnaliseResponse>(`/analises/${encodeURIComponent(id)}`)

export const listarAnalisesDoPonto = (codigoPonto: string) =>
  requisitar<AnaliseResponse[]>(
    `/analises/ponto/${encodeURIComponent(codigoPonto)}`,
  )

/**
 * `inicio` e `fim` são obrigatórios no backend, sem valor padrão. Exigi-los na
 * assinatura impede em tempo de compilação a chamada que responderia 400 — e
 * com o corpo de erro do Spring, sem `detalhes`.
 */
export const listarAnalisesPorPeriodo = (inicio: Instante, fim: Instante) =>
  requisitar<AnaliseResponse[]>(`/analises${query({ inicio, fim })}`)

/* Ocorrências --------------------------------------------------------------- */

/**
 * @param status sem filtro, devolve todas as **não resolvidas** ordenadas pelo
 *               prazo mais próximo de vencer. Com filtro, as daquele status
 *               por data de abertura. A ordem vem do servidor: não reordenar.
 */
export const listarOcorrencias = (status?: StatusOcorrencia) =>
  requisitar<OcorrenciaResponse[]>(`/ocorrencias${query({ status })}`)

export const buscarOcorrencia = (id: string) =>
  requisitar<OcorrenciaResponse>(`/ocorrencias/${encodeURIComponent(id)}`)

/** Responde 200 (não 201) com a ocorrência inteira já atualizada. */
export const registrarTratativa = (id: string, corpo: TratativaRequest) =>
  requisitar<OcorrenciaResponse>(
    `/ocorrencias/${encodeURIComponent(id)}/tratativas`,
    { metodo: 'POST', corpo },
  )

/** Encerra em definitivo: RESOLVIDA é terminal no backend. */
export const resolverOcorrencia = (id: string, corpo: TratativaRequest) =>
  requisitar<OcorrenciaResponse>(
    `/ocorrencias/${encodeURIComponent(id)}/resolucao`,
    { metodo: 'POST', corpo },
  )

/* Painel -------------------------------------------------------------------- */

export const indicadoresGerais = () =>
  requisitar<IndicadoresConformidade>('/painel/conformidade')

export const indicadoresDoPonto = (codigoPonto: string) =>
  requisitar<IndicadoresConformidade>(
    `/painel/conformidade/${encodeURIComponent(codigoPonto)}`,
  )

/**
 * Catálogo da norma, em ordem significativa (microbiológicos primeiro). É a
 * fonte que monta o formulário de coleta. Imutável em tempo de execução.
 */
export const catalogoParametros = () =>
  requisitar<ParametroCatalogado[]>('/painel/parametros')
