package br.com.hidrovigia.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import br.com.hidrovigia.api.dto.AnaliseDto;
import br.com.hidrovigia.dominio.analise.Analise;
import br.com.hidrovigia.servico.AnaliseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analises")
@Tag(name = "2. Analises",
        description = "Registro de coletas e confronto com o padrao de potabilidade")
public class AnaliseController {

    private final AnaliseService servico;

    public AnaliseController(AnaliseService servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Registra uma analise e abre ocorrencia se houver nao-conformidade")
    public ResponseEntity<AnaliseDto.Saida> registrar(
            @Valid @RequestBody AnaliseDto.Entrada entrada) {

        Analise analise = servico.registrar(entrada.codigoPonto(), entrada.coletor(),
                entrada.coletadoEm(), entrada.leiturasDominio());

        return ResponseEntity
                .created(URI.create("/api/analises/" + analise.getId()))
                .body(AnaliseDto.Saida.de(analise));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma analise pelo identificador")
    public AnaliseDto.Saida buscar(@PathVariable String id) {
        return AnaliseDto.Saida.de(servico.buscarPorId(id));
    }

    @GetMapping("/ponto/{codigoPonto}")
    @Operation(summary = "Lista as analises de um ponto, da mais recente para a mais antiga")
    public List<AnaliseDto.Saida> listarPorPonto(@PathVariable String codigoPonto) {
        return servico.listarPorPonto(codigoPonto).stream().map(AnaliseDto.Saida::de).toList();
    }

    @GetMapping
    @Operation(summary = "Lista as analises coletadas dentro de um periodo")
    public List<AnaliseDto.Saida> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fim) {

        return servico.listarPorPeriodo(inicio, fim).stream().map(AnaliseDto.Saida::de).toList();
    }
}
