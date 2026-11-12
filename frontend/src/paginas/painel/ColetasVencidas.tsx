import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { chaves } from '../../api/chaves'
import { situacaoColetas } from '../../api/recursos'
import { AvisoErro, Carregando, Vazio } from '../../componentes/Estado'
import { IconeRelogio } from '../../componentes/Icones'
import { Selo } from '../../componentes/Selo'
import { formatarData } from '../../dominio/formatos'
import { ROTULO_TIPO_FONTE } from '../../dominio/rotulos'

/**
 * Pontos cuja última coleta passou da frequência mínima do tipo de fonte.
 *
 * É o complemento da fila de ocorrências: ela só enxerga o que foi medido, e
 * um ponto que deixou de ser amostrado nunca reprova nada. A ordem — nunca
 * coletados, depois os mais atrasados — vem do servidor.
 */
export function ColetasVencidas() {
  const situacoes = useQuery({
    queryKey: chaves.situacaoColetas(),
    queryFn: situacaoColetas,
    refetchInterval: 60_000,
  })

  const vencidas = situacoes.data?.filter((situacao) => situacao.vencida)

  return (
    <section className="secao" aria-labelledby="titulo-coletas-vencidas">
      <div className="secao__cabecalho">
        <h2 id="titulo-coletas-vencidas">Coletas vencidas</h2>
        <Link to="/coletas">Registrar coleta</Link>
      </div>

      {situacoes.isPending && <Carregando>Carregando a rotina de coleta…</Carregando>}

      {situacoes.isError && (
        <AvisoErro
          erro={situacoes.error}
          aoTentarNovamente={() => void situacoes.refetch()}
        />
      )}

      {vencidas &&
        (vencidas.length === 0 ? (
          <Vazio>
            Todos os pontos ativos foram coletados dentro da frequência mínima
            do seu tipo de fonte.
          </Vazio>
        ) : (
          <ul className="lista-limpa">
            {vencidas.map((situacao) => (
              <li key={situacao.codigoPonto} className="cartao fila-item">
                <div className="fila-item__selos">
                  <Selo tom="critica" icone={<IconeRelogio />}>
                    {situacao.ultimaColeta ? 'Coleta atrasada' : 'Nunca coletado'}
                  </Selo>
                  <span className="fila-item__violacoes">
                    {ROTULO_TIPO_FONTE[situacao.tipoFonte]} · a cada{' '}
                    {situacao.frequenciaMinimaDias} dias
                  </span>
                </div>

                <p className="fila-item__ponto">
                  {situacao.codigoPonto}{' '}
                  <span className="fila-item__violacoes">{situacao.nomePonto}</span>
                </p>

                {situacao.ultimaColeta && situacao.proximaColetaAte && (
                  <p className="fila-item__violacoes">
                    Última coleta em {formatarData(situacao.ultimaColeta)}; a
                    próxima devia ter sido até{' '}
                    {formatarData(situacao.proximaColetaAte)}.
                  </p>
                )}
              </li>
            ))}
          </ul>
        ))}
    </section>
  )
}
