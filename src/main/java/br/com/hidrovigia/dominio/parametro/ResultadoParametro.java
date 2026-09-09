package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Objeto de valor imutavel com o veredito de um parametro medido.
 *
 * <p>E o que a avaliacao de {@link ParametroPotabilidade} devolve e o que fica
 * gravado como subdocumento dentro da colecao {@code analises}. Carrega o
 * limite aplicado junto com o valor medido de proposito: a analise precisa ser
 * auditavel anos depois, mesmo que a norma mude nesse meio tempo.
 */
public record ResultadoParametro(
        String codigo,
        String nome,
        BigDecimal valor,
        String unidade,
        BigDecimal limiteMinimo,
        BigDecimal limiteMaximo,
        boolean conforme,
        String mensagem,
        RiscoSanitario risco,
        String referenciaLegal) {

    public ResultadoParametro {
        Objects.requireNonNull(codigo, "codigo do parametro e obrigatorio");
        Objects.requireNonNull(nome, "nome do parametro e obrigatorio");
        Objects.requireNonNull(valor, "valor medido e obrigatorio");
        Objects.requireNonNull(risco, "risco sanitario e obrigatorio");
    }

    public boolean naoConforme() {
        return !conforme;
    }
}
