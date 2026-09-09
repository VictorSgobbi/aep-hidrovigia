package br.com.hidrovigia.dominio.parametro;

/**
 * Lancada quando uma analise informa um codigo de parametro fora do catalogo.
 *
 * <p>Aceitar um codigo desconhecido gravaria no banco um resultado que ninguem
 * consegue confrontar com a norma depois — por isso a analise inteira e
 * rejeitada em vez de ignorar o parametro invalido.
 */
public class ParametroDesconhecidoException extends RuntimeException {

    private final String codigo;

    public ParametroDesconhecidoException(String codigo) {
        super("Parametro nao previsto no padrao de potabilidade: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
