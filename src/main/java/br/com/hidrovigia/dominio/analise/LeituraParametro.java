package br.com.hidrovigia.dominio.analise;

import java.math.BigDecimal;

/**
 * Uma medicao bruta informada pelo tecnico, antes de ser confrontada com a norma.
 *
 * <p>E a entrada do registro de analise. Vira {@code ResultadoParametro} depois
 * que o catalogo aplica o limite legal correspondente ao codigo.
 *
 * <p>A ordem em que as leituras chegam e preservada na analise gravada, entao a
 * entrada e uma lista e nao um mapa.
 */
public record LeituraParametro(String codigo, BigDecimal valor) {

    public LeituraParametro {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("codigo do parametro medido e obrigatorio");
        }
        if (valor == null) {
            throw new IllegalArgumentException("valor medido e obrigatorio para " + codigo);
        }
        codigo = codigo.trim().toUpperCase();
    }
}
