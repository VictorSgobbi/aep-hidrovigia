package br.com.hidrovigia.repositorio;

import java.util.List;

import br.com.hidrovigia.Fixtures;
import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracao real contra um MongoDB 7 efemero.
 *
 * <p>Confirma o que os testes com mock nao alcancam: que os subdocumentos
 * aninhados sobrevivem ao ciclo de gravacao e leitura, e que as consultas
 * derivadas realmente navegam pelos campos internos do documento.
 *
 * <p>A classe inteira e ignorada em maquina sem Docker
 * ({@code disabledWithoutDocker}), entao o build continua verde e a cobertura
 * exigida e sustentada pelos testes de unidade.
 */
@DataMongoTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Integracao dos repositorios com MongoDB")
class RepositoriosMongoIntegracaoTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private PontoMonitoramentoRepository pontos;

    @Autowired
    private AnaliseRepository analises;

    @Autowired
    private OcorrenciaRepository ocorrencias;

    @Autowired
    private MongoTemplate template;

    @BeforeEach
    void limpar() {
        pontos.deleteAll();
        analises.deleteAll();
        ocorrencias.deleteAll();
    }

    @Test
    @DisplayName("grava o ponto com localizacao e responsavel aninhados no mesmo documento")
    void gravaPontoComSubdocumentos() {
        PontoMonitoramento salvo = pontos.save(Fixtures.pontoNovo());

        assertThat(salvo.getId()).isNotNull();

        Document bruto = template.getCollection("pontos_monitoramento")
                .find(new Document("codigo", "PMA-001")).first();

        assertThat(bruto).isNotNull();
        assertThat(bruto.get("localizacao", Document.class).getString("municipio"))
                .isEqualTo("Maringa");
        assertThat(bruto.get("responsavel", Document.class).getString("nome"))
                .isEqualTo("Ana Souza");
    }

    @Test
    @DisplayName("consulta por municipio navegando pelo subdocumento localizacao")
    void consultaPorCampoAninhado() {
        pontos.save(Fixtures.pontoNovo());

        assertThat(pontos.findByLocalizacaoMunicipioIgnoreCase("maringa")).hasSize(1);
        assertThat(pontos.findByLocalizacaoMunicipioIgnoreCase("sarandi")).isEmpty();
    }

    @Test
    @DisplayName("respeita a unicidade do codigo e o filtro de ativos")
    void codigoUnicoEFiltroDeAtivos() {
        PontoMonitoramento ativo = pontos.save(Fixtures.pontoNovo());

        PontoMonitoramento desativado = PontoMonitoramento.cadastrar("PMA-002", "Cisterna",
                ativo.getTipoFonte(), 40, Fixtures.localizacao(), Fixtures.responsavel());
        desativado.desativar();
        pontos.save(desativado);

        assertThat(pontos.existsByCodigo("PMA-001")).isTrue();
        assertThat(pontos.findByCodigo("PMA-001")).isPresent();
        assertThat(pontos.findByAtivoTrue()).hasSize(1);
        assertThat(pontos.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("preserva o array de parametros medidos na analise")
    void gravaAnaliseComArrayDeParametros() {
        PontoMonitoramento ponto = pontos.save(Fixtures.pontoNovo());
        Analise salva = analises.save(Analise.registrar(ponto, "Tecnico Bruno",
                java.time.Instant.now().minusSeconds(3600), Fixtures.parametrosConformes()));

        Analise lida = analises.findById(salva.getId()).orElseThrow();

        assertThat(lida.getParametros())
                .extracting(ResultadoParametro::codigo)
                .containsExactly("ECOLI", "CRL", "PH", "TURB");
        assertThat(lida.getParametros().get(1).valor()).isEqualByComparingTo("0.8");
        assertThat(lida.getParametros().get(1).limiteMaximo()).isEqualByComparingTo("2.0");
        assertThat(lida.conforme()).isTrue();
        assertThat(lida.getResultado().qtdParametros()).isEqualTo(4);
    }

    @Test
    @DisplayName("conta analises pelo campo aninhado resultado.conforme")
    void contaPeloResultadoConsolidado() {
        PontoMonitoramento ponto = pontos.save(Fixtures.pontoNovo());
        java.time.Instant agora = java.time.Instant.now().minusSeconds(600);

        analises.save(Analise.registrar(ponto, "Bruno", agora, Fixtures.parametrosConformes()));
        analises.save(Analise.registrar(ponto, "Bruno", agora,
                Fixtures.parametrosComViolacaoCritica()));

        assertThat(analises.countByResultadoConforme(true)).isEqualTo(1);
        assertThat(analises.countByResultadoConforme(false)).isEqualTo(1);
        assertThat(analises.countByPontoIdAndResultadoConforme(ponto.getId(), false)).isEqualTo(1);
        assertThat(analises.countByPontoId(ponto.getId())).isEqualTo(2);
        assertThat(analises.findByPontoIdOrderByColetadoEmDesc(ponto.getId())).hasSize(2);
    }

    @Test
    @DisplayName("preserva os dois arrays de subdocumentos da ocorrencia")
    void gravaOcorrenciaComDoisArrays() {
        Ocorrencia aberta = Fixtures.ocorrenciaAberta();
        aberta.registrarTratativa("Ponto isolado da rede", "Leonardo");

        Ocorrencia salva = ocorrencias.save(aberta);
        Ocorrencia lida = ocorrencias.findById(salva.getId()).orElseThrow();

        assertThat(lida.getParametrosViolados()).hasSize(2);
        assertThat(lida.getTratativas()).hasSize(1);
        assertThat(lida.getTratativas().get(0).por()).isEqualTo("Leonardo");
        assertThat(lida.getStatus()).isEqualTo(StatusOcorrencia.EM_TRATATIVA);

        Document bruto = template.getCollection("ocorrencias")
                .find(new Document("_id", new org.bson.types.ObjectId(salva.getId()))).first();
        assertThat(bruto).isNotNull();
        assertThat(bruto.getList("parametrosViolados", Document.class)).hasSize(2);
        assertThat(bruto.getList("tratativas", Document.class)).hasSize(1);
    }

    @Test
    @DisplayName("filtra ocorrencias por status e pela analise de origem")
    void consultasDeOcorrencia() {
        Ocorrencia aberta = ocorrencias.save(Fixtures.ocorrenciaAberta());

        Ocorrencia outra = Fixtures.ocorrenciaAberta();
        outra.resolver("Contraprova conforme", "Victor");
        ocorrencias.save(outra);

        List<Ocorrencia> pendentes =
                ocorrencias.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA);

        assertThat(pendentes).hasSize(1);
        assertThat(ocorrencias.findByStatusOrderByAbertaEmDesc(StatusOcorrencia.RESOLVIDA))
                .hasSize(1);
        assertThat(ocorrencias.countByStatus(StatusOcorrencia.RESOLVIDA)).isEqualTo(1);
        assertThat(ocorrencias.countByStatusNot(StatusOcorrencia.RESOLVIDA)).isEqualTo(1);
        assertThat(ocorrencias.findByAnaliseId(aberta.getAnaliseId())).isPresent();
        assertThat(ocorrencias.findByPontoIdOrderByAbertaEmDesc(aberta.getPontoId())).hasSize(2);
    }
}
