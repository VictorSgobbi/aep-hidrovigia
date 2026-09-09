package br.com.hidrovigia.dominio.ocorrencia;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.gravidade.ClassificadorPorQuantidade;
import br.com.hidrovigia.dominio.gravidade.ClassificadorPorRiscoSanitario;
import br.com.hidrovigia.dominio.gravidade.Gravidade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Ocorrencia de nao-conformidade")
class OcorrenciaTest {

    @Test
    @DisplayName("abre critica com prazo de 24 horas quando ha deteccao microbiologica")
    void abreCritica() {
        Analise analise = Fixtures.analiseNaoConforme();

        Ocorrencia ocorrencia = Ocorrencia.abrir(analise,
                new ClassificadorPorRiscoSanitario(), Fixtures.violados(analise));

        assertThat(ocorrencia.getGravidade()).isEqualTo(Gravidade.CRITICA);
        assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.ABERTA);
        assertThat(ocorrencia.getPoliticaClassificacao()).isEqualTo("risco-sanitario");
        assertThat(ocorrencia.getAnaliseId()).isEqualTo("analise-2");
        assertThat(ocorrencia.getPontoCodigo()).isEqualTo("PMA-001");
        assertThat(ocorrencia.getTratativas()).isEmpty();
        assertThat(ocorrencia.getPrazoLimite())
                .isEqualTo(ocorrencia.getAbertaEm().plus(24, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("grava qual politica classificou a gravidade")
    void gravaPoliticaUtilizada() {
        Analise analise = Fixtures.analiseNaoConforme();

        Ocorrencia porQuantidade = Ocorrencia.abrir(analise,
                new ClassificadorPorQuantidade(), Fixtures.violados(analise));

        assertThat(porQuantidade.getPoliticaClassificacao()).isEqualTo("quantidade-de-desvios");
        assertThat(porQuantidade.getGravidade()).isEqualTo(Gravidade.ALTA);
    }

    @Test
    @DisplayName("copia os parametros violados como subdocumentos")
    void copiaParametrosViolados() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();

        assertThat(ocorrencia.getParametrosViolados())
                .extracting(ParametroViolado::codigo)
                .containsExactly("ECOLI", "CRL");
        assertThat(ocorrencia.getParametrosViolados().get(0).limiteAplicado())
                .isEqualTo("ausencia (UFC/100mL)");
        assertThat(ocorrencia.getParametrosViolados().get(0).mensagem())
                .contains("detectado na amostra");
    }

    @Test
    @DisplayName("registra tratativa e avanca para em tratativa")
    void registraTratativa() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();

        ocorrencia.registrarTratativa("Coleta de contraprova solicitada", "Leonardo");

        assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.EM_TRATATIVA);
        assertThat(ocorrencia.getTratativas()).hasSize(1);
        assertThat(ocorrencia.getTratativas().get(0).acao())
                .isEqualTo("Coleta de contraprova solicitada");
        assertThat(ocorrencia.getTratativas().get(0).por()).isEqualTo("Leonardo");
        assertThat(ocorrencia.getTratativas().get(0).statusResultante())
                .isEqualTo(StatusOcorrencia.EM_TRATATIVA);
    }

    @Test
    @DisplayName("resolve direto de aberta, sem exigir tratativa intermediaria")
    void resolveDiretoDeAberta() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();

        ocorrencia.resolver("Recloracao executada e contraprova conforme", "Victor");

        assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.RESOLVIDA);
        assertThat(ocorrencia.getStatus().finalizada()).isTrue();
        assertThat(ocorrencia.getTratativas()).hasSize(1);
    }

    @Test
    @DisplayName("acumula tratativas ate a resolucao")
    void acumulaTratativas() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();

        ocorrencia.registrarTratativa("Ponto isolado da rede", "Leonardo");
        ocorrencia.registrarTratativa("Desinfeccao do reservatorio", "Bruno");
        ocorrencia.resolver("Contraprova conforme", "Victor");

        assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.RESOLVIDA);
        assertThat(ocorrencia.getTratativas())
                .extracting(Tratativa::acao)
                .containsExactly("Ponto isolado da rede", "Desinfeccao do reservatorio",
                        "Contraprova conforme");
        assertThat(ocorrencia.getTratativas())
                .as("cada acao grava o status que ela produziu")
                .extracting(Tratativa::statusResultante)
                .containsExactly(StatusOcorrencia.EM_TRATATIVA, StatusOcorrencia.EM_TRATATIVA,
                        StatusOcorrencia.RESOLVIDA);
    }

    @Test
    @DisplayName("aceita quantas tratativas forem necessarias antes de resolver")
    void aceitaMuitasTratativas() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();

        for (int i = 1; i <= 5; i++) {
            ocorrencia.registrarTratativa("Acao " + i, "Leonardo");
        }

        assertThat(ocorrencia.getTratativas()).hasSize(5);
        assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.EM_TRATATIVA);
    }

    @Test
    @DisplayName("recusa qualquer acao depois de resolvida")
    void recusaAcaoAposResolver() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();
        ocorrencia.resolver("Contraprova conforme", "Victor");

        assertThatThrownBy(() -> ocorrencia.resolver("Resolvendo de novo", "Victor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transicao invalida");

        assertThatThrownBy(() -> ocorrencia.registrarTratativa("Acao tardia", "Bruno"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transicao invalida");

        assertThat(ocorrencia.getTratativas()).hasSize(1);
    }

    @Test
    @DisplayName("nao gera ocorrencia a partir de analise conforme")
    void recusaAnaliseConforme() {
        Analise conforme = Fixtures.analiseConforme();

        assertThatThrownBy(() -> Ocorrencia.abrir(conforme,
                new ClassificadorPorRiscoSanitario(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("analise conforme nao gera ocorrencia");
    }

    @Test
    @DisplayName("exige analise persistida e classificador informado")
    void exigePreCondicoes() {
        Analise semId = Analise.registrar(Fixtures.pontoPersistido(), "Tecnico",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Fixtures.parametrosComViolacaoCritica());

        assertThatThrownBy(() -> Ocorrencia.abrir(semId,
                new ClassificadorPorRiscoSanitario(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persistida");

        assertThatThrownBy(() -> Ocorrencia.abrir(Fixtures.analiseNaoConforme(), null, List.of()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Ocorrencia.abrir(null,
                new ClassificadorPorRiscoSanitario(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("considera vencida apenas enquanto nao resolvida")
    void controlaVencimento() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();
        Instant depoisDoPrazo = ocorrencia.getPrazoLimite().plus(1, ChronoUnit.HOURS);
        Instant dentroDoPrazo = ocorrencia.getAbertaEm().plus(1, ChronoUnit.HOURS);

        assertThat(ocorrencia.estaVencida(dentroDoPrazo)).isFalse();
        assertThat(ocorrencia.estaVencida(depoisDoPrazo)).isTrue();

        ocorrencia.resolver("Resolvida fora do prazo", "Victor");
        assertThat(ocorrencia.estaVencida(depoisDoPrazo)).isFalse();

        assertThatThrownBy(() -> ocorrencia.estaVencida(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("expoe as tratativas como copia, protegendo o historico")
    void tratativasImutaveisParaFora() {
        Ocorrencia ocorrencia = Fixtures.ocorrenciaAberta();
        ocorrencia.registrarTratativa("Acao", "Bruno");

        assertThatThrownBy(() -> ocorrencia.getTratativas().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("declara as transicoes validas no proprio enum")
    void transicoesDeclaradas() {
        assertThat(StatusOcorrencia.ABERTA.proximosPossiveis())
                .containsExactlyInAnyOrder(StatusOcorrencia.EM_TRATATIVA, StatusOcorrencia.RESOLVIDA);
        assertThat(StatusOcorrencia.EM_TRATATIVA.proximosPossiveis())
                .containsExactlyInAnyOrder(StatusOcorrencia.EM_TRATATIVA, StatusOcorrencia.RESOLVIDA);
        assertThat(StatusOcorrencia.RESOLVIDA.proximosPossiveis()).isEmpty();

        assertThat(StatusOcorrencia.ABERTA.podeTransicionarPara(null)).isFalse();
        assertThat(StatusOcorrencia.ABERTA.finalizada()).isFalse();
        assertThat(StatusOcorrencia.EM_TRATATIVA.getDescricao()).isEqualTo("Em tratativa");
    }

    @Test
    @DisplayName("permanecer em tratativa e o unico laco permitido no ciclo de vida")
    void unicoLacoPermitido() {
        assertThat(StatusOcorrencia.EM_TRATATIVA.podeTransicionarPara(StatusOcorrencia.EM_TRATATIVA))
                .as("acumular tratativas na mesma ocorrencia")
                .isTrue();

        assertThat(StatusOcorrencia.ABERTA.podeTransicionarPara(StatusOcorrencia.ABERTA))
                .as("nao existe reabrir o que ja esta aberto")
                .isFalse();
        assertThat(StatusOcorrencia.RESOLVIDA.podeTransicionarPara(StatusOcorrencia.RESOLVIDA))
                .as("nao existe resolver duas vezes")
                .isFalse();
        assertThat(StatusOcorrencia.EM_TRATATIVA.podeTransicionarPara(StatusOcorrencia.ABERTA))
                .as("nao existe voltar para aberta")
                .isFalse();
        assertThat(StatusOcorrencia.RESOLVIDA.podeTransicionarPara(StatusOcorrencia.EM_TRATATIVA))
                .as("nao existe reabrir uma resolvida")
                .isFalse();
    }

    @Test
    @DisplayName("tratativa exige acao e responsavel")
    void tratativaValidada() {
        assertThatThrownBy(() -> new Tratativa("  ", "Bruno", Instant.now(), StatusOcorrencia.ABERTA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acao");

        assertThatThrownBy(() -> new Tratativa("Acao", null, Instant.now(), StatusOcorrencia.ABERTA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("responsavel");

        Tratativa semData = new Tratativa("Acao", "Bruno", null, StatusOcorrencia.ABERTA);
        assertThat(semData.em()).isNotNull();
    }

    @Test
    @DisplayName("parametro violado recusa resultado conforme")
    void parametroVioladoRecusaConforme() {
        assertThatThrownBy(() -> ParametroViolado.de(Fixtures.avaliar("PH", "7.0"), "entre 6 e 9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao gera violacao");

        assertThatThrownBy(() -> ParametroViolado.de(null, "limite"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigatorio");
    }

    @Test
    @DisplayName("descreve a ocorrencia com gravidade, ponto e status")
    void descricaoLegivel() {
        assertThat(Fixtures.ocorrenciaAberta().toString())
                .isEqualTo("Ocorrencia CRITICA em PMA-001 (ABERTA)");
    }
}
