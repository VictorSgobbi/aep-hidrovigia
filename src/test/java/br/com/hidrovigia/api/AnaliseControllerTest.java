package br.com.hidrovigia.api;

import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.parametro.ParametroDesconhecidoException;
import br.com.hidrovigia.servico.AnaliseService;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnaliseController.class)
@DisplayName("API de analises")
class AnaliseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnaliseService servico;

    private static final String COLETA_CONFORME = """
            {
              "codigoPonto": "PMA-001",
              "coletor": "Tecnico Bruno",
              "coletadoEm": "2026-09-01T10:00:00Z",
              "leituras": [
                { "codigo": "ECOLI", "valor": 0 },
                { "codigo": "CRL",   "valor": 0.8 },
                { "codigo": "PH",    "valor": 7.2 },
                { "codigo": "TURB",  "valor": 0.4 }
              ]
            }
            """;

    @Test
    @DisplayName("POST devolve 201 com o veredito consolidado")
    void registraConforme() throws Exception {
        when(servico.registrar(anyString(), anyString(), any(Instant.class), any()))
                .thenReturn(Fixtures.analiseConforme());

        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COLETA_CONFORME))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conforme").value(true))
                .andExpect(jsonPath("$.pontoCodigo").value("PMA-001"))
                .andExpect(jsonPath("$.qtdParametros").value(4))
                .andExpect(jsonPath("$.qtdNaoConformidades").value(0))
                .andExpect(jsonPath("$.percentualConformidade").value(100.0))
                .andExpect(jsonPath("$.parametros[0].referenciaLegal")
                        .value("Portaria GM/MS n. 888/2021"));
    }

    @Test
    @DisplayName("POST devolve o detalhe de cada parametro reprovado")
    void registraNaoConforme() throws Exception {
        when(servico.registrar(anyString(), anyString(), any(Instant.class), any()))
                .thenReturn(Fixtures.analiseNaoConforme());

        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COLETA_CONFORME))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conforme").value(false))
                .andExpect(jsonPath("$.qtdNaoConformidades").value(2))
                .andExpect(jsonPath("$.parametros[0].codigo").value("ECOLI"))
                .andExpect(jsonPath("$.parametros[0].conforme").value(false))
                .andExpect(jsonPath("$.parametros[0].risco").value("MICROBIOLOGICO"))
                .andExpect(jsonPath("$.parametros[0].mensagem")
                        .value(org.hamcrest.Matchers.containsString("detectado na amostra")));
    }

    @Test
    @DisplayName("POST devolve 400 quando nao ha nenhuma leitura")
    void recusaSemLeituras() throws Exception {
        String semLeituras = """
                {
                  "codigoPonto": "PMA-001",
                  "coletor": "Tecnico",
                  "coletadoEm": "2026-09-01T10:00:00Z",
                  "leituras": []
                }
                """;

        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semLeituras))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes").isNotEmpty());
    }

    @Test
    @DisplayName("POST devolve 422 quando o parametro esta fora do catalogo")
    void recusaParametroDesconhecido() throws Exception {
        when(servico.registrar(anyString(), anyString(), any(Instant.class), any()))
                .thenThrow(new ParametroDesconhecidoException("CHUMBO"));

        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COLETA_CONFORME))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erro").value("Parametro fora do catalogo"));
    }

    @Test
    @DisplayName("POST devolve 409 quando o ponto esta desativado")
    void recusaPontoDesativado() throws Exception {
        when(servico.registrar(anyString(), anyString(), any(Instant.class), any()))
                .thenThrow(new RegraNegocioException("Ponto PMA-001 esta desativado"));

        mockMvc.perform(post("/api/analises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COLETA_CONFORME))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET busca por id, lista por ponto e lista por periodo")
    void consultas() throws Exception {
        when(servico.buscarPorId("analise-1")).thenReturn(Fixtures.analiseConforme());
        when(servico.listarPorPonto("PMA-001"))
                .thenReturn(List.of(Fixtures.analiseConforme(), Fixtures.analiseNaoConforme()));
        when(servico.listarPorPeriodo(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(Fixtures.analiseConforme()));

        mockMvc.perform(get("/api/analises/analise-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("analise-1"));

        mockMvc.perform(get("/api/analises/ponto/PMA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/analises")
                        .param("inicio", "2026-09-01T00:00:00Z")
                        .param("fim", "2026-09-30T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
