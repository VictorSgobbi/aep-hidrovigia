package br.com.hidrovigia.repositorio;

import java.util.List;
import java.util.Optional;

import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso a colecao {@code pontos_monitoramento}.
 *
 * <p>A consulta por municipio navega pelo subdocumento aninhado
 * {@code localizacao.municipio} — um caso em que o documento denormalizado
 * dispensa qualquer juncao.
 */
@Repository
public interface PontoMonitoramentoRepository extends MongoRepository<PontoMonitoramento, String> {

    Optional<PontoMonitoramento> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<PontoMonitoramento> findByAtivoTrue();

    List<PontoMonitoramento> findByLocalizacaoMunicipioIgnoreCase(String municipio);
}
