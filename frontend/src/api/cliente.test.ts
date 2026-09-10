import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { query, requisitar } from './cliente'
import { ErroApi } from './erros'
import type { PontoResponse } from './tipos'

/**
 * Com MSW em vez de um mock de `fetch`: assim o caminho real de `Response`,
 * `.text()`, `.json()` e cabeçalhos executa de verdade. Mockar `fetch` faria o
 * teste checar a própria ideia do mock sobre o que é uma resposta HTTP — e é
 * justamente aí que moram os erros.
 */
const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }))
afterEach(() => servidor.resetHandlers())
afterAll(() => servidor.close())

const PONTO: PontoResponse = {
  id: 'ponto-1',
  codigo: 'PMA-001',
  nome: 'Poco da Escola Rural Sao Jose',
  tipoFonte: 'POCO_ARTESIANO',
  descricaoFonte: 'Poco artesiano',
  populacaoAtendida: 320,
  localizacao: { municipio: 'Maringa', uf: 'PR' },
  responsavel: { nome: 'Ana Souza', contato: 'ana.souza@exemplo.gov.br' },
  ativo: true,
  criadoEm: '2026-01-15T09:00:00Z',
}

describe('requisitar', () => {
  it('devolve o corpo tipado num 201 e envia o JSON no formato certo', async () => {
    let corpoRecebido: unknown
    let contentType: string | null = null

    servidor.use(
      http.post('/api/pontos', async ({ request }) => {
        corpoRecebido = await request.json()
        contentType = request.headers.get('content-type')
        return HttpResponse.json(PONTO, {
          status: 201,
          headers: { Location: '/api/pontos/ponto-1' },
        })
      }),
    )

    const criado = await requisitar<PontoResponse>('/pontos', {
      metodo: 'POST',
      corpo: { codigo: 'PMA-001' },
    })

    expect(criado.codigo).toBe('PMA-001')
    expect(criado.ativo).toBe(true)
    expect(corpoRecebido).toEqual({ codigo: 'PMA-001' })
    expect(contentType).toContain('application/json')
  })

  it('transforma um 409 em ErroApi em vez de devolver a Response', async () => {
    servidor.use(
      http.post('/api/pontos', () =>
        HttpResponse.json(
          {
            instante: '2026-09-09T12:00:00Z',
            status: 409,
            erro: 'Regra de negocio violada',
            mensagem: 'Ja existe um ponto de monitoramento com o codigo PMA-001',
            detalhes: [],
          },
          { status: 409 },
        ),
      ),
    )

    const falha = await requisitar('/pontos', {
      metodo: 'POST',
      corpo: { codigo: 'PMA-001' },
    }).catch((erro: unknown) => erro)

    expect(falha).toBeInstanceOf(ErroApi)
    expect((falha as ErroApi).status).toBe(409)
    expect((falha as ErroApi).titulo).toBe('Regra de negocio violada')
  })

  it('nao manda Content-Type quando a requisicao nao tem corpo', async () => {
    // POST /pontos/{id}/reativacao nao tem corpo: mandar o cabecalho faria o
    // Spring tentar desserializar nada e responder 400.
    let contentType: string | null = 'ainda-nao-lido'

    servidor.use(
      http.post('/api/pontos/ponto-1/reativacao', ({ request }) => {
        contentType = request.headers.get('content-type')
        return HttpResponse.json(PONTO)
      }),
    )

    await requisitar<PontoResponse>('/pontos/ponto-1/reativacao', {
      metodo: 'POST',
    })

    expect(contentType).toBeNull()
  })

  it('propaga cancelamento cru, sem virar erro de rede', async () => {
    // O React Query cancela requisicoes; se isso virasse ErroApi, a tela
    // mostraria "sem conexao" a cada troca de filtro.
    servidor.use(
      http.get('/api/pontos', async () => {
        await new Promise((resolve) => setTimeout(resolve, 50))
        return HttpResponse.json([])
      }),
    )

    const controle = new AbortController()
    const promessa = requisitar('/pontos', { sinal: controle.signal })
    controle.abort()

    const falha = await promessa.catch((erro: unknown) => erro)

    expect(falha).not.toBeInstanceOf(ErroApi)
    expect((falha as Error).name).toBe('AbortError')
  })

  it('vira ErroApi de rede quando a requisicao nao sai', async () => {
    servidor.use(http.get('/api/pontos', () => HttpResponse.error()))

    const falha = await requisitar('/pontos').catch((erro: unknown) => erro)

    expect(falha).toBeInstanceOf(ErroApi)
    expect((falha as ErroApi).status).toBe(0)
  })

  it('devolve null em corpo vazio e o texto cru quando nao e JSON', async () => {
    servidor.use(
      http.get('/api/vazio', () => new HttpResponse(null, { status: 200 })),
      http.get('/api/texto', () =>
        HttpResponse.text('relatorio,em,csv', { status: 200 }),
      ),
    )

    await expect(requisitar('/vazio')).resolves.toBeNull()
    await expect(requisitar('/texto')).resolves.toBe('relatorio,em,csv')
  })

  it('nao estoura SyntaxError quando o corpo se anuncia JSON e nao e', async () => {
    servidor.use(
      http.get('/api/pontos', () =>
        new HttpResponse('<html>erro do proxy</html>', {
          status: 502,
          headers: { 'content-type': 'application/json' },
        }),
      ),
    )

    const falha = await requisitar('/pontos').catch((erro: unknown) => erro)

    expect(falha).toBeInstanceOf(ErroApi)
    expect((falha as ErroApi).origem).toBe('desconhecida')
  })
})

describe('query', () => {
  it('omite undefined e string vazia', () => {
    // `?municipio=` nao e branco para o backend e mudaria o filtro aplicado.
    expect(query({ municipio: '', apenasAtivos: true })).toBe(
      '?apenasAtivos=true',
    )
    expect(query({ status: undefined })).toBe('')
    expect(query({})).toBe('')
  })

  it('escapa o valor', () => {
    expect(query({ municipio: 'Sao Jose do Rio Preto' })).toBe(
      '?municipio=Sao+Jose+do+Rio+Preto',
    )
  })
})
