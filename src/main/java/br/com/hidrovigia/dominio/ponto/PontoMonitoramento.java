package br.com.hidrovigia.dominio.ponto;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Local onde a agua e coletada para analise.
 *
 * <p>Raiz da colecao {@code pontos_monitoramento}. Carrega dois subdocumentos
 * aninhados — {@link Localizacao} e {@link Responsavel} — e e referenciado por
 * {@code analises} e {@code ocorrencias} atraves do seu identificador.
 *
 * <p>O ponto nunca e apagado: pocos desativados precisam continuar existindo
 * para que o historico de analises permaneca legivel. {@link #desativar()}
 * marca a saida de operacao sem quebrar as referencias.
 */
@Document(collection = "pontos_monitoramento")
public class PontoMonitoramento {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private String nome;
    private TipoFonte tipoFonte;
    private int populacaoAtendida;
    private Localizacao localizacao;
    private Responsavel responsavel;
    private boolean ativo;
    private Instant criadoEm;

    @PersistenceCreator
    public PontoMonitoramento(String id,
                              String codigo,
                              String nome,
                              TipoFonte tipoFonte,
                              int populacaoAtendida,
                              Localizacao localizacao,
                              Responsavel responsavel,
                              boolean ativo,
                              Instant criadoEm) {
        this.id = id;
        this.codigo = exigirTexto(codigo, "codigo do ponto").toUpperCase();
        this.nome = exigirTexto(nome, "nome do ponto");
        this.tipoFonte = Objects.requireNonNull(tipoFonte, "tipo de fonte e obrigatorio");
        this.populacaoAtendida = exigirPopulacaoValida(populacaoAtendida);
        this.localizacao = Objects.requireNonNull(localizacao, "localizacao e obrigatoria");
        this.responsavel = Objects.requireNonNull(responsavel, "responsavel e obrigatorio");
        this.ativo = ativo;
        this.criadoEm = criadoEm == null ? Instant.now() : criadoEm;
    }

    /**
     * Cria um ponto novo, ativo e com data de cadastro no instante atual.
     */
    public static PontoMonitoramento cadastrar(String codigo,
                                               String nome,
                                               TipoFonte tipoFonte,
                                               int populacaoAtendida,
                                               Localizacao localizacao,
                                               Responsavel responsavel) {
        return new PontoMonitoramento(null, codigo, nome, tipoFonte, populacaoAtendida,
                localizacao, responsavel, true, Instant.now());
    }

    /**
     * Atualiza os dados cadastrais mutaveis. Codigo e data de cadastro sao
     * imutaveis por serem a identidade do ponto no historico.
     */
    public void atualizar(String nome,
                          TipoFonte tipoFonte,
                          int populacaoAtendida,
                          Localizacao localizacao,
                          Responsavel responsavel) {
        this.nome = exigirTexto(nome, "nome do ponto");
        this.tipoFonte = Objects.requireNonNull(tipoFonte, "tipo de fonte e obrigatorio");
        this.populacaoAtendida = exigirPopulacaoValida(populacaoAtendida);
        this.localizacao = Objects.requireNonNull(localizacao, "localizacao e obrigatoria");
        this.responsavel = Objects.requireNonNull(responsavel, "responsavel e obrigatorio");
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio");
        }
        return valor.trim();
    }

    private static int exigirPopulacaoValida(int populacao) {
        if (populacao < 0) {
            throw new IllegalArgumentException("populacao atendida nao pode ser negativa");
        }
        return populacao;
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public TipoFonte getTipoFonte() {
        return tipoFonte;
    }

    public int getPopulacaoAtendida() {
        return populacaoAtendida;
    }

    public Localizacao getLocalizacao() {
        return localizacao;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public String toString() {
        return codigo + " - " + nome + " (" + localizacao.descricao() + ")";
    }
}
