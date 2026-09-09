package br.com.hidrovigia.dominio.gravidade;

/**
 * Urgencia da resposta exigida por uma nao-conformidade.
 *
 * <p>Cada nivel carrega o prazo em que a ocorrencia precisa ser tratada. Um
 * resultado microbiologico positivo obriga acao no mesmo dia; um desvio de cor
 * aparente pode aguardar a proxima rotina de campo.
 */
public enum Gravidade {

    CRITICA("Critica", 24),
    ALTA("Alta", 72),
    MEDIA("Media", 168);

    private final String descricao;
    private final int prazoHoras;

    Gravidade(String descricao, int prazoHoras) {
        this.descricao = descricao;
        this.prazoHoras = prazoHoras;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPrazoHoras() {
        return prazoHoras;
    }
}
