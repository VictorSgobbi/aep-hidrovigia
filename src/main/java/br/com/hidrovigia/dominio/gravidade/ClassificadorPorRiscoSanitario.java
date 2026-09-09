package br.com.hidrovigia.dominio.gravidade;

import java.util.Comparator;
import java.util.List;

import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.parametro.RiscoSanitario;

/**
 * Politica padrao: a gravidade e ditada pelo parametro de maior risco.
 *
 * <p>Dez desvios organolepticos continuam sendo menos urgentes que uma unica
 * deteccao de <i>E. coli</i>, entao a contagem de violacoes nao entra na conta.
 */
public class ClassificadorPorRiscoSanitario implements ClassificadorGravidade {

    @Override
    public Gravidade classificar(List<ResultadoParametro> naoConformidades) {
        RiscoSanitario pior = exigirNaoConformidades(naoConformidades).stream()
                .map(ResultadoParametro::risco)
                .max(Comparator.comparingInt(RiscoSanitario::getPeso))
                .orElseThrow();

        return switch (pior) {
            case MICROBIOLOGICO -> Gravidade.CRITICA;
            case DESINFECCAO, FISICO_QUIMICO -> Gravidade.ALTA;
            case ORGANOLEPTICO -> Gravidade.MEDIA;
        };
    }

    @Override
    public String nome() {
        return "risco-sanitario";
    }
}
