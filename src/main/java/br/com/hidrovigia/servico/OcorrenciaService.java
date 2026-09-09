package br.com.hidrovigia.servico;

import java.util.ArrayList;
import java.util.List;

import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.dominio.gravidade.ClassificadorGravidade;
import br.com.hidrovigia.dominio.ocorrencia.Ocorrencia;
import br.com.hidrovigia.dominio.ocorrencia.ParametroViolado;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.dominio.parametro.CatalogoParametros;
import br.com.hidrovigia.dominio.parametro.ResultadoParametro;
import br.com.hidrovigia.repositorio.OcorrenciaRepository;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.springframework.stereotype.Service;

/**
 * Casos de uso da fila de nao-conformidades.
 *
 * <p>Recebe o classificador de gravidade por injecao: trocar a politica do
 * municipio e trocar o bean, sem tocar neste servico.
 */
@Service
public class OcorrenciaService {

    private final OcorrenciaRepository repositorio;
    private final CatalogoParametros catalogo;
    private final ClassificadorGravidade classificador;

    public OcorrenciaService(OcorrenciaRepository repositorio,
                             CatalogoParametros catalogo,
                             ClassificadorGravidade classificador) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.classificador = classificador;
    }

    /**
     * Abre a ocorrencia correspondente a uma analise reprovada.
     *
     * @throws RegraNegocioException se a analise estiver conforme ou se ja
     *                               existir ocorrencia aberta para ela
     */
    public Ocorrencia abrirPara(Analise analise) {
        if (analise.conforme()) {
            throw new RegraNegocioException(
                    "Analise " + analise.getId() + " esta conforme e nao gera ocorrencia");
        }
        // Cada analise reprovada gera exatamente uma ocorrencia. A checagem
        // aqui devolve 409 em vez de deixar o indice unico estourar como 500;
        // o indice continua sendo a garantia real sob concorrencia.
        if (repositorio.findByAnaliseId(analise.getId()).isPresent()) {
            throw new RegraNegocioException(
                    "Analise " + analise.getId() + " ja possui ocorrencia aberta");
        }

        List<ParametroViolado> violados = new ArrayList<>();
        for (ResultadoParametro resultado : analise.naoConformidades()) {
            String limite = catalogo.buscar(resultado.codigo()).descricaoDoLimite();
            violados.add(ParametroViolado.de(resultado, limite));
        }

        return repositorio.save(Ocorrencia.abrir(analise, classificador, violados));
    }

    public Ocorrencia buscarPorId(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.ocorrencia(id));
    }

    /**
     * @param status filtro opcional; quando nulo, devolve as pendentes ordenadas
     *               pelo prazo mais proximo de vencer
     */
    public List<Ocorrencia> listar(StatusOcorrencia status) {
        if (status == null) {
            return repositorio.findByStatusNotOrderByPrazoLimiteAsc(StatusOcorrencia.RESOLVIDA);
        }
        return repositorio.findByStatusOrderByAbertaEmDesc(status);
    }

    public List<Ocorrencia> listarPorPonto(String pontoId) {
        return repositorio.findByPontoIdOrderByAbertaEmDesc(pontoId);
    }

    /**
     * @throws RegraNegocioException se a ocorrencia ja estiver resolvida
     */
    public Ocorrencia registrarTratativa(String id, String acao, String por) {
        Ocorrencia ocorrencia = buscarPorId(id);
        try {
            ocorrencia.registrarTratativa(acao, por);
        } catch (IllegalStateException erro) {
            throw new RegraNegocioException(erro.getMessage());
        }
        return repositorio.save(ocorrencia);
    }

    /**
     * @throws RegraNegocioException se a ocorrencia ja estiver resolvida
     */
    public Ocorrencia resolver(String id, String acao, String por) {
        Ocorrencia ocorrencia = buscarPorId(id);
        try {
            ocorrencia.resolver(acao, por);
        } catch (IllegalStateException erro) {
            throw new RegraNegocioException(erro.getMessage());
        }
        return repositorio.save(ocorrencia);
    }

    public long contarPendentes() {
        return repositorio.countByStatusNot(StatusOcorrencia.RESOLVIDA);
    }
}
