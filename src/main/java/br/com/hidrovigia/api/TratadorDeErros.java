package br.com.hidrovigia.api;

import java.util.List;

import br.com.hidrovigia.api.dto.ErroResponse;
import br.com.hidrovigia.dominio.parametro.ParametroDesconhecidoException;
import br.com.hidrovigia.servico.excecao.RecursoNaoEncontradoException;
import br.com.hidrovigia.servico.excecao.RegraNegocioException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz as excecoes do dominio e da aplicacao em respostas HTTP.
 *
 * <p>Concentrar isso aqui mantem os controllers limpos: nenhum deles precisa
 * saber que "codigo de ponto duplicado" e um 409 e "parametro fora do
 * catalogo" e um 422.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(404, "Recurso nao encontrado", erro.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraDeNegocio(RegraNegocioException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(409, "Regra de negocio violada", erro.getMessage()));
    }

    @ExceptionHandler(ParametroDesconhecidoException.class)
    public ResponseEntity<ErroResponse> parametroDesconhecido(ParametroDesconhecidoException erro) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErroResponse.de(422, "Parametro fora do catalogo", erro.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException erro) {
        List<String> detalhes = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest()
                .body(ErroResponse.de(400, "Requisicao invalida",
                        "Um ou mais campos nao passaram na validacao", detalhes));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> argumentoInvalido(IllegalArgumentException erro) {
        return ResponseEntity.badRequest()
                .body(ErroResponse.de(400, "Requisicao invalida", erro.getMessage()));
    }
}
