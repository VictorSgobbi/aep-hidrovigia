import type { ParametroCatalogado } from '../../api/tipos'
import { SeloRisco } from '../../componentes/Selo'
import { paraDecimal } from '../../dominio/formatos'
import { preverConformidade } from '../../dominio/limites'

/**
 * Uma linha do formulário de coleta: marcar o parâmetro e digitar o valor.
 *
 * Só existe uma linha por item do catálogo da norma, sem botão de "adicionar
 * parâmetro". Isso torna impossíveis por construção dois erros que a API
 * recusaria: código repetido na mesma análise (409) e código fora do catálogo
 * (422).
 */
export function LinhaParametro({
  parametro,
  indice,
  marcado,
  valor,
  erro,
  aoMarcar,
  aoDigitar,
}: {
  parametro: ParametroCatalogado
  /** Posição na lista que será ENVIADA, para casar com `leituras[n].valor`. */
  indice: number
  marcado: boolean
  valor: string
  erro?: string | undefined
  aoMarcar: (marcado: boolean) => void
  aoDigitar: (valor: string) => void
}) {
  const idValor = `valor-${parametro.codigo}`
  const idDica = `dica-${parametro.codigo}`
  const idPrevia = `previa-${parametro.codigo}`
  const idErro = erro ? `erro-${parametro.codigo}` : undefined

  const previsao = marcado
    ? preverConformidade(parametro, paraDecimal(valor))
    : 'indefinida'

  return (
    <li className={`linha-parametro ${marcado ? 'linha-parametro--ativa' : ''}`}>
      <label className="linha-parametro__marcar">
        <input
          type="checkbox"
          checked={marcado}
          onChange={(evento) => aoMarcar(evento.target.checked)}
        />
        <span>
          <strong>{parametro.nome}</strong>
          <span className="linha-parametro__codigo">{parametro.codigo}</span>
        </span>
      </label>

      <div className="linha-parametro__medida">
        <label className="so-leitor-de-tela" htmlFor={idValor}>
          Valor medido de {parametro.nome}
        </label>
        <input
          id={idValor}
          /*
            O name e o caminho do campo no DTO, entao a mensagem de um 400
            volta para o input certo sem tabela de traducao.
          */
          name={`leituras[${indice}].valor`}
          type="text"
          inputMode="decimal"
          disabled={!marcado}
          value={valor}
          aria-invalid={erro ? true : undefined}
          aria-describedby={[idErro, idDica, marcado ? idPrevia : undefined]
            .filter(Boolean)
            .join(' ')}
          onChange={(evento) => aoDigitar(evento.target.value)}
          /*
            NAO existe atributo `max` aqui, de proposito. Ele viria de
            parametro.limiteMaximo, que para os parametros de ausencia (ECOLI,
            CTOT) vale 0 — e com max={0} seria impossivel digitar 14 UFC/100mL,
            justamente o valor que abre uma ocorrencia critica. O limite e
            comunicado como dica e verificado pela previa.
          */
        />
        <span className="linha-parametro__unidade" aria-hidden="true">
          {parametro.unidade || '—'}
        </span>
      </div>

      <p className="linha-parametro__dica" id={idDica}>
        Limite: {parametro.limite}
      </p>

      {marcado && previsao !== 'indefinida' && (
        <p
          className={`linha-parametro__previa linha-parametro__previa--${previsao}`}
          id={idPrevia}
        >
          {previsao === 'fora'
            ? 'Prévia: fora do limite'
            : 'Prévia: dentro do limite'}
        </p>
      )}

      {erro && (
        <p className="campo__erro" id={idErro}>
          {erro}
        </p>
      )}

      <details className="linha-parametro__norma">
        <summary>Base legal</summary>
        <p>
          <SeloRisco risco={parametro.risco} /> {parametro.referenciaLegal}
        </p>
      </details>
    </li>
  )
}
