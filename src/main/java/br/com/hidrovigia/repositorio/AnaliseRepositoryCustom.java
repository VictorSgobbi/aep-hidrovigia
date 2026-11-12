package br.com.hidrovigia.repositorio;

import java.time.Instant;
import java.util.List;

/**
 * Consultas de {@code analises} que nao cabem em metodo derivado: agregacoes
 * que o banco calcula, em vez de trazer os documentos para a memoria.
 */
public interface AnaliseRepositoryCustom {

    /**
     * Serie diaria de um parametro em um ponto, em ordem cronologica.
     *
     * @param pontoId        identificador do ponto
     * @param codigoParametro codigo do catalogo, como gravado em {@code parametros.codigo}
     * @param inicio         inicio do periodo, inclusivo
     * @param fim            fim do periodo, inclusivo
     */
    List<LeituraDiaria> serieDiaria(String pontoId, String codigoParametro,
                                    Instant inicio, Instant fim);
}
