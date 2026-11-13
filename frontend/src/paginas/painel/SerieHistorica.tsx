import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { chaves } from '../../api/chaves'
import { catalogoParametros, serieHistorica } from '../../api/recursos'
import type { SerieHistorica as DadosDaSerie } from '../../api/tipos'
import { AvisoErro, Carregando, Vazio } from '../../componentes/Estado'
import { formatarData, formatarMedida } from '../../dominio/formatos'
import { escalarSerie } from '../../dominio/serie'

const MOLDURA = { largura: 640, altura: 220, margem: 24 }

/** Cloro residual primeiro: é o parâmetro em que tendência antecede o desvio. */
const PARAMETRO_INICIAL = 'CRL'

/**
 * Média diária de um parâmetro num ponto, com a faixa da norma.
 *
 * Os números vêm prontos da agregação do MongoDB; aqui só se desenha. Dia com
 * leitura reprovada ganha marcador vermelho **e** fica listado no texto abaixo
 * — cor nunca é o único canal.
 */
export function SerieHistorica({ codigoPonto }: { codigoPonto: string }) {
  const [parametro, setParametro] = useState(PARAMETRO_INICIAL)

  const catalogo = useQuery({
    queryKey: chaves.catalogo(),
    queryFn: catalogoParametros,
    staleTime: Infinity,
  })

  const serie = useQuery({
    queryKey: chaves.serie(codigoPonto, parametro),
    queryFn: () => serieHistorica(codigoPonto, parametro),
  })

  return (
    <section className="secao" aria-labelledby="titulo-serie">
      <div className="secao__cabecalho">
        <h2 id="titulo-serie">Série histórica de {codigoPonto}</h2>

        <label className="painel-escopo">
          <span>Parâmetro</span>
          <select
            value={parametro}
            onChange={(evento) => setParametro(evento.target.value)}
          >
            {(catalogo.data ?? []).map((item) => (
              <option key={item.codigo} value={item.codigo}>
                {item.nome}
              </option>
            ))}
            {!catalogo.data && <option value={parametro}>{parametro}</option>}
          </select>
        </label>
      </div>

      {serie.isPending && <Carregando>Calculando a série…</Carregando>}

      {serie.isError && (
        <AvisoErro erro={serie.error} aoTentarNovamente={() => void serie.refetch()} />
      )}

      {serie.data &&
        (serie.data.dias.length === 0 ? (
          <Vazio>
            Nenhuma leitura de {serie.data.nomeParametro} em {codigoPonto} nos
            últimos 90 dias.
          </Vazio>
        ) : (
          <Grafico serie={serie.data} />
        ))}
    </section>
  )
}

function Grafico({ serie }: { serie: DadosDaSerie }) {
  const grafico = escalarSerie(serie, MOLDURA)
  const { largura, margem } = MOLDURA
  const reprovados = serie.dias.filter((dia) => dia.foraDoLimite > 0)
  const medida = (valor: number) => formatarMedida(valor, serie.unidade)

  return (
    <figure className="cartao serie">
      <svg
        className="serie__grafico"
        viewBox={`0 0 ${largura} ${MOLDURA.altura}`}
        role="img"
        aria-label={`Média diária de ${serie.nomeParametro} em ${serie.dias.length} dias com coleta`}
      >
        {grafico.yLimiteMaximo !== undefined && (
          <Limite y={grafico.yLimiteMaximo} rotulo={`máx. ${medida(serie.limiteMaximo!)}`} />
        )}
        {grafico.yLimiteMinimo !== undefined && (
          <Limite y={grafico.yLimiteMinimo} rotulo={`mín. ${medida(serie.limiteMinimo!)}`} />
        )}

        <polyline
          className="serie__linha"
          points={grafico.pontos.map(({ x, y }) => `${x},${y}`).join(' ')}
        />

        {grafico.pontos.map((ponto) => (
          <circle
            key={ponto.dia}
            className={ponto.fora ? 'serie__ponto serie__ponto--fora' : 'serie__ponto'}
            cx={ponto.x}
            cy={ponto.y}
            r={ponto.fora ? 6 : 4.5}
          >
            <title>
              {formatarData(`${ponto.dia}T12:00:00Z`)}: {medida(ponto.media)}
              {ponto.fora ? ' — fora do limite' : ''}
            </title>
          </circle>
        ))}

        <text className="serie__eixo" x={margem} y={MOLDURA.altura - 4}>
          {formatarData(serie.inicio)}
        </text>
        <text className="serie__eixo" x={largura - margem} y={MOLDURA.altura - 4} textAnchor="end">
          {formatarData(serie.fim)}
        </text>
      </svg>

      <figcaption className="serie__legenda">
        {serie.dias.length} {serie.dias.length === 1 ? 'dia' : 'dias'} com coleta;{' '}
        {reprovados.length === 0
          ? 'nenhuma leitura fora do limite da norma.'
          : `fora do limite em ${reprovados
              .map((dia) => formatarData(`${dia.dia}T12:00:00Z`))
              .join(', ')}.`}
      </figcaption>
    </figure>
  )
}

function Limite({ y, rotulo }: { y: number; rotulo: string }) {
  return (
    <g className="serie__limite">
      <line x1={MOLDURA.margem} x2={MOLDURA.largura - MOLDURA.margem} y1={y} y2={y} />
      <text x={MOLDURA.largura - MOLDURA.margem} y={y - 4} textAnchor="end">
        {rotulo}
      </text>
    </g>
  )
}
