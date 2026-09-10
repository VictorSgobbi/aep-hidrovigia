import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { chaves } from '../../api/chaves'
import {
  listarAnalisesDoPonto,
  listarAnalisesPorPeriodo,
} from '../../api/recursos'
import type { AnaliseResponse } from '../../api/tipos'
import { AvisoErro, Carregando, Vazio } from '../../componentes/Estado'
import { SeloConformidade } from '../../componentes/Selo'
import {
  formatarDataHora,
  formatarPercentual,
  paraDateLocal,
} from '../../dominio/formatos'
import { FormularioColeta } from './FormularioColeta'
import { VereditoAnalise } from './VereditoAnalise'
import './ColetasPage.css'

/** Últimos 30 dias: `inicio` e `fim` são obrigatórios na API, sem padrão. */
function periodoPadrao() {
  const fim = new Date()
  const inicio = new Date(fim.getTime() - 30 * 86_400_000)
  return { inicio: paraDateLocal(inicio), fim: paraDateLocal(fim) }
}

export function ColetasPage() {
  const [parametros] = useSearchParams()
  const [ultima, setUltima] = useState<AnaliseResponse | null>(null)
  const [periodo, setPeriodo] = useState(periodoPadrao)
  const [erroPeriodo, setErroPeriodo] = useState<string | null>(null)

  const pontoDaUrl = parametros.get('ponto') ?? undefined

  // Com um ponto escolhido, o histórico dele; senão, o período.
  const codigoDoHistorico = ultima?.pontoCodigo ?? pontoDaUrl

  const historicoDoPonto = useQuery({
    queryKey: chaves.analisesDoPonto(codigoDoHistorico ?? ''),
    queryFn: () => listarAnalisesDoPonto(codigoDoHistorico as string),
    enabled: Boolean(codigoDoHistorico),
  })

  const historicoDoPeriodo = useQuery({
    queryKey: chaves.analisesPorPeriodo(periodo.inicio, periodo.fim),
    queryFn: () =>
      listarAnalisesPorPeriodo(
        `${periodo.inicio}T00:00:00.000Z`,
        `${periodo.fim}T23:59:59.999Z`,
      ),
    enabled: !codigoDoHistorico,
  })

  const historico = codigoDoHistorico ? historicoDoPonto : historicoDoPeriodo

  const trocarPeriodo = (campo: 'inicio' | 'fim', valor: string) => {
    const proximo = { ...periodo, [campo]: valor }

    // A API responde 409 se o início for depois do fim; barrar aqui deixa a
    // mensagem inline, junto do campo que a pessoa acabou de mudar.
    if (proximo.inicio > proximo.fim) {
      setErroPeriodo('O início do período não pode ser depois do fim.')
      return
    }
    setErroPeriodo(null)
    setPeriodo(proximo)
  }

  return (
    <>
      <h1>Coletas</h1>

      <div className="coletas-layout">
        <section className="cartao" aria-labelledby="titulo-registrar">
          <h2 id="titulo-registrar">Registrar coleta</h2>
          <FormularioColeta
            codigoPontoInicial={pontoDaUrl}
            aoRegistrar={setUltima}
          />
        </section>

        <div>
          {ultima && (
            <section className="secao" aria-labelledby="titulo-veredito">
              <h2 id="titulo-veredito">Resultado da coleta</h2>
              <VereditoAnalise analise={ultima} />
            </section>
          )}

          <section className="secao" aria-labelledby="titulo-historico">
            <div className="secao__cabecalho">
              <h2 id="titulo-historico">
                {codigoDoHistorico
                  ? `Histórico de ${codigoDoHistorico}`
                  : 'Coletas do período'}
              </h2>
            </div>

            {!codigoDoHistorico && (
              <div className="periodo">
                <label>
                  <span>De</span>
                  <input
                    type="date"
                    value={periodo.inicio}
                    onChange={(evento) =>
                      trocarPeriodo('inicio', evento.target.value)
                    }
                  />
                </label>
                <label>
                  <span>até</span>
                  <input
                    type="date"
                    value={periodo.fim}
                    onChange={(evento) =>
                      trocarPeriodo('fim', evento.target.value)
                    }
                  />
                </label>
              </div>
            )}

            {erroPeriodo && (
              <p className="campo__erro" role="alert">
                {erroPeriodo}
              </p>
            )}

            {historico.isPending && <Carregando />}

            {historico.isError && (
              <AvisoErro
                erro={historico.error}
                aoTentarNovamente={() => void historico.refetch()}
              />
            )}

            {historico.data &&
              (historico.data.length === 0 ? (
                <Vazio>Nenhuma coleta registrada neste recorte.</Vazio>
              ) : (
                <div className="tabela--rolavel">
                  <table className="tabela">
                    <caption>
                      Coletas em ordem cronológica inversa, como a API devolve
                    </caption>
                    <thead>
                      <tr>
                        <th scope="col">Coletado em</th>
                        <th scope="col">Ponto</th>
                        <th scope="col">Parâmetros</th>
                        <th scope="col">Conformidade</th>
                        <th scope="col">Situação</th>
                      </tr>
                    </thead>
                    <tbody>
                      {historico.data.map((analise) => (
                        <tr key={analise.id}>
                          <th scope="row" data-rotulo="Coletado em">
                            {formatarDataHora(analise.coletadoEm)}
                          </th>
                          <td data-rotulo="Ponto">{analise.pontoCodigo}</td>
                          <td data-rotulo="Parâmetros" className="numerico">
                            {analise.qtdParametros - analise.qtdNaoConformidades}
                            /{analise.qtdParametros}
                          </td>
                          <td data-rotulo="Conformidade" className="numerico">
                            {formatarPercentual(analise.percentualConformidade)}
                          </td>
                          <td data-rotulo="Situação">
                            <SeloConformidade conforme={analise.conforme} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ))}
          </section>
        </div>
      </div>
    </>
  )
}
