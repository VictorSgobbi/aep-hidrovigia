package br.com.hidrovigia.dominio.ponto;

/**
 * Origem da agua monitorada em um ponto.
 *
 * <p>Determina a rotina de coleta esperada: um poco artesiano de escola exige
 * frequencia diferente de um manancial superficial que abastece um distrito.
 */
public enum TipoFonte {

    POCO_ARTESIANO("Poco artesiano"),
    MANANCIAL_SUPERFICIAL("Manancial superficial"),
    NASCENTE("Nascente"),
    CISTERNA("Cisterna"),
    REDE_PUBLICA("Rede publica de distribuicao"),
    CAMINHAO_PIPA("Caminhao-pipa");

    private final String descricao;

    TipoFonte(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
