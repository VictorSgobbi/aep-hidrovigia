package br.com.hidrovigia.api.dto;

import java.time.Instant;

import br.com.hidrovigia.dominio.ponto.Localizacao;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.Responsavel;
import br.com.hidrovigia.dominio.ponto.TipoFonte;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Contratos de entrada e saida do cadastro de pontos de monitoramento.
 *
 * <p>Os DTOs ficam separados das entidades de proposito: o formato da API pode
 * mudar sem arrastar a modelagem das colecoes junto.
 */
public final class PontoDto {

    private PontoDto() {
    }

    @Schema(name = "LocalizacaoRequest")
    public record LocalizacaoEntrada(
            @NotBlank(message = "municipio e obrigatorio")
            String municipio,

            @NotBlank(message = "uf e obrigatoria")
            @Size(min = 2, max = 2, message = "uf deve ter duas letras")
            String uf,

            Double latitude,
            Double longitude) {

        public Localizacao paraDominio() {
            return new Localizacao(municipio, uf, latitude, longitude);
        }

        public static LocalizacaoEntrada de(Localizacao localizacao) {
            return new LocalizacaoEntrada(localizacao.municipio(), localizacao.uf(),
                    localizacao.latitude(), localizacao.longitude());
        }
    }

    @Schema(name = "ResponsavelRequest")
    public record ResponsavelEntrada(
            @NotBlank(message = "nome do responsavel e obrigatorio")
            String nome,

            String registro,

            @NotBlank(message = "contato do responsavel e obrigatorio")
            String contato) {

        public Responsavel paraDominio() {
            return new Responsavel(nome, registro, contato);
        }

        public static ResponsavelEntrada de(Responsavel responsavel) {
            return new ResponsavelEntrada(responsavel.nome(), responsavel.registro(),
                    responsavel.contato());
        }
    }

    @Schema(name = "PontoRequest", description = "Dados para cadastrar ou atualizar um ponto")
    public record Entrada(
            @NotBlank(message = "codigo do ponto e obrigatorio")
            @Schema(example = "PMA-001")
            String codigo,

            @NotBlank(message = "nome do ponto e obrigatorio")
            @Schema(example = "Poco da Escola Rural Sao Jose")
            String nome,

            @NotNull(message = "tipo de fonte e obrigatorio")
            TipoFonte tipoFonte,

            @PositiveOrZero(message = "populacao atendida nao pode ser negativa")
            @Schema(example = "320")
            int populacaoAtendida,

            @NotNull(message = "localizacao e obrigatoria")
            @Valid
            LocalizacaoEntrada localizacao,

            @NotNull(message = "responsavel e obrigatorio")
            @Valid
            ResponsavelEntrada responsavel) {
    }

    @Schema(name = "PontoResponse")
    public record Saida(
            String id,
            String codigo,
            String nome,
            TipoFonte tipoFonte,
            String descricaoFonte,
            int populacaoAtendida,
            LocalizacaoEntrada localizacao,
            ResponsavelEntrada responsavel,
            boolean ativo,
            Instant criadoEm) {

        public static Saida de(PontoMonitoramento ponto) {
            return new Saida(
                    ponto.getId(),
                    ponto.getCodigo(),
                    ponto.getNome(),
                    ponto.getTipoFonte(),
                    ponto.getTipoFonte().getDescricao(),
                    ponto.getPopulacaoAtendida(),
                    LocalizacaoEntrada.de(ponto.getLocalizacao()),
                    ResponsavelEntrada.de(ponto.getResponsavel()),
                    ponto.isAtivo(),
                    ponto.getCriadoEm());
        }
    }
}
