import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { chaves } from '../../api/chaves'
import { listarOcorrencias } from '../../api/recursos'
import type { StatusOcorrencia } from '../../api/tipos'
import { AvisoErro, Carregando, Vazio } from '../../componentes/Estado'
import { Prazo } from '../../componentes/Prazo'
import { SeloGravidade, SeloStatus } from '../../componentes/Selo'
import { formatarMedida } from '../../dominio/formatos'
import { useAgora } from '../../hooks/useAgora'
import { DetalheOcorrencia } from './DetalheOcorrencia'
import './OcorrenciasPage.css'

/**
 * As abas.
 *
 * "Pendentes" é a primeira e a padrão: é para resolver essa fila que o sistema
 * existe. Ela não manda `status` — sem filtro, a API devolve todas as não
 * resolvidas já ordenadas pelo prazo mais próximo de vencer.
 */
const ABAS: { chave: string; rotulo: string; status?: StatusOcorrencia }[] = [
  { chave: 'pendentes', rotulo: 'Pendentes' },
  { chave: 'ABERTA', rotulo: 'Abertas', status: 'ABERTA' },
  { chave: 'EM_TRATATIVA', rotulo: 'Em tratativa', status: 'EM_TRATATIVA' },
  { chave: 'RESOLVIDA', rotulo: 'Resolvidas', status: 'RESOLVIDA' },
]

export function OcorrenciasPage() {
  const [parametros, setParametros] = useSearchParams()
  const agora = useAgora()

  const abaAtual = parametros.get('aba') ?? 'pendentes'
  const selecionada = parametros.get('ocorrencia')
  const aba = ABAS.find((item) => item.chave === abaAtual) ?? ABAS[0]

  const consulta = useQuery({
    queryKey: chaves.ocorrencias(aba?.status),
    queryFn: () => listarOcorrencias(aba?.status),
  })

  const trocar = (mudancas: Record<string, string | null>) => {
    const proximos = new URLSearchParams(parametros)
    for (const [chave, valor] of Object.entries(mudancas)) {
      if (valor === null) proximos.delete(chave)
      else proximos.set(chave, valor)
    }
    setParametros(proximos)
  }

  return (
    <div className={selecionada ? 'com-detalhe' : ''}>
      <div>
        <h1>Ocorrências</h1>

        <div className="abas" role="tablist" aria-label="Filtro por situação">
          {ABAS.map((item) => (
            <button
              key={item.chave}
              type="button"
              role="tab"
              aria-selected={item.chave === abaAtual}
              className="abas__item"
              onClick={() => trocar({ aba: item.chave, ocorrencia: null })}
            >
              {item.rotulo}
            </button>
          ))}
        </div>

        {consulta.isPending && <Carregando>Carregando ocorrências…</Carregando>}

        {consulta.isError && (
          <AvisoErro
            erro={consulta.error}
            aoTentarNovamente={() => void consulta.refetch()}
          />
        )}

        {consulta.data &&
          (consulta.data.length === 0 ? (
            <Vazio>
              {abaAtual === 'pendentes'
                ? 'Nenhuma pendência aberta. Toda análise reprovada gera uma ocorrência, e todas foram resolvidas.'
                : 'Nenhuma ocorrência nesta situação.'}
            </Vazio>
          ) : (
            <ul className="lista-limpa">
              {consulta.data.map((ocorrencia) => (
                <li key={ocorrencia.id}>
                  <button
                    type="button"
                    className={`cartao ocorrencia-item ${
                      ocorrencia.id === selecionada
                        ? 'ocorrencia-item--ativa'
                        : ''
                    }`}
                    aria-current={
                      ocorrencia.id === selecionada ? 'true' : undefined
                    }
                    onClick={() => trocar({ ocorrencia: ocorrencia.id })}
                  >
                    <span className="ocorrencia-item__selos">
                      <SeloGravidade
                        gravidade={ocorrencia.gravidade}
                        prazoHoras={ocorrencia.prazoHoras}
                      />
                      <SeloStatus status={ocorrencia.status} />
                    </span>

                    <span className="ocorrencia-item__ponto">
                      {ocorrencia.pontoCodigo}
                    </span>

                    <span className="ocorrencia-item__violados">
                      {ocorrencia.parametrosViolados.map((parametro) => (
                        <span key={parametro.codigo}>
                          {parametro.nome}:{' '}
                          {formatarMedida(
                            parametro.valorMedido,
                            parametro.unidade,
                          )}{' '}
                          <span className="ocorrencia-item__limite">
                            (limite: {parametro.limiteAplicado})
                          </span>
                        </span>
                      ))}
                    </span>

                    <Prazo ocorrencia={ocorrencia} agora={agora} />
                  </button>
                </li>
              ))}
            </ul>
          ))}
      </div>

      {selecionada && (
        <DetalheOcorrencia
          id={selecionada}
          aoFechar={() => trocar({ ocorrencia: null })}
        />
      )}
    </div>
  )
}
