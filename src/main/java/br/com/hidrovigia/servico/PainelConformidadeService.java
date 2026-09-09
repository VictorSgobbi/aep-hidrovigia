package br.com.hidrovigia.servico;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.repositorio.AnaliseRepository;
import br.com.hidrovigia.repositorio.OcorrenciaRepository;

import org.springframework.stereotype.Service;

/**
 * Indicadores de conformidade por ponto e no conjunto do sistema.
 *
 * <p>Recebe um {@link Clock} por injecao em vez de chamar {@code Instant.now()}
 * direto: o calculo de ocorrencias vencidas depende do instante atual, e sem
 * relogio injetavel esse trecho seria intestavel.
 */
@Service
public class PainelConformidadeService {

    private final AnaliseRepository analiseRepositorio;
    private final OcorrenciaRepository ocorrenciaRepositorio;
    private final PontoMonitoramentoService pontoService;
    private final Clock relogio;

    public PainelConformidadeService(AnaliseRepository analiseRepositorio,
                                     OcorrenciaRepository ocorrenciaRepositorio,
                                     PontoMonitoramentoService pontoService,
                                     Clock relogio) {
        this.analiseRepositorio = analiseRepositorio;
        this.ocorrenciaRepositorio = ocorrenciaRepositorio;
        this.pontoService = pontoService;
        this.relogio = relogio;
    }

    /** Consolidado de todo o sistema. */
    public IndicadoresConformidade geral() {
        long conformes = analiseRepositorio.countByResultadoConforme(true);
        long naoConformes = analiseRepositorio.countByResultadoConforme(false);
        List<Ocorrencia> pendentes =
                ocorrenciaRepositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA);

        return IndicadoresConformidade.de("geral", conformes, naoConformes,
                pendentes.size(), contarVencidas(pendentes));
    }

    /** Consolidado de um unico ponto de monitoramento. */
    public IndicadoresConformidade porPonto(String codigoPonto) {
        PontoMonitoramento ponto = pontoService.buscarPorCodigo(codigoPonto);

        long conformes = analiseRepositorio.countByPontoIdAndResultadoConforme(ponto.getId(), true);
        long naoConformes = analiseRepositorio.countByPontoIdAndResultadoConforme(ponto.getId(), false);

        List<Ocorrencia> pendentes = ocorrenciaRepositorio
                .findByPontoIdOrderByAbertaEmDesc(ponto.getId()).stream()
                .filter(ocorrencia -> !ocorrencia.getStatus().finalizada())
                .toList();

        return IndicadoresConformidade.de(ponto.getCodigo(), conformes, naoConformes,
                pendentes.size(), contarVencidas(pendentes));
    }

    private long contarVencidas(List<Ocorrencia> pendentes) {
        Instant agora = Instant.now(relogio);
        return pendentes.stream().filter(ocorrencia -> ocorrencia.estaVencida(agora)).count();
    }
}
