import type { ReactNode } from 'react'
import type { Gravidade, RiscoSanitario, StatusOcorrencia } from '../api/tipos'
import {
  ROTULO_GRAVIDADE,
  ROTULO_RISCO,
  ROTULO_STATUS,
  rotular,
} from '../dominio/rotulos'
import {
  IconeAtencao,
  IconeCerto,
  IconeCirculo,
  IconeLosango,
  IconeRelogio,
  IconeTriangulo,
} from './Icones'

type Tom = 'critica' | 'alta' | 'media' | 'conforme' | 'neutro'

interface SeloProps {
  tom: Tom
  icone?: ReactNode
  children: ReactNode
}

/**
 * Etiqueta com texto e, quando faz diferença, ícone.
 *
 * Cor é sempre o **terceiro** canal: quem não distingue as cores lê o texto e
 * vê a forma do ícone. Nenhuma informação do sistema depende só do tom.
 */
export function Selo({ tom, icone, children }: SeloProps) {
  return (
    <span className={`selo selo--${tom}`}>
      {icone}
      {children}
    </span>
  )
}

const ICONE_GRAVIDADE: Record<Gravidade, ReactNode> = {
  CRITICA: <IconeTriangulo />,
  ALTA: <IconeLosango />,
  MEDIA: <IconeCirculo />,
}

const TOM_GRAVIDADE: Record<Gravidade, Tom> = {
  CRITICA: 'critica',
  ALTA: 'alta',
  MEDIA: 'media',
}

/** Gravidade com o prazo de resposta ao lado, que é o que dá urgência. */
export function SeloGravidade({
  gravidade,
  prazoHoras,
}: {
  gravidade: Gravidade
  prazoHoras?: number
}) {
  const tom = TOM_GRAVIDADE[gravidade] ?? 'neutro'
  const icone = ICONE_GRAVIDADE[gravidade] ?? <IconeCirculo />

  return (
    <Selo tom={tom} icone={icone}>
      {rotular(ROTULO_GRAVIDADE, gravidade)}
      {prazoHoras !== undefined && (
        <span className="selo__extra">· {prazoHoras} h</span>
      )}
    </Selo>
  )
}

const ICONE_STATUS: Record<StatusOcorrencia, ReactNode> = {
  ABERTA: <IconeAtencao />,
  EM_TRATATIVA: <IconeRelogio />,
  RESOLVIDA: <IconeCerto />,
}

export function SeloStatus({ status }: { status: StatusOcorrencia }) {
  return (
    <Selo
      tom={status === 'RESOLVIDA' ? 'conforme' : 'neutro'}
      icone={ICONE_STATUS[status] ?? <IconeCirculo />}
    >
      {rotular(ROTULO_STATUS, status)}
    </Selo>
  )
}

const TOM_RISCO: Record<RiscoSanitario, Tom> = {
  MICROBIOLOGICO: 'critica',
  DESINFECCAO: 'alta',
  FISICO_QUIMICO: 'alta',
  ORGANOLEPTICO: 'media',
}

export function SeloRisco({ risco }: { risco: RiscoSanitario }) {
  return (
    <Selo tom={TOM_RISCO[risco] ?? 'neutro'}>
      {rotular(ROTULO_RISCO, risco)}
    </Selo>
  )
}

/** Veredito de uma análise ou de um parâmetro. */
export function SeloConformidade({ conforme }: { conforme: boolean }) {
  return (
    <Selo
      tom={conforme ? 'conforme' : 'critica'}
      icone={conforme ? <IconeCerto /> : <IconeAtencao />}
    >
      {conforme ? 'Conforme' : 'Não conforme'}
    </Selo>
  )
}
