package br.com.hidrovigia.config;

import java.time.Clock;

import br.com.hidrovigia.dominio.gravidade.ClassificadorGravidade;
import br.com.hidrovigia.dominio.gravidade.ClassificadorPorRiscoSanitario;
import br.com.hidrovigia.dominio.parametro.CatalogoParametros;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica os objetos de dominio que os servicos recebem por injecao.
 *
 * <p>O pacote {@code dominio} nao depende do Spring: quem amarra as
 * implementacoes concretas e esta classe, o que mantem as regras de
 * potabilidade testaveis sem subir contexto nenhum.
 */
@Configuration
public class DominioConfig {

    @Bean
    public CatalogoParametros catalogoParametros() {
        return CatalogoParametros.padraoBrasileiro();
    }

    /**
     * Politica de gravidade em vigor. Trocar por
     * {@code ClassificadorPorQuantidade} muda a regra em toda a aplicacao.
     */
    @Bean
    public ClassificadorGravidade classificadorGravidade() {
        return new ClassificadorPorRiscoSanitario();
    }

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
