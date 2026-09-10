import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { chaves } from '../../api/chaves'
import {
  catalogoParametros,
  listarPontos,
  registrarAnalise,
} from '../../api/recursos'
import type { AnaliseResponse, LeituraRequest } from '../../api/tipos'
import { Campo } from '../../componentes/Campo'
import { AvisoErro, Carregando } from '../../componentes/Estado'
import { paraDatetimeLocal, paraDecimal, paraInstante } from '../../dominio/formatos'
import { useErrosDeCampo } from '../../hooks/useErrosDeCampo'
import { LinhaParametro } from './LinhaParametro'

interface Props {
  codigoPontoInicial?: string | undefined
  aoRegistrar: (analise: AnaliseResponse) => void
}

export function FormularioColeta({ codigoPontoInicial, aoRegistrar }: Props) {
  const clienteQuery = useQueryClient()

  const [codigoPonto, setCodigoPonto] = useState(codigoPontoInicial ?? '')
  const [coletor, setColetor] = useState('')
  const [coletadoEm, setColetadoEm] = useState(() =>
    paraDatetimeLocal(new Date()),
  )
  const [marcados, setMarcados] = useState<Record<string, boolean>>({})
  // Valores como texto: converter a cada tecla perderia "0," e o zero à
  // esquerda enquanto a pessoa digita.
  const [valores, setValores] = useState<Record<string, string>>({})
  const [problemaLocal, setProblemaLocal] = useState<string | null>(null)

  const catalogo = useQuery({
    queryKey: chaves.catalogo(),
    queryFn: catalogoParametros,
    // O catálogo da norma não muda em tempo de execução.
    staleTime: Number.POSITIVE_INFINITY,
  })

  const pontos = useQuery({
    queryKey: chaves.pontos({ apenasAtivos: true }),
    queryFn: () => listarPontos({ apenasAtivos: true }),
  })

  const pontoEscolhido = pontos.data?.find(
    (ponto) => ponto.codigo === codigoPonto,
  )

  /** Só os marcados, na ordem do catálogo — é a lista que vai no corpo. */
  const leiturasSelecionadas = useMemo(() => {
    if (!catalogo.data) return []
    return catalogo.data.filter((parametro) => marcados[parametro.codigo])
  }, [catalogo.data, marcados])

  const registrar = useMutation({
    mutationFn: (leituras: LeituraRequest[]) =>
      registrarAnalise({
        codigoPonto,
        coletor,
        coletadoEm: paraInstante(coletadoEm) ?? '',
        leituras,
      }),
    onSuccess: (analise) => {
      // A ocorrência é aberta pelo backend e não vem nesta resposta: a fila e
      // o painel precisam ser reconsultados.
      void clienteQuery.invalidateQueries({ queryKey: ['ocorrencias'] })
      void clienteQuery.invalidateQueries({ queryKey: ['painel'] })
      void clienteQuery.invalidateQueries({ queryKey: ['analises'] })

      // Mantém ponto e coletor: registrar duas coletas seguidas é comum.
      setMarcados({})
      setValores({})
      aoRegistrar(analise)
    },
  })

  const camposRenderizados = useMemo(
    () => [
      'codigoPonto',
      'coletor',
      'coletadoEm',
      ...leiturasSelecionadas.map((_, indice) => `leituras[${indice}].valor`),
    ],
    [leiturasSelecionadas],
  )

  const erros = useErrosDeCampo(registrar.error, camposRenderizados)

  const enviar = () => {
    setProblemaLocal(null)

    if (pontoEscolhido && !pontoEscolhido.ativo) {
      // A API responderia 409. Barrar antes deixa a mensagem mais direta.
      setProblemaLocal('Este ponto está desativado e não aceita novas coletas.')
      return
    }

    const instante = paraInstante(coletadoEm)
    if (!instante) {
      setProblemaLocal('Informe uma data e hora de coleta válidas.')
      return
    }
    if (Date.parse(instante) > Date.now()) {
      setProblemaLocal('A data da coleta não pode estar no futuro.')
      return
    }
    if (leiturasSelecionadas.length === 0) {
      setProblemaLocal('Marque ao menos um parâmetro medido.')
      return
    }

    const leituras: LeituraRequest[] = []
    for (const parametro of leiturasSelecionadas) {
      const valor = paraDecimal(valores[parametro.codigo] ?? '')
      if (valor === null) {
        setProblemaLocal(
          `Informe um valor numérico não negativo para ${parametro.nome}.`,
        )
        return
      }
      leituras.push({ codigo: parametro.codigo, valor })
    }

    registrar.mutate(leituras)
  }

  if (catalogo.isPending) {
    return <Carregando>Carregando o catálogo da norma…</Carregando>
  }

  if (catalogo.isError) {
    // Sem catálogo o formulário não pode ser montado: registrar às cegas
    // produziria 422 em cada código.
    return (
      <AvisoErro
        erro={catalogo.error}
        aoTentarNovamente={() => void catalogo.refetch()}
      />
    )
  }

  return (
    <form
      className="coleta-form"
      onSubmit={(evento) => {
        evento.preventDefault()
        enviar()
      }}
    >
      {erros.erro && <AvisoErro erro={erros.erro} extras={erros.gerais} />}

      {problemaLocal && (
        <div className="aviso-erro" role="alert">
          <p className="aviso-erro__mensagem">{problemaLocal}</p>
        </div>
      )}

      <Campo
        name="codigoPonto"
        rotulo="Ponto de coleta"
        obrigatorio
        erro={erros.de('codigoPonto')}
      >
        {(atributos) => (
          <select
            {...atributos}
            value={codigoPonto}
            onChange={(evento) => setCodigoPonto(evento.target.value)}
          >
            <option value="">Selecione…</option>
            {pontos.data?.map((ponto) => (
              <option key={ponto.id} value={ponto.codigo}>
                {ponto.codigo} — {ponto.nome}
              </option>
            ))}
          </select>
        )}
      </Campo>

      {pontoEscolhido && (
        <p className="coleta-form__ponto">
          {pontoEscolhido.nome} · {pontoEscolhido.localizacao.municipio}/
          {pontoEscolhido.localizacao.uf} · atende{' '}
          {pontoEscolhido.populacaoAtendida} pessoas
        </p>
      )}

      <Campo
        name="coletor"
        rotulo="Quem coletou"
        obrigatorio
        erro={erros.de('coletor')}
      >
        {(atributos) => (
          <input
            {...atributos}
            type="text"
            value={coletor}
            onChange={(evento) => setColetor(evento.target.value)}
          />
        )}
      </Campo>

      <Campo
        name="coletadoEm"
        rotulo="Data e hora da coleta"
        obrigatorio
        erro={erros.de('coletadoEm')}
        dica="Não pode estar no futuro."
      >
        {(atributos) => (
          <input
            {...atributos}
            type="datetime-local"
            value={coletadoEm}
            max={paraDatetimeLocal(new Date())}
            onChange={(evento) => setColetadoEm(evento.target.value)}
          />
        )}
      </Campo>

      <fieldset className="coleta-form__parametros">
        <legend>Parâmetros medidos</legend>
        <p className="campo__dica">
          Uma linha por parâmetro do catálogo da norma, na ordem em que ela os
          organiza — microbiológicos primeiro.
        </p>

        <ul className="lista-limpa">
          {catalogo.data.map((parametro) => {
            const indice = leiturasSelecionadas.findIndex(
              (item) => item.codigo === parametro.codigo,
            )

            return (
              <LinhaParametro
                key={parametro.codigo}
                parametro={parametro}
                indice={indice < 0 ? 0 : indice}
                marcado={marcados[parametro.codigo] ?? false}
                valor={valores[parametro.codigo] ?? ''}
                erro={indice < 0 ? undefined : erros.de(`leituras[${indice}].valor`)}
                aoMarcar={(marcado) =>
                  setMarcados((atual) => ({
                    ...atual,
                    [parametro.codigo]: marcado,
                  }))
                }
                aoDigitar={(valor) =>
                  setValores((atual) => ({
                    ...atual,
                    [parametro.codigo]: valor,
                  }))
                }
              />
            )
          })}
        </ul>
      </fieldset>

      <button type="submit" className="botao" disabled={registrar.isPending}>
        {registrar.isPending ? 'Registrando…' : 'Registrar coleta'}
      </button>
    </form>
  )
}
