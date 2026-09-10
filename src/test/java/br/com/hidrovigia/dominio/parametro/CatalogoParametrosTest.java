package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Catalogo de parametros de potabilidade")
class CatalogoParametrosTest {

    private final CatalogoParametros catalogo = CatalogoParametros.padraoBrasileiro();

    @Test
    @DisplayName("registra os oito parametros previstos na PoC")
    void registraParametrosPrevistos() {
        assertThat(catalogo.tamanho()).isEqualTo(8);
        assertThat(catalogo.codigos())
                .containsExactlyInAnyOrder("ECOLI", "CTOT", "CRL", "PH",
                        "TURB", "NITRATO", "FLUOR", "COR");
        assertThat(catalogo.todos()).hasSize(8);
    }

    @ParameterizedTest(name = "{0} aplica limite \"{1}\"")
    @CsvSource({
            "ECOLI,   ausencia (UFC/100mL)",
            "CTOT,    ausencia (UFC/100mL)",
            "CRL,     entre 0.2 e 2.0 mg/L",
            "PH,      entre 6.0 e 9.0",
            "TURB,    ate 5.0 uT",
            "NITRATO, ate 10.0 mg/L",
            "FLUOR,   ate 1.5 mg/L",
            "COR,     ate 15.0 uH"
    })
    @DisplayName("aplica o limite normativo de cada parametro")
    void aplicaLimiteNormativo(String codigo, String limiteEsperado) {
        assertThat(catalogo.buscar(codigo).descricaoDoLimite()).isEqualTo(limiteEsperado);
    }

    @ParameterizedTest(name = "{0} e classificado como risco {1}")
    @CsvSource({
            "ECOLI,   MICROBIOLOGICO",
            "CTOT,    MICROBIOLOGICO",
            "CRL,     DESINFECCAO",
            "PH,      FISICO_QUIMICO",
            "TURB,    FISICO_QUIMICO",
            "NITRATO, FISICO_QUIMICO",
            "FLUOR,   FISICO_QUIMICO",
            "COR,     ORGANOLEPTICO"
    })
    @DisplayName("associa cada parametro ao seu risco sanitario")
    void associaRisco(String codigo, RiscoSanitario riscoEsperado) {
        assertThat(catalogo.buscar(codigo).getRisco()).isEqualTo(riscoEsperado);
    }

    @Test
    @DisplayName("preserva a ordem de declaracao, que e a ordem publicada pela API")
    void preservaOrdemDeDeclaracao() {
        // GET /api/painel/parametros expoe o catalogo nesta ordem. Um mapa
        // imutavel da JDK (Map.copyOf) randomizaria a iteracao a cada execucao
        // da JVM e deixaria a resposta da API instavel entre restarts.
        assertThat(catalogo.todos())
                .extracting(ParametroPotabilidade::getCodigo)
                .containsExactly("ECOLI", "CTOT", "CRL", "PH",
                        "TURB", "NITRATO", "FLUOR", "COR");
        assertThat(catalogo.codigos())
                .containsExactly("ECOLI", "CTOT", "CRL", "PH",
                        "TURB", "NITRATO", "FLUOR", "COR");
    }

    @Test
    @DisplayName("aceita o codigo em qualquer caixa e com espacos")
    void normalizaCodigo() {
        assertThat(catalogo.buscar("  ecoli  ").getCodigo()).isEqualTo("ECOLI");
        assertThat(catalogo.contem("Turb")).isTrue();
        assertThat(catalogo.contem("  ph ")).isTrue();
    }

    @Test
    @DisplayName("rejeita codigo fora do catalogo")
    void rejeitaCodigoDesconhecido() {
        assertThat(catalogo.contem("CHUMBO")).isFalse();
        assertThat(catalogo.contem(null)).isFalse();

        assertThatThrownBy(() -> catalogo.buscar("CHUMBO"))
                .isInstanceOf(ParametroDesconhecidoException.class)
                .hasMessageContaining("CHUMBO")
                .extracting(erro -> ((ParametroDesconhecidoException) erro).getCodigo())
                .isEqualTo("CHUMBO");
    }

    @Test
    @DisplayName("aponta a norma de referencia em todos os parametros")
    void apontaNorma() {
        assertThat(catalogo.todos())
                .allSatisfy(parametro -> assertThat(parametro.getReferenciaLegal())
                        .isEqualTo("Portaria GM/MS n. 888/2021"));
    }

    @Test
    @DisplayName("recusa catalogo vazio, nulo ou com codigo duplicado")
    void recusaCatalogoInvalido() {
        assertThatThrownBy(() -> new CatalogoParametros(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");

        assertThatThrownBy(() -> new CatalogoParametros(null))
                .isInstanceOf(NullPointerException.class);

        List<ParametroPotabilidade> duplicados = List.of(
                new ParametroMaximo("TURB", "Turbidez", "uT", "norma",
                        RiscoSanitario.FISICO_QUIMICO, new BigDecimal("5")),
                new ParametroMaximo("turb", "Turbidez repetida", "uT", "norma",
                        RiscoSanitario.FISICO_QUIMICO, new BigDecimal("1")));

        assertThatThrownBy(() -> new CatalogoParametros(duplicados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicado");
    }

    @Test
    @DisplayName("expoe colecoes imutaveis")
    void colecoesImutaveis() {
        assertThatThrownBy(() -> catalogo.todos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> catalogo.codigos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
