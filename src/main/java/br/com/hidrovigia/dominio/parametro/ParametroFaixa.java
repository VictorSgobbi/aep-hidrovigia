package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Parametro que so e conforme dentro de um intervalo fechado.
 *
 * <p>Casos tipicos: pH, que a norma recomenda manter entre 6,0 e 9,0 na rede de
 * distribuicao, e cloro residual livre, que precisa existir em quantidade
 * suficiente para desinfetar sem tornar a agua impalatavel.
 */
public class ParametroFaixa extends ParametroPotabilidade {

    private final BigDecimal minimo;
    private final BigDecimal maximo;

    public ParametroFaixa(String codigo,
                          String nome,
                          String unidade,
                          String referenciaLegal,
                          RiscoSanitario risco,
                          BigDecimal minimo,
                          BigDecimal maximo) {
        super(codigo, nome, unidade, referenciaLegal, risco);
        this.minimo = Objects.requireNonNull(minimo, "limite minimo e obrigatorio");
        this.maximo = Objects.requireNonNull(maximo, "limite maximo e obrigatorio");
        if (minimo.compareTo(maximo) > 0) {
            throw new IllegalArgumentException(
                    "limite minimo nao pode ser maior que o maximo no parametro " + codigo);
        }
    }

    @Override
    protected boolean dentroDoLimite(BigDecimal valor) {
        return valor.compareTo(minimo) >= 0 && valor.compareTo(maximo) <= 0;
    }

    @Override
    protected String descreverViolacao(BigDecimal valor) {
        String posicao = valor.compareTo(minimo) < 0 ? "abaixo do minimo" : "acima do maximo";
        return getNome() + " " + posicao + ": " + valor.toPlainString()
                + formatarUnidade() + " (esperado " + descricaoDoLimite() + ")";
    }

    @Override
    public BigDecimal limiteMinimo() {
        return minimo;
    }

    @Override
    public BigDecimal limiteMaximo() {
        return maximo;
    }

    @Override
    public String descricaoDoLimite() {
        return "entre " + minimo.toPlainString() + " e " + maximo.toPlainString() + formatarUnidade();
    }

    private String formatarUnidade() {
        return getUnidade().isEmpty() ? "" : " " + getUnidade();
    }
}
