package br.com.hidrovigia.servico;

import java.util.List;

import br.com.hidrovigia.dominio.ponto.Localizacao;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.dominio.ponto.Responsavel;
import br.com.hidrovigia.dominio.ponto.TipoFonte;
import br.com.hidrovigia.repositorio.PontoMonitoramentoRepository;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.springframework.stereotype.Service;

/**
 * Casos de uso do cadastro de pontos de monitoramento.
 */
@Service
public class PontoMonitoramentoService {

    private final PontoMonitoramentoRepository repositorio;

    public PontoMonitoramentoService(PontoMonitoramentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * @throws RegraNegocioException se o codigo ja estiver em uso
     */
    public PontoMonitoramento cadastrar(String codigo,
                                        String nome,
                                        TipoFonte tipoFonte,
                                        int populacaoAtendida,
                                        Localizacao localizacao,
                                        Responsavel responsavel) {
        PontoMonitoramento ponto = PontoMonitoramento.cadastrar(
                codigo, nome, tipoFonte, populacaoAtendida, localizacao, responsavel);

        if (repositorio.existsByCodigo(ponto.getCodigo())) {
            throw new RegraNegocioException(
                    "Ja existe um ponto de monitoramento com o codigo " + ponto.getCodigo());
        }
        return repositorio.save(ponto);
    }

    public PontoMonitoramento buscarPorId(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.ponto(id));
    }

    public PontoMonitoramento buscarPorCodigo(String codigo) {
        String normalizado = codigo == null ? "" : codigo.trim().toUpperCase();
        return repositorio.findByCodigo(normalizado)
                .orElseThrow(() -> RecursoNaoEncontradoException.ponto(codigo));
    }

    public List<PontoMonitoramento> listar(boolean apenasAtivos) {
        return apenasAtivos ? repositorio.findByAtivoTrue() : repositorio.findAll();
    }

    public List<PontoMonitoramento> listarPorMunicipio(String municipio) {
        return repositorio.findByLocalizacaoMunicipioIgnoreCase(municipio);
    }

    public PontoMonitoramento atualizar(String id,
                                        String nome,
                                        TipoFonte tipoFonte,
                                        int populacaoAtendida,
                                        Localizacao localizacao,
                                        Responsavel responsavel) {
        PontoMonitoramento ponto = buscarPorId(id);
        ponto.atualizar(nome, tipoFonte, populacaoAtendida, localizacao, responsavel);
        return repositorio.save(ponto);
    }

    /**
     * Marca o ponto como fora de operacao. O documento nao e removido para que
     * as analises historicas continuem apontando para um ponto existente.
     */
    public PontoMonitoramento desativar(String id) {
        PontoMonitoramento ponto = buscarPorId(id);
        if (!ponto.isAtivo()) {
            throw new RegraNegocioException(
                    "Ponto " + ponto.getCodigo() + " ja esta desativado");
        }
        ponto.desativar();
        return repositorio.save(ponto);
    }

    public PontoMonitoramento reativar(String id) {
        PontoMonitoramento ponto = buscarPorId(id);
        ponto.reativar();
        return repositorio.save(ponto);
    }
}
