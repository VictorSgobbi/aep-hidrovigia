package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Parametro com valor maximo permitido e sem piso.
 *
 * <p>E a forma mais comum na norma: turbidez, cor aparente, fluoreto e nitrato
 * sao aceitos em qualquer concentracao abaixo do teto, inclusive zero.
 */
public class ParametroMaximo extends ParametroPotabilidade {

    private final BigDecimal maximo;

    public ParametroMaximo(String codigo,
                           String nome,
                           String unidade,
                           String referenciaLegal,
                           RiscoSanitario risco,
                           BigDecimal maximo) {
        super(codigo, nome, unidade, referenciaLegal, risco);
        this.maximo = Objects.requireNonNull(maximo, "valor maximo permitido e obrigatorio");
        if (maximo.signum() < 0) {
            throw new IllegalArgumentException(
                    "valor maximo permitido nao pode ser negativo no parametro " + codigo);
        }
    }

    @Override
    protected boolean dentroDoLimite(BigDecimal valor) {
        return valor.compareTo(maximo) <= 0;
    }

    @Override
    protected String descreverViolacao(BigDecimal valor) {
        return getNome() + " acima do valor maximo permitido: " + valor.toPlainString()
                + formatarUnidade() + " (limite " + maximo.toPlainString() + formatarUnidade() + ")";
    }

    @Override
    public BigDecimal limiteMinimo() {
        return null;
    }

    @Override
    public BigDecimal limiteMaximo() {
        return maximo;
    }

    @Override
    public String descricaoDoLimite() {
        return "ate " + maximo.toPlainString() + formatarUnidade();
    }

    private String formatarUnidade() {
        return getUnidade().isEmpty() ? "" : " " + getUnidade();
    }
}
