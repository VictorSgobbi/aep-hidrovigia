package br.com.hidrovigia.dominio.gravidade;

import java.util.List;

import br.com.hidrovigia.dominio.parametro.ResultadoParametro;

/**
 * Estrategia de classificacao da gravidade de uma ocorrencia.
 *
 * <p>Municipios diferentes priorizam de formas diferentes: uns respondem pelo
 * risco sanitario do pior parametro violado, outros pelo acumulo de desvios na
 * mesma amostra. Trocar de politica precisa ser uma troca de implementacao, nao
 * uma edicao no meio do servico de analise.
 */
public interface ClassificadorGravidade {

    /**
     * @param naoConformidades parametros reprovados na analise, nunca vazio
     * @return nivel de urgencia da ocorrencia a ser aberta
     * @throws IllegalArgumentException se a lista for nula ou vazia
     */
    Gravidade classificar(List<ResultadoParametro> naoConformidades);

    /** Nome da politica, gravado na ocorrencia para tornar a decisao rastreavel. */
    String nome();

    /**
     * Guarda comum as implementacoes: classificar sem violacao nao faz sentido.
     */
    default List<ResultadoParametro> exigirNaoConformidades(List<ResultadoParametro> lista) {
        if (lista == null || lista.isEmpty()) {
            throw new IllegalArgumentException(
                    "nao ha o que classificar: a analise nao possui nao-conformidades");
        }
        return lista;
    }
}
