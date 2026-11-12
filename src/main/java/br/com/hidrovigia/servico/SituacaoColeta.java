package br.com.hidrovigia.servico;

import java.time.Instant;

import br.com.hidrovigia.dominio.ponto.TipoFonte;

/**
 * Em dia ou atrasada: a rotina de coleta de um ponto frente a frequencia
 * minima do seu tipo de fonte.
 *
 * <p>{@code ultimaColeta} e {@code proximaColetaAte} sao nulos quando o ponto
 * nunca teve coleta — e um ponto assim conta como vencido, porque ninguem sabe
 * se a agua dele e potavel.
 */
public record SituacaoColeta(
        String codigoPonto,
        String nomePonto,
        TipoFonte tipoFonte,
        long frequenciaMinimaDias,
        Instant ultimaColeta,
        Instant proximaColetaAte,
        boolean vencida) {
}
