import type { ReactNode } from 'react'
import { useId } from 'react'

interface Props {
  /**
   * O caminho JSON do campo no DTO (`localizacao.uf`, `leituras[0].valor`).
   *
   * É o mesmo nome que o backend usa em `detalhes` quando reprova a validação,
   * e é isso que permite ligar a mensagem ao input sem tabela de tradução.
   */
  name: string
  rotulo: string
  obrigatorio?: boolean
  /** Texto de apoio, como o limite da norma. */
  dica?: ReactNode
  /** Mensagem do servidor para este campo. */
  erro?: string | undefined
  children: (atributos: {
    id: string
    name: string
    'aria-invalid': true | undefined
    'aria-describedby': string | undefined
    required: boolean | undefined
  }) => ReactNode
}

/**
 * Rótulo, controle e mensagens de um campo de formulário.
 *
 * O controle vem por função para o chamador escolher `input`, `select` ou
 * `textarea` sem o componente precisar conhecer todos os casos, e sem
 * placeholder fazendo papel de rótulo.
 */
export function Campo({
  name,
  rotulo,
  obrigatorio,
  dica,
  erro,
  children,
}: Props) {
  const id = useId()
  const idDica = dica ? `${id}-dica` : undefined
  const idErro = erro ? `${id}-erro` : undefined

  const descricao = [idErro, idDica].filter(Boolean).join(' ') || undefined

  return (
    <div className={`campo ${erro ? 'campo--invalido' : ''}`}>
      <label className="campo__rotulo" htmlFor={id}>
        {rotulo}
        {obrigatorio && (
          <>
            <span aria-hidden="true"> *</span>
            <span className="so-leitor-de-tela"> (obrigatório)</span>
          </>
        )}
      </label>

      {children({
        id,
        name,
        'aria-invalid': erro ? true : undefined,
        'aria-describedby': descricao,
        required: obrigatorio || undefined,
      })}

      {dica && (
        <p className="campo__dica" id={idDica}>
          {dica}
        </p>
      )}
      {erro && (
        <p className="campo__erro" id={idErro}>
          {erro}
        </p>
      )}
    </div>
  )
}
