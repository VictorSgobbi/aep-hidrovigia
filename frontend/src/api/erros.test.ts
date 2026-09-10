import { describe, expect, it } from 'vitest'
import { ErroApi, extrairCampos } from './erros'

describe('ErroApi.deResposta', () => {
  it('entende o envelope do TratadorDeErros', () => {
    // Corpo real de POST /api/pontos com codigo repetido.
    const erro = ErroApi.deResposta(409, {
      instante: '2026-09-09T12:00:00.123Z',
      status: 409,
      erro: 'Regra de negocio violada',
      mensagem: 'Ja existe um ponto de monitoramento com o codigo PMA-001',
      detalhes: [],
    })

    expect(erro.origem).toBe('envelope')
    expect(erro.status).toBe(409)
    expect(erro.titulo).toBe('Regra de negocio violada')
    expect(erro.message).toBe(
      'Ja existe um ponto de monitoramento com o codigo PMA-001',
    )
    expect(erro.detalhes).toEqual([])
    expect(erro.eDoCliente).toBe(true)
  })

  it('entende o corpo padrao do Spring, que nao passa pelo TratadorDeErros', () => {
    // Acontece de verdade: <input type="datetime-local"> manda "2026-09-09T14:30",
    // sem offset, o Jackson recusa e o erro nem chega ao Bean Validation.
    const erro = ErroApi.deResposta(400, {
      timestamp: '2026-09-09T12:00:00.123+00:00',
      status: 400,
      error: 'Bad Request',
      message: 'JSON parse error: Cannot deserialize value of type Instant',
      path: '/api/analises',
    })

    expect(erro.origem).toBe('spring')
    expect(erro.titulo).toBe('Bad Request')
    // A mensagem tecnica em ingles vira detalhe; a tela mostra portugues.
    expect(erro.message).toBe(
      'A requisição está inválida. Revise os dados enviados.',
    )
    expect(erro.detalhes).toEqual([
      'JSON parse error: Cannot deserialize value of type Instant',
    ])
  })

  it('nao quebra com corpo que nao e JSON', () => {
    // O proxy do Vite devolve HTML quando a API esta fora.
    const erro = ErroApi.deResposta(502, '<html><body>Bad Gateway</body></html>')

    expect(erro.origem).toBe('desconhecida')
    expect(erro.status).toBe(502)
    expect(erro.message).toBe(
      'Erro inesperado no servidor. Tente novamente em instantes.',
    )
    expect(erro.eDoCliente).toBe(false)
  })

  it('discrimina pela chave e nao pelo status: 400 chega nas duas formas', () => {
    const validacao = ErroApi.deResposta(400, {
      status: 400,
      erro: 'Requisicao invalida',
      mensagem: 'Um ou mais campos nao passaram na validacao',
      detalhes: ['codigo: codigo do ponto e obrigatorio'],
    })
    const corpoIlegivel = ErroApi.deResposta(400, {
      status: 400,
      error: 'Bad Request',
      message: 'Required parameter is not present',
      path: '/api/analises',
    })

    expect(validacao.origem).toBe('envelope')
    expect(corpoIlegivel.origem).toBe('spring')
  })

  it('tem um texto em portugues para cada status que a API usa', () => {
    const mensagem = (status: number, corpo: unknown = null) =>
      ErroApi.deResposta(status, corpo).message

    expect(mensagem(404)).toBe('Registro não encontrado.')
    expect(mensagem(409)).toBe(
      'A operação foi recusada por uma regra de negócio.',
    )
    expect(mensagem(422)).toBe('Parâmetro fora do catálogo da norma.')
    expect(mensagem(405)).toBe('Requisição incompatível com a API.')
    expect(mensagem(415)).toBe('Requisição incompatível com a API.')
    expect(mensagem(418)).toBe('Não foi possível concluir a operação.')
    expect(mensagem(500)).toBe(
      'Erro inesperado no servidor. Tente novamente em instantes.',
    )
  })

  it('ignora corpo de texto em branco em vez de virar um detalhe vazio', () => {
    expect(ErroApi.deResposta(500, '   ').detalhes).toEqual([])
    expect(ErroApi.deResposta(500, null).detalhes).toEqual([])
  })

  it('trata envelope com detalhes ausente como lista vazia', () => {
    // detalhes e sempre enviado pelo backend, mas o tipo permite ausencia.
    const erro = ErroApi.deResposta(404, {
      erro: 'Recurso nao encontrado',
      mensagem: 'Analise nao encontrada: sumiu',
    })

    expect(erro.detalhes).toEqual([])
    expect(erro.camposInvalidos).toEqual({})
  })
})

describe('ErroApi.deRede', () => {
  it('usa status 0 e aponta para o backend fora do ar', () => {
    const erro = ErroApi.deRede(new TypeError('Failed to fetch'))

    expect(erro.origem).toBe('rede')
    expect(erro.status).toBe(0)
    expect(erro.message).toContain('localhost:8080')
    // Status 0 nao e 4xx: o retry do React Query depende dessa distincao.
    expect(erro.eDoCliente).toBe(false)
  })
})

describe('extrairCampos', () => {
  it('mapeia cada detalhe do 400 para o caminho do campo', () => {
    // As cinco formas que o backend produz de verdade.
    const mapa = extrairCampos([
      'codigo: codigo do ponto e obrigatorio',
      'localizacao.uf: uf deve ter duas letras',
      'populacaoAtendida: populacao atendida nao pode ser negativa',
      'leituras[0].valor: valor medido e obrigatorio',
      'leituras: informe ao menos um parametro medido',
    ])

    expect(mapa).toEqual({
      codigo: 'codigo do ponto e obrigatorio',
      'localizacao.uf': 'uf deve ter duas letras',
      populacaoAtendida: 'populacao atendida nao pode ser negativa',
      'leituras[0].valor': 'valor medido e obrigatorio',
      leituras: 'informe ao menos um parametro medido',
    })
  })

  it('corta no primeiro ": ", porque a mensagem pode conter dois-pontos', () => {
    const mapa = extrairCampos([
      'coletadoEm: data invalida: use o formato ISO-8601',
    ])

    expect(mapa).toEqual({
      coletadoEm: 'data invalida: use o formato ISO-8601',
    })
  })

  it('ignora linha sem separador e mantem a primeira mensagem de cada campo', () => {
    const mapa = extrairCampos([
      'mensagem solta sem campo',
      'uf: primeira',
      'uf: segunda',
      '',
      ': sem nome de campo',
    ])

    expect(mapa).toEqual({ uf: 'primeira' })
  })
})
