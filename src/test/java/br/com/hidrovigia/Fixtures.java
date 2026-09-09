package br.com.hidrovigia;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.analise.ResultadoAnalise;
import br.com.hidrovigia.dominio.gravidade.ClassificadorPorRiscoSanitario;
import br.com.hidrovigia.dominio.gravidade.Gravidade;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.ParametroViolado;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.dominio.ponto.Localizacao;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.Responsavel;
import br.com.hidrovigia.dominio.ponto.TipoFonte;

/**
 * Construtores de objetos de dominio para os testes.
 *
 * <p>Concentrar as montagens aqui evita repetir dez linhas de setup em cada
 * classe de teste e deixa visivel o que cada cenario realmente muda.
 *
 * <p>Todas as datas derivam de {@link #REFERENCIA}, um instante fixo, e nunca
 * de {@code Instant.now()}: fixture ancorada no relogio da maquina produz
 * objetos diferentes a cada execucao e e a origem tipica de teste
 * intermitente. Quem precisa de "agora" injeta um {@code Clock} fixo derivado
 * desta constante.
 */
public final class Fixtures {

    public static final CatalogoParametros CATALOGO = CatalogoParametros.padraoBrasileiro();

    /** Instante de referencia de toda a suite. */
    public static final Instant REFERENCIA = Instant.parse("2026-01-15T09:00:00Z");

    /** Relogio parado em {@link #REFERENCIA}, para injetar onde couber. */
    public static Clock relogioFixo() {
        return Clock.fixed(REFERENCIA, ZoneOffset.UTC);
    }

    private Fixtures() {
    }

    public static Localizacao localizacao() {
        return new Localizacao("Maringa", "PR", -23.4205, -51.9331);
    }

    public static Responsavel responsavel() {
        return new Responsavel("Ana Souza", "CREA-PR 123456", "ana.souza@exemplo.gov.br");
    }

    /** Ponto novo, ainda sem identificador atribuido pelo banco. */
    public static PontoMonitoramento pontoNovo() {
        return PontoMonitoramento.cadastrar("PMA-001", "Poco da Escola Rural Sao Jose",
                TipoFonte.POCO_ARTESIANO, 320, localizacao(), responsavel());
    }

    /** Ponto como volta do banco, ja com identificador. */
    public static PontoMonitoramento pontoPersistido(String id) {
        return new PontoMonitoramento(id, "PMA-001", "Poco da Escola Rural Sao Jose",
                TipoFonte.POCO_ARTESIANO, 320, localizacao(), responsavel(), true,
                REFERENCIA.minus(30, ChronoUnit.DAYS));
    }

    public static PontoMonitoramento pontoPersistido() {
        return pontoPersistido("ponto-1");
    }

    /** Avalia um valor pelo catalogo real, produzindo um veredito coerente. */
    public static ResultadoParametro avaliar(String codigo, String valor) {
        return CATALOGO.buscar(codigo).avaliar(new BigDecimal(valor));
    }

    /** Conjunto de parametros todos dentro do padrao. */
    public static List<ResultadoParametro> parametrosConformes() {
        return List.of(
                avaliar("ECOLI", "0"),
                avaliar("CRL", "0.8"),
                avaliar("PH", "7.2"),
                avaliar("TURB", "0.4"));
    }

    /** Conjunto com uma deteccao microbiologica e cloro insuficiente. */
    public static List<ResultadoParametro> parametrosComViolacaoCritica() {
        return List.of(
                avaliar("ECOLI", "8"),
                avaliar("CRL", "0.05"),
                avaliar("PH", "7.0"),
                avaliar("TURB", "1.2"));
    }

    /** Analise conforme, ja com identificador. */
    public static Analise analiseConforme() {
        List<ResultadoParametro> parametros = parametrosConformes();
        return new Analise("analise-1", "ponto-1", "PMA-001",
                REFERENCIA.minus(2, ChronoUnit.HOURS), "Tecnico Bruno",
                parametros, new ResultadoAnalise(true, parametros.size(), 0), REFERENCIA);
    }

    /** Analise reprovada, ja com identificador. */
    public static Analise analiseNaoConforme() {
        return analiseNaoConforme("analise-2");
    }

    /**
     * Analise reprovada com identificador escolhido.
     *
     * <p>Necessario sempre que um teste precisa de duas ocorrencias: cada
     * analise reprovada gera exatamente uma, garantida por indice unico em
     * {@code analiseId}.
     */
    public static Analise analiseNaoConforme(String id) {
        List<ResultadoParametro> parametros = parametrosComViolacaoCritica();
        int reprovados = (int) parametros.stream().filter(ResultadoParametro::naoConforme).count();
        return new Analise(id, "ponto-1", "PMA-001",
                REFERENCIA.minus(1, ChronoUnit.HOURS), "Tecnico Bruno",
                parametros, new ResultadoAnalise(false, parametros.size(), reprovados),
                REFERENCIA);
    }

    public static List<ParametroViolado> violados(Analise analise) {
        List<ParametroViolado> lista = new ArrayList<>();
        for (ResultadoParametro resultado : analise.naoConformidades()) {
            lista.add(ParametroViolado.de(resultado,
                    CATALOGO.buscar(resultado.codigo()).descricaoDoLimite()));
        }
        return lista;
    }

    /** Ocorrencia aberta a partir de uma analise reprovada. */
    public static Ocorrencia ocorrenciaAberta() {
        return ocorrenciaAberta("analise-2");
    }

    /** Ocorrencia aberta a partir da analise reprovada indicada. */
    public static Ocorrencia ocorrenciaAberta(String analiseId) {
        Analise analise = analiseNaoConforme(analiseId);
        return Ocorrencia.abrir(analise, new ClassificadorPorRiscoSanitario(), violados(analise));
    }

    /**
     * Ocorrencia como volta do banco, com identificador e status controlado.
     *
     * <p>Aberta em {@link #REFERENCIA}, com prazo critico de 24 horas. Quem
     * testa vencimento compara contra um {@code Clock} fixo antes ou depois de
     * {@code REFERENCIA.plus(24h)}.
     */
    public static Ocorrencia ocorrenciaPersistida(String id, StatusOcorrencia status) {
        Analise analise = analiseNaoConforme();
        return new Ocorrencia(id, analise.getId(), analise.getPontoId(), analise.getPontoCodigo(),
                Gravidade.CRITICA, "risco-sanitario", status, REFERENCIA,
                REFERENCIA.plus(24, ChronoUnit.HOURS), violados(analise), new ArrayList<>());
    }
}
