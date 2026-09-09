package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Parametro de faixa")
class ParametroFaixaTest {

    private static ParametroFaixa ph() {
        return new ParametroFaixa("PH", "pH", "", "Portaria GM/MS 888/2021",
                RiscoSanitario.FISICO_QUIMICO, new BigDecimal("6.0"), new BigDecimal("9.0"));
    }

    private static ParametroFaixa cloroResidualLivre() {
        return new ParametroFaixa("CRL", "Cloro residual livre", "mg/L", "Portaria GM/MS 888/2021",
                RiscoSanitario.DESINFECCAO, new BigDecimal("0.2"), new BigDecimal("2.0"));
    }

    @ParameterizedTest(name = "pH {0} esta dentro do padrao")
    @ValueSource(strings = {"6.0", "6.5", "7.2", "8.9", "9.0"})
    @DisplayName("aprova valores dentro do intervalo, inclusive nos extremos")
    void aprovaValoresDentroDoIntervalo(String valor) {
        ResultadoParametro resultado = ph().avaliar(new BigDecimal(valor));

        assertThat(resultado.conforme()).isTrue();
        assertThat(resultado.naoConforme()).isFalse();
        assertThat(resultado.mensagem()).contains("dentro do padrao");
    }

    @ParameterizedTest(name = "pH {0} reprova")
    @ValueSource(strings = {"0", "5.9", "9.1", "14"})
    @DisplayName("reprova valores fora do intervalo")
    void reprovaValoresForaDoIntervalo(String valor) {
        ResultadoParametro resultado = ph().avaliar(new BigDecimal(valor));

        assertThat(resultado.conforme()).isFalse();
        assertThat(resultado.naoConforme()).isTrue();
    }

    @ParameterizedTest(name = "cloro {0} mg/L -> {1}")
    @CsvSource({
            "0.0,  abaixo do minimo",
            "0.19, abaixo do minimo",
            "2.01, acima do maximo",
            "5.0,  acima do maximo"
    })
    @DisplayName("aponta de que lado o valor estourou o limite")
    void apontaLadoDaViolacao(String valor, String trechoEsperado) {
        ResultadoParametro resultado = cloroResidualLivre().avaliar(new BigDecimal(valor));

        assertThat(resultado.conforme()).isFalse();
        assertThat(resultado.mensagem()).contains(trechoEsperado);
    }

    @Test
    @DisplayName("registra o limite aplicado junto do valor medido")
    void registraLimiteAplicado() {
        ResultadoParametro resultado = cloroResidualLivre().avaliar(new BigDecimal("1.0"));

        assertThat(resultado.codigo()).isEqualTo("CRL");
        assertThat(resultado.unidade()).isEqualTo("mg/L");
        assertThat(resultado.limiteMinimo()).isEqualByComparingTo("0.2");
        assertThat(resultado.limiteMaximo()).isEqualByComparingTo("2.0");
        assertThat(resultado.risco()).isEqualTo(RiscoSanitario.DESINFECCAO);
        assertThat(resultado.referenciaLegal()).isEqualTo("Portaria GM/MS 888/2021");
    }

    @Test
    @DisplayName("descreve o limite de forma legivel")
    void descreveLimite() {
        assertThat(cloroResidualLivre().descricaoDoLimite()).isEqualTo("entre 0.2 e 2.0 mg/L");
        assertThat(ph().descricaoDoLimite()).isEqualTo("entre 6.0 e 9.0");
    }

    @Test
    @DisplayName("recusa faixa com minimo maior que o maximo")
    void recusaFaixaInvertida() {
        assertThatThrownBy(() -> new ParametroFaixa("X", "Invertido", "mg/L", "norma",
                RiscoSanitario.FISICO_QUIMICO, new BigDecimal("9"), new BigDecimal("6")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimo nao pode ser maior");
    }

    @Test
    @DisplayName("recusa construcao sem limites")
    void recusaLimitesNulos() {
        assertThatThrownBy(() -> new ParametroFaixa("X", "Sem minimo", "mg/L", "norma",
                RiscoSanitario.FISICO_QUIMICO, null, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new ParametroFaixa("X", "Sem maximo", "mg/L", "norma",
                RiscoSanitario.FISICO_QUIMICO, BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("recusa valor medido nulo ou negativo")
    void recusaValorInvalido() {
        assertThatThrownBy(() -> ph().avaliar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigatorio");

        assertThatThrownBy(() -> ph().avaliar(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("recusa metadados obrigatorios em branco")
    void recusaMetadadosEmBranco() {
        assertThatThrownBy(() -> new ParametroFaixa("  ", "pH", "", "norma",
                RiscoSanitario.FISICO_QUIMICO, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigo");

        assertThatThrownBy(() -> new ParametroFaixa("PH", null, "", "norma",
                RiscoSanitario.FISICO_QUIMICO, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome");

        assertThatThrownBy(() -> new ParametroFaixa("PH", "pH", "", "  ",
                RiscoSanitario.FISICO_QUIMICO, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referencia legal");

        assertThatThrownBy(() -> new ParametroFaixa("PH", "pH", "", "norma",
                null, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("identidade e comparada pelo codigo do parametro")
    void identidadePeloCodigo() {
        ParametroPotabilidade umPh = ph();
        ParametroPotabilidade outroPh = new ParametroFaixa("PH", "Potencial hidrogenionico", "",
                "outra norma", RiscoSanitario.ORGANOLEPTICO, BigDecimal.ONE, BigDecimal.TEN);

        assertThat(umPh)
                .isEqualTo(umPh)
                .isEqualTo(outroPh)
                .hasSameHashCodeAs(outroPh)
                .isNotEqualTo(cloroResidualLivre())
                .isNotEqualTo("PH");

        assertThat(umPh.toString()).isEqualTo("PH (entre 6.0 e 9.0)");
    }
}
