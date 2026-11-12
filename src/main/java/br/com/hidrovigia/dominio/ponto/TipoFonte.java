package br.com.hidrovigia.dominio.ponto;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Origem da agua monitorada em um ponto.
 *
 * <p>Determina a rotina de coleta esperada: um poco artesiano de escola exige
 * frequencia diferente de um manancial superficial que abastece um distrito.
 *
 * <p><b>Verificar antes da apresentacao.</b> A Portaria GM/MS n. 888/2021 define
 * a frequencia de amostragem pelo tipo de manancial e pela populacao atendida.
 * Os intervalos abaixo sao uma simplificacao didatica dessa tabela — fontes
 * superficiais e de distribuicao, mais expostas, semanais; fontes subterraneas
 * e de reservacao, mensais — e nao substituem a norma.
 */
public enum TipoFonte {

    POCO_ARTESIANO("Poco artesiano", Duration.ofDays(30)),
    MANANCIAL_SUPERFICIAL("Manancial superficial", Duration.ofDays(7)),
    NASCENTE("Nascente", Duration.ofDays(30)),
    CISTERNA("Cisterna", Duration.ofDays(30)),
    REDE_PUBLICA("Rede publica de distribuicao", Duration.ofDays(7)),
    CAMINHAO_PIPA("Caminhao-pipa", Duration.ofDays(7));

    private final String descricao;
    private final Duration frequenciaMinima;

    TipoFonte(String descricao, Duration frequenciaMinima) {
        this.descricao = descricao;
        this.frequenciaMinima = frequenciaMinima;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Intervalo maximo admitido entre duas coletas deste tipo de fonte. */
    public Duration getFrequenciaMinima() {
        return frequenciaMinima;
    }

    /** Instante ate o qual a proxima coleta precisa acontecer. */
    public Instant proximaColetaAte(Instant ultimaColeta) {
        return Objects.requireNonNull(ultimaColeta, "ultima coleta e obrigatoria")
                .plus(frequenciaMinima);
    }
}
