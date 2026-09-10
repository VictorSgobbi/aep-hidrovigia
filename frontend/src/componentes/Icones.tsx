/**
 * Ícones em SVG inline.
 *
 * Cada gravidade tem uma **forma** diferente, não só uma cor: é o segundo
 * canal de informação, que sobrevive a impressão em preto e branco e a
 * daltonismo. O terceiro é o texto do selo.
 */

interface Props {
  /** Ícone é decorativo por padrão: o texto ao lado já diz o que ele diz. */
  titulo?: string
}

function svg(caminho: React.ReactNode, { titulo }: Props) {
  return (
    <svg
      className="icone"
      viewBox="0 0 16 16"
      width="14"
      height="14"
      aria-hidden={titulo ? undefined : true}
      role={titulo ? 'img' : undefined}
      focusable="false"
    >
      {titulo && <title>{titulo}</title>}
      {caminho}
    </svg>
  )
}

/** Triângulo: gravidade crítica. */
export const IconeTriangulo = (props: Props) =>
  svg(<path d="M8 1.5 15 14H1L8 1.5Z" fill="currentColor" />, props)

/** Losango: gravidade alta. */
export const IconeLosango = (props: Props) =>
  svg(<path d="M8 1 15 8 8 15 1 8 8 1Z" fill="currentColor" />, props)

/** Círculo: gravidade média. */
export const IconeCirculo = (props: Props) =>
  svg(<circle cx="8" cy="8" r="6.5" fill="currentColor" />, props)

/** Verificado: conforme, resolvida. */
export const IconeCerto = (props: Props) =>
  svg(
    <path
      d="M2 8.5 6 12.5 14 4"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    />,
    props,
  )

/** Relógio: prazo, em tratativa. */
export const IconeRelogio = (props: Props) =>
  svg(
    <>
      <circle
        cx="8"
        cy="8"
        r="6.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path
        d="M8 4.5V8.5l3 1.8"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </>,
    props,
  )

/** Exclamação: erro, prazo vencido. */
export const IconeAtencao = (props: Props) =>
  svg(
    <>
      <circle cx="8" cy="8" r="6.5" fill="currentColor" />
      <path
        d="M8 4.5v4.2M8 11.2v.6"
        stroke="var(--cor-superficie)"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </>,
    props,
  )
