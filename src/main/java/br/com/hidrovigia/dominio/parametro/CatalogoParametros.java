package br.com.hidrovigia.dominio.parametro;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Conjunto de parametros aceitos pela PoC, indexado por codigo.
 *
 * <p>O catalogo e a fronteira entre o que a norma define e o que o sistema
 * aceita registrar: uma analise so entra no banco se todos os seus codigos
 * existirem aqui. Isso mantem os documentos da colecao {@code analises}
 * confrontaveis com a norma anos depois.
 *
 * <p><strong>Atencao antes da entrega final:</strong> os limites abaixo seguem
 * o padrao de potabilidade brasileiro (Portaria GM/MS n. 888/2021, que alterou
 * o Anexo XX da Portaria de Consolidacao n. 5/2017). Confira cada valor contra
 * o texto oficial antes de defender o trabalho — a PoC nao substitui a norma.
 */
public class CatalogoParametros {

    private static final String NORMA = "Portaria GM/MS n. 888/2021";

    private final Map<String, ParametroPotabilidade> porCodigo;

    public CatalogoParametros(Collection<ParametroPotabilidade> parametros) {
        Objects.requireNonNull(parametros, "lista de parametros e obrigatoria");
        if (parametros.isEmpty()) {
            throw new IllegalArgumentException("catalogo nao pode ficar vazio");
        }
        Map<String, ParametroPotabilidade> mapa = new LinkedHashMap<>();
        for (ParametroPotabilidade parametro : parametros) {
            Objects.requireNonNull(parametro, "parametro do catalogo nao pode ser nulo");
            ParametroPotabilidade anterior = mapa.put(normalizar(parametro.getCodigo()), parametro);
            if (anterior != null) {
                throw new IllegalArgumentException(
                        "codigo de parametro duplicado no catalogo: " + parametro.getCodigo());
            }
        }
        this.porCodigo = Map.copyOf(mapa);
    }

    /**
     * Catalogo com os parametros de potabilidade cobertos por esta PoC.
     */
    public static CatalogoParametros padraoBrasileiro() {
        return new CatalogoParametros(List.of(
                new ParametroAusencia("ECOLI", "Escherichia coli", "UFC/100mL", NORMA),
                new ParametroAusencia("CTOT", "Coliformes totais", "UFC/100mL", NORMA),
                new ParametroFaixa("CRL", "Cloro residual livre", "mg/L", NORMA,
                        RiscoSanitario.DESINFECCAO, new BigDecimal("0.2"), new BigDecimal("2.0")),
                new ParametroFaixa("PH", "pH", "", NORMA,
                        RiscoSanitario.FISICO_QUIMICO, new BigDecimal("6.0"), new BigDecimal("9.0")),
                new ParametroMaximo("TURB", "Turbidez", "uT", NORMA,
                        RiscoSanitario.FISICO_QUIMICO, new BigDecimal("5.0")),
                new ParametroMaximo("NITRATO", "Nitrato (como N)", "mg/L", NORMA,
                        RiscoSanitario.FISICO_QUIMICO, new BigDecimal("10.0")),
                new ParametroMaximo("FLUOR", "Fluoreto", "mg/L", NORMA,
                        RiscoSanitario.FISICO_QUIMICO, new BigDecimal("1.5")),
                new ParametroMaximo("COR", "Cor aparente", "uH", NORMA,
                        RiscoSanitario.ORGANOLEPTICO, new BigDecimal("15.0"))));
    }

    /**
     * @throws ParametroDesconhecidoException se o codigo nao estiver no catalogo
     */
    public ParametroPotabilidade buscar(String codigo) {
        ParametroPotabilidade parametro = porCodigo.get(normalizar(codigo));
        if (parametro == null) {
            throw new ParametroDesconhecidoException(codigo);
        }
        return parametro;
    }

    public boolean contem(String codigo) {
        return porCodigo.containsKey(normalizar(codigo));
    }

    public List<ParametroPotabilidade> todos() {
        return List.copyOf(porCodigo.values());
    }

    public Set<String> codigos() {
        return porCodigo.keySet();
    }

    public int tamanho() {
        return porCodigo.size();
    }

    private static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }
}
