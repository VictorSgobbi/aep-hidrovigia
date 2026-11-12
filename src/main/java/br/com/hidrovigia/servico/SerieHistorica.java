package br.com.hidrovigia.servico;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.repositorio.LeituraDiaria;

/**
 * Serie historica de um parametro em um ponto, com o limite da norma ao lado
 * para o grafico desenhar a faixa permitida.
 *
 * <p>O limite e o do catalogo em vigor. Cada leitura ja traz, em
 * {@code foraDoLimite}, o veredito dado com o limite gravado na analise — que e
 * o que vale se a norma tiver mudado no meio do periodo.
 */
public record SerieHistorica(
        String codigoPonto,
        String codigoParametro,
        String nomeParametro,
        String unidade,
        BigDecimal limiteMinimo,
        BigDecimal limiteMaximo,
        Instant inicio,
        Instant fim,
        List<LeituraDiaria> dias) {
}
