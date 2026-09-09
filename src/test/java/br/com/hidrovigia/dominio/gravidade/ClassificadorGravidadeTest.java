package br.com.hidrovigia.dominio.gravidade;

import java.math.BigDecimal;
import java.util.List;

import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.parametro.RiscoSanitario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Estrategias de classificacao de gravidade")
class ClassificadorGravidadeTest {

    private static ResultadoParametro violacao(String codigo, RiscoSanitario risco) {
        return new ResultadoParametro(codigo, codigo, BigDecimal.ONE, "mg/L",
                null, BigDecimal.ZERO, false, "violado", risco, "norma");
    }

    @Nested
    @DisplayName("Por risco sanitario")
    class PorRiscoSanitario {

        private final ClassificadorGravidade classificador = new ClassificadorPorRiscoSanitario();

        @ParameterizedTest(name = "pior risco {0} resulta em gravidade {1}")
        @CsvSource({
                "MICROBIOLOGICO, CRITICA",
                "DESINFECCAO,    ALTA",
                "FISICO_QUIMICO, ALTA",
                "ORGANOLEPTICO,  MEDIA"
        })
        @DisplayName("mapeia cada risco ao seu nivel de urgencia")
        void mapeiaRiscoParaGravidade(RiscoSanitario risco, Gravidade esperada) {
            assertThat(classificador.classificar(List.of(violacao("X", risco)))).isEqualTo(esperada);
        }

        @Test
        @DisplayName("considera apenas o pior risco, ignorando a quantidade")
        void consideraApenasOPiorRisco() {
            List<ResultadoParametro> muitosDesviosLeves = List.of(
                    violacao("COR", RiscoSanitario.ORGANOLEPTICO),
                    violacao("A", RiscoSanitario.ORGANOLEPTICO),
                    violacao("B", RiscoSanitario.ORGANOLEPTICO),
                    violacao("C", RiscoSanitario.ORGANOLEPTICO));

            List<ResultadoParametro> umaDeteccaoMicrobiologica = List.of(
                    violacao("ECOLI", RiscoSanitario.MICROBIOLOGICO));

            assertThat(classificador.classificar(muitosDesviosLeves)).isEqualTo(Gravidade.MEDIA);
            assertThat(classificador.classificar(umaDeteccaoMicrobiologica)).isEqualTo(Gravidade.CRITICA);
        }

        @Test
        @DisplayName("eleva a gravidade quando ha mistura de riscos")
        void elevaPeloPior() {
            List<ResultadoParametro> mistura = List.of(
                    violacao("COR", RiscoSanitario.ORGANOLEPTICO),
                    violacao("CRL", RiscoSanitario.DESINFECCAO),
                    violacao("PH", RiscoSanitario.FISICO_QUIMICO));

            assertThat(classificador.classificar(mistura)).isEqualTo(Gravidade.ALTA);
        }

        @Test
        @DisplayName("identifica a politica pelo nome")
        void identificaPolitica() {
            assertThat(classificador.nome()).isEqualTo("risco-sanitario");
        }
    }

    @Nested
    @DisplayName("Por quantidade de desvios")
    class PorQuantidade {

        private final ClassificadorGravidade classificador = new ClassificadorPorQuantidade();

        @Test
        @DisplayName("um desvio e media, dois e alta, tres ou mais e critica")
        void escalaComAQuantidade() {
            ResultadoParametro leve = violacao("COR", RiscoSanitario.ORGANOLEPTICO);

            assertThat(classificador.classificar(List.of(leve)))
                    .isEqualTo(Gravidade.MEDIA);
            assertThat(classificador.classificar(List.of(leve, leve)))
                    .isEqualTo(Gravidade.ALTA);
            assertThat(classificador.classificar(List.of(leve, leve, leve)))
                    .isEqualTo(Gravidade.CRITICA);
            assertThat(classificador.classificar(List.of(leve, leve, leve, leve)))
                    .isEqualTo(Gravidade.CRITICA);
        }

        @Test
        @DisplayName("ignora o risco sanitario de cada parametro")
        void ignoraRisco() {
            assertThat(classificador.classificar(
                    List.of(violacao("ECOLI", RiscoSanitario.MICROBIOLOGICO))))
                    .isEqualTo(Gravidade.MEDIA);
        }

        @Test
        @DisplayName("identifica a politica pelo nome")
        void identificaPolitica() {
            assertThat(classificador.nome()).isEqualTo("quantidade-de-desvios");
        }
    }

    @Test
    @DisplayName("nenhuma politica classifica analise sem nao-conformidade")
    void recusaListaVazia() {
        List<ClassificadorGravidade> politicas =
                List.of(new ClassificadorPorRiscoSanitario(), new ClassificadorPorQuantidade());

        for (ClassificadorGravidade politica : politicas) {
            assertThatThrownBy(() -> politica.classificar(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao possui nao-conformidades");

            assertThatThrownBy(() -> politica.classificar(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("cada gravidade carrega o prazo de tratativa")
    void gravidadeCarregaPrazo() {
        assertThat(Gravidade.CRITICA.getPrazoHoras()).isEqualTo(24);
        assertThat(Gravidade.ALTA.getPrazoHoras()).isEqualTo(72);
        assertThat(Gravidade.MEDIA.getPrazoHoras()).isEqualTo(168);
        assertThat(Gravidade.CRITICA.getDescricao()).isEqualTo("Critica");
    }

    @Test
    @DisplayName("risco sanitario compara urgencia entre si")
    void comparaRiscos() {
        assertThat(RiscoSanitario.MICROBIOLOGICO.maisGraveQue(RiscoSanitario.DESINFECCAO)).isTrue();
        assertThat(RiscoSanitario.ORGANOLEPTICO.maisGraveQue(RiscoSanitario.FISICO_QUIMICO)).isFalse();
        assertThat(RiscoSanitario.ORGANOLEPTICO.maisGraveQue(null)).isTrue();
        assertThat(RiscoSanitario.DESINFECCAO.getDescricao()).isEqualTo("Desinfeccao");
        assertThat(RiscoSanitario.DESINFECCAO.getPeso()).isEqualTo(3);
    }
}
