import { useMemo } from 'react'
import { ErroApi } from '../api/erros'

export interface ErrosDeCampo {
  /** Mensagem do campo, pelo mesmo caminho JSON que foi enviado. */
  de: (caminho: string) => string | undefined
  /**
   * O que o backend reportou e nenhum campo da tela exibe. Precisa aparecer
   * em algum lugar visível, senão o 400 renderiza como nada.
   */
  gerais: string[]
  erro: ErroApi | null
}

/**
 * Distribui um erro de validação entre os campos do formulário.
 *
 * @param camposRenderizados os `name` dos inputs que a tela realmente mostra.
 *
 * Este segundo parâmetro é o ponto do hook. O backend também reporta erro do
 * nível da lista — `"leituras: informe ao menos um parametro medido"`, cujo
 * campo é `leituras` — e nenhum input tem esse nome, porque a lista é um
 * conjunto de linhas. Sem saber o que a tela exibe, essa mensagem entraria no
 * mapa e desapareceria. Aqui ela cai em `gerais`.
 */
export function useErrosDeCampo(
  erro: unknown,
  camposRenderizados: readonly string[],
): ErrosDeCampo {
  // A lista costuma ser um literal, com identidade nova a cada render. A chave
  // serializada e extraida aqui, e nao dentro do array de dependencias, para
  // o exhaustive-deps poder verificar estaticamente.
  const chaveDosCampos = camposRenderizados.join('|')

  return useMemo(() => {
    const api = erro instanceof ErroApi ? erro : null

    if (!api) {
      return { de: () => undefined, gerais: [], erro: null }
    }

    const campos = api.camposInvalidos
    const exibidos = new Set(chaveDosCampos === '' ? [] : chaveDosCampos.split('|'))

    // Sobra: detalhe cujo campo a tela não mostra, e linha que não tem a
    // forma "campo: mensagem".
    const gerais = api.detalhes.filter((linha) => {
      const corte = linha.indexOf(': ')
      if (corte <= 0) return true
      return !exibidos.has(linha.slice(0, corte).trim())
    })

    return {
      de: (caminho) => campos[caminho],
      gerais,
      erro: api,
    }
  }, [erro, chaveDosCampos])
}
