package br.com.hidrovigia.repositorio;

import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso a colecao {@code analises}.
 *
 * <p>As contagens por conformidade consultam {@code resultado.conforme}, campo
 * do subdocumento consolidado. E exatamente por isso que ele existe: sem esse
 * resumo gravado, o painel teria de varrer o array de parametros de cada
 * analise para saber quantas reprovaram.
 */
@Repository
public interface AnaliseRepository extends MongoRepository<Analise, String> {

    List<Analise> findByPontoIdOrderByColetadoEmDesc(String pontoId);

    List<Analise> findByColetadoEmBetweenOrderByColetadoEmDesc(Instant inicio, Instant fim);

    List<Analise> findByPontoIdAndColetadoEmBetweenOrderByColetadoEmDesc(
            String pontoId, Instant inicio, Instant fim);

    long countByPontoId(String pontoId);

    long countByResultadoConforme(boolean conforme);

    long countByPontoIdAndResultadoConforme(String pontoId, boolean conforme);
}
