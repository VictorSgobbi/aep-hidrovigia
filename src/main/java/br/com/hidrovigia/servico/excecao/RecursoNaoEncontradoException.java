package br.com.hidrovigia.servico.excecao;

/**
 * Lancada quando um identificador informado nao corresponde a nenhum documento.
 * Traduzida para HTTP 404 pelo tratador de erros da API.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException ponto(String referencia) {
        return new RecursoNaoEncontradoException(
                "Ponto de monitoramento nao encontrado: " + referencia);
    }

    public static RecursoNaoEncontradoException analise(String id) {
        return new RecursoNaoEncontradoException("Analise nao encontrada: " + id);
    }

    public static RecursoNaoEncontradoException ocorrencia(String id) {
        return new RecursoNaoEncontradoException("Ocorrencia nao encontrada: " + id);
    }
}
