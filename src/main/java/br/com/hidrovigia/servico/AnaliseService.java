package br.com.hidrovigia.servico;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.analise.LeituraParametro;
import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.repositorio.AnaliseRepository;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.springframework.stereotype.Service;

/**
 * Caso de uso central da PoC: registrar uma coleta e reagir ao resultado.
 *
 * <p>O fluxo completo em um metodo: localiza o ponto, confronta cada leitura
 * com o catalogo da norma, consolida a analise e — se algum parametro reprovar
 * — abre a ocorrencia com prazo de resposta. E esse encadeamento que transforma
 * "planilha de coleta" em vigilancia.
 */
@Service
public class AnaliseService {

    private final AnaliseRepository repositorio;
    private final PontoMonitoramentoService pontoService;
    private final OcorrenciaService ocorrenciaService;
    private final CatalogoParametros catalogo;

    public AnaliseService(AnaliseRepository repositorio,
                          PontoMonitoramentoService pontoService,
                          OcorrenciaService ocorrenciaService,
                          CatalogoParametros catalogo) {
        this.repositorio = repositorio;
        this.pontoService = pontoService;
        this.ocorrenciaService = ocorrenciaService;
        this.catalogo = catalogo;
    }

    /**
     * Registra uma analise e abre ocorrencia quando houver nao-conformidade.
     *
     * @param codigoPonto codigo do ponto de coleta
     * @param coletor     quem realizou a coleta
     * @param coletadoEm  instante da coleta
     * @param leituras    medicoes brutas, na ordem em que devem ser gravadas
     * @throws RecursoNaoEncontradoException se o ponto nao existir
     * @throws RegraNegocioException         se o ponto estiver desativado ou a
     *                                       lista de leituras vier vazia ou com
     *                                       codigo repetido
     */
    public Analise registrar(String codigoPonto,
                             String coletor,
                             Instant coletadoEm,
                             List<LeituraParametro> leituras) {
        PontoMonitoramento ponto = pontoService.buscarPorCodigo(codigoPonto);

        if (!ponto.isAtivo()) {
            throw new RegraNegocioException(
                    "Ponto " + ponto.getCodigo() + " esta desativado e nao aceita novas coletas");
        }
        if (leituras == null || leituras.isEmpty()) {
            throw new RegraNegocioException("Informe ao menos um parametro medido");
        }

        List<ResultadoParametro> avaliados = avaliar(leituras);

        Analise analise;
        try {
            analise = repositorio.save(Analise.registrar(ponto, coletor, coletadoEm, avaliados));
        } catch (IllegalArgumentException erro) {
            throw new RegraNegocioException(erro.getMessage());
        }

        if (!analise.conforme()) {
            ocorrenciaService.abrirPara(analise);
        }
        return analise;
    }

    private List<ResultadoParametro> avaliar(List<LeituraParametro> leituras) {
        List<String> jaMedidos = new ArrayList<>();
        List<ResultadoParametro> avaliados = new ArrayList<>();

        for (LeituraParametro leitura : leituras) {
            if (jaMedidos.contains(leitura.codigo())) {
                throw new RegraNegocioException(
                        "Parametro informado duas vezes na mesma analise: " + leitura.codigo());
            }
            jaMedidos.add(leitura.codigo());

            try {
                avaliados.add(catalogo.buscar(leitura.codigo()).avaliar(leitura.valor()));
            } catch (IllegalArgumentException erro) {
                throw new RegraNegocioException(erro.getMessage());
            }
        }
        return avaliados;
    }

    public Analise buscarPorId(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.analise(id));
    }

    public List<Analise> listarPorPonto(String codigoPonto) {
        PontoMonitoramento ponto = pontoService.buscarPorCodigo(codigoPonto);
        return repositorio.findByPontoIdOrderByColetadoEmDesc(ponto.getId());
    }

    public List<Analise> listarPorPeriodo(Instant inicio, Instant fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe inicio e fim do periodo");
        }
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("Inicio do periodo nao pode ser posterior ao fim");
        }
        return repositorio.findByColetadoEmBetweenOrderByColetadoEmDesc(inicio, fim);
    }
}
