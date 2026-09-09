package br.com.hidrovigia.api.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo padrao das respostas de erro da API.
 */
@Schema(name = "Erro")
public record ErroResponse(
        Instant instante,
        int status,
        String erro,
        String mensagem,
        List<String> detalhes) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, List.of());
    }

    public static ErroResponse de(int status, String erro, String mensagem, List<String> detalhes) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, detalhes);
    }
}
