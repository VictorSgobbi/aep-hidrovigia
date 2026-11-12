package br.com.hidrovigia.servico;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.dominio.parametro.ParametroPotabilidade;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.repositorio.AnaliseRepository;

import org.springframework.stereotype.Service;

/**
 * Serie historica de um parametro em um ponto de monitoramento.
 *
 * <p>Valida o pedido contra o catalogo e o cadastro, resolve o periodo e
 * delega o calculo a agregacao do MongoDB. Sem periodo informado, devolve os
 * ultimos {@link #PERIODO_PADRAO} contados pelo {@link Clock} injetado.
 */
@Service
public class SerieHistoricaService {

    static final Duration PERIODO_PADRAO = Duration.ofDays(90);

    private final PontoMonitoramentoService pontoService;
    private final AnaliseRepository analiseRepositorio;
    private final CatalogoParametros catalogo;
    private final Clock relogio;

    public SerieHistoricaService(PontoMonitoramentoService pontoService,
                                 AnaliseRepository analiseRepositorio,
                                 CatalogoParametros catalogo,
                                 Clock relogio) {
        this.pontoService = pontoService;
        this.analiseRepositorio = analiseRepositorio;
        this.catalogo = catalogo;
        this.relogio = relogio;
    }

    /**
     * @param inicio opcional; padrao: {@code fim} menos {@link #PERIODO_PADRAO}
     * @param fim    opcional; padrao: agora
     * @throws br.com.hidrovigia.dominio.parametro.ParametroDesconhecidoException
     *         se o parametro nao estiver no catalogo
     * @throws br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException
     *         se o ponto nao existir
     * @throws IllegalArgumentException se o inicio for posterior ao fim
     */
    public SerieHistorica serie(String codigoPonto, String codigoParametro,
                                Instant inicio, Instant fim) {
        ParametroPotabilidade parametro = catalogo.buscar(codigoParametro);
        PontoMonitoramento ponto = pontoService.buscarPorCodigo(codigoPonto);

        Instant ate = fim == null ? Instant.now(relogio) : fim;
        Instant desde = inicio == null ? ate.minus(PERIODO_PADRAO) : inicio;
        if (desde.isAfter(ate)) {
            throw new IllegalArgumentException("inicio do periodo posterior ao fim");
        }

        return new SerieHistorica(ponto.getCodigo(), parametro.getCodigo(), parametro.getNome(),
                parametro.getUnidade(), parametro.limiteMinimo(), parametro.limiteMaximo(),
                desde, ate,
                analiseRepositorio.serieDiaria(ponto.getId(), parametro.getCodigo(), desde, ate));
    }
}
