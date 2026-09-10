import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { chaves } from '../../api/chaves'
import { registrarTratativa, resolverOcorrencia } from '../../api/recursos'
import type { OcorrenciaResponse } from '../../api/tipos'
import { Campo } from '../../componentes/Campo'
import { AvisoErro } from '../../componentes/Estado'
import { useErrosDeCampo } from '../../hooks/useErrosDeCampo'

const CAMPOS = ['acao', 'por'] as const

/** Lembra quem está operando entre um registro e o próximo. */
const CHAVE_RESPONSAVEL = 'hidrovigia:responsavel'

function responsavelLembrado(): string {
  try {
    return localStorage.getItem(CHAVE_RESPONSAVEL) ?? ''
  } catch {
    // Navegador com armazenamento bloqueado: o campo só começa vazio.
    return ''
  }
}

/**
 * Registra uma ação na ocorrência, ou a encerra.
 *
 * Os dois caminhos usam o mesmo corpo (`acao` e `por`) e devolvem a ocorrência
 * inteira atualizada, então a resposta alimenta o cache direto, sem refetch.
 *
 * Encerrar pede confirmação porque `RESOLVIDA` é terminal no backend: não
 * existe caminho para reabrir.
 */
export function FormularioTratativa({
  ocorrencia,
}: {
  ocorrencia: OcorrenciaResponse
}) {
  const clienteQuery = useQueryClient()
  const [acao, setAcao] = useState('')
  const [por, setPor] = useState(responsavelLembrado)
  const [confirmandoEncerrar, setConfirmandoEncerrar] = useState(false)

  const aoConcluir = (atualizada: OcorrenciaResponse) => {
    // A resposta já é o estado novo: aproveita em vez de buscar de novo.
    clienteQuery.setQueryData(chaves.ocorrencia(atualizada.id), atualizada)
    // A fila e o painel mudam junto (pendências, prazos vencidos).
    void clienteQuery.invalidateQueries({ queryKey: ['ocorrencias'] })
    void clienteQuery.invalidateQueries({ queryKey: ['painel'] })

    try {
      localStorage.setItem(CHAVE_RESPONSAVEL, por)
    } catch {
      // Sem armazenamento: apenas não lembra na próxima.
    }

    setAcao('')
    setConfirmandoEncerrar(false)
  }

  const tratativa = useMutation({
    mutationFn: () => registrarTratativa(ocorrencia.id, { acao, por }),
    onSuccess: aoConcluir,
  })

  const resolucao = useMutation({
    mutationFn: () => resolverOcorrencia(ocorrencia.id, { acao, por }),
    onSuccess: aoConcluir,
  })

  const enviando = tratativa.isPending || resolucao.isPending
  const erros = useErrosDeCampo(tratativa.error ?? resolucao.error, CAMPOS)

  if (ocorrencia.status === 'RESOLVIDA') {
    const ultima = ocorrencia.tratativas.at(-1)

    return (
      <p className="tratativa-encerrada">
        Ocorrência encerrada
        {ultima ? ` por ${ultima.por}` : ''}. Não há reabertura: a resolução é
        definitiva.
      </p>
    )
  }

  return (
    <form
      className="tratativa-form"
      onSubmit={(evento) => {
        evento.preventDefault()
        tratativa.mutate()
      }}
    >
      <h3>Registrar ação</h3>

      {erros.erro && <AvisoErro erro={erros.erro} extras={erros.gerais} />}

      <Campo
        name="acao"
        rotulo="O que foi feito"
        obrigatorio
        erro={erros.de('acao')}
        dica="Ex.: ponto isolado da rede e recloração do reservatório."
      >
        {(atributos) => (
          <textarea
            {...atributos}
            value={acao}
            onChange={(evento) => setAcao(evento.target.value)}
          />
        )}
      </Campo>

      <Campo name="por" rotulo="Responsável" obrigatorio erro={erros.de('por')}>
        {(atributos) => (
          <input
            {...atributos}
            type="text"
            value={por}
            onChange={(evento) => setPor(evento.target.value)}
          />
        )}
      </Campo>

      <div className="tratativa-form__acoes">
        <button type="submit" className="botao" disabled={enviando}>
          {tratativa.isPending ? 'Registrando…' : 'Registrar ação'}
        </button>

        {confirmandoEncerrar ? (
          <>
            <button
              type="button"
              className="botao botao--perigo"
              disabled={enviando}
              onClick={() => resolucao.mutate()}
            >
              {resolucao.isPending ? 'Encerrando…' : 'Confirmar encerramento'}
            </button>
            <button
              type="button"
              className="botao botao--secundario"
              onClick={() => setConfirmandoEncerrar(false)}
            >
              Cancelar
            </button>
          </>
        ) : (
          <button
            type="button"
            className="botao botao--secundario"
            disabled={enviando}
            onClick={() => setConfirmandoEncerrar(true)}
          >
            Encerrar ocorrência
          </button>
        )}
      </div>

      {confirmandoEncerrar && (
        <p className="tratativa-form__aviso" role="alert">
          Encerrar é definitivo — a ocorrência não pode ser reaberta.
        </p>
      )}
    </form>
  )
}
