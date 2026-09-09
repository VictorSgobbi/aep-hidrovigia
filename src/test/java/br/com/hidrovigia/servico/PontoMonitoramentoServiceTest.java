package br.com.hidrovigia.servico;

import java.util.List;
import java.util.Optional;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.repositorio.PontoMonitoramentoRepository;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servico de pontos de monitoramento")
class PontoMonitoramentoServiceTest {

    @Mock
    private PontoMonitoramentoRepository repositorio;

    @InjectMocks
    private PontoMonitoramentoService servico;

    @Test
    @DisplayName("cadastra ponto quando o codigo esta livre")
    void cadastraPonto() {
        when(repositorio.existsByCodigo("PMA-001")).thenReturn(false);
        when(repositorio.save(any(PontoMonitoramento.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        PontoMonitoramento salvo = servico.cadastrar("pma-001", "Poco da Escola",
                TipoFonte.POCO_ARTESIANO, 320, Fixtures.localizacao(), Fixtures.responsavel());

        ArgumentCaptor<PontoMonitoramento> capturado =
                ArgumentCaptor.forClass(PontoMonitoramento.class);
        verify(repositorio).save(capturado.capture());

        assertThat(capturado.getValue().getCodigo()).isEqualTo("PMA-001");
        assertThat(capturado.getValue().isAtivo()).isTrue();
        assertThat(salvo.getNome()).isEqualTo("Poco da Escola");
    }

    @Test
    @DisplayName("recusa cadastro com codigo ja utilizado")
    void recusaCodigoDuplicado() {
        when(repositorio.existsByCodigo("PMA-001")).thenReturn(true);

        assertThatThrownBy(() -> servico.cadastrar("PMA-001", "Outro poco",
                TipoFonte.NASCENTE, 10, Fixtures.localizacao(), Fixtures.responsavel()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Ja existe um ponto");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("busca por identificador e por codigo normalizado")
    void buscaPonto() {
        PontoMonitoramento ponto = Fixtures.pontoPersistido();
        when(repositorio.findById("ponto-1")).thenReturn(Optional.of(ponto));
        when(repositorio.findByCodigo("PMA-001")).thenReturn(Optional.of(ponto));

        assertThat(servico.buscarPorId("ponto-1")).isSameAs(ponto);
        assertThat(servico.buscarPorCodigo("  pma-001 ")).isSameAs(ponto);
    }

    @Test
    @DisplayName("lanca not found quando o ponto nao existe")
    void pontoInexistente() {
        when(repositorio.findById("xpto")).thenReturn(Optional.empty());
        when(repositorio.findByCodigo("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscarPorId("xpto"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("xpto");

        assertThatThrownBy(() -> servico.buscarPorCodigo("nao-existe"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("lista todos ou apenas os ativos conforme o filtro")
    void listaComFiltro() {
        when(repositorio.findByAtivoTrue()).thenReturn(List.of(Fixtures.pontoPersistido()));
        when(repositorio.findAll()).thenReturn(List.of(
                Fixtures.pontoPersistido("a"), Fixtures.pontoPersistido("b")));

        assertThat(servico.listar(true)).hasSize(1);
        assertThat(servico.listar(false)).hasSize(2);
    }

    @Test
    @DisplayName("lista pontos por municipio navegando no subdocumento")
    void listaPorMunicipio() {
        when(repositorio.findByLocalizacaoMunicipioIgnoreCase("maringa"))
                .thenReturn(List.of(Fixtures.pontoPersistido()));

        assertThat(servico.listarPorMunicipio("maringa")).hasSize(1);
    }

    @Test
    @DisplayName("atualiza dados cadastrais do ponto existente")
    void atualizaPonto() {
        PontoMonitoramento ponto = Fixtures.pontoPersistido();
        when(repositorio.findById("ponto-1")).thenReturn(Optional.of(ponto));
        when(repositorio.save(ponto)).thenReturn(ponto);

        PontoMonitoramento atualizado = servico.atualizar("ponto-1", "Poco Reformado",
                TipoFonte.NASCENTE, 400, Fixtures.localizacao(), Fixtures.responsavel());

        assertThat(atualizado.getNome()).isEqualTo("Poco Reformado");
        assertThat(atualizado.getTipoFonte()).isEqualTo(TipoFonte.NASCENTE);
        assertThat(atualizado.getPopulacaoAtendida()).isEqualTo(400);
    }

    @Test
    @DisplayName("desativa ponto ativo e recusa desativar duas vezes")
    void desativaPonto() {
        PontoMonitoramento ponto = Fixtures.pontoPersistido();
        when(repositorio.findById("ponto-1")).thenReturn(Optional.of(ponto));
        when(repositorio.save(ponto)).thenReturn(ponto);

        assertThat(servico.desativar("ponto-1").isAtivo()).isFalse();

        assertThatThrownBy(() -> servico.desativar("ponto-1"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta desativado");
    }

    @Test
    @DisplayName("reativa ponto desativado")
    void reativaPonto() {
        PontoMonitoramento ponto = Fixtures.pontoPersistido();
        ponto.desativar();
        when(repositorio.findById("ponto-1")).thenReturn(Optional.of(ponto));
        when(repositorio.save(ponto)).thenReturn(ponto);

        assertThat(servico.reativar("ponto-1").isAtivo()).isTrue();
    }
}
