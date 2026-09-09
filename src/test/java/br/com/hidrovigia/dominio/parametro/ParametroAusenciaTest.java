package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Parametro microbiologico de ausencia obrigatoria")
class ParametroAusenciaTest {

    private static ParametroAusencia escherichiaColi() {
        return new ParametroAusencia("ECOLI", "Escherichia coli", "UFC/100mL",
                "Portaria GM/MS 888/2021");
    }

    @Test
    @DisplayName("aprova somente contagem zero")
    void aprovaApenasAusencia() {
        ResultadoParametro resultado = escherichiaColi().avaliar(BigDecimal.ZERO);

        assertThat(resultado.conforme()).isTrue();
        assertThat(resultado.mensagem()).contains("dentro do padrao");
    }

    @ParameterizedTest(name = "contagem {0} reprova")
    @ValueSource(strings = {"1", "0.5", "12", "2400"})
    @DisplayName("reprova qualquer deteccao, por menor que seja")
    void reprovaQualquerDeteccao(String valor) {
        ResultadoParametro resultado = escherichiaColi().avaliar(new BigDecimal(valor));

        assertThat(resultado.conforme()).isFalse();
        assertThat(resultado.mensagem())
                .contains("detectado na amostra")
                .contains("exigida ausencia");
    }

    @Test
    @DisplayName("trata zero com escala decimal diferente como ausencia")
    void zeroComEscalaDiferente() {
        assertThat(escherichiaColi().avaliar(new BigDecimal("0.00")).conforme()).isTrue();
    }

    @Test
    @DisplayName("classifica o risco como microbiologico sem permitir escolha")
    void riscoSempreMicrobiologico() {
        ParametroAusencia coliformes = new ParametroAusencia("CTOT", "Coliformes totais",
                "UFC/100mL", "Portaria GM/MS 888/2021");

        assertThat(coliformes.getRisco()).isEqualTo(RiscoSanitario.MICROBIOLOGICO);
        assertThat(coliformes.avaliar(BigDecimal.ONE).risco())
                .isEqualTo(RiscoSanitario.MICROBIOLOGICO);
    }

    @Test
    @DisplayName("expoe teto zero e nenhum piso")
    void limites() {
        ParametroAusencia ecoli = escherichiaColi();

        assertThat(ecoli.limiteMinimo()).isNull();
        assertThat(ecoli.limiteMaximo()).isEqualByComparingTo("0");
        assertThat(ecoli.descricaoDoLimite()).isEqualTo("ausencia (UFC/100mL)");
    }

    @Test
    @DisplayName("descreve ausencia sem parenteses quando nao ha unidade")
    void descricaoSemUnidade() {
        ParametroAusencia semUnidade = new ParametroAusencia("X", "Indicador", "", "norma");

        assertThat(semUnidade.descricaoDoLimite()).isEqualTo("ausencia");
    }
}
