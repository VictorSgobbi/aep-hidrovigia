import type { ReactNode } from 'react'

/**
 * Um número grande do painel.
 *
 * `apoio` existe para o número nunca ficar sozinho: "40,0%" diz pouco, e
 * "2 de 5 análises" é inequívoco. Vale especialmente para o percentual da
 * análise, que o backend manda como `double` sem arredondar.
 */
export function Indicador({
  rotulo,
  valor,
  apoio,
  atencao = false,
  children,
}: {
  rotulo: string
  valor: ReactNode
  apoio?: ReactNode
  /** Destaca em vermelho. Use só quando exige ação, não para "é ruim". */
  atencao?: boolean
  children?: ReactNode
}) {
  return (
    <li className={`cartao indicador ${atencao ? 'indicador--atencao' : ''}`}>
      <p className="indicador__rotulo">{rotulo}</p>
      <p className="indicador__numero">{valor}</p>
      {apoio && <p className="indicador__apoio">{apoio}</p>}
      {children}
    </li>
  )
}

/** Barra de proporção, para acompanhar um percentual. */
export function Barra({
  percentual,
  descricao,
}: {
  percentual: number
  descricao: string
}) {
  return (
    <div
      className="barra"
      role="meter"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(percentual)}
      aria-valuetext={descricao}
    >
      <div
        className="barra__preenchido"
        style={{ width: `${Math.min(100, Math.max(0, percentual))}%` }}
      />
    </div>
  )
}
