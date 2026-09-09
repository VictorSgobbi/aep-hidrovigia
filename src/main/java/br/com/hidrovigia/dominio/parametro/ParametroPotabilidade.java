package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Um parametro do padrao de potabilidade brasileiro.
 *
 * <p>A regra de conformidade nao e a mesma para todos os parametros: pH vive
 * dentro de uma faixa, turbidez tem apenas teto e <i>E. coli</i> precisa estar
 * ausente. Em vez de espalhar {@code if} por codigo de parametro, cada forma de
 * limite vira uma subclasse que responde a mesma pergunta do seu jeito.
 *
 * <p>{@link #avaliar(BigDecimal)} e um <i>template method</i> deliberadamente
 * {@code final}: ele fixa o formato do veredito e delega as subclasses somente
 * a decisao sobre o limite e a redacao da violacao.
 */
public abstract class ParametroPotabilidade {

    private final String codigo;
    private final String nome;
    private final String unidade;
    private final String referenciaLegal;
    private final RiscoSanitario risco;

    protected ParametroPotabilidade(String codigo,
                                    String nome,
                                    String unidade,
                                    String referenciaLegal,
                                    RiscoSanitario risco) {
        this.codigo = exigirTexto(codigo, "codigo do parametro");
        this.nome = exigirTexto(nome, "nome do parametro");
        this.unidade = unidade == null ? "" : unidade.trim();
        this.referenciaLegal = exigirTexto(referenciaLegal, "referencia legal");
        this.risco = Objects.requireNonNull(risco, "risco sanitario e obrigatorio");
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio");
        }
        return valor.trim();
    }

    /**
     * Confronta o valor medido com o limite normativo deste parametro.
     *
     * @param valor resultado laboratorial ou de campo
     * @return veredito imutavel, ja com o limite aplicado registrado
     * @throws IllegalArgumentException se o valor for nulo ou negativo
     */
    public final ResultadoParametro avaliar(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException(
                    "valor medido e obrigatorio para o parametro " + codigo);
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(
                    "valor medido nao pode ser negativo para o parametro " + codigo);
        }

        boolean conforme = dentroDoLimite(valor);
        String mensagem = conforme
                ? nome + " dentro do padrao (" + descricaoDoLimite() + ")"
                : descreverViolacao(valor);

        return new ResultadoParametro(
                codigo, nome, valor, unidade,
                limiteMinimo(), limiteMaximo(),
                conforme, mensagem, risco, referenciaLegal);
    }

    /** Regra de conformidade especifica de cada forma de limite. */
    protected abstract boolean dentroDoLimite(BigDecimal valor);

    /** Texto explicando por que o valor reprovou. */
    protected abstract String descreverViolacao(BigDecimal valor);

    /** Limite inferior aplicavel, ou {@code null} quando nao houver. */
    public abstract BigDecimal limiteMinimo();

    /** Limite superior aplicavel, ou {@code null} quando nao houver. */
    public abstract BigDecimal limiteMaximo();

    /** Descricao legivel do limite, usada em mensagens e na documentacao da API. */
    public abstract String descricaoDoLimite();

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getReferenciaLegal() {
        return referenciaLegal;
    }

    public RiscoSanitario getRisco() {
        return risco;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ParametroPotabilidade outro)) {
            return false;
        }
        return codigo.equals(outro.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    @Override
    public String toString() {
        return codigo + " (" + descricaoDoLimite() + ")";
    }
}
