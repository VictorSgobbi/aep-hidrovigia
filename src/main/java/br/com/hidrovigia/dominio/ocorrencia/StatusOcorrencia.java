package br.com.hidrovigia.dominio.ocorrencia;

import java.util.Set;

/**
 * Estados pelos quais uma ocorrencia passa.
 *
 * <p>As transicoes validas ficam declaradas no proprio enum, e nao espalhadas
 * em {@code if} pelo servico: uma ocorrencia resolvida nunca volta a ser aberta,
 * e nenhum caminho pula a etapa de tratativa.
 *
 * <p>Uma unica transicao para o mesmo estado e permitida:
 * {@code EM_TRATATIVA -> EM_TRATATIVA}. Ela existe porque a vigilancia registra
 * quantas acoes forem necessarias antes de encerrar a pendencia, e cada acao e
 * uma transicao que grava uma tratativa no historico. {@code ABERTA} nao tem
 * esse laco: a primeira acao registrada e justamente o que tira a ocorrencia
 * da fila de nao-iniciadas.
 */
public enum StatusOcorrencia {

    ABERTA("Aberta"),
    EM_TRATATIVA("Em tratativa"),
    RESOLVIDA("Resolvida");

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
            // Permanecer em tratativa e o que permite acumular acoes na mesma
            // ocorrencia; sem isso, a segunda tratativa seria recusada.
            case EM_TRATATIVA -> Set.of(EM_TRATATIVA, RESOLVIDA);
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
