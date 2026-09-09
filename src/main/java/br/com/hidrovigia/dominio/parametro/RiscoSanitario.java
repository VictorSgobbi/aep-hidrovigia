package br.com.hidrovigia.dominio.parametro;

/**
 * Natureza do risco associado a um parametro de potabilidade.
 *
 * <p>O peso ordena a urgencia da resposta: uma contaminacao microbiologica
 * exige acao imediata, enquanto um desvio organoleptico afeta a aceitacao da
 * agua sem oferecer risco agudo a saude. E esse peso que as estrategias de
 * classificacao de gravidade usam para decidir o prazo de tratativa.
 */
public enum RiscoSanitario {

    /** Presenca de organismos indicadores de contaminacao fecal. */
    MICROBIOLOGICO("Microbiologico", 4),

    /** Falha na barreira de desinfeccao, como cloro residual insuficiente. */
    DESINFECCAO("Desinfeccao", 3),

    /** Substancia quimica com valor maximo permitido definido em norma. */
    FISICO_QUIMICO("Fisico-quimico", 2),

    /** Caracteristica que afeta aparencia, sabor ou odor da agua. */
    ORGANOLEPTICO("Organoleptico", 1);

    private final String descricao;
    private final int peso;

    RiscoSanitario(String descricao, int peso) {
        this.descricao = descricao;
        this.peso = peso;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPeso() {
        return peso;
    }

    /**
     * @return true se este risco e mais urgente que o informado.
     */
    public boolean maisGraveQue(RiscoSanitario outro) {
        return outro == null || this.peso > outro.peso;
    }
}
