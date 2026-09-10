import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { chaves } from '../../api/chaves'
import {
  indicadoresDoPonto,
  indicadoresGerais,
  listarOcorrencias,
  listarPontos,
} from '../../api/recursos'
import { AvisoErro, Carregando, Vazio } from '../../componentes/Estado'
import { Barra, Indicador } from '../../componentes/Indicador'
import { Prazo } from '../../componentes/Prazo'
import { SeloGravidade, SeloStatus } from '../../componentes/Selo'
import { formatarNumero, formatarPercentual } from '../../dominio/formatos'
import { estadoDoPrazo } from '../../dominio/prazos'
import { useAgora } from '../../hooks/useAgora'
import './PainelPage.css'

const GERAL = 'geral'

export function PainelPage() {
  const [escopo, setEscopo] = useState(GERAL)
  const agora = useAgora()

  const indicadores = useQuery({
    queryKey: chaves.painel(escopo),
    queryFn: () =>
      escopo === GERAL ? indicadoresGerais() : indicadoresDoPonto(escopo),
    // O painel é a tela que fica aberta: vale reconsultar sozinho.
    refetchInterval: 60_000,
  })

  // Sem filtro de status, a API devolve as não resolvidas já ordenadas pelo
  // prazo mais próximo de vencer. A ordem vem do servidor: não reordenar.
  const fila = useQuery({
    queryKey: chaves.ocorrencias(),
    queryFn: () => listarOcorrencias(),
    refetchInterval: 60_000,
  })

  const pontos = useQuery({
    queryKey: chaves.pontos({ apenasAtivos: true }),
    queryFn: () => listarPontos({ apenasAtivos: true }),
  })

  return (
    <>
      <div className="secao__cabecalho">
        <h1>Painel de conformidade</h1>

        <label className="painel-escopo">
          <span>Escopo</span>
          <select
            value={escopo}
            onChange={(evento) => setEscopo(evento.target.value)}
          >
            <option value={GERAL}>Todos os pontos</option>
            {pontos.data?.map((ponto) => (
              <option key={ponto.id} value={ponto.codigo}>
                {ponto.codigo} — {ponto.nome}
              </option>
            ))}
          </select>
        </label>
      </div>

      {indicadores.isPending && <Carregando>Carregando indicadores…</Carregando>}

      {indicadores.isError && (
        <AvisoErro
          erro={indicadores.error}
          aoTentarNovamente={() => void indicadores.refetch()}
        />
      )}

      {indicadores.data &&
        (indicadores.data.totalAnalises === 0 ? (
          <Vazio
            acao={
              <Link className="botao" to="/coletas">
                Registrar a primeira coleta
              </Link>
            }
          >
            Nenhuma análise registrada
            {escopo === GERAL ? '' : ` em ${escopo}`} ainda. Os indicadores
            aparecem aqui depois da primeira coleta.
          </Vazio>
        ) : (
          <ul className="grade-indicadores">
            <Indicador
              rotulo="Conformidade"
              valor={formatarPercentual(
                indicadores.data.percentualConformidade,
              )}
              apoio={`${formatarNumero(indicadores.data.analisesConformes)} de ${formatarNumero(indicadores.data.totalAnalises)} análises dentro do padrão`}
            >
              <Barra
                percentual={indicadores.data.percentualConformidade}
                descricao={`${indicadores.data.analisesConformes} de ${indicadores.data.totalAnalises} análises conformes`}
              />
            </Indicador>

            <Indicador
              rotulo="Análises registradas"
              valor={formatarNumero(indicadores.data.totalAnalises)}
              apoio={`${formatarNumero(indicadores.data.analisesNaoConformes)} reprovaram`}
            />

            <Indicador
              rotulo="Pendências abertas"
              valor={formatarNumero(indicadores.data.ocorrenciasPendentes)}
              apoio="ocorrências ainda não resolvidas"
              atencao={indicadores.data.ocorrenciasPendentes > 0}
            />

            <Indicador
              rotulo="Prazos vencidos"
              valor={formatarNumero(indicadores.data.ocorrenciasVencidas)}
              apoio="pendências fora do prazo da norma"
              atencao={indicadores.data.ocorrenciasVencidas > 0}
            />
          </ul>
        ))}

      <section className="secao" aria-labelledby="titulo-fila">
        <div className="secao__cabecalho">
          <h2 id="titulo-fila">Vencendo primeiro</h2>
          <Link to="/ocorrencias">Ver todas as ocorrências</Link>
        </div>

        {fila.isPending && <Carregando>Carregando a fila…</Carregando>}

        {fila.isError && (
          <AvisoErro
            erro={fila.error}
            aoTentarNovamente={() => void fila.refetch()}
          />
        )}

        {fila.data &&
          (fila.data.length === 0 ? (
            <Vazio>
              Nenhuma pendência aberta. Toda análise reprovada gera uma
              ocorrência, e todas foram resolvidas.
            </Vazio>
          ) : (
            <ul className="lista-limpa">
              {fila.data.slice(0, 5).map((ocorrencia) => {
                const { vencida } = estadoDoPrazo(ocorrencia, agora)

                return (
                  <li key={ocorrencia.id} className="cartao fila-item">
                    <div className="fila-item__selos">
                      <SeloGravidade
                        gravidade={ocorrencia.gravidade}
                        prazoHoras={ocorrencia.prazoHoras}
                      />
                      <SeloStatus status={ocorrencia.status} />
                      {vencida && <strong className="fila-item__vencida">Fora do prazo</strong>}
                    </div>

                    <p className="fila-item__ponto">
                      <Link to={`/ocorrencias?ocorrencia=${ocorrencia.id}`}>
                        {ocorrencia.pontoCodigo}
                      </Link>{' '}
                      <span className="fila-item__violacoes">
                        {ocorrencia.parametrosViolados
                          .map((parametro) => parametro.nome)
                          .join(', ')}
                      </span>
                    </p>

                    <Prazo ocorrencia={ocorrencia} agora={agora} />
                  </li>
                )
              })}
            </ul>
          ))}
      </section>
    </>
  )
}
