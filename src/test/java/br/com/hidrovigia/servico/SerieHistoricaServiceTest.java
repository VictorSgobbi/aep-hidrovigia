package br.com.hidrovigia.servico;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.parametro.ParametroDesconhecidoException;
import br.com.hidrovigia.repositorio.AnaliseRepository;
import br.com.hidrovigia.repositorio.LeituraDiaria;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Serie historica por parametro")
class SerieHistoricaServiceTest {

    @Mock
    private PontoMonitoramentoService pontoService;

    @Mock
    private AnaliseRepository analiseRepositorio;

    private SerieHistoricaService servico;

    @BeforeEach
    void montar() {
        servico = new SerieHistoricaService(pontoService, analiseRepositorio,
                Fixtures.CATALOGO, Fixtures.relogioFixo());
    }

    @Test
    @DisplayName("sem periodo, devolve os ultimos 90 dias com o limite da norma")
    void periodoPadrao() {
        Instant inicioEsperado = Fixtures.REFERENCIA.minus(SerieHistoricaService.PERIODO_PADRAO);
        List<LeituraDiaria> dias = List.of(new LeituraDiaria(LocalDate.of(2026, 1, 10),
                new BigDecimal("6.8"), new BigDecimal("7.000"), new BigDecimal("7.2"), 2, 0));
        when(pontoService.buscarPorCodigo("PMA-001")).thenReturn(Fixtures.pontoPersistido());
        when(analiseRepositorio.serieDiaria("ponto-1", "PH", inicioEsperado, Fixtures.REFERENCIA))
                .thenReturn(dias);

        SerieHistorica serie = servico.serie("PMA-001", "ph", null, null);

        assertThat(serie.codigoParametro()).as("codigo normalizado pelo catalogo").isEqualTo("PH");
        assertThat(serie.nomeParametro()).isEqualTo("pH");
        assertThat(serie.limiteMinimo()).isEqualByComparingTo("6.0");
        assertThat(serie.limiteMaximo()).isEqualByComparingTo("9.0");
        assertThat(serie.inicio()).isEqualTo(inicioEsperado);
        assertThat(serie.fim()).isEqualTo(Fixtures.REFERENCIA);
        assertThat(serie.dias()).isEqualTo(dias);
    }

    @Test
    @DisplayName("respeita o periodo informado")
    void periodoInformado() {
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        Instant fim = Instant.parse("2026-01-05T00:00:00Z");
        when(pontoService.buscarPorCodigo("PMA-001")).thenReturn(Fixtures.pontoPersistido());
        when(analiseRepositorio.serieDiaria("ponto-1", "TURB", inicio, fim)).thenReturn(List.of());

        SerieHistorica serie = servico.serie("PMA-001", "TURB", inicio, fim);

        assertThat(serie.dias()).isEmpty();
        assertThat(serie.limiteMinimo()).as("turbidez so tem maximo").isNull();
    }

    @Test
    @DisplayName("recusa periodo invertido")
    void periodoInvertido() {
        when(pontoService.buscarPorCodigo("PMA-001")).thenReturn(Fixtures.pontoPersistido());

        assertThatThrownBy(() -> servico.serie("PMA-001", "PH",
                Fixtures.REFERENCIA, Fixtures.REFERENCIA.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("parametro fora do catalogo nao chega a consultar o banco")
    void parametroDesconhecido() {
        assertThatThrownBy(() -> servico.serie("PMA-001", "CHUMBO", null, null))
                .isInstanceOf(ParametroDesconhecidoException.class);
        verify(analiseRepositorio, never()).serieDiaria(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("ponto inexistente propaga o 404 do cadastro")
    void pontoInexistente() {
        when(pontoService.buscarPorCodigo("XXX")).thenThrow(RecursoNaoEncontradoException.ponto("XXX"));

        assertThatThrownBy(() -> servico.serie("XXX", "PH", null, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
