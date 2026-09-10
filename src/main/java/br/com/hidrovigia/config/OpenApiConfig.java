package br.com.hidrovigia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao interativa servida em {@code /swagger-ui.html}.
 *
 * <p>A PoC e demonstrada pela interface em {@code /}, servida do proprio jar.
 * O Swagger continua sendo onde o contrato da API se inspeciona — inclusive
 * para conferir o que o frontend consome.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hidroVigiaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("HidroVigia")
                .version("1.0.0")
                .description("""
                        PoC de vigilancia da qualidade da agua em sistemas de abastecimento \
                        de pequeno porte.

                        ODS 6 - Agua Potavel e Saneamento (metas 6.1, 6.3 e 6.4).

                        Fluxo principal: cadastrar ponto, registrar analise, o sistema \
                        confronta cada parametro com o padrao de potabilidade brasileiro \
                        e abre ocorrencia com prazo de resposta quando algum reprova.

                        AEP 2026.2 - 6S - Engenharia de Software.""")
                .contact(new Contact().name("Victor, Bruno e Leonardo"))
                .license(new License().name("MIT")));
    }
}
