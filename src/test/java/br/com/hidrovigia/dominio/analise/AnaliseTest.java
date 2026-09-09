package br.com.hidrovigia.dominio.analise;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Analise de potabilidade")
class AnaliseTest {

    private final PontoMonitoramento ponto = Fixtures.pontoPersistido();
    private final Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);

    @Test
    @DisplayName("consolida como conforme quando nenhum parametro reprova")
    void consolidaConforme() {
        Analise analise = Analise.registrar(ponto, "Tecnico Bruno", ontem,
                Fixtures.parametrosConformes());

        assertThat(analise.conforme()).isTrue();
        assertThat(analise.naoConformidades()).isEmpty();
        assertThat(analise.getResultado().qtdParametros()).isEqualTo(4);
        assertThat(analise.getResultado().qtdNaoConformidades()).isZero();
        assertThat(analise.getResultado().percentualConformidade()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("consolida como reprovada e lista os parametros violados")
    void consolidaNaoConforme() {
        Analise analise = Analise.registrar(ponto, "Tecnico Bruno", ontem,
                Fixtures.parametrosComViolacaoCritica());

        assertThat(analise.conforme()).isFalse();
        assertThat(analise.naoConformidades())
                .extracting(ResultadoParametro::codigo)
                .containsExactly("ECOLI", "CRL");
        assertThat(analise.getResultado().qtdNaoConformidades()).isEqualTo(2);
        assertThat(analise.getResultado().percentualConformidade()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("copia dados do ponto para permitir leitura sem juntar colecoes")
    void copiaDadosDoPonto() {
        Analise analise = Analise.registrar(ponto, "  Tecnico Bruno  ", ontem,
                Fixtures.parametrosConformes());

        assertThat(analise.getPontoId()).isEqualTo("ponto-1");
        assertThat(analise.getPontoCodigo()).isEqualTo("PMA-001");
        assertThat(analise.getColetor()).isEqualTo("Tecnico Bruno");
        assertThat(analise.getColetadoEm()).isEqualTo(ontem);
        assertThat(analise.getRegistradoEm()).isNotNull();
        assertThat(analise.getId()).isNull();
    }

    @Test
    @DisplayName("recusa coleta com data no futuro")
    void recusaColetaFutura() {
        Instant amanha = Instant.now().plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> Analise.registrar(ponto, "Tecnico", amanha,
                Fixtures.parametrosConformes()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("recusa analise sem parametros")
    void recusaSemParametros() {
        assertThatThrownBy(() -> Analise.registrar(ponto, "Tecnico", ontem, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos um parametro");

        assertThatThrownBy(() -> Analise.registrar(ponto, "Tecnico", ontem, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos um parametro");
    }

    @Test
    @DisplayName("recusa ponto ainda nao persistido")
    void recusaPontoSemIdentificador() {
        assertThatThrownBy(() -> Analise.registrar(Fixtures.pontoNovo(), "Tecnico", ontem,
                Fixtures.parametrosConformes()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persistido");
    }

    @Test
    @DisplayName("recusa coletor em branco e ponto nulo")
    void recusaColetorEmBranco() {
        assertThatThrownBy(() -> Analise.registrar(ponto, "   ", ontem,
                Fixtures.parametrosConformes()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coletor");

        assertThatThrownBy(() -> Analise.registrar(null, "Tecnico", ontem,
                Fixtures.parametrosConformes()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("expoe a lista de parametros como copia imutavel")
    void parametrosImutaveis() {
        Analise analise = Analise.registrar(ponto, "Tecnico", ontem,
                Fixtures.parametrosConformes());

        assertThatThrownBy(() -> analise.getParametros().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("descreve a analise com ponto, data e desvios")
    void descricaoLegivel() {
        assertThat(Fixtures.analiseConforme().toString()).contains("PMA-001", "(conforme)");
        assertThat(Fixtures.analiseNaoConforme().toString()).contains("PMA-001", "2 desvios");
    }

    @Test
    @DisplayName("resultado consolidado recusa numeros incoerentes")
    void resultadoConsolidadoCoerente() {
        assertThatThrownBy(() -> new ResultadoAnalise(true, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos um parametro");

        assertThatThrownBy(() -> new ResultadoAnalise(false, 3, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incoerente");

        assertThatThrownBy(() -> new ResultadoAnalise(false, 3, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incoerente");

        assertThatThrownBy(() -> new ResultadoAnalise(true, 3, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao confere");
    }
}
