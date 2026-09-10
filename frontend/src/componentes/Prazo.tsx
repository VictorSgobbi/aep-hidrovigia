import type { OcorrenciaResponse } from '../api/tipos'
import { estadoDoPrazo } from '../dominio/prazos'
import { IconeAtencao, IconeRelogio } from './Icones'

/**
 * Prazo de resposta de uma ocorrência: texto, medidor e estado.
 *
 * O medidor é um `role="meter"` com `aria-valuetext` em português, então quem
 * usa leitor de tela ouve "6 horas restantes de 24" em vez de "72 por cento".
 *
 * A faixa vencida ganha hachura diagonal além da cor, para o estado sobreviver
 * a monocromático.
 */
export function Prazo({
  ocorrencia,
  agora,
}: {
  ocorrencia: OcorrenciaResponse
  agora: number
}) {
  const { vencida, consumido, texto } = estadoDoPrazo(ocorrencia, agora)

  const descricao = vencida
    ? `Prazo de ${ocorrencia.prazoHoras} horas vencido ${texto}`
    : `Vence ${texto}, de um prazo de ${ocorrencia.prazoHoras} horas`

  return (
    <div className={`prazo ${vencida ? 'prazo--vencida' : ''}`}>
      <p className="prazo__texto">
        {vencida ? <IconeAtencao /> : <IconeRelogio />}
        {vencida ? <strong>Vencida {texto}</strong> : <>Vence {texto}</>}
      </p>

      <div
        className="prazo__medidor"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(consumido)}
        aria-valuetext={descricao}
      >
        <div
          className="prazo__preenchido"
          style={{ width: `${consumido}%` }}
        />
      </div>
    </div>
  )
}
