import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { chaves } from '../../api/chaves'
import {
  desativarPonto,
  listarPontos,
  reativarPonto,
} from '../../api/recursos'
import type { PontoResponse } from '../../api/tipos'
import { AvisoErro, Carregando, Vazio } from '../../componentes/Estado'
import { Selo } from '../../componentes/Selo'
import { formatarNumero } from '../../dominio/formatos'
import { ROTULO_TIPO_FONTE, rotular } from '../../dominio/rotulos'
import { FormularioPonto } from './FormularioPonto'
import './PontosPage.css'

export function PontosPage() {
  const clienteQuery = useQueryClient()
  const [parametros, setParametros] = useSearchParams()
  const [municipio, setMunicipio] = useState('')
  const [apenasAtivos, setApenasAtivos] = useState(false)
  const [emEdicao, setEmEdicao] = useState<PontoResponse | null>(null)
  const [criando, setCriando] = useState(false)

  const filtroMunicipio = municipio.trim()
  // Quando `municipio` vem preenchido, o backend IGNORA apenasAtivos. A tela
  // reflete isso desabilitando a caixa, em vez de fingir que os dois valem.
  const filtros = filtroMunicipio
    ? { municipio: filtroMunicipio }
    : { apenasAtivos }

  const consulta = useQuery({
    queryKey: chaves.pontos(filtros),
    queryFn: () => listarPontos(filtros),
  })

  const aoMudarSituacao = (ponto: PontoResponse) => {
    // As duas respostas trazem o ponto já atualizado: aproveita em vez de
    // buscar de novo. O DELETE responde 200 com corpo, não 204.
    clienteQuery.setQueryData(chaves.ponto(ponto.id), ponto)
    void clienteQuery.invalidateQueries({ queryKey: ['pontos'] })
  }

  const desativar = useMutation({
    mutationFn: desativarPonto,
    onSuccess: aoMudarSituacao,
  })

  const reativar = useMutation({
    mutationFn: reativarPonto,
    onSuccess: aoMudarSituacao,
  })

  const erroDeAcao = desativar.error ?? reativar.error
  const selecionado = parametros.get('ponto')

  return (
    <>
      <div className="secao__cabecalho">
        <h1>Pontos de monitoramento</h1>
        <button
          type="button"
          className="botao"
          onClick={() => {
            setCriando(true)
            setEmEdicao(null)
          }}
        >
          Novo ponto
        </button>
      </div>

      <div className="pontos-filtros">
        <label className="campo">
          <span className="campo__rotulo">Município</span>
          <input
            type="search"
            value={municipio}
            placeholder="Maringá"
            onChange={(evento) => setMunicipio(evento.target.value)}
          />
        </label>

        <label className="pontos-filtros__ativos">
          <input
            type="checkbox"
            checked={apenasAtivos}
            disabled={filtroMunicipio !== ''}
            onChange={(evento) => setApenasAtivos(evento.target.checked)}
          />
          <span>
            Apenas ativos
            {filtroMunicipio !== '' && (
              <span className="campo__dica">
                O filtro por município retorna ativos e inativos.
              </span>
            )}
          </span>
        </label>
      </div>

      {erroDeAcao && <AvisoErro erro={erroDeAcao} />}

      {consulta.isPending && <Carregando>Carregando pontos…</Carregando>}

      {consulta.isError && (
        <AvisoErro
          erro={consulta.error}
          aoTentarNovamente={() => void consulta.refetch()}
        />
      )}

      {consulta.data &&
        (consulta.data.length === 0 ? (
          <Vazio
            acao={
              filtroMunicipio ? (
                <button
                  type="button"
                  className="botao botao--secundario"
                  onClick={() => setMunicipio('')}
                >
                  Limpar filtro
                </button>
              ) : (
                <button
                  type="button"
                  className="botao"
                  onClick={() => setCriando(true)}
                >
                  Cadastrar o primeiro ponto
                </button>
              )
            }
          >
            {filtroMunicipio
              ? `Nenhum ponto em «${filtroMunicipio}».`
              : 'Nenhum ponto cadastrado. O sistema começa pelo cadastro do ponto de coleta.'}
          </Vazio>
        ) : (
          <div className="tabela--rolavel">
            <table className="tabela">
              <caption>
                {consulta.data.length} ponto(s) no recorte selecionado
              </caption>
              <thead>
                <tr>
                  <th scope="col">Código</th>
                  <th scope="col">Nome</th>
                  <th scope="col">Fonte</th>
                  <th scope="col">Local</th>
                  <th scope="col">População</th>
                  <th scope="col">Situação</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                {consulta.data.map((ponto) => (
                  <tr
                    key={ponto.id}
                    aria-current={ponto.id === selecionado ? 'true' : undefined}
                  >
                    <th scope="row" data-rotulo="Código">
                      {ponto.codigo}
                    </th>
                    <td data-rotulo="Nome">{ponto.nome}</td>
                    <td data-rotulo="Fonte">
                      {/* O descricaoFonte do servidor vem sem acento: usamos o
                          rotulo proprio. */}
                      {rotular(ROTULO_TIPO_FONTE, ponto.tipoFonte)}
                    </td>
                    <td data-rotulo="Local">
                      {ponto.localizacao.municipio}/{ponto.localizacao.uf}
                    </td>
                    <td data-rotulo="População" className="numerico">
                      {formatarNumero(ponto.populacaoAtendida)}
                    </td>
                    <td data-rotulo="Situação">
                      <Selo tom={ponto.ativo ? 'conforme' : 'neutro'}>
                        {ponto.ativo ? 'Ativo' : 'Desativado'}
                      </Selo>
                    </td>
                    <td data-rotulo="Ações">
                      <div className="pontos-acoes">
                        <Link to={`/coletas?ponto=${ponto.codigo}`}>
                          Coletar
                        </Link>

                        <button
                          type="button"
                          className="pontos-acoes__link"
                          onClick={() => {
                            setEmEdicao(ponto)
                            setCriando(false)
                          }}
                        >
                          Editar
                        </button>

                        {/*
                          Só a ação possível aparece. O 409 "ja esta
                          desativado" fica inalcançável pela interface.
                        */}
                        {ponto.ativo ? (
                          <button
                            type="button"
                            className="pontos-acoes__link pontos-acoes__link--perigo"
                            disabled={desativar.isPending}
                            onClick={() => desativar.mutate(ponto.id)}
                          >
                            Desativar
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="pontos-acoes__link"
                            disabled={reativar.isPending}
                            onClick={() => reativar.mutate(ponto.id)}
                          >
                            Reativar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}

      {(criando || emEdicao) && (
        <div className="modal" role="dialog" aria-modal="true" aria-label="Ponto de monitoramento">
          <div className="modal__conteudo">
            <FormularioPonto
              {...(emEdicao ? { ponto: emEdicao } : {})}
              aoFechar={() => {
                setCriando(false)
                setEmEdicao(null)
                // Fecha também a ficha, se estava aberta pela URL.
                if (selecionado) {
                  const proximos = new URLSearchParams(parametros)
                  proximos.delete('ponto')
                  setParametros(proximos)
                }
              }}
            />
          </div>
        </div>
      )}
    </>
  )
}
