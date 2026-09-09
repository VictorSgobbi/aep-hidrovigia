package br.com.hidrovigia.dominio.analise;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Uma coleta avaliada contra o padrao de potabilidade.
 *
 * <p>Raiz da colecao {@code analises}. Guarda a lista de parametros medidos
 * como subdocumentos e um {@link ResultadoAnalise} consolidado. Referencia o
 * ponto pelo identificador e repete o codigo do ponto no documento — uma
 * desnormalizacao deliberada, para o painel listar ocorrencias sem precisar
 * juntar duas colecoes a cada leitura.
 *
 * <p>A analise nasce fechada: depois de registrada, nenhum parametro entra ou
 * sai. Corrigir uma coleta significa registrar outra analise, nao editar a
 * anterior — o historico de vigilancia precisa ser imutavel.
 */
@Document(collection = "analises")
public class Analise {

    @Id
    private String id;

    @Indexed
    private String pontoId;

    private String pontoCodigo;

    @Indexed
    private Instant coletadoEm;

    private String coletor;
    private List<ResultadoParametro> parametros;
    private ResultadoAnalise resultado;
    private Instant registradoEm;

    @PersistenceCreator
    public Analise(String id,
                   String pontoId,
                   String pontoCodigo,
                   Instant coletadoEm,
                   String coletor,
                   List<ResultadoParametro> parametros,
                   ResultadoAnalise resultado,
                   Instant registradoEm) {
        this.id = id;
        this.pontoId = Objects.requireNonNull(pontoId, "ponto da analise e obrigatorio");
        this.pontoCodigo = Objects.requireNonNull(pontoCodigo, "codigo do ponto e obrigatorio");
        this.coletadoEm = Objects.requireNonNull(coletadoEm, "data da coleta e obrigatoria");
        this.coletor = exigirTexto(coletor, "identificacao do coletor");
        this.parametros = List.copyOf(
                Objects.requireNonNull(parametros, "parametros da analise sao obrigatorios"));
        this.resultado = Objects.requireNonNull(resultado, "resultado consolidado e obrigatorio");
        this.registradoEm = registradoEm == null ? Instant.now() : registradoEm;
    }

    /**
     * Registra uma analise ja avaliada, consolidando o veredito.
     *
     * @param ponto      ponto de coleta, ja persistido
     * @param coletor    quem realizou a coleta
     * @param coletadoEm instante da coleta, que nao pode estar no futuro
     * @param avaliados  parametros ja confrontados com a norma
     * @throws IllegalArgumentException se a coleta for futura ou nao houver parametros
     */
    public static Analise registrar(PontoMonitoramento ponto,
                                    String coletor,
                                    Instant coletadoEm,
                                    List<ResultadoParametro> avaliados) {
        Objects.requireNonNull(ponto, "ponto de monitoramento e obrigatorio");
        Objects.requireNonNull(coletadoEm, "data da coleta e obrigatoria");

        if (avaliados == null || avaliados.isEmpty()) {
            throw new IllegalArgumentException("analise precisa conter ao menos um parametro");
        }
        if (coletadoEm.isAfter(Instant.now())) {
            throw new IllegalArgumentException("data da coleta nao pode estar no futuro");
        }
        if (ponto.getId() == null) {
            throw new IllegalArgumentException("ponto precisa estar persistido antes da analise");
        }

        long reprovados = avaliados.stream().filter(ResultadoParametro::naoConforme).count();
        ResultadoAnalise consolidado =
                new ResultadoAnalise(reprovados == 0, avaliados.size(), (int) reprovados);

        return new Analise(null, ponto.getId(), ponto.getCodigo(), coletadoEm,
                coletor, avaliados, consolidado, Instant.now());
    }

    /** Parametros que reprovaram, na ordem em que foram medidos. */
    public List<ResultadoParametro> naoConformidades() {
        return parametros.stream().filter(ResultadoParametro::naoConforme).toList();
    }

    public boolean conforme() {
        return resultado.conforme();
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatoria");
        }
        return valor.trim();
    }

    public String getId() {
        return id;
    }

    public String getPontoId() {
        return pontoId;
    }

    public String getPontoCodigo() {
        return pontoCodigo;
    }

    public Instant getColetadoEm() {
        return coletadoEm;
    }

    public String getColetor() {
        return coletor;
    }

    public List<ResultadoParametro> getParametros() {
        return parametros;
    }

    public ResultadoAnalise getResultado() {
        return resultado;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    @Override
    public String toString() {
        return "Analise " + pontoCodigo + " em " + coletadoEm
                + (conforme() ? " (conforme)" : " (" + resultado.qtdNaoConformidades() + " desvios)");
    }
}
