package br.com.hidrovigia.dominio.ponto;

/**
 * Subdocumento aninhado com o responsavel tecnico pelo ponto.
 *
 * <p>A vigilancia precisa saber a quem cobrar quando uma ocorrencia vence o
 * prazo, entao o contato viaja junto do ponto e nao em cadastro separado.
 */
public record Responsavel(
        String nome,
        String registro,
        String contato) {

    public Responsavel {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do responsavel e obrigatorio");
        }
        if (contato == null || contato.isBlank()) {
            throw new IllegalArgumentException("contato do responsavel e obrigatorio");
        }
        nome = nome.trim();
        contato = contato.trim();
        registro = registro == null ? null : registro.trim();
    }
}
