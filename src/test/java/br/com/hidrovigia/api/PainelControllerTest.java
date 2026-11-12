package br.com.hidrovigia.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.com.hidrovigia.config.DominioConfig;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.servico.IndicadoresConformidade;
import br.com.hidrovigia.servico.MonitoramentoColetaService;
import br.com.hidrovigia.servico.PainelConformidadeService;
import br.com.hidrovigia.servico.SituacaoColeta;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PainelController.class)
@Import(DominioConfig.class)
@DisplayName("API do painel de conformidade")
class PainelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PainelConformidadeService painel;

    @MockBean
    private MonitoramentoColetaService monitoramento;

    @Test
    @DisplayName("GET coletas devolve a situacao de cada ponto, com o ponto sem coleta sem datas")
    void situacaoDasColetas() throws Exception {
        Instant ultima = Instant.parse("2026-01-01T10:00:00Z");
        when(monitoramento.situacaoDosPontosAtivos()).thenReturn(List.of(
                new SituacaoColeta("PMA-002", "Manancial do Distrito",
                        TipoFonte.MANANCIAL_SUPERFICIAL, 7, null, null, true),
                new SituacaoColeta("PMA-001", "Poco da Escola", TipoFonte.POCO_ARTESIANO, 30,
                        ultima, ultima.plus(30, ChronoUnit.DAYS), false)));

        mockMvc.perform(get("/api/painel/coletas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigoPonto").value("PMA-002"))
                .andExpect(jsonPath("$[0].vencida").value(true))
                .andExpect(jsonPath("$[0].ultimaColeta").doesNotExist())
                .andExpect(jsonPath("$[1].tipoFonte").value("POCO_ARTESIANO"))
                .andExpect(jsonPath("$[1].frequenciaMinimaDias").value(30))
                .andExpect(jsonPath("$[1].proximaColetaAte").value("2026-01-31T10:00:00Z"));
    }

    @Test
    @DisplayName("GET conformidade devolve o consolidado geral")
    void conformidadeGeral() throws Exception {
        when(painel.geral()).thenReturn(
                IndicadoresConformidade.de("geral", 17, 3, 2, 1));

        mockMvc.perform(get("/api/painel/conformidade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo").value("geral"))
                .andExpect(jsonPath("$.totalAnalises").value(20))
                .andExpect(jsonPath("$.percentualConformidade").value(85.0))
                .andExpect(jsonPath("$.ocorrenciasPendentes").value(2))
                .andExpect(jsonPath("$.ocorrenciasVencidas").value(1));
    }

    @Test
    @DisplayName("GET conformidade por ponto usa o codigo como escopo")
    void conformidadePorPonto() throws Exception {
        when(painel.porPonto("PMA-001")).thenReturn(
                IndicadoresConformidade.de("PMA-001", 8, 2, 1, 0));

        mockMvc.perform(get("/api/painel/conformidade/PMA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escopo").value("PMA-001"))
                .andExpect(jsonPath("$.percentualConformidade").value(80.0));
    }

    @Test
    @DisplayName("GET conformidade devolve 404 para ponto inexistente")
    void pontoInexistente() throws Exception {
        when(painel.porPonto("XXX")).thenThrow(RecursoNaoEncontradoException.ponto("XXX"));

        mockMvc.perform(get("/api/painel/conformidade/XXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET parametros publica o catalogo com limite e norma")
    void catalogoPublicado() throws Exception {
        mockMvc.perform(get("/api/painel/parametros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].codigo").value("ECOLI"))
                .andExpect(jsonPath("$[0].limite").value("ausencia (UFC/100mL)"))
                .andExpect(jsonPath("$[0].risco").value("MICROBIOLOGICO"))
                .andExpect(jsonPath("$[0].referenciaLegal").value("Portaria GM/MS n. 888/2021"));
    }
}
