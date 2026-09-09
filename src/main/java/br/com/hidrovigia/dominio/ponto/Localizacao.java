package br.com.hidrovigia.dominio.ponto;

/**
 * Subdocumento aninhado com a localizacao do ponto de monitoramento.
 *
 * <p>Fica embutido no documento de {@code pontos_monitoramento} em vez de virar
 * colecao propria: nao existe consulta que busque localizacao sem o ponto.
 */
public record Localizacao(
        String municipio,
        String uf,
        Double latitude,
        Double longitude) {

    public Localizacao {
        if (municipio == null || municipio.isBlank()) {
            throw new IllegalArgumentException("municipio e obrigatorio na localizacao");
        }
        if (uf == null || uf.trim().length() != 2) {
            throw new IllegalArgumentException("uf deve ter duas letras");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("latitude fora do intervalo valido");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("longitude fora do intervalo valido");
        }
        municipio = municipio.trim();
        uf = uf.trim().toUpperCase();
    }

    public boolean possuiCoordenadas() {
        return latitude != null && longitude != null;
    }

    public String descricao() {
        return municipio + "/" + uf;
    }
}
