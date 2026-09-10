package br.com.hidrovigia.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.dominio.gravidade.Gravidade;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.ParametroViolado;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.dominio.ocorrencia.Tratativa;
import br.com.hidrovigia.dominio.parametro.RiscoSanitario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Contratos de entrada e saida da fila de ocorrencias.
 */
public final class OcorrenciaDto {

    private OcorrenciaDto() {
    }

    @Schema(name = "TratativaRequest")
    public record TratativaEntrada(
            @NotBlank(message = "descricao da acao e obrigatoria")
            @Schema(example = "Recloracao do reservatorio executada")
            String acao,

            @NotBlank(message = "responsavel pela acao e obrigatorio")
            @Schema(example = "Leonardo")
            String por) {
    }

    @Schema(name = "ParametroVioladoResponse")
    public record ParametroVioladoSaida(
            String codigo,
            String nome,
            BigDecimal valorMedido,
            String unidade,
            String limiteAplicado,
            RiscoSanitario risco,
            String mensagem) {

        public static ParametroVioladoSaida de(ParametroViolado violado) {
            return new ParametroVioladoSaida(violado.codigo(), violado.nome(),
                    violado.valorMedido(), violado.unidade(), violado.limiteAplicado(),
                    violado.risco(), violado.mensagem());
        }
    }

    @Schema(name = "TratativaResponse")
    public record TratativaSaida(String acao, String por, Instant em, StatusOcorrencia statusResultante) {

        public static TratativaSaida de(Tratativa tratativa) {
            return new TratativaSaida(tratativa.acao(), tratativa.por(), tratativa.em(),
                    tratativa.statusResultante());
        }
    }

    @Schema(name = "OcorrenciaResponse")
    public record Saida(
            String id,
            String analiseId,
            String pontoId,
            String pontoCodigo,
            Gravidade gravidade,
            int prazoHoras,
            String politicaClassificacao,
            StatusOcorrencia status,
            Instant abertaEm,
            Instant prazoLimite,
            boolean vencida,
            List<ParametroVioladoSaida> parametrosViolados,
            List<TratativaSaida> tratativas) {

        /**
         * @param agora instante de referencia para decidir se o prazo venceu,
         *              vindo do {@code Clock} injetado — e nao de
         *              {@code Instant.now()} escondido aqui, que tornaria o
         *              campo {@code vencida} impossivel de testar
         */
        public static Saida de(Ocorrencia ocorrencia, Instant agora) {
            return new Saida(
                    ocorrencia.getId(),
                    ocorrencia.getAnaliseId(),
                    ocorrencia.getPontoId(),
                    ocorrencia.getPontoCodigo(),
                    ocorrencia.getGravidade(),
                    ocorrencia.getGravidade().getPrazoHoras(),
                    ocorrencia.getPoliticaClassificacao(),
                    ocorrencia.getStatus(),
                    ocorrencia.getAbertaEm(),
                    ocorrencia.getPrazoLimite(),
                    ocorrencia.estaVencida(agora),
                    ocorrencia.getParametrosViolados().stream()
                            .map(ParametroVioladoSaida::de).toList(),
                    ocorrencia.getTratativas().stream().map(TratativaSaida::de).toList());
        }
    }
}
