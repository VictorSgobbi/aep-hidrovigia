package br.com.hidrovigia.dominio.ocorrencia;

import java.util.Set;

/**
 * Estados pelos quais uma ocorrencia passa.
 *
 * <p>As transicoes validas ficam declaradas no proprio enum, e nao espalhadas
 * em {@code if} pelo servico: uma ocorrencia resolvida nunca volta a ser aberta,
 * e nenhum caminho pula a etapa de tratativa.
 */
public enum StatusOcorrencia {

    RESOLVIDA("Resolvida"),
    EM_TRATATIVA("Em tratativa"),
    ABERTA("Aberta");

    private final String descricao;

    StatusOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Estados alcancaveis a partir deste. */
    public Set<StatusOcorrencia> proximosPossiveis() {
        return switch (this) {
            case ABERTA -> Set.of(EM_TRATATIVA, RESOLVIDA);
            case EM_TRATATIVA -> Set.of(RESOLVIDA);
            case RESOLVIDA -> Set.of();
        };
    }

    public boolean podeTransicionarPara(StatusOcorrencia destino) {
        return destino != null && proximosPossiveis().contains(destino);
    }

    public boolean finalizada() {
        return this == RESOLVIDA;
    }
}
