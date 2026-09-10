import { useQuery } from '@tanstack/react-query'
import { chaves } from '../../api/chaves'
import { buscarOcorrencia } from '../../api/recursos'
import { AvisoErro, Carregando } from '../../componentes/Estado'
import { Prazo } from '../../componentes/Prazo'
import {
  SeloGravidade,
  SeloRisco,
  SeloStatus,
} from '../../componentes/Selo'
import { formatarDataHora, formatarMedida } from '../../dominio/formatos'
import { ROTULO_STATUS, rotular } from '../../dominio/rotulos'
import { useAgora } from '../../hooks/useAgora'
import { FormularioTratativa } from './FormularioTratativa'

/**
 * Ficha de uma ocorrência, aberta por `?ocorrencia=<id>`.
 *
 * O estado mora na URL, então o botão de voltar funciona e é possível retomar
 * um ponto exato durante a gravação.
 */
export function DetalheOcorrencia({
  id,
  aoFechar,
}: {
  id: string
  aoFechar: () => void
}) {
  const agora = useAgora()

  const consulta = useQuery({
    queryKey: chaves.ocorrencia(id),
    queryFn: () => buscarOcorrencia(id),
  })

  return (
    <aside className="detalhe" aria-labelledby="titulo-detalhe">
      <div className="detalhe__topo">
        <h2 id="titulo-detalhe">Ocorrência</h2>
        <button
          type="button"
          className="botao botao--secundario"
          onClick={aoFechar}
        >
          Fechar
        </button>
      </div>

      {consulta.isPending && <Carregando />}

      {consulta.isError && (
        <AvisoErro
          erro={consulta.error}
          aoTentarNovamente={() => void consulta.refetch()}
        />
      )}

      {consulta.data && (
        <>
          <div className="detalhe__selos">
            <SeloGravidade
              gravidade={consulta.data.gravidade}
              prazoHoras={consulta.data.prazoHoras}
            />
            <SeloStatus status={consulta.data.status} />
          </div>

          <Prazo ocorrencia={consulta.data} agora={agora} />

          <dl className="detalhe__dados">
            <dt>Ponto</dt>
            <dd>{consulta.data.pontoCodigo}</dd>

            <dt>Aberta em</dt>
            <dd>{formatarDataHora(consulta.data.abertaEm)}</dd>

            <dt>Prazo limite</dt>
            <dd>{formatarDataHora(consulta.data.prazoLimite)}</dd>

            {/*
              Rastreabilidade: a gravidade nao e um palpite, e o resultado de
              uma politica que a ocorrencia grava. Trocar a politica no backend
              muda a classificacao, e este campo diz qual valeu.
            */}
            <dt>Classificada por</dt>
            <dd>
              {consulta.data.politicaClassificacao === 'risco-sanitario'
                ? 'pior risco sanitário da amostra'
                : consulta.data.politicaClassificacao}
            </dd>
          </dl>

          <section aria-labelledby="titulo-violados">
            <h3 id="titulo-violados">Parâmetros violados</h3>

            <div className="tabela--rolavel">
              <table className="tabela">
                <caption>
                  Limites da Portaria GM/MS n. 888/2021 aplicados na coleta
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Parâmetro</th>
                    <th scope="col">Medido</th>
                    <th scope="col">Limite</th>
                    <th scope="col">Risco</th>
                  </tr>
                </thead>
                <tbody>
                  {consulta.data.parametrosViolados.map((parametro) => (
                    <tr key={parametro.codigo}>
                      <th scope="row" data-rotulo="Parâmetro">
                        {parametro.nome}
                        <span className="detalhe__mensagem">
                          {parametro.mensagem}
                        </span>
                      </th>
                      <td data-rotulo="Medido" className="numerico">
                        {formatarMedida(
                          parametro.valorMedido,
                          parametro.unidade,
                        )}
                      </td>
                      <td data-rotulo="Limite">{parametro.limiteAplicado}</td>
                      <td data-rotulo="Risco">
                        <SeloRisco risco={parametro.risco} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section aria-labelledby="titulo-tratativas">
            <h3 id="titulo-tratativas">Histórico de tratativas</h3>

            {consulta.data.tratativas.length === 0 ? (
              <p className="detalhe__sem-tratativa">
                Nenhuma ação registrada ainda.
              </p>
            ) : (
              <ol className="linha-tempo">
                {consulta.data.tratativas.map((tratativa, indice) => (
                  <li
                    key={`${tratativa.em}-${indice}`}
                    className="linha-tempo__item"
                  >
                    <p className="linha-tempo__acao">{tratativa.acao}</p>
                    <p className="linha-tempo__meta">
                      {tratativa.por} · {formatarDataHora(tratativa.em)} ·{' '}
                      {rotular(ROTULO_STATUS, tratativa.statusResultante)}
                    </p>
                  </li>
                ))}
              </ol>
            )}
          </section>

          <FormularioTratativa ocorrencia={consulta.data} />
        </>
      )}
    </aside>
  )
}
