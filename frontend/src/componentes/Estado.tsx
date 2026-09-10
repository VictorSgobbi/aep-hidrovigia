import type { ReactNode } from 'react'
import { ErroApi } from '../api/erros'
import { IconeAtencao } from './Icones'

/** Espera. `aria-busy` faz o leitor de tela anunciar que algo está carregando. */
export function Carregando({ children = 'Carregando…' }: { children?: ReactNode }) {
  return (
    <p className="estado estado--carregando" aria-busy="true">
      {children}
    </p>
  )
}

/**
 * Lista vazia. Recebe uma ação, porque "nenhum registro" sem saída deixa a
 * tela num beco — e é assim que o sistema abre na primeira execução.
 */
export function Vazio({
  children,
  acao,
}: {
  children: ReactNode
  acao?: ReactNode
}) {
  return (
    <div className="estado estado--vazio">
      <p>{children}</p>
      {acao}
    </div>
  )
}

/**
 * Falha.
 *
 * Mostra o texto em português e guarda a mensagem técnica num `<details>`
 * fechado: o usuário não precisa ver o nome da exceção Java, e quem está
 * depurando não deveria precisar abrir o DevTools.
 *
 * `role="alert"` faz o leitor de tela anunciar sem esperar o foco.
 */
export function AvisoErro({
  erro,
  aoTentarNovamente,
  extras = [],
}: {
  erro: unknown
  aoTentarNovamente?: () => void
  /** Mensagens que nenhum campo do formulário exibiu. */
  extras?: readonly string[]
}) {
  const api = erro instanceof ErroApi ? erro : null
  const titulo = api?.titulo ?? 'Não foi possível concluir'
  const mensagem =
    api?.message ??
    (erro instanceof Error ? erro.message : 'Erro inesperado na aplicação.')

  const tecnicos = [...(api?.detalhes ?? [])].filter(
    (linha) => !extras.includes(linha),
  )

  return (
    <div className="aviso-erro" role="alert">
      <p className="aviso-erro__titulo">
        <IconeAtencao />
        {titulo}
      </p>

      <p className="aviso-erro__mensagem">{mensagem}</p>

      {extras.length > 0 && (
        <ul className="aviso-erro__lista">
          {extras.map((linha) => (
            <li key={linha}>{linha}</li>
          ))}
        </ul>
      )}

      {tecnicos.length > 0 && (
        <details className="aviso-erro__tecnico">
          <summary>Detalhes técnicos</summary>
          <ul>
            {tecnicos.map((linha) => (
              <li key={linha}>{linha}</li>
            ))}
          </ul>
        </details>
      )}

      {aoTentarNovamente && (
        <button type="button" className="botao" onClick={aoTentarNovamente}>
          Tentar novamente
        </button>
      )}
    </div>
  )
}
