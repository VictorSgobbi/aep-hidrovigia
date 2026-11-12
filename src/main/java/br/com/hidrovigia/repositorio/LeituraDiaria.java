package br.com.hidrovigia.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um dia da serie historica de um parametro em um ponto: o resumo de todas as
 * leituras daquele parametro coletadas no dia (UTC).
 *
 * <p>{@code foraDoLimite} conta as leituras reprovadas no dia — o grafico
 * marca o dia mesmo quando a media ficou dentro do padrao.
 */
public record LeituraDiaria(
        LocalDate dia,
        BigDecimal minimo,
        BigDecimal media,
        BigDecimal maximo,
        long leituras,
        long foraDoLimite) {
}
