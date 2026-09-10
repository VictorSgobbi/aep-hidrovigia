import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { chaves } from '../../api/chaves'
import { listarOcorrencias } from '../../api/recursos'
import type { AnaliseResponse } from '../../api/tipos'
import { SeloConformidade, SeloGravidade } from '../../componentes/Selo'
import {
  formatarDataHora,
  formatarMedida,
  formatarPercentual,
} from '../../dominio/formatos'

/**
 * Procura a ocorrência que o backend abriu junto com a análise.
 *
 * O `POST /api/analises` abre a ocorrência como efeito colateral e **não** a
 * devolve na resposta. Como a fila sem filtro traz todas as não resolvidas,
 * basta achar pelo `analiseId` — sem endpoint novo e sem adivinhar id.
 */
function useOcorrenciaDaAnalise(analiseId: string, habilitado: boolean) {
  const fila = useQuery({
    queryKey: chaves.ocorrencias(),
    queryFn: () => listarOcorrencias(),
    enabled: habilitado,
  })

  return fila.data?.find((ocorrencia) => ocorrencia.analiseId === analiseId)
}

/**
 * Veredito devolvido pelo 201.
 *
 * É a informação que motiva o produto, então recebe foco e é anunciada por
 * `aria-live`: quem registrou a coleta não deveria precisar procurar o
 * resultado na tela.
 */
export function VereditoAnalise({ analise }: { analise: AnaliseResponse }) {
  const alvoDoFoco = useRef<HTMLDivElement>(null)
  const ocorrencia = useOcorrenciaDaAnalise(analise.id, !analise.conforme)

  useEffect(() => {
    alvoDoFoco.current?.focus()
  }, [analise.id])

  return (
    <div
      className={`veredito veredito--${analise.conforme ? 'conforme' : 'reprovada'}`}
      ref={alvoDoFoco}
      tabIndex={-1}
      role="status"
      aria-live="assertive"
    >
      <div className="veredito__topo">
        <SeloConformidade conforme={analise.conforme} />
        <span className="veredito__resumo">
          {analise.qtdNaoConformidades} de {analise.qtdParametros} parâmetros
          fora do padrão ·{' '}
          {formatarPercentual(analise.percentualConformidade)} de conformidade
        </span>
      </div>

      <p className="veredito__meta">
        {analise.pontoCodigo} · coletado em{' '}
        {formatarDataHora(analise.coletadoEm)} por {analise.coletor}
      </p>

      <div className="tabela--rolavel">
        <table className="tabela">
          <caption>Veredito de cada parâmetro medido</caption>
          <thead>
            <tr>
              <th scope="col">Parâmetro</th>
              <th scope="col">Medido</th>
              <th scope="col">Situação</th>
            </tr>
          </thead>
          <tbody>
            {analise.parametros.map((parametro) => (
              <tr key={parametro.codigo}>
                <th scope="row" data-rotulo="Parâmetro">
                  {parametro.nome}
                  {!parametro.conforme && (
                    <span className="veredito__mensagem">
                      {parametro.mensagem}
                    </span>
                  )}
                </th>
                <td data-rotulo="Medido" className="numerico">
                  {formatarMedida(parametro.valor, parametro.unidade)}
                </td>
                <td data-rotulo="Situação">
                  <SeloConformidade conforme={parametro.conforme} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {!analise.conforme && (
        <div className="veredito__ocorrencia">
          <p>
            <strong>Uma ocorrência foi aberta automaticamente</strong> para esta
            análise, com prazo de resposta definido pela gravidade.
          </p>

          {ocorrencia ? (
            <p className="veredito__ocorrencia-dados">
              <SeloGravidade
                gravidade={ocorrencia.gravidade}
                prazoHoras={ocorrencia.prazoHoras}
              />{' '}
              <Link to={`/ocorrencias?ocorrencia=${ocorrencia.id}`}>
                Abrir na fila de pendências
              </Link>
            </p>
          ) : (
            <p className="veredito__ocorrencia-dados">
              <Link to="/ocorrencias">Ver a fila de pendências</Link>
            </p>
          )}
        </div>
      )}
    </div>
  )
}
