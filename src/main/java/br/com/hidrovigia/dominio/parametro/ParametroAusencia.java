package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;

/**
 * Parametro microbiologico que exige ausencia no volume analisado.
 *
 * <p>Nao existe concentracao aceitavel de <i>Escherichia coli</i> em agua para
 * consumo humano: qualquer contagem diferente de zero reprova. O valor medido
 * chega como contagem em UFC/100 mL justamente para manter a mesma interface
 * dos demais parametros — zero significa "ausente".
 *
 * <p>O risco e sempre {@link RiscoSanitario#MICROBIOLOGICO}, entao a subclasse
 * nem oferece essa escolha a quem a constroi.
 */
public class ParametroAusencia extends ParametroPotabilidade {

    private static final BigDecimal AUSENTE = BigDecimal.ZERO;

    public ParametroAusencia(String codigo,
                             String nome,
                             String unidade,
                             String referenciaLegal) {
        super(codigo, nome, unidade, referenciaLegal, RiscoSanitario.MICROBIOLOGICO);
    }

    @Override
    protected boolean dentroDoLimite(BigDecimal valor) {
        return valor.compareTo(AUSENTE) == 0;
    }

    @Override
    protected String descreverViolacao(BigDecimal valor) {
        return getNome() + " detectado na amostra: " + valor.toPlainString()
                + formatarUnidade() + " (exigida ausencia)";
    }

    @Override
    public BigDecimal limiteMinimo() {
        return null;
    }

    @Override
    public BigDecimal limiteMaximo() {
        return AUSENTE;
    }

    @Override
    public String descricaoDoLimite() {
        return "ausencia" + (getUnidade().isEmpty() ? "" : " (" + getUnidade() + ")");
    }

    private String formatarUnidade() {
        return getUnidade().isEmpty() ? "" : " " + getUnidade();
    }
}
