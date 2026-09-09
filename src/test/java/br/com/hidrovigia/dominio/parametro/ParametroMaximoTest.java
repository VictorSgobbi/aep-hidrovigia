package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Parametro com valor maximo permitido")
class ParametroMaximoTest {

    private static ParametroMaximo turbidez() {
        return new ParametroMaximo("TURB", "Turbidez", "uT", "Portaria GM/MS 888/2021",
                RiscoSanitario.FISICO_QUIMICO, new BigDecimal("5.0"));
    }

    @ParameterizedTest(name = "turbidez {0} uT aprova")
    @ValueSource(strings = {"0", "0.5", "3.2", "5.0"})
    @DisplayName("aprova qualquer valor ate o teto, inclusive zero e o proprio teto")
    void aprovaAteOTeto(String valor) {
        assertThat(turbidez().avaliar(new BigDecimal(valor)).conforme()).isTrue();
    }

    @ParameterizedTest(name = "turbidez {0} uT reprova")
    @ValueSource(strings = {"5.01", "6", "40"})
    @DisplayName("reprova valores acima do teto")
    void reprovaAcimaDoTeto(String valor) {
        ResultadoParametro resultado = turbidez().avaliar(new BigDecimal(valor));

        assertThat(resultado.conforme()).isFalse();
        assertThat(resultado.mensagem())
                .contains("acima do valor maximo permitido")
                .contains("limite 5.0 uT");
    }

    @Test
    @DisplayName("nao possui limite inferior")
    void naoPossuiLimiteInferior() {
        ParametroMaximo turbidez = turbidez();

        assertThat(turbidez.limiteMinimo()).isNull();
        assertThat(turbidez.limiteMaximo()).isEqualByComparingTo("5.0");
        assertThat(turbidez.descricaoDoLimite()).isEqualTo("ate 5.0 uT");
    }

    @Test
    @DisplayName("omite a unidade na descricao quando ela nao existe")
    void omiteUnidadeVazia() {
        ParametroMaximo semUnidade = new ParametroMaximo("X", "Indice", null, "norma",
                RiscoSanitario.ORGANOLEPTICO, new BigDecimal("3"));

        assertThat(semUnidade.getUnidade()).isEmpty();
        assertThat(semUnidade.descricaoDoLimite()).isEqualTo("ate 3");
        assertThat(semUnidade.avaliar(new BigDecimal("4")).mensagem()).doesNotContain("null");
    }

    @Test
    @DisplayName("recusa teto nulo ou negativo")
    void recusaTetoInvalido() {
        assertThatThrownBy(() -> new ParametroMaximo("X", "Sem teto", "mg/L", "norma",
                RiscoSanitario.FISICO_QUIMICO, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new ParametroMaximo("X", "Teto negativo", "mg/L", "norma",
                RiscoSanitario.FISICO_QUIMICO, new BigDecimal("-0.1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode ser negativo");
    }
}
