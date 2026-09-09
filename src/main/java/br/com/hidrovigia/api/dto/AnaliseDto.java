package br.com.hidrovigia.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.analise.LeituraParametro;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.parametro.RiscoSanitario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Contratos de entrada e saida do registro de analises.
 */
public final class AnaliseDto {

    private AnaliseDto() {
    }

    @Schema(name = "LeituraRequest", description = "Uma medicao de campo ou laboratorio")
    public record LeituraEntrada(
            @NotBlank(message = "codigo do parametro e obrigatorio")
            @Schema(example = "CRL", description = "Codigo do parametro no catalogo")
            String codigo,

            @NotNull(message = "valor medido e obrigatorio")
            @Schema(example = "0.8")
            BigDecimal valor) {

        public LeituraParametro paraDominio() {
            return new LeituraParametro(codigo, valor);
        }
    }

    @Schema(name = "AnaliseRequest")
    public record Entrada(
            @NotBlank(message = "codigo do ponto e obrigatorio")
            @Schema(example = "PMA-001")
            String codigoPonto,

            @NotBlank(message = "identificacao do coletor e obrigatoria")
            @Schema(example = "Tecnico Bruno")
            String coletor,

            @NotNull(message = "data da coleta e obrigatoria")
            Instant coletadoEm,

            @NotEmpty(message = "informe ao menos um parametro medido")
            @Valid
            List<LeituraEntrada> leituras) {

        public List<LeituraParametro> leiturasDominio() {
            return leituras.stream().map(LeituraEntrada::paraDominio).toList();
        }
    }

    @Schema(name = "ParametroAvaliado")
    public record ParametroSaida(
            String codigo,
            String nome,
            BigDecimal valor,
            String unidade,
            BigDecimal limiteMinimo,
            BigDecimal limiteMaximo,
            boolean conforme,
            String mensagem,
            RiscoSanitario risco,
            String referenciaLegal) {

        public static ParametroSaida de(ResultadoParametro resultado) {
            return new ParametroSaida(
                    resultado.codigo(), resultado.nome(), resultado.valor(), resultado.unidade(),
                    resultado.limiteMinimo(), resultado.limiteMaximo(), resultado.conforme(),
                    resultado.mensagem(), resultado.risco(), resultado.referenciaLegal());
        }
    }

    @Schema(name = "AnaliseResponse")
    public record Saida(
            String id,
            String pontoId,
            String pontoCodigo,
            Instant coletadoEm,
            String coletor,
            boolean conforme,
            int qtdParametros,
            int qtdNaoConformidades,
            double percentualConformidade,
            List<ParametroSaida> parametros,
            Instant registradoEm) {

        public static Saida de(Analise analise) {
            return new Saida(
                    analise.getId(),
                    analise.getPontoId(),
                    analise.getPontoCodigo(),
                    analise.getColetadoEm(),
                    analise.getColetor(),
                    analise.conforme(),
                    analise.getResultado().qtdParametros(),
                    analise.getResultado().qtdNaoConformidades(),
                    analise.getResultado().percentualConformidade(),
                    analise.getParametros().stream().map(ParametroSaida::de).toList(),
                    analise.getRegistradoEm());
        }
    }
}
