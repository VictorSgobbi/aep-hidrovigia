package br.com.hidrovigia.api;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.gravidade.Gravidade;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.servico.OcorrenciaService;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OcorrenciaController.class)
@DisplayName("API de ocorrencias")
class OcorrenciaControllerTest {

    /**
     * O controller decide {@code vencida} pelo {@code Clock} injetado. Parado
     * em {@link Fixtures#REFERENCIA}, o prazo de 24 h das fixtures esta sempre
     * a vencer — e o teste de vencimento avanca o relogio de proposito.
     */
    @TestConfiguration
    static class RelogioDeTeste {

        @Bean
        Clock relogio() {
            return Fixtures.relogioFixo();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcorrenciaService servico;

    private static final String TRATATIVA = """
            { "acao": "Recloracao do reservatorio executada", "por": "Leonardo" }
            """;

    @Test
    @DisplayName("GET sem filtro lista as pendentes com prazo e parametros violados")
    void listaPendentes() throws Exception {
        when(servico.listar(null))
                .thenReturn(List.of(Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA)));

        mockMvc.perform(get("/api/ocorrencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].gravidade").value("CRITICA"))
                .andExpect(jsonPath("$[0].prazoHoras").value(24))
                .andExpect(jsonPath("$[0].status").value("ABERTA"))
                .andExpect(jsonPath("$[0].vencida").value(false))
                .andExpect(jsonPath("$[0].parametrosViolados[0].codigo").value("ECOLI"))
                .andExpect(jsonPath("$[0].parametrosViolados[0].limiteAplicado")
                        .value("ausencia (UFC/100mL)"));
    }

    @Test
    @DisplayName("GET marca como vencida a pendente que passou do prazo")
    void marcaVencida() throws Exception {
        Ocorrencia foraDoPrazo = new Ocorrencia("oc-9", "analise-2", "ponto-1", "PMA-001",
                Gravidade.CRITICA, "risco-sanitario", StatusOcorrencia.ABERTA,
                Fixtures.REFERENCIA.minus(48, ChronoUnit.HOURS),
                Fixtures.REFERENCIA.minus(24, ChronoUnit.HOURS),
                Fixtures.violados(Fixtures.analiseNaoConforme()), List.of());

        when(servico.listar(null)).thenReturn(List.of(foraDoPrazo));

        mockMvc.perform(get("/api/ocorrencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vencida").value(true));
    }

    @Test
    @DisplayName("GET com filtro de status usa o status informado")
    void listaPorStatus() throws Exception {
        when(servico.listar(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of(
                        Fixtures.ocorrenciaPersistida("oc-2", StatusOcorrencia.RESOLVIDA)));

        mockMvc.perform(get("/api/ocorrencias").param("status", "RESOLVIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVIDA"));
    }

    @Test
    @DisplayName("GET por id devolve a ocorrencia")
    void buscaPorId() throws Exception {
        when(servico.buscarPorId("oc-1"))
                .thenReturn(Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA));

        mockMvc.perform(get("/api/ocorrencias/oc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("oc-1"))
                .andExpect(jsonPath("$.politicaClassificacao").value("risco-sanitario"));
    }

    @Test
    @DisplayName("POST em tratativas registra a acao e avanca o status")
    void registraTratativa() throws Exception {
        Ocorrencia emTratativa = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA);
        emTratativa.registrarTratativa("Recloracao do reservatorio executada", "Leonardo");
        when(servico.registrarTratativa(eq("oc-1"), anyString(), anyString()))
                .thenReturn(emTratativa);

        mockMvc.perform(post("/api/ocorrencias/oc-1/tratativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRATATIVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_TRATATIVA"))
                .andExpect(jsonPath("$.tratativas[0].por").value("Leonardo"));
    }

    @Test
    @DisplayName("POST em resolucao encerra a ocorrencia")
    void resolve() throws Exception {
        Ocorrencia resolvida = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA);
        resolvida.resolver("Contraprova conforme", "Victor");
        when(servico.resolver(eq("oc-1"), anyString(), anyString())).thenReturn(resolvida);

        mockMvc.perform(post("/api/ocorrencias/oc-1/resolucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRATATIVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDA"));
    }

    @Test
    @DisplayName("POST devolve 409 ao tentar agir em ocorrencia ja resolvida")
    void recusaAcaoEmResolvida() throws Exception {
        when(servico.resolver(eq("oc-1"), anyString(), anyString()))
                .thenThrow(new RegraNegocioException("transicao invalida: ocorrencia resolvida"));

        mockMvc.perform(post("/api/ocorrencias/oc-1/resolucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRATATIVA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST devolve 400 quando a tratativa vem sem acao ou responsavel")
    void recusaTratativaIncompleta() throws Exception {
        mockMvc.perform(post("/api/ocorrencias/oc-1/tratativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"acao\": \"\", \"por\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes").isNotEmpty());
    }
}
