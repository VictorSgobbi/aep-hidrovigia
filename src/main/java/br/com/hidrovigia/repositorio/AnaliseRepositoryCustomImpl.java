package br.com.hidrovigia.repositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Serie historica calculada pelo proprio MongoDB.
 *
 * <p>O pipeline e escrito em documentos BSON, na mesma forma em que se
 * escreveria no shell, para que o que esta documentado em
 * {@code docs/modelagem-nosql.md} seja literalmente o que roda:
 *
 * <pre>
 * $match   pontoId e periodo        — usa o indice { pontoId: 1, coletadoEm: -1 }
 * $unwind  parametros               — um documento por leitura
 * $match   parametros.codigo        — so o parametro pedido
 * $group   $dateTrunc(coletadoEm, dia) com $min, $avg, $max e a contagem
 * $sort    dia crescente
 * </pre>
 *
 * <p>O valor medido e gravado como texto (e assim que o Spring Data persiste
 * {@link BigDecimal}, sem perder casas decimais), entao o {@code $group}
 * converte com {@code $toDecimal} antes de agregar. Comparar o texto daria
 * "10" menor que "9".
 */
class AnaliseRepositoryCustomImpl implements AnaliseRepositoryCustom {

    private static final int CASAS_DA_MEDIA = 3;

    private final MongoTemplate template;

    AnaliseRepositoryCustomImpl(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public List<LeituraDiaria> serieDiaria(String pontoId, String codigoParametro,
                                           Instant inicio, Instant fim) {
        Document valor = new Document("$toDecimal", "$parametros.valor");
        Document reprovada = new Document("$cond", List.of("$parametros.conforme", 0, 1));

        List<Document> pipeline = List.of(
                new Document("$match", new Document("pontoId", pontoId)
                        .append("coletadoEm", new Document("$gte", Date.from(inicio))
                                .append("$lte", Date.from(fim)))),
                new Document("$unwind", "$parametros"),
                new Document("$match", new Document("parametros.codigo", codigoParametro)),
                new Document("$group", new Document("_id", new Document("$dateTrunc",
                        new Document("date", "$coletadoEm").append("unit", "day")))
                        .append("minimo", new Document("$min", valor))
                        .append("media", new Document("$avg", valor))
                        .append("maximo", new Document("$max", valor))
                        .append("leituras", new Document("$sum", 1))
                        .append("foraDoLimite", new Document("$sum", reprovada))),
                new Document("$sort", new Document("_id", 1)));

        List<LeituraDiaria> serie = new ArrayList<>();
        for (Document dia : template.getCollection(template.getCollectionName(Analise.class))
                .aggregate(pipeline)) {
            serie.add(new LeituraDiaria(
                    LocalDate.ofInstant(dia.getDate("_id").toInstant(), ZoneOffset.UTC),
                    decimal(dia, "minimo"),
                    decimal(dia, "media").setScale(CASAS_DA_MEDIA, RoundingMode.HALF_UP),
                    decimal(dia, "maximo"),
                    dia.getInteger("leituras"),
                    dia.getInteger("foraDoLimite")));
        }
        return serie;
    }

    private static BigDecimal decimal(Document documento, String campo) {
        return documento.get(campo, Decimal128.class).bigDecimalValue();
    }
}
