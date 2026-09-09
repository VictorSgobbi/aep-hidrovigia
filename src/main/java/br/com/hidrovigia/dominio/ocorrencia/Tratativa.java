package br.com.hidrovigia.dominio.ocorrencia;

import java.time.Instant;

/**
 * Subdocumento aninhado com uma acao tomada sobre a ocorrencia.
 *
 * <p>A lista de tratativas e o que transforma a ocorrencia em prova de que a
 * vigilancia agiu: cada entrada diz o que foi feito, por quem e quando.
 */
public record Tratativa(
        String acao,
        String por,
        Instant em,
        StatusOcorrencia statusResultante) {

    public Tratativa {
        if (acao == null || acao.isBlank()) {
            throw new IllegalArgumentException("descricao da acao e obrigatoria");
        }
        if (por == null || por.isBlank()) {
            throw new IllegalArgumentException("responsavel pela acao e obrigatorio");
        }
        acao = acao.trim();
        por = por.trim();
        em = em == null ? Instant.now() : em;
    }
}
