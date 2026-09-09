package br.com.hidrovigia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PoC HidroVigia — vigilancia da qualidade da agua em sistemas de
 * abastecimento de pequeno porte.
 *
 * <p>AEP 2026.2 — 6S — Engenharia de Software. ODS 6: Agua Potavel e Saneamento.
 */
@SpringBootApplication
public class HidroVigiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HidroVigiaApplication.class, args);
    }
}
