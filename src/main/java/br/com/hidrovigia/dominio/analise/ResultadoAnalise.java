package br.com.hidrovigia.dominio.analise;

/**
 * Subdocumento aninhado com o veredito consolidado de uma analise.
 *
 * <p>Os numeros sao derivados da lista de parametros, mas ficam gravados no
 * documento de proposito: o painel de conformidade precisa filtrar e contar
 * analises reprovadas sem varrer o array de parametros de cada uma.
 */
public record ResultadoAnalise(
        boolean conforme,
        int qtdParametros,
        int qtdNaoConformidades) {

    public ResultadoAnalise {
        if (qtdParametros <= 0) {
            throw new IllegalArgumentException("analise precisa ter ao menos um parametro");
        }
        if (qtdNaoConformidades < 0 || qtdNaoConformidades > qtdParametros) {
            throw new IllegalArgumentException(
                    "quantidade de nao-conformidades incoerente com a de parametros");
        }
        if (conforme != (qtdNaoConformidades == 0)) {
            throw new IllegalArgumentException(
                    "conformidade nao confere com a quantidade de nao-conformidades");
        }
    }

    public double percentualConformidade() {
        return (qtdParametros - qtdNaoConformidades) * 100.0 / qtdParametros;
    }
}
