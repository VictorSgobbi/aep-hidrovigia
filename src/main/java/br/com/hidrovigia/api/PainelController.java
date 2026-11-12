package br.com.hidrovigia.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.dominio.parametro.RiscoSanitario;
import br.com.hidrovigia.servico.IndicadoresConformidade;
import br.com.hidrovigia.servico.MonitoramentoColetaService;
import br.com.hidrovigia.servico.PainelConformidadeService;
import br.com.hidrovigia.servico.SerieHistorica;
import br.com.hidrovigia.servico.SerieHistoricaService;
import br.com.hidrovigia.servico.SituacaoColeta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel")
@Tag(name = "4. Painel", description = "Indicadores de conformidade e catalogo da norma")
public class PainelController {

    private final PainelConformidadeService painel;
    private final MonitoramentoColetaService monitoramento;
    private final SerieHistoricaService serieHistorica;
    private final CatalogoParametros catalogo;

    public PainelController(PainelConformidadeService painel,
                            MonitoramentoColetaService monitoramento,
                            SerieHistoricaService serieHistorica,
                            CatalogoParametros catalogo) {
        this.painel = painel;
        this.monitoramento = monitoramento;
        this.serieHistorica = serieHistorica;
        this.catalogo = catalogo;
    }

    @GetMapping("/conformidade")
    @Operation(summary = "Consolidado de conformidade de todo o sistema")
    public IndicadoresConformidade geral() {
        return painel.geral();
    }

    @GetMapping("/conformidade/{codigoPonto}")
    @Operation(summary = "Consolidado de conformidade de um ponto")
    public IndicadoresConformidade porPonto(@PathVariable String codigoPonto) {
        return painel.porPonto(codigoPonto);
    }

    @GetMapping("/coletas")
    @Operation(summary = "Rotina de coleta dos pontos ativos, coletas vencidas primeiro")
    public List<SituacaoColeta> coletas() {
        return monitoramento.situacaoDosPontosAtivos();
    }

    @GetMapping("/serie/{codigoPonto}")
    @Operation(summary = "Serie historica diaria de um parametro em um ponto, calculada por agregacao")
    public SerieHistorica serie(
            @PathVariable String codigoPonto,
            @RequestParam String parametro,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim) {
        return serieHistorica.serie(codigoPonto, parametro, inicio, fim);
    }

    @GetMapping("/parametros")
    @Operation(summary = "Catalogo de parametros e limites aplicados pela PoC")
    public List<ParametroCatalogado> parametros() {
        return catalogo.todos().stream()
                .map(parametro -> new ParametroCatalogado(
                        parametro.getCodigo(),
                        parametro.getNome(),
                        parametro.getUnidade(),
                        parametro.descricaoDoLimite(),
                        parametro.limiteMinimo(),
                        parametro.limiteMaximo(),
                        parametro.getRisco(),
                        parametro.getReferenciaLegal()))
                .toList();
    }

    /** Descricao publica de um parametro do catalogo. */
    public record ParametroCatalogado(
            String codigo,
            String nome,
            String unidade,
            String limite,
            BigDecimal limiteMinimo,
            BigDecimal limiteMaximo,
            RiscoSanitario risco,
            String referenciaLegal) {
    }
}
