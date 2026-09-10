package br.com.hidrovigia.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Encaminha as rotas da interface para o {@code index.html}.
 *
 * <p>O roteamento dessas telas e do React, nao do Spring: um refresh do
 * navegador em {@code /ocorrencias} chega aqui como uma requisicao de verdade,
 * e sem este encaminhamento nao existiria controller nem arquivo estatico
 * naquele caminho — o usuario receberia 404 ao apertar F5.
 *
 * <p>A lista e explicita de proposito. Um curinga {@code /**} tambem
 * resolveria, mas devolveria o index.html com HTTP 200 para
 * {@code /api/pontoss} — um erro de digitacao na API viraria uma pagina HTML
 * em vez do 404 em JSON que o {@link br.com.hidrovigia.api.TratadorDeErros}
 * entrega. Ao acrescentar uma rota no React, acrescente aqui.
 *
 * <p>A raiz {@code /} nao precisa entrar: o Spring Boot serve
 * {@code static/index.html} como pagina inicial automaticamente.
 */
@Controller
public class RotasSpaConfig {

    @GetMapping({
            "/painel",
            "/pontos", "/pontos/**",
            "/coletas", "/coletas/**",
            "/ocorrencias", "/ocorrencias/**"
    })
    public String encaminharParaSpa() {
        return "forward:/index.html";
    }
}
