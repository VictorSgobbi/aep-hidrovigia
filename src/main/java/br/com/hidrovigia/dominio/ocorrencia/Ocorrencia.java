package br.com.hidrovigia.dominio.ocorrencia;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.gravidade.ClassificadorGravidade;
import br.com.hidrovigia.dominio.gravidade.Gravidade;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Pendencia aberta a partir de uma analise reprovada.
 *
 * <p>Raiz da colecao {@code ocorrencias}. Guarda dois arrays de subdocumentos:
 * os parametros que reprovaram, congelados no momento da abertura, e o
 * historico de tratativas, que cresce conforme a vigilancia age.
 *
 * <p>O ciclo de vida e controlado por {@link StatusOcorrencia}: o agregado
 * recusa qualquer transicao que o enum nao autorize, entao nao existe caminho
 * para resolver duas vezes a mesma ocorrencia nem para reabrir uma resolvida.
 */
@Document(collection = "ocorrencias")
public class Ocorrencia {

    @Id
    private String id;

    @Indexed
    private String analiseId;

    @Indexed
    private String pontoId;

    private String pontoCodigo;
    private Gravidade gravidade;
    private String politicaClassificacao;
    private StatusOcorrencia status;
    private Instant abertaEm;
    private Instant prazoLimite;
    private List<ParametroViolado> parametrosViolados;
    private List<Tratativa> tratativas;

    @PersistenceCreator
    public Ocorrencia(String id,
                      String analiseId,
                      String pontoId,
                      String pontoCodigo,
                      Gravidade gravidade,
                      String politicaClassificacao,
                      StatusOcorrencia status,
                      Instant abertaEm,
                      Instant prazoLimite,
                      List<ParametroViolado> parametrosViolados,
                      List<Tratativa> tratativas) {
        this.id = id;
        this.analiseId = Objects.requireNonNull(analiseId, "analise de origem e obrigatoria");
        this.pontoId = Objects.requireNonNull(pontoId, "ponto e obrigatorio");
        this.pontoCodigo = Objects.requireNonNull(pontoCodigo, "codigo do ponto e obrigatorio");
        this.gravidade = Objects.requireNonNull(gravidade, "gravidade e obrigatoria");
        this.politicaClassificacao = Objects.requireNonNull(
                politicaClassificacao, "politica de classificacao e obrigatoria");
        this.status = Objects.requireNonNull(status, "status e obrigatorio");
        this.abertaEm = Objects.requireNonNull(abertaEm, "data de abertura e obrigatoria");
        this.prazoLimite = Objects.requireNonNull(prazoLimite, "prazo limite e obrigatorio");
        this.parametrosViolados = List.copyOf(Objects.requireNonNull(
                parametrosViolados, "parametros violados sao obrigatorios"));
        this.tratativas = new ArrayList<>(tratativas == null ? List.of() : tratativas);
    }

    /**
     * Abre uma ocorrencia para uma analise reprovada.
     *
     * @throws IllegalArgumentException se a analise estiver conforme
     */
    public static Ocorrencia abrir(Analise analise,
                                   ClassificadorGravidade classificador,
                                   List<ParametroViolado> violados) {
        Objects.requireNonNull(analise, "analise e obrigatoria");
        Objects.requireNonNull(classificador, "classificador de gravidade e obrigatorio");

        if (analise.conforme()) {
            throw new IllegalArgumentException("analise conforme nao gera ocorrencia");
        }
        if (analise.getId() == null) {
            throw new IllegalArgumentException("analise precisa estar persistida");
        }

        List<ResultadoParametro> naoConformidades = analise.naoConformidades();
        Gravidade gravidade = classificador.classificar(naoConformidades);
        Instant agora = Instant.now();

        return new Ocorrencia(null,
                analise.getId(),
                analise.getPontoId(),
                analise.getPontoCodigo(),
                gravidade,
                classificador.nome(),
                StatusOcorrencia.ABERTA,
                agora,
                agora.plus(Duration.ofHours(gravidade.getPrazoHoras())),
                violados,
                new ArrayList<>());
    }

    /**
     * Registra uma acao sem encerrar a pendencia.
     *
     * @throws IllegalStateException se a ocorrencia ja estiver resolvida
     */
    public void registrarTratativa(String acao, String por) {
        transicionar(StatusOcorrencia.EM_TRATATIVA, acao, por);
    }

    /**
     * Registra a acao final e encerra a pendencia.
     *
     * @throws IllegalStateException se a ocorrencia ja estiver resolvida
     */
    public void resolver(String acao, String por) {
        transicionar(StatusOcorrencia.RESOLVIDA, acao, por);
    }

    private void transicionar(StatusOcorrencia destino, String acao, String por) {
        if (!status.podeTransicionarPara(destino)) {
            throw new IllegalStateException("transicao invalida: ocorrencia "
                    + status.getDescricao().toLowerCase() + " nao pode ir para "
                    + destino.getDescricao().toLowerCase());
        }
        tratativas.add(new Tratativa(acao, por, Instant.now(), destino));
        status = destino;
    }

    /** Uma ocorrencia esta vencida quando passou do prazo sem ser resolvida. */
    public boolean estaVencida(Instant agora) {
        Objects.requireNonNull(agora, "instante de referencia e obrigatorio");
        return !status.finalizada() && agora.isAfter(prazoLimite);
    }

    public String getId() {
        return id;
    }

    public String getAnaliseId() {
        return analiseId;
    }

    public String getPontoId() {
        return pontoId;
    }

    public String getPontoCodigo() {
        return pontoCodigo;
    }

    public Gravidade getGravidade() {
        return gravidade;
    }

    public String getPoliticaClassificacao() {
        return politicaClassificacao;
    }

    public StatusOcorrencia getStatus() {
        return status;
    }

    public Instant getAbertaEm() {
        return abertaEm;
    }

    public Instant getPrazoLimite() {
        return prazoLimite;
    }

    public List<ParametroViolado> getParametrosViolados() {
        return parametrosViolados;
    }

    public List<Tratativa> getTratativas() {
        return List.copyOf(tratativas);
    }

    @Override
    public String toString() {
        return "Ocorrencia " + gravidade + " em " + pontoCodigo + " (" + status + ")";
    }
}
