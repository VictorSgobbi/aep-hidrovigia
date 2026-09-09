package br.com.hidrovigia.servico;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.analise.LeituraParametro;
import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.dominio.parametro.ParametroDesconhecidoException;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.repositorio.AnaliseRepository;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Servico de registro de analises")
class AnaliseServiceTest {

    @Mock
    private AnaliseRepository repositorio;

    @Mock
    private PontoMonitoramentoService pontoService;

    @Mock
    private OcorrenciaService ocorrenciaService;

    private final CatalogoParametros catalogo = CatalogoParametros.padraoBrasileiro();
    private AnaliseService servico;

    private final Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);

    @BeforeEach
    void preparar() {
        servico = new AnaliseService(repositorio, pontoService, ocorrenciaService, catalogo);
        when(pontoService.buscarPorCodigo("PMA-001")).thenReturn(Fixtures.pontoPersistido());
        when(repositorio.save(any(Analise.class))).thenAnswer(chamada -> {
            Analise recebida = chamada.getArgument(0);
            return new Analise("analise-salva", recebida.getPontoId(), recebida.getPontoCodigo(),
                    recebida.getColetadoEm(), recebida.getColetor(), recebida.getParametros(),
                    recebida.getResultado(), recebida.getRegistradoEm());
        });
    }

    private static List<LeituraParametro> leituras(String... paresCodigoValor) {
        return java.util.stream.IntStream.range(0, paresCodigoValor.length / 2)
                .mapToObj(i -> new LeituraParametro(
                        paresCodigoValor[i * 2], new BigDecimal(paresCodigoValor[i * 2 + 1])))
                .toList();
    }

    @Test
    @DisplayName("registra analise conforme sem abrir ocorrencia")
    void registraAnaliseConforme() {
        Analise analise = servico.registrar("PMA-001", "Tecnico Bruno", ontem,
                leituras("ECOLI", "0", "CRL", "0.8", "PH", "7.2", "TURB", "0.4"));

        assertThat(analise.conforme()).isTrue();
        assertThat(analise.getId()).isEqualTo("analise-salva");
        assertThat(analise.getParametros()).hasSize(4);
        verify(ocorrenciaService, never()).abrirPara(any());
    }

    @Test
    @DisplayName("abre ocorrencia quando algum parametro reprova")
    void abreOcorrenciaQuandoReprova() {
        Analise analise = servico.registrar("PMA-001", "Tecnico Bruno", ontem,
                leituras("ECOLI", "12", "CRL", "0.05", "PH", "7.0"));

        assertThat(analise.conforme()).isFalse();
        assertThat(analise.naoConformidades())
                .extracting(ResultadoParametro::codigo)
                .containsExactly("ECOLI", "CRL");
        verify(ocorrenciaService).abrirPara(analise);
    }

    @Test
    @DisplayName("preserva a ordem em que os parametros foram informados")
    void preservaOrdemDasLeituras() {
        Analise analise = servico.registrar("PMA-001", "Tecnico", ontem,
                leituras("TURB", "1.0", "PH", "7.0", "ECOLI", "0"));

        assertThat(analise.getParametros())
                .extracting(ResultadoParametro::codigo)
                .containsExactly("TURB", "PH", "ECOLI");
    }

    @Test
    @DisplayName("aplica o limite legal correspondente a cada codigo")
    void aplicaLimiteLegal() {
        Analise analise = servico.registrar("PMA-001", "Tecnico", ontem,
                leituras("CRL", "0.8"));

        ResultadoParametro cloro = analise.getParametros().get(0);
        assertThat(cloro.limiteMinimo()).isEqualByComparingTo("0.2");
        assertThat(cloro.limiteMaximo()).isEqualByComparingTo("2.0");
        assertThat(cloro.referenciaLegal()).isEqualTo("Portaria GM/MS n. 888/2021");
    }

    @Test
    @DisplayName("recusa coleta em ponto desativado")
    void recusaPontoDesativado() {
        PontoMonitoramento desativado = Fixtures.pontoPersistido();
        desativado.desativar();
        when(pontoService.buscarPorCodigo("PMA-001")).thenReturn(desativado);

        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", ontem,
                leituras("PH", "7.0")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("desativado");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("recusa analise sem nenhum parametro")
    void recusaSemParametros() {
        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", ontem, List.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ao menos um parametro");

        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", ontem, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("recusa o mesmo parametro informado duas vezes")
    void recusaParametroRepetido() {
        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", ontem,
                leituras("PH", "7.0", "ph", "8.0")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("duas vezes");
    }

    @Test
    @DisplayName("rejeita a analise inteira se um codigo estiver fora do catalogo")
    void rejeitaCodigoDesconhecido() {
        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", ontem,
                leituras("PH", "7.0", "CHUMBO", "0.5")))
                .isInstanceOf(ParametroDesconhecidoException.class)
                .hasMessageContaining("CHUMBO");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("traduz coleta futura em erro de regra de negocio")
    void traduzColetaFutura() {
        Instant amanha = Instant.now().plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", amanha,
                leituras("PH", "7.0")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("traduz valor negativo em erro de regra de negocio")
    void traduzValorNegativo() {
        assertThatThrownBy(() -> servico.registrar("PMA-001", "Tecnico", ontem,
                List.of(new LeituraParametro("PH", new BigDecimal("-1")))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("busca analise por identificador")
    void buscaPorId() {
        when(repositorio.findById("analise-1")).thenReturn(Optional.of(Fixtures.analiseConforme()));
        when(repositorio.findById("sumiu")).thenReturn(Optional.empty());

        assertThat(servico.buscarPorId("analise-1").getPontoCodigo()).isEqualTo("PMA-001");
        assertThatThrownBy(() -> servico.buscarPorId("sumiu"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("lista analises de um ponto pela ordem cronologica inversa")
    void listaPorPonto() {
        when(repositorio.findByPontoIdOrderByColetadoEmDesc("ponto-1"))
                .thenReturn(List.of(Fixtures.analiseNaoConforme(), Fixtures.analiseConforme()));

        assertThat(servico.listarPorPonto("PMA-001")).hasSize(2);
    }

    @Test
    @DisplayName("lista analises por periodo e valida o intervalo")
    void listaPorPeriodo() {
        Instant inicio = ontem;
        Instant fim = Instant.now();
        when(repositorio.findByColetadoEmBetweenOrderByColetadoEmDesc(inicio, fim))
                .thenReturn(List.of(Fixtures.analiseConforme()));

        assertThat(servico.listarPorPeriodo(inicio, fim)).hasSize(1);

        assertThatThrownBy(() -> servico.listarPorPeriodo(fim, inicio))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("posterior ao fim");

        assertThatThrownBy(() -> servico.listarPorPeriodo(null, fim))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inicio e fim");
    }
}
