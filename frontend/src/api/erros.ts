/**
 * Normalização de falhas da API.
 *
 * O backend produz **duas** formas de erro, e existem duas outras fontes de
 * falha. Todas as quatro viram um `ErroApi`, para nenhuma tela precisar saber
 * disso:
 *
 * | Origem                | Forma                                              | Quando                                                          |
 * |-----------------------|----------------------------------------------------|-----------------------------------------------------------------|
 * | `TratadorDeErros`     | `{instante, status, erro, mensagem, detalhes[]}`    | 404, 409, 422 e 400 de validação                                 |
 * | `/error` do Spring    | `{timestamp, status, error, message, path}`         | JSON malformado, data sem offset, `?status=FOO`, 405, 415, 500   |
 * | corpo não-JSON        | HTML ou vazio                                       | proxy do Vite quando a API está fora                            |
 * | `fetch` rejeitando    | —                                                   | backend não subiu                                               |
 *
 * A discriminação é **pela chave, não pelo status**: um 400 chega nas duas
 * primeiras formas, dependendo de a validação ter chegado ao Bean Validation
 * ou de o Jackson ter recusado o corpo antes disso.
 */

export type OrigemErro = 'envelope' | 'spring' | 'rede' | 'desconhecida'

export class ErroApi extends Error {
  /** Status HTTP, ou 0 quando a requisição não chegou a sair. */
  readonly status: number
  /** Categoria curta, para o título do aviso. */
  readonly titulo: string
  /** Sempre um array: o envelope manda `[]` quando não há detalhe. */
  readonly detalhes: string[]
  /** Caminho do campo -> mensagem, extraído de `detalhes`. */
  readonly camposInvalidos: Record<string, string>
  readonly origem: OrigemErro
  /** Corpo original, para o bloco de detalhes técnicos. */
  readonly bruto: unknown

  private constructor(dados: {
    status: number
    titulo: string
    mensagem: string
    detalhes: string[]
    origem: OrigemErro
    bruto: unknown
  }) {
    super(dados.mensagem)
    this.name = 'ErroApi'
    this.status = dados.status
    this.titulo = dados.titulo
    this.detalhes = dados.detalhes
    this.camposInvalidos = extrairCampos(dados.detalhes)
    this.origem = dados.origem
    this.bruto = dados.bruto
  }

  static deRede(causa: unknown): ErroApi {
    return new ErroApi({
      status: 0,
      titulo: 'Sem conexão com a API',
      mensagem:
        'Não foi possível falar com o servidor. Ele está rodando em localhost:8080?',
      detalhes: [],
      origem: 'rede',
      bruto: causa,
    })
  }

  static deResposta(status: number, corpo: unknown): ErroApi {
    if (ehEnvelope(corpo)) {
      return new ErroApi({
        status,
        titulo: corpo.erro,
        mensagem: corpo.mensagem,
        detalhes: Array.isArray(corpo.detalhes) ? corpo.detalhes : [],
        origem: 'envelope',
        bruto: corpo,
      })
    }

    if (ehSpring(corpo)) {
      return new ErroApi({
        status,
        titulo: corpo.error,
        // A mensagem do Spring é técnica e em inglês (nome de exceção Java).
        // A tela mostra o texto em português por status e guarda a original
        // no bloco de detalhes.
        mensagem: mensagemPorStatus(status),
        detalhes: corpo.message ? [corpo.message] : [],
        origem: 'spring',
        bruto: corpo,
      })
    }

    // Corpo inesperado: HTML de erro do proxy, texto puro ou vazio.
    return new ErroApi({
      status,
      titulo: `Erro HTTP ${status}`,
      mensagem: mensagemPorStatus(status),
      detalhes: typeof corpo === 'string' && corpo.trim() !== '' ? [corpo] : [],
      origem: 'desconhecida',
      bruto: corpo,
    })
  }

  /** 4xx é consequência do que foi enviado: reenviar não ajuda. */
  get eDoCliente(): boolean {
    return this.status >= 400 && this.status < 500
  }
}

interface Envelope {
  erro: string
  mensagem: string
  detalhes?: string[]
}

interface Spring {
  error: string
  path: string
  message?: string
}

function ehObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null
}

function ehEnvelope(corpo: unknown): corpo is Envelope {
  return (
    ehObjeto(corpo) &&
    typeof corpo.erro === 'string' &&
    typeof corpo.mensagem === 'string'
  )
}

function ehSpring(corpo: unknown): corpo is Spring {
  return (
    ehObjeto(corpo) &&
    typeof corpo.error === 'string' &&
    typeof corpo.path === 'string'
  )
}

function mensagemPorStatus(status: number): string {
  switch (status) {
    case 400:
      return 'A requisição está inválida. Revise os dados enviados.'
    case 404:
      return 'Registro não encontrado.'
    case 405:
    case 415:
      return 'Requisição incompatível com a API.'
    case 409:
      return 'A operação foi recusada por uma regra de negócio.'
    case 422:
      return 'Parâmetro fora do catálogo da norma.'
    default:
      return status >= 500
        ? 'Erro inesperado no servidor. Tente novamente em instantes.'
        : 'Não foi possível concluir a operação.'
  }
}

/**
 * Transforma as linhas de `detalhes` em um mapa de caminho do campo -> mensagem.
 *
 * O backend formata cada entrada como `campo + ": " + mensagem`:
 *
 *   "codigo: codigo do ponto e obrigatorio"
 *   "localizacao.uf: uf deve ter duas letras"
 *   "leituras[0].valor: valor medido e obrigatorio"
 *
 * Três cuidados:
 *
 * - O corte é no **primeiro** `": "`. Mensagem de Bean Validation pode conter
 *   dois-pontos, e cortar no último quebraria o caminho do campo.
 * - Se o mesmo campo aparecer duas vezes, vale a primeira mensagem.
 * - Uma linha sem `": "` é ignorada aqui, mas continua visível na lista
 *   `detalhes`.
 *
 * Atenção: entrar no mapa **não** garante que alguém vá exibir. O backend
 * também reporta erro do nível da lista, como
 * `"leituras: informe ao menos um parametro medido"`, cujo campo é `leituras`
 * — e nenhum input tem esse nome, porque a lista é um conjunto de linhas.
 * Por isso `useErrosDeCampo` recebe quais campos a tela realmente renderiza e
 * separa o que sobrou; sem isso, um 400 renderizaria como absolutamente nada.
 */
export function extrairCampos(detalhes: string[]): Record<string, string> {
  const mapa: Record<string, string> = {}

  for (const linha of detalhes) {
    const corte = linha.indexOf(': ')
    if (corte <= 0) continue

    const campo = linha.slice(0, corte).trim()
    const mensagem = linha.slice(corte + 2).trim()

    if (campo !== '' && mensagem !== '' && !(campo in mapa)) {
      mapa[campo] = mensagem
    }
  }

  return mapa
}
