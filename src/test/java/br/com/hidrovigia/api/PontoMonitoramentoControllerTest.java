package br.com.hidrovigia.api;

import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.servico.PontoMonitoramentoService;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PontoMonitoramentoController.class)
@DisplayName("API de pontos de monitoramento")
class PontoMonitoramentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PontoMonitoramentoService servico;

    private static final String CORPO_VALIDO = """
            {
              "codigo": "PMA-001",
              "nome": "Poco da Escola Rural Sao Jose",
              "tipoFonte": "POCO_ARTESIANO",
              "populacaoAtendida": 320,
              "localizacao": { "municipio": "Maringa", "uf": "PR",
                               "latitude": -23.4205, "longitude": -51.9331 },
              "responsavel": { "nome": "Ana Souza", "registro": "CREA-PR 123456",
                               "contato": "ana.souza@exemplo.gov.br" }
            }
            """;

    @Test
    @DisplayName("POST devolve 201 com o ponto criado e o cabecalho Location")
    void cadastra() throws Exception {
        when(servico.cadastrar(anyString(), anyString(), any(TipoFonte.class), anyInt(),
                any(), any())).thenReturn(Fixtures.pontoPersistido());

        mockMvc.perform(post("/api/pontos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/pontos/ponto-1"))
                .andExpect(jsonPath("$.codigo").value("PMA-001"))
                .andExpect(jsonPath("$.descricaoFonte").value("Poco artesiano"))
                .andExpect(jsonPath("$.localizacao.municipio").value("Maringa"))
                .andExpect(jsonPath("$.responsavel.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("POST devolve 400 detalhando os campos invalidos")
    void recusaCorpoInvalido() throws Exception {
        String semCodigoNemUf = """
                {
                  "codigo": "",
                  "nome": "Sem codigo",
                  "tipoFonte": "CISTERNA",
                  "populacaoAtendida": -5,
                  "localizacao": { "municipio": "Maringa", "uf": "PARANA" },
                  "responsavel": { "nome": "Ana", "contato": "ana@exemplo.gov.br" }
                }
                """;

        mockMvc.perform(post("/api/pontos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semCodigoNemUf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisicao invalida"))
                .andExpect(jsonPath("$.detalhes").isNotEmpty());
    }

    @Test
    @DisplayName("POST devolve 409 quando o codigo ja existe")
    void recusaCodigoDuplicado() throws Exception {
        when(servico.cadastrar(anyString(), anyString(), any(TipoFonte.class), anyInt(),
                any(), any()))
                .thenThrow(new RegraNegocioException("Ja existe um ponto com o codigo PMA-001"));

        mockMvc.perform(post("/api/pontos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Ja existe um ponto com o codigo PMA-001"));
    }

    @Test
    @DisplayName("GET lista todos, apenas ativos ou por municipio")
    void lista() throws Exception {
        List<PontoMonitoramento> um = List.of(Fixtures.pontoPersistido());
        when(servico.listar(false)).thenReturn(um);
        when(servico.listar(true)).thenReturn(um);
        when(servico.listarPorMunicipio("Maringa")).thenReturn(um);

        mockMvc.perform(get("/api/pontos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/pontos").param("apenasAtivos", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pontos").param("municipio", "Maringa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PMA-001"));
    }

    @Test
    @DisplayName("GET por id e por codigo devolvem o ponto")
    void busca() throws Exception {
        when(servico.buscarPorId("ponto-1")).thenReturn(Fixtures.pontoPersistido());
        when(servico.buscarPorCodigo("PMA-001")).thenReturn(Fixtures.pontoPersistido());

        mockMvc.perform(get("/api/pontos/ponto-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ponto-1"));

        mockMvc.perform(get("/api/pontos/codigo/PMA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("PMA-001"));
    }

    @Test
    @DisplayName("GET devolve 404 quando o ponto nao existe")
    void naoEncontrado() throws Exception {
        when(servico.buscarPorId("sumiu")).thenThrow(RecursoNaoEncontradoException.ponto("sumiu"));

        mockMvc.perform(get("/api/pontos/sumiu"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Recurso nao encontrado"));
    }

    @Test
    @DisplayName("PUT atualiza os dados cadastrais")
    void atualiza() throws Exception {
        when(servico.atualizar(eq("ponto-1"), anyString(), any(TipoFonte.class), anyInt(),
                any(), any())).thenReturn(Fixtures.pontoPersistido());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/pontos/ponto-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ponto-1"));
    }

    @Test
    @DisplayName("DELETE desativa e POST em reativacao reativa")
    void desativaEReativa() throws Exception {
        PontoMonitoramento desativado = Fixtures.pontoPersistido();
        desativado.desativar();

        when(servico.desativar("ponto-1")).thenReturn(desativado);
        when(servico.reativar("ponto-1")).thenReturn(Fixtures.pontoPersistido());

        mockMvc.perform(delete("/api/pontos/ponto-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(post("/api/pontos/ponto-1/reativacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }
}
