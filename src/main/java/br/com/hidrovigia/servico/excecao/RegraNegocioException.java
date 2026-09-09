package br.com.hidrovigia.servico.excecao;

/**
 * Lancada quando a operacao e sintaticamente valida mas viola uma regra do
 * dominio — codigo de ponto ja usado, coleta em ponto desativado e afins.
 * Traduzida para HTTP 409 pelo tratador de erros da API.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
