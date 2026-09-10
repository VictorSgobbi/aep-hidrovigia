import { QueryClient } from '@tanstack/react-query'
import { ErroApi } from './erros'

/**
 * O padrão do React Query são 3 retentativas em qualquer erro.
 *
 * Aqui isso seria errado: um 409 ("ponto desativado", "código já existe") ou um
 * 422 ("parâmetro fora do catálogo") é consequência do que foi enviado, e
 * reenviar quatro vezes só atrasa a mensagem que o usuário precisa ler. Só
 * falha de rede — quando a requisição não chegou a sair — merece nova tentativa.
 */
export function criarQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15_000,
        retry: (tentativas, erro) =>
          erro instanceof ErroApi && erro.status === 0 && tentativas < 2,
      },
      mutations: {
        retry: false,
      },
    },
  })
}
