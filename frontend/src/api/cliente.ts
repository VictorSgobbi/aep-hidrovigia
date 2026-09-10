import { ErroApi } from './erros'

/**
 * O único `fetch` do projeto.
 *
 * `BASE` é relativo de propósito: em desenvolvimento o proxy do Vite encaminha
 * `/api` para :8080, e em produção o próprio Spring serve a SPA na mesma
 * origem. Nenhum código lê hostname, e não existe VITE_API_URL para errar.
 */
const BASE = '/api'

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE'

interface Opcoes {
  metodo?: Metodo
  corpo?: unknown
  sinal?: AbortSignal
}

export async function requisitar<T>(
  caminho: string,
  opcoes: Opcoes = {},
): Promise<T> {
  const { metodo = 'GET', corpo, sinal } = opcoes

  const cabecalhos: Record<string, string> = { Accept: 'application/json' }
  // POST /pontos/{id}/reativacao não tem corpo: mandar Content-Type sem body
  // faria o Spring tentar desserializar e responder 400.
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'

  let resposta: Response
  try {
    // As chaves opcionais sao omitidas em vez de recebidas como undefined:
    // exigencia do exactOptionalPropertyTypes, e de todo jeito e o que se quer
    // — uma requisicao sem corpo nao deve carregar a chave `body`.
    resposta = await fetch(BASE + caminho, {
      method: metodo,
      headers: cabecalhos,
      ...(corpo === undefined ? {} : { body: JSON.stringify(corpo) }),
      ...(sinal ? { signal: sinal } : {}),
    })
  } catch (causa) {
    // Cancelamento não é falha de rede: propaga cru para o React Query
    // reconhecer como abortado em vez de mostrar erro ao usuário.
    if (ehCancelamento(causa)) throw causa
    throw ErroApi.deRede(causa)
  }

  const conteudo = await lerCorpo(resposta)
  if (!resposta.ok) throw ErroApi.deResposta(resposta.status, conteudo)
  return conteudo as T
}

/**
 * Reconhece cancelamento pelo `name`, e não por `instanceof DOMException`.
 *
 * O objeto de erro do abort vem do realm de quem implementa o fetch (undici no
 * Node, jsdom nos testes), então `instanceof` contra o DOMException global dá
 * falso negativo — e o cancelamento seria tratado como falha de rede, fazendo
 * a tela mostrar "sem conexão" a cada troca de filtro.
 */
function ehCancelamento(causa: unknown): boolean {
  return (
    typeof causa === 'object' &&
    causa !== null &&
    'name' in causa &&
    (causa as { name?: unknown }).name === 'AbortError'
  )
}

/**
 * Nenhum endpoint desta API devolve 204 — o DELETE de ponto responde 200 com
 * o ponto desativado no corpo. O guarda existe para não explodir se mudar.
 */
async function lerCorpo(resposta: Response): Promise<unknown> {
  if (resposta.status === 204) return null

  const tipo = resposta.headers.get('content-type') ?? ''
  const texto = await resposta.text()
  if (texto === '') return null

  if (tipo.includes('application/json')) {
    try {
      return JSON.parse(texto)
    } catch {
      // JSON anunciado e inválido: devolve o texto para o normalizador tratar
      // como corpo inesperado, em vez de estourar um SyntaxError na tela.
      return texto
    }
  }

  return texto
}

/**
 * Monta a query string omitindo `undefined` e string vazia — evita mandar
 * `?municipio=`, que no backend não é branco e mudaria o filtro aplicado.
 */
export function query(
  params: Record<string, string | number | boolean | undefined>,
): string {
  const busca = new URLSearchParams()

  for (const [chave, valor] of Object.entries(params)) {
    if (valor === undefined || valor === '') continue
    busca.set(chave, String(valor))
  }

  const texto = busca.toString()
  return texto === '' ? '' : `?${texto}`
}
