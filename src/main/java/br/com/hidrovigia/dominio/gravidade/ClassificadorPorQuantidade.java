package br.com.hidrovigia.dominio.gravidade;

import java.util.List;

import br.com.hidrovigia.dominio.parametro.ResultadoParametro;

/**
 * Politica alternativa: a gravidade cresce com o numero de desvios na amostra.
 *
 * <p>Util para operadores que tratam acumulo de nao-conformidades como sinal de
 * falha sistemica no tratamento, independentemente de qual parametro reprovou.
 * Existe para demonstrar que a classificacao e um ponto de extensao real do
 * dominio, nao uma regra fixa.
 */
public class ClassificadorPorQuantidade implements ClassificadorGravidade {

    private static final int LIMIAR_CRITICO = 3;
    private static final int LIMIAR_ALTO = 2;

    @Override
    public Gravidade classificar(List<ResultadoParametro> naoConformidades) {
        int quantidade = exigirNaoConformidades(naoConformidades).size();

        if (quantidade >= LIMIAR_CRITICO) {
            return Gravidade.CRITICA;
        }
        if (quantidade == LIMIAR_ALTO) {
            return Gravidade.ALTA;
        }
        return Gravidade.MEDIA;
    }

    @Override
    public String nome() {
        return "quantidade-de-desvios";
    }
}
