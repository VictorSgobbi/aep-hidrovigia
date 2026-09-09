package br.com.hidrovigia.dominio.ponto;

import br.com.hidrovigia.Fixtures;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ponto de monitoramento")
class PontoMonitoramentoTest {

    @Test
    @DisplayName("nasce ativo, com codigo em caixa alta e data de cadastro")
    void nasceAtivo() {
        PontoMonitoramento ponto = PontoMonitoramento.cadastrar("pma-002", "  Cisterna Comunitaria  ",
                TipoFonte.CISTERNA, 80, Fixtures.localizacao(), Fixtures.responsavel());

        assertThat(ponto.getId()).isNull();
        assertThat(ponto.getCodigo()).isEqualTo("PMA-002");
        assertThat(ponto.getNome()).isEqualTo("Cisterna Comunitaria");
        assertThat(ponto.isAtivo()).isTrue();
        assertThat(ponto.getCriadoEm()).isNotNull();
        assertThat(ponto.getTipoFonte().getDescricao()).isEqualTo("Cisterna");
    }

    @Test
    @DisplayName("permite atualizar dados cadastrais sem trocar o codigo")
    void atualizaDadosCadastrais() {
        PontoMonitoramento ponto = Fixtures.pontoPersistido();
        Localizacao novaLocalizacao = new Localizacao("Sarandi", "pr", null, null);
        Responsavel novoResponsavel = new Responsavel("Carlos Lima", null, "carlos@exemplo.gov.br");

        ponto.atualizar("Poco Reformado", TipoFonte.NASCENTE, 500, novaLocalizacao, novoResponsavel);

        assertThat(ponto.getCodigo()).isEqualTo("PMA-001");
        assertThat(ponto.getNome()).isEqualTo("Poco Reformado");
        assertThat(ponto.getTipoFonte()).isEqualTo(TipoFonte.NASCENTE);
        assertThat(ponto.getPopulacaoAtendida()).isEqualTo(500);
        assertThat(ponto.getLocalizacao().uf()).isEqualTo("PR");
        assertThat(ponto.getResponsavel().registro()).isNull();
    }

    @Test
    @DisplayName("desativa e reativa sem perder o historico")
    void desativaEReativa() {
        PontoMonitoramento ponto = Fixtures.pontoPersistido();

        ponto.desativar();
        assertThat(ponto.isAtivo()).isFalse();

        ponto.reativar();
        assertThat(ponto.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("recusa cadastro sem codigo, nome, fonte, localizacao ou responsavel")
    void recusaCamposObrigatorios() {
        assertThatThrownBy(() -> PontoMonitoramento.cadastrar("  ", "Nome",
                TipoFonte.POCO_ARTESIANO, 10, Fixtures.localizacao(), Fixtures.responsavel()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigo do ponto");

        assertThatThrownBy(() -> PontoMonitoramento.cadastrar("PMA-003", null,
                TipoFonte.POCO_ARTESIANO, 10, Fixtures.localizacao(), Fixtures.responsavel()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome do ponto");

        assertThatThrownBy(() -> PontoMonitoramento.cadastrar("PMA-003", "Nome",
                null, 10, Fixtures.localizacao(), Fixtures.responsavel()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> PontoMonitoramento.cadastrar("PMA-003", "Nome",
                TipoFonte.POCO_ARTESIANO, 10, null, Fixtures.responsavel()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> PontoMonitoramento.cadastrar("PMA-003", "Nome",
                TipoFonte.POCO_ARTESIANO, 10, Fixtures.localizacao(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("recusa populacao atendida negativa")
    void recusaPopulacaoNegativa() {
        assertThatThrownBy(() -> PontoMonitoramento.cadastrar("PMA-004", "Nome",
                TipoFonte.REDE_PUBLICA, -1, Fixtures.localizacao(), Fixtures.responsavel()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("populacao atendida");
    }

    @Test
    @DisplayName("descreve o ponto com codigo, nome e municipio")
    void descricaoLegivel() {
        assertThat(Fixtures.pontoPersistido().toString())
                .isEqualTo("PMA-001 - Poco da Escola Rural Sao Jose (Maringa/PR)");
    }

    @Test
    @DisplayName("localizacao valida coordenadas e normaliza a UF")
    void localizacaoValida() {
        Localizacao comCoordenadas = new Localizacao(" Maringa ", "pr", -23.4, -51.9);

        assertThat(comCoordenadas.municipio()).isEqualTo("Maringa");
        assertThat(comCoordenadas.uf()).isEqualTo("PR");
        assertThat(comCoordenadas.possuiCoordenadas()).isTrue();

        Localizacao semCoordenadas = new Localizacao("Sarandi", "PR", null, null);
        assertThat(semCoordenadas.possuiCoordenadas()).isFalse();

        assertThatThrownBy(() -> new Localizacao("", "PR", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("municipio");
        assertThatThrownBy(() -> new Localizacao("Maringa", "PARANA", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uf");
        assertThatThrownBy(() -> new Localizacao("Maringa", "PR", -100.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
        assertThatThrownBy(() -> new Localizacao("Maringa", "PR", null, 200.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }

    @Test
    @DisplayName("responsavel exige nome e contato")
    void responsavelValido() {
        assertThatThrownBy(() -> new Responsavel(" ", "CREA", "contato"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome do responsavel");

        assertThatThrownBy(() -> new Responsavel("Ana", "CREA", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contato do responsavel");
    }
}
