package br.com.hidrovigia.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.com.hidrovigia.dominio.analise.LeituraParametro;
import br.com.hidrovigia.dominio.ponto.Localizacao;
import br.com.hidrovigia.dominio.ponto.Responsavel;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.repositorio.PontoMonitoramentoRepository;
import br.com.hidrovigia.servico.AnaliseService;
import br.com.hidrovigia.servico.PontoMonitoramentoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Popula a base com dados de demonstracao na subida da aplicacao.
 *
 * <p>Existe para que o Swagger UI abra com conteudo real na gravacao do video,
 * em vez de listas vazias. Nao roda no perfil de teste e nao repete a carga se
 * ja houver pontos cadastrados.
 */
@Component
@ConditionalOnProperty(name = "hidrovigia.carga-demonstracao", havingValue = "true")
public class CargaDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDemonstracao.class);

    private final PontoMonitoramentoRepository pontoRepositorio;
    private final PontoMonitoramentoService pontoService;
    private final AnaliseService analiseService;

    public CargaDemonstracao(PontoMonitoramentoRepository pontoRepositorio,
                             PontoMonitoramentoService pontoService,
                             AnaliseService analiseService) {
        this.pontoRepositorio = pontoRepositorio;
        this.pontoService = pontoService;
        this.analiseService = analiseService;
    }

    @Override
    public void run(String... args) {
        if (pontoRepositorio.count() > 0) {
            log.info("Carga de demonstracao ignorada: a base ja possui pontos cadastrados.");
            return;
        }

        cadastrarPontos();
        registrarAnalises();

        log.info("Carga de demonstracao concluida. Abra http://localhost:8080/swagger-ui.html");
    }

    private void cadastrarPontos() {
        pontoService.cadastrar("PMA-001", "Poco da Escola Rural Sao Jose",
                TipoFonte.POCO_ARTESIANO, 320,
                new Localizacao("Maringa", "PR", -23.4205, -51.9331),
                new Responsavel("Ana Souza", "CREA-PR 123456", "ana.souza@exemplo.gov.br"));

        pontoService.cadastrar("PMA-002", "Reservatorio do Distrito de Iguatemi",
                TipoFonte.MANANCIAL_SUPERFICIAL, 2400,
                new Localizacao("Maringa", "PR", -23.3608, -52.0847),
                new Responsavel("Carlos Lima", "CRQ-PR 98765", "carlos.lima@exemplo.gov.br"));

        pontoService.cadastrar("PMA-003", "Cisterna da Associacao Agua Viva",
                TipoFonte.CISTERNA, 85,
                new Localizacao("Sarandi", "PR", -23.4436, -51.8761),
                new Responsavel("Marta Reis", null, "marta.reis@exemplo.org.br"));
    }

    private void registrarAnalises() {
        Instant agora = Instant.now();

        // Rotina normal: tudo dentro do padrao.
        analiseService.registrar("PMA-001", "Tecnico Bruno", agora.minus(7, ChronoUnit.DAYS),
                leituras("ECOLI", "0", "CTOT", "0", "CRL", "0.9", "PH", "7.1", "TURB", "0.6"));

        analiseService.registrar("PMA-002", "Tecnico Leonardo", agora.minus(6, ChronoUnit.DAYS),
                leituras("ECOLI", "0", "CRL", "1.2", "PH", "7.4", "TURB", "1.8", "COR", "8"));

        // Falha de desinfeccao: cloro abaixo do minimo, gravidade alta.
        analiseService.registrar("PMA-003", "Tecnico Bruno", agora.minus(3, ChronoUnit.DAYS),
                leituras("ECOLI", "0", "CRL", "0.05", "PH", "6.8", "TURB", "2.1"));

        // Contaminacao microbiologica: gravidade critica, prazo de 24 horas.
        analiseService.registrar("PMA-003", "Tecnico Leonardo", agora.minus(1, ChronoUnit.DAYS),
                leituras("ECOLI", "14", "CTOT", "62", "CRL", "0.0", "PH", "6.4", "TURB", "7.3"));

        // Desvio apenas organoleptico: gravidade media.
        analiseService.registrar("PMA-002", "Tecnico Victor", agora.minus(12, ChronoUnit.HOURS),
                leituras("ECOLI", "0", "CRL", "0.7", "PH", "7.2", "COR", "22"));
    }

    /**
     * Monta a lista de leituras a partir de pares codigo/valor.
     */
    private static List<LeituraParametro> leituras(String... pares) {
        if (pares.length % 2 != 0) {
            throw new IllegalArgumentException("informe pares de codigo e valor");
        }
        return java.util.stream.IntStream.range(0, pares.length / 2)
                .mapToObj(i -> new LeituraParametro(pares[i * 2], new BigDecimal(pares[i * 2 + 1])))
                .toList();
    }
}
