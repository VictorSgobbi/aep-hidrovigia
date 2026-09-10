import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { atualizarPonto, cadastrarPonto } from '../../api/recursos'
import type { PontoRequest, PontoResponse, TipoFonte } from '../../api/tipos'
import { Campo } from '../../componentes/Campo'
import { AvisoErro } from '../../componentes/Estado'
import { ROTULO_TIPO_FONTE } from '../../dominio/rotulos'
import { useErrosDeCampo } from '../../hooks/useErrosDeCampo'

const CAMPOS = [
  'codigo',
  'nome',
  'tipoFonte',
  'populacaoAtendida',
  'localizacao.municipio',
  'localizacao.uf',
  'localizacao.latitude',
  'localizacao.longitude',
  'responsavel.nome',
  'responsavel.registro',
  'responsavel.contato',
] as const

interface Estado {
  codigo: string
  nome: string
  tipoFonte: TipoFonte
  populacaoAtendida: string
  municipio: string
  uf: string
  latitude: string
  longitude: string
  responsavelNome: string
  responsavelRegistro: string
  responsavelContato: string
}

function estadoInicial(ponto?: PontoResponse): Estado {
  return {
    codigo: ponto?.codigo ?? '',
    nome: ponto?.nome ?? '',
    tipoFonte: ponto?.tipoFonte ?? 'POCO_ARTESIANO',
    populacaoAtendida: String(ponto?.populacaoAtendida ?? ''),
    municipio: ponto?.localizacao.municipio ?? '',
    uf: ponto?.localizacao.uf ?? '',
    latitude: ponto?.localizacao.latitude?.toString() ?? '',
    longitude: ponto?.localizacao.longitude?.toString() ?? '',
    responsavelNome: ponto?.responsavel.nome ?? '',
    responsavelRegistro: ponto?.responsavel.registro ?? '',
    responsavelContato: ponto?.responsavel.contato ?? '',
  }
}

function paraRequest(estado: Estado): PontoRequest {
  const numero = (texto: string) =>
    texto.trim() === '' ? undefined : Number(texto)

  return {
    codigo: estado.codigo,
    nome: estado.nome,
    tipoFonte: estado.tipoFonte,
    populacaoAtendida: Number(estado.populacaoAtendida || 0),
    localizacao: {
      municipio: estado.municipio,
      uf: estado.uf.toUpperCase(),
      ...(numero(estado.latitude) !== undefined
        ? { latitude: numero(estado.latitude) }
        : {}),
      ...(numero(estado.longitude) !== undefined
        ? { longitude: numero(estado.longitude) }
        : {}),
    },
    responsavel: {
      nome: estado.responsavelNome,
      ...(estado.responsavelRegistro.trim() === ''
        ? {}
        : { registro: estado.responsavelRegistro }),
      contato: estado.responsavelContato,
    },
  }
}

/**
 * Cadastro e edição de um ponto de monitoramento.
 *
 * Na edição o `codigo` fica somente leitura e **continua sendo enviado**: o
 * backend exige o campo (`@NotBlank`) e o ignora, porque o código é imutável.
 * Não remova o campo do corpo "porque não é usado" — sem ele a validação
 * reprova com 400.
 */
export function FormularioPonto({
  ponto,
  aoFechar,
}: {
  ponto?: PontoResponse
  aoFechar: () => void
}) {
  const clienteQuery = useQueryClient()
  const [estado, setEstado] = useState(() => estadoInicial(ponto))
  const editando = ponto !== undefined

  const salvar = useMutation({
    mutationFn: () =>
      editando
        ? atualizarPonto(ponto.id, paraRequest(estado))
        : cadastrarPonto(paraRequest(estado)),
    onSuccess: (salvo) => {
      clienteQuery.setQueryData(['pontos', 'id', salvo.id], salvo)
      void clienteQuery.invalidateQueries({ queryKey: ['pontos'] })
      void clienteQuery.invalidateQueries({ queryKey: ['painel'] })
      aoFechar()
    },
  })

  const erros = useErrosDeCampo(salvar.error, CAMPOS)

  // O 409 de codigo duplicado nao vem em `detalhes`, entao a mensagem e
  // copiada para o campo a que ela se refere.
  const erroCodigo =
    erros.de('codigo') ??
    (erros.erro?.status === 409 ? erros.erro.message : undefined)

  const campo = (chave: keyof Estado) => ({
    value: estado[chave],
    onChange: (
      evento: React.ChangeEvent<
        HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
      >,
    ) => setEstado((atual) => ({ ...atual, [chave]: evento.target.value })),
  })

  return (
    <form
      className="ponto-form"
      onSubmit={(evento) => {
        evento.preventDefault()
        salvar.mutate()
      }}
    >
      <h2>{editando ? `Editar ${ponto.codigo}` : 'Novo ponto'}</h2>

      {erros.erro && erros.erro.status !== 409 && (
        <AvisoErro erro={erros.erro} extras={erros.gerais} />
      )}

      <Campo
        name="codigo"
        rotulo="Código"
        obrigatorio
        erro={erroCodigo}
        dica={
          editando
            ? 'O código identifica o ponto e não pode mudar.'
            : 'Ex.: PMA-001. É convertido para maiúsculas.'
        }
      >
        {(atributos) => (
          <input
            {...atributos}
            type="text"
            readOnly={editando}
            aria-readonly={editando || undefined}
            {...campo('codigo')}
          />
        )}
      </Campo>

      <Campo
        name="nome"
        rotulo="Nome do ponto"
        obrigatorio
        erro={erros.de('nome')}
      >
        {(atributos) => <input {...atributos} type="text" {...campo('nome')} />}
      </Campo>

      <Campo
        name="tipoFonte"
        rotulo="Tipo de fonte"
        obrigatorio
        erro={erros.de('tipoFonte')}
      >
        {(atributos) => (
          <select {...atributos} {...campo('tipoFonte')}>
            {Object.entries(ROTULO_TIPO_FONTE).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>
                {rotulo}
              </option>
            ))}
          </select>
        )}
      </Campo>

      <Campo
        name="populacaoAtendida"
        rotulo="População atendida"
        obrigatorio
        erro={erros.de('populacaoAtendida')}
      >
        {(atributos) => (
          <input
            {...atributos}
            type="number"
            min={0}
            step={1}
            {...campo('populacaoAtendida')}
          />
        )}
      </Campo>

      <fieldset className="ponto-form__grupo">
        <legend>Localização</legend>

        <Campo
          name="localizacao.municipio"
          rotulo="Município"
          obrigatorio
          erro={erros.de('localizacao.municipio')}
        >
          {(atributos) => (
            <input {...atributos} type="text" {...campo('municipio')} />
          )}
        </Campo>

        <Campo
          name="localizacao.uf"
          rotulo="UF"
          obrigatorio
          erro={erros.de('localizacao.uf')}
          dica="Duas letras."
        >
          {(atributos) => (
            <input
              {...atributos}
              type="text"
              maxLength={2}
              pattern="[A-Za-z]{2}"
              {...campo('uf')}
            />
          )}
        </Campo>

        {/*
          Latitude e longitude sao opcionais, mas fora do intervalo o backend
          responde 400 com IllegalArgumentException, sem detalhe por campo. Os
          limites do input evitam gastar esse erro.
        */}
        <Campo
          name="localizacao.latitude"
          rotulo="Latitude"
          erro={erros.de('localizacao.latitude')}
        >
          {(atributos) => (
            <input
              {...atributos}
              type="number"
              min={-90}
              max={90}
              step="any"
              {...campo('latitude')}
            />
          )}
        </Campo>

        <Campo
          name="localizacao.longitude"
          rotulo="Longitude"
          erro={erros.de('localizacao.longitude')}
        >
          {(atributos) => (
            <input
              {...atributos}
              type="number"
              min={-180}
              max={180}
              step="any"
              {...campo('longitude')}
            />
          )}
        </Campo>
      </fieldset>

      <fieldset className="ponto-form__grupo">
        <legend>Responsável</legend>

        <Campo
          name="responsavel.nome"
          rotulo="Nome do responsável"
          obrigatorio
          erro={erros.de('responsavel.nome')}
        >
          {(atributos) => (
            <input {...atributos} type="text" {...campo('responsavelNome')} />
          )}
        </Campo>

        <Campo
          name="responsavel.registro"
          rotulo="Registro profissional"
          erro={erros.de('responsavel.registro')}
          dica="Opcional. Ex.: CREA-PR 123456."
        >
          {(atributos) => (
            <input
              {...atributos}
              type="text"
              {...campo('responsavelRegistro')}
            />
          )}
        </Campo>

        <Campo
          name="responsavel.contato"
          rotulo="Contato"
          obrigatorio
          erro={erros.de('responsavel.contato')}
        >
          {(atributos) => (
            <input
              {...atributos}
              type="text"
              {...campo('responsavelContato')}
            />
          )}
        </Campo>
      </fieldset>

      <div className="ponto-form__acoes">
        <button type="submit" className="botao" disabled={salvar.isPending}>
          {salvar.isPending ? 'Salvando…' : 'Salvar'}
        </button>
        <button
          type="button"
          className="botao botao--secundario"
          onClick={aoFechar}
        >
          Cancelar
        </button>
      </div>
    </form>
  )
}
