package br.com.hidrovigia.config;

import br.com.hidrovigia.api.PontoMonitoramentoController;
import br.com.hidrovigia.servico.PontoMonitoramentoService;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Este teste existe pela regra de nao-sombreamento, e nao por cobertura.
 *
 * <p>A tentacao, ao acrescentar uma tela nova, e trocar a lista explicita de
 * rotas por um curinga. O segundo caso abaixo e o que reprova essa mudanca:
 * com {@code /**}, um caminho errado da API passaria a devolver HTML com 200 e
 * o 404 em JSON desapareceria.
 */
@WebMvcTest({ RotasSpaConfig.class, PontoMonitoramentoController.class })
@DisplayName("Rotas da interface")
class RotasSpaConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PontoMonitoramentoService pontoService;

    @Test
    @DisplayName("um refresh numa rota da interface volta para o index.html")
    void encaminhaRotasDaInterface() throws Exception {
        for (String rota : new String[] {
                "/painel", "/pontos", "/coletas", "/ocorrencias",
                "/ocorrencias/oc-1", "/pontos/ponto-1" }) {
            mockMvc.perform(get(rota))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    @DisplayName("nao sombreia a API: caminho errado continua devolvendo 404 em JSON")
    void naoSombreiaApi() throws Exception {
        when(pontoService.buscarPorId("nao-existe"))
                .thenThrow(RecursoNaoEncontradoException.ponto("nao-existe"));

        mockMvc.perform(get("/api/pontos/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Recurso nao encontrado"));
    }
}
