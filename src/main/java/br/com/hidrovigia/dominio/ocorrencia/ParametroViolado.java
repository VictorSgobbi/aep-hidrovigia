package br.com.hidrovigia.dominio.ocorrencia;

import java.math.BigDecimal;

import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.parametro.RiscoSanitario;

/**
 * Subdocumento aninhado descrevendo um parametro que reprovou.
 *
 * <p>Copia o essencial do {@link ResultadoParametro} para dentro da ocorrencia
 * em vez de referenciar a analise: quem abre a fila de pendencias precisa ver o
 * que esta errado sem carregar a analise inteira.
 */
public record ParametroViolado(
        String codigo,
        String nome,
        BigDecimal valorMedido,
        String unidade,
        String limiteAplicado,
        RiscoSanitario risco,
        String mensagem) {

    /**
     * Converte o veredito de um parametro reprovado em subdocumento.
     *
     * @throws IllegalArgumentException se o resultado informado estiver conforme
     */
    public static ParametroViolado de(ResultadoParametro resultado, String limiteAplicado) {
        if (resultado == null) {
            throw new IllegalArgumentException("resultado do parametro e obrigatorio");
        }
        if (resultado.conforme()) {
            throw new IllegalArgumentException(
                    "parametro conforme nao gera violacao: " + resultado.codigo());
        }
        return new ParametroViolado(
                resultado.codigo(),
                resultado.nome(),
                resultado.valor(),
                resultado.unidade(),
                limiteAplicado,
                resultado.risco(),
                resultado.mensagem());
    }
}
