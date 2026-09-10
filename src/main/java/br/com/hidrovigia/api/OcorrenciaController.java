package br.com.hidrovigia.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.api.dto.OcorrenciaDto;
import br.com.hidrovigia.dominio.ocorrencia.StatusOcorrencia;
import br.com.hidrovigia.servico.OcorrenciaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ocorrencias")
@Tag(name = "3. Ocorrencias",
        description = "Fila de nao-conformidades com prazo de resposta")
public class OcorrenciaController {

    private final OcorrenciaService servico;
    private final Clock relogio;

    public OcorrenciaController(OcorrenciaService servico, Clock relogio) {
        this.servico = servico;
        this.relogio = relogio;
    }

    @GetMapping
    @Operation(summary = "Lista ocorrencias; sem filtro devolve as pendentes por prazo")
    public List<OcorrenciaDto.Saida> listar(
            @RequestParam(required = false) StatusOcorrencia status) {
        Instant agora = relogio.instant();
        return servico.listar(status).stream()
                .map(ocorrencia -> OcorrenciaDto.Saida.de(ocorrencia, agora))
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma ocorrencia pelo identificador")
    public OcorrenciaDto.Saida buscar(@PathVariable String id) {
        return OcorrenciaDto.Saida.de(servico.buscarPorId(id), relogio.instant());
    }

    @PostMapping("/{id}/tratativas")
    @Operation(summary = "Registra uma acao sem encerrar a ocorrencia")
    public OcorrenciaDto.Saida registrarTratativa(
            @PathVariable String id,
            @Valid @RequestBody OcorrenciaDto.TratativaEntrada entrada) {
        return OcorrenciaDto.Saida.de(
                servico.registrarTratativa(id, entrada.acao(), entrada.por()), relogio.instant());
    }

    @PostMapping("/{id}/resolucao")
    @Operation(summary = "Registra a acao final e encerra a ocorrencia")
    public OcorrenciaDto.Saida resolver(
            @PathVariable String id,
            @Valid @RequestBody OcorrenciaDto.TratativaEntrada entrada) {
        return OcorrenciaDto.Saida.de(
                servico.resolver(id, entrada.acao(), entrada.por()), relogio.instant());
    }
}
