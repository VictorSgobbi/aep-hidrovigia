package br.com.hidrovigia.servico;

import java.util.List;
import java.util.Optional;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.gravidade.ClassificadorPorRiscoSanitario;
import br.com.hidrovigia.dominio.gravidade.Gravidade;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.ParametroViolado;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.repositorio.OcorrenciaRepository;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servico de ocorrencias")
class OcorrenciaServiceTest {

    @Mock
    private OcorrenciaRepository repositorio;

    private OcorrenciaService servico;

    @BeforeEach
    void preparar() {
        servico = new OcorrenciaService(repositorio,
                CatalogoParametros.padraoBrasileiro(), new ClassificadorPorRiscoSanitario());
    }

    @Test
    @DisplayName("abre ocorrencia critica a partir de analise reprovada")
    void abreOcorrencia() {
        when(repositorio.save(any(Ocorrencia.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        Ocorrencia ocorrencia = servico.abrirPara(Fixtures.analiseNaoConforme());

        assertThat(ocorrencia.getGravidade()).isEqualTo(Gravidade.CRITICA);
        assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.ABERTA);
        assertThat(ocorrencia.getParametrosViolados())
                .extracting(ParametroViolado::codigo)
                .containsExactly("ECOLI", "CRL");
    }

    @Test
    @DisplayName("copia o limite legal de cada parametro violado")
    void copiaLimiteLegal() {
        when(repositorio.save(any(Ocorrencia.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        Ocorrencia ocorrencia = servico.abrirPara(Fixtures.analiseNaoConforme());

        assertThat(ocorrencia.getParametrosViolados())
                .extracting(ParametroViolado::limiteAplicado)
                .containsExactly("ausencia (UFC/100mL)", "entre 0.2 e 2.0 mg/L");
    }

    @Test
    @DisplayName("recusa abrir ocorrencia para analise conforme")
    void recusaAnaliseConforme() {
        assertThatThrownBy(() -> servico.abrirPara(Fixtures.analiseConforme()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("esta conforme");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("busca ocorrencia por identificador")
    void buscaPorId() {
        when(repositorio.findById("oc-1"))
                .thenReturn(Optional.of(Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA)));
        when(repositorio.findById("sumiu")).thenReturn(Optional.empty());

        assertThat(servico.buscarPorId("oc-1").getId()).isEqualTo("oc-1");
        assertThatThrownBy(() -> servico.buscarPorId("sumiu"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("sumiu");
    }

    @Test
    @DisplayName("sem filtro, lista as pendentes pelo prazo mais proximo de vencer")
    void listaPendentesPorPrazo() {
        when(repositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of(Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA)));

        assertThat(servico.listar(null)).hasSize(1);
    }

    @Test
    @DisplayName("com filtro, lista pelo status informado")
    void listaPorStatus() {
        when(repositorio.findByStatusOrderByAbertaEmDesc(StatusOcorrencia.RESOLVIDA))
                .thenReturn(List.of(
                        Fixtures.ocorrenciaPersistida("oc-2", StatusOcorrencia.RESOLVIDA)));

        assertThat(servico.listar(StatusOcorrencia.RESOLVIDA)).hasSize(1);
    }

    @Test
    @DisplayName("lista ocorrencias de um ponto")
    void listaPorPonto() {
        when(repositorio.findByPontoIdOrderByAbertaEmDesc("ponto-1"))
                .thenReturn(List.of(Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA)));

        assertThat(servico.listarPorPonto("ponto-1")).hasSize(1);
    }

    @Test
    @DisplayName("registra tratativa e avanca o status")
    void registraTratativa() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.ABERTA);
        when(repositorio.findById("oc-1")).thenReturn(Optional.of(ocorrencia));
        when(repositorio.save(ocorrencia)).thenReturn(ocorrencia);

        Ocorrencia atualizada = servico.registrarTratativa("oc-1", "Ponto isolado", "Leonardo");

        assertThat(atualizada.getStatus()).isEqualTo(StatusOcorrencia.EM_TRATATIVA);
        assertThat(atualizada.getTratativas()).hasSize(1);
    }

    @Test
    @DisplayName("resolve a ocorrencia e encerra o ciclo")
    void resolveOcorrencia() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.EM_TRATATIVA);
        when(repositorio.findById("oc-1")).thenReturn(Optional.of(ocorrencia));
        when(repositorio.save(ocorrencia)).thenReturn(ocorrencia);

        Ocorrencia resolvida = servico.resolver("oc-1", "Contraprova conforme", "Victor");

        assertThat(resolvida.getStatus()).isEqualTo(StatusOcorrencia.RESOLVIDA);
    }

    @Test
    @DisplayName("traduz transicao invalida em erro de regra de negocio")
    void traduzTransicaoInvalida() {
        Ocorrencia resolvida = Fixtures.ocorrenciaPersistida("oc-1", StatusOcorrencia.RESOLVIDA);
        when(repositorio.findById("oc-1")).thenReturn(Optional.of(resolvida));

        assertThatThrownBy(() -> servico.resolver("oc-1", "De novo", "Victor"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("transicao invalida");

        assertThatThrownBy(() -> servico.registrarTratativa("oc-1", "Tardia", "Bruno"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("transicao invalida");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("conta as ocorrencias ainda pendentes")
    void contaPendentes() {
        when(repositorio.countByStatusNot(StatusOcorrencia.RESOLVIDA)).thenReturn(7L);

        assertThat(servico.contarPendentes()).isEqualTo(7L);
    }
}
