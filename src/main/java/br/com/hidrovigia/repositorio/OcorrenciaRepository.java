package br.com.hidrovigia.repositorio;

import java.util.List;
import java.util.Optional;

import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Acesso a colecao {@code ocorrencias}.
 */
@Repository
public interface OcorrenciaRepository extends MongoRepository<Ocorrencia, String> {

    List<Ocorrencia> findByStatusOrderByAbertaEmDesc(StatusOcorrencia status);

    List<Ocorrencia> findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia status);

    List<Ocorrencia> findByPontoIdOrderByAbertaEmDesc(String pontoId);

    Optional<Ocorrencia> findByAnaliseId(String analiseId);

    long countByStatus(StatusOcorrencia status);

    long countByStatusNot(StatusOcorrencia status);
}
