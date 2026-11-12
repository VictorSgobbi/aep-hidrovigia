package br.com.hidrovigia.servico;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.analise.ResultadoAnalise;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.repositorio.AnaliseRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Alerta de coleta vencida")
class MonitoramentoColetaServiceTest {

    @Mock
    private PontoMonitoramentoService pontoService;

    @Mock
    private AnaliseRepository analiseRepositorio;

    private MonitoramentoColetaService servico;

    @BeforeEach
    void montar() {
        servico = new MonitoramentoColetaService(
                pontoService, analiseRepositorio, Fixtures.relogioFixo());
    }

    private static PontoMonitoramento ponto(String id, String codigo, TipoFonte tipo) {
        return new PontoMonitoramento(id, codigo, "Ponto " + codigo, tipo, 100,
                Fixtures.localizacao(), Fixtures.responsavel(), true,
                Fixtures.REFERENCIA.minus(90, ChronoUnit.DAYS));
    }

    private static Optional<Analise> coletadaHa(String pontoId, String codigo, long dias) {
        return Optional.of(new Analise("an-" + pontoId, pontoId, codigo,
                Fixtures.REFERENCIA.minus(dias, ChronoUnit.DAYS), "Tecnico Bruno",
                Fixtures.parametrosConformes(), new ResultadoAnalise(true, 4, 0),
                Fixtures.REFERENCIA));
    }

    @Test
    @DisplayName("ponto coletado dentro da frequencia do tipo de fonte esta em dia")
    void emDia() {
        when(pontoService.listar(true)).thenReturn(List.of(ponto("p1", "PMA-001", TipoFonte.POCO_ARTESIANO)));
        when(analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc("p1"))
                .thenReturn(coletadaHa("p1", "PMA-001", 10));

        SituacaoColeta situacao = servico.situacaoDosPontosAtivos().get(0);

        assertThat(situacao.vencida()).isFalse();
        assertThat(situacao.frequenciaMinimaDias()).isEqualTo(30);
        assertThat(situacao.ultimaColeta()).isEqualTo(Fixtures.REFERENCIA.minus(10, ChronoUnit.DAYS));
        assertThat(situacao.proximaColetaAte()).isEqualTo(Fixtures.REFERENCIA.plus(20, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("a mesma idade de coleta vence um manancial superficial e nao um poco")
    void frequenciaDependeDoTipoDeFonte() {
        when(pontoService.listar(true)).thenReturn(List.of(
                ponto("p1", "PMA-001", TipoFonte.POCO_ARTESIANO),
                ponto("p2", "PMA-002", TipoFonte.MANANCIAL_SUPERFICIAL)));
        when(analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc("p1"))
                .thenReturn(coletadaHa("p1", "PMA-001", 10));
        when(analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc("p2"))
                .thenReturn(coletadaHa("p2", "PMA-002", 10));

        List<SituacaoColeta> situacoes = servico.situacaoDosPontosAtivos();

        assertThat(situacoes).extracting(SituacaoColeta::codigoPonto, SituacaoColeta::vencida)
                .as("vencidas primeiro")
                .containsExactly(tuple("PMA-002", true), tuple("PMA-001", false));
    }

    @Test
    @DisplayName("ponto que nunca teve coleta conta como vencido e vem antes de todos")
    void semColetaEVencido() {
        when(pontoService.listar(true)).thenReturn(List.of(
                ponto("p1", "PMA-001", TipoFonte.MANANCIAL_SUPERFICIAL),
                ponto("p3", "PMA-003", TipoFonte.CISTERNA)));
        when(analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc("p1"))
                .thenReturn(coletadaHa("p1", "PMA-001", 40));
        when(analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc("p3"))
                .thenReturn(Optional.empty());

        List<SituacaoColeta> situacoes = servico.situacaoDosPontosAtivos();

        assertThat(situacoes).allMatch(SituacaoColeta::vencida);
        SituacaoColeta semColeta = situacoes.get(0);
        assertThat(semColeta.codigoPonto()).isEqualTo("PMA-003");
        assertThat(semColeta.ultimaColeta()).isNull();
        assertThat(semColeta.proximaColetaAte()).isNull();
    }

    @Test
    @DisplayName("vence no instante seguinte ao limite, e nao no proprio limite")
    void limiteExato() {
        when(pontoService.listar(true)).thenReturn(List.of(ponto("p1", "PMA-001", TipoFonte.REDE_PUBLICA)));
        when(analiseRepositorio.findFirstByPontoIdOrderByColetadoEmDesc("p1"))
                .thenReturn(coletadaHa("p1", "PMA-001", 7));

        SituacaoColeta situacao = servico.situacaoDosPontosAtivos().get(0);

        assertThat(situacao.proximaColetaAte()).isEqualTo(Fixtures.REFERENCIA);
        assertThat(situacao.vencida()).isFalse();
    }

    @Test
    @DisplayName("so considera pontos ativos")
    void apenasAtivos() {
        when(pontoService.listar(true)).thenReturn(List.of());

        assertThat(servico.situacaoDosPontosAtivos()).isEmpty();
    }

    @Test
    @DisplayName("frequencia de cada tipo de fonte projeta o prazo da proxima coleta")
    void tipoFonteProjetaProximaColeta() {
        Instant ultima = Fixtures.REFERENCIA;

        assertThat(TipoFonte.CISTERNA.proximaColetaAte(ultima))
                .isEqualTo(ultima.plus(30, ChronoUnit.DAYS));
        assertThat(TipoFonte.CAMINHAO_PIPA.proximaColetaAte(ultima))
                .isEqualTo(ultima.plus(7, ChronoUnit.DAYS));
        assertThat(TipoFonte.values())
                .allMatch(tipo -> !tipo.getFrequenciaMinima().isNegative()
                        && !tipo.getFrequenciaMinima().isZero());
    }
}
