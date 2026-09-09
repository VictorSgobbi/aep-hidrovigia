package br.com.hidrovigia.servico;

/**
 * Consolidado apresentado no painel de conformidade.
 *
 * <p>Escopo diz a que o recorte se refere — "geral" ou o codigo do ponto — para
 * que o mesmo formato sirva as duas visoes.
 */
public record IndicadoresConformidade(
        String escopo,
        long totalAnalises,
        long analisesConformes,
        long analisesNaoConformes,
        double percentualConformidade,
        long ocorrenciasPendentes,
        long ocorrenciasVencidas) {

    public static IndicadoresConformidade de(String escopo,
                                             long conformes,
                                             long naoConformes,
                                             long pendentes,
                                             long vencidas) {
        long total = conformes + naoConformes;
        double percentual = total == 0 ? 0.0 : arredondar(conformes * 100.0 / total);
        return new IndicadoresConformidade(
                escopo, total, conformes, naoConformes, percentual, pendentes, vencidas);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public boolean semDados() {
        return totalAnalises == 0;
    }
}
