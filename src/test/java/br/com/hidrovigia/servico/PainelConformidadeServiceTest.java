package br.com.hidrovigia.servico;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.repositorio.AnaliseRepository;
import br.com.hidrovigia.repositorio.OcorrenciaRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Painel de conformidade")
class PainelConformidadeServiceTest {

    @Mock
    private AnaliseRepository analiseRepositorio;

    @Mock
    private OcorrenciaRepository ocorrenciaRepositorio;

    @Mock
    private PontoMonitoramentoService pontoService;

    private PainelConformidadeService painelCom(Clock relogio) {
        return new PainelConformidadeService(
                analiseRepositorio, ocorrenciaRepositorio, pontoService, relogio);
    }

    private static Clock relogioFixoEm(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("consolida percentual de conformidade do sistema inteiro")
    void consolidaGeral() {
        when(analiseRepositorio.countByResultadoConforme(true)).thenReturn(17L);
        when(analiseRepositorio.countByResultadoConforme(false)).thenReturn(3L);
        when(ocorrenciaRepositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of());

        IndicadoresConformidade indicadores = painelCom(Clock.systemUTC()).geral();

        assertThat(indicadores.escopo()).isEqualTo("geral");
        assertThat(indicadores.totalAnalises()).isEqualTo(20L);
        assertThat(indicadores.analisesConformes()).isEqualTo(17L);
        assertThat(indicadores.analisesNaoConformes()).isEqualTo(3L);
        assertThat(indicadores.percentualConformidade()).isEqualTo(85.0);
        assertThat(indicadores.semDados()).isFalse();
    }

    @Test
    @DisplayName("nao divide por zero quando ainda nao ha analises")
    void semAnalises() {
        when(analiseRepositorio.countByResultadoConforme(true)).thenReturn(0L);
        when(analiseRepositorio.countByResultadoConforme(false)).thenReturn(0L);
        when(ocorrenciaRepositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of());

        IndicadoresConformidade indicadores = painelCom(Clock.systemUTC()).geral();

        assertThat(indicadores.totalAnalises()).isZero();
        assertThat(indicadores.percentualConformidade()).isZero();
        assertThat(indicadores.semDados()).isTrue();
    }

    @Test
    @DisplayName("arredonda o percentual em duas casas")
    void arredondaPercentual() {
        when(analiseRepositorio.countByResultadoConforme(true)).thenReturn(2L);
        when(analiseRepositorio.countByResultadoConforme(false)).thenReturn(1L);
        when(ocorrenciaRepositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of());

        assertThat(painelCom(Clock.systemUTC()).geral().percentualConformidade())
                .isEqualTo(66.67);
    }

    @Test
    @DisplayName("conta como vencidas as pendentes que passaram do prazo")
    void contaVencidas() {
        Ocorrencia pendente = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA);
        Instant depoisDoPrazo = pendente.getPrazoLimite().plus(2, ChronoUnit.HOURS);

        when(analiseRepositorio.countByResultadoConforme(true)).thenReturn(1L);
        when(analiseRepositorio.countByResultadoConforme(false)).thenReturn(1L);
        when(ocorrenciaRepositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of(pendente));

        IndicadoresConformidade indicadores = painelCom(relogioFixoEm(depoisDoPrazo)).geral();

        assertThat(indicadores.ocorrenciasPendentes()).isEqualTo(1L);
        assertThat(indicadores.ocorrenciasVencidas()).isEqualTo(1L);
    }

    @Test
    @DisplayName("nao conta como vencida a pendente ainda dentro do prazo")
    void naoContaDentroDoPrazo() {
        Ocorrencia pendente = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA);
        Instant dentroDoPrazo = pendente.getAbertaEm().plus(1, ChronoUnit.HOURS);

        when(analiseRepositorio.countByResultadoConforme(true)).thenReturn(1L);
        when(analiseRepositorio.countByResultadoConforme(false)).thenReturn(1L);
        when(ocorrenciaRepositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of(pendente));

        IndicadoresConformidade indicadores = painelCom(relogioFixoEm(dentroDoPrazo)).geral();

        assertThat(indicadores.ocorrenciasPendentes()).isEqualTo(1L);
        assertThat(indicadores.ocorrenciasVencidas()).isZero();
    }

    @Test
    @DisplayName("consolida por ponto usando o codigo como escopo")
    void consolidaPorPonto() {
        when(pontoService.buscarPorCodigo("PMA-001")).thenReturn(Fixtures.pontoPersistido());
        when(analiseRepositorio.countByPontoIdAndResultadoConforme("ponto-1", true)).thenReturn(8L);
        when(analiseRepositorio.countByPontoIdAndResultadoConforme("ponto-1", false)).thenReturn(2L);
        when(ocorrenciaRepositorio.findByPontoIdOrderByAbertaEmDesc("ponto-1"))
                .thenReturn(List.of(
                        Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA),
                        Fixtures.ocorrenciaPersistida("oc-2", StatusOcorrencia.RESOLVIDA)));

        IndicadoresConformidade indicadores =
                painelCom(Clock.systemUTC()).porPonto("PMA-001");

        assertThat(indicadores.escopo()).isEqualTo("PMA-001");
        assertThat(indicadores.totalAnalises()).isEqualTo(10L);
        assertThat(indicadores.percentualConformidade()).isEqualTo(80.0);
        assertThat(indicadores.ocorrenciasPendentes())
                .as("a resolvida nao entra na fila de pendencias")
                .isEqualTo(1L);
    }
}
