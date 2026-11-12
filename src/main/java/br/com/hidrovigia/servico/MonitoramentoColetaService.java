package br.com.hidrovigia.servico;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.repositorio.AnaliseRepository;

import org.springframework.stereotype.Service;

/**
 * Alerta de coleta vencida: compara a ultima coleta de cada ponto ativo com a
 * frequencia minima do seu tipo de fonte.
 *
 * <p>A fila de ocorrencias so enxerga o que foi medido. Um ponto que deixou de
 * ser amostrado nao reprova analise nenhuma e some do radar — e exatamente o
 * caso que este servico traz de volta. Como o {@link PainelConformidadeService},
 * recebe o {@link Clock} por injecao para que "vencida" seja testavel.
 */
@Service
public class MonitoramentoColetaService {

    private final PontoMonitoramentoService pontoService;
    private final AnaliseRepository analiseRepositorio;
    private final Clock relogio;

    public MonitoramentoColetaService(PontoMonitoramentoService pontoService,
                                      AnaliseRepository analiseRepositorio,
                                      Clock relogio) {
        this.pontoService = pontoService;
        this.analiseRepositorio = analiseRepositorio;
        this.relogio = relogio;
    }

    /**
     * Situacao de coleta dos pontos ativos, vencidas primeiro e, entre elas,
     * as mais atrasadas no topo.
     */
    public List<SituacaoColeta> situacaoDosPontosAtivos() {
        Instant agora = Instant.now(relogio);
        return pontoService.listar(true).stream()
                .map(ponto -> situacaoDe(ponto, agora))
                .sorted(Comparator.comparing(SituacaoColeta::vencida).reversed()
                        .thenComparing(SituacaoColeta::proximaColetaAte,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    private SituacaoColeta situacaoDe(PontoMonitoramento ponto, Instant agora) {
        TipoFonte tipo = ponto.getTipoFonte();
        long frequenciaDias = tipo.getFrequenciaMinima().toDays();

        return analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc(ponto.getId())
                .map(Analise::getColetadoEm)
                .map(ultima -> {
                    Instant limite = tipo.proximaColetaAte(ultima);
                    return new SituacaoColeta(ponto.getCodigo(), ponto.getNome(), tipo,
                            frequenciaDias, ultima, limite, agora.isAfter(limite));
                })
                .orElseGet(() -> new SituacaoColeta(ponto.getCodigo(), ponto.getNome(), tipo,
                        frequenciaDias, null, null, true));
    }
}
