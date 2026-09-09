package br.com.hidrovigia.api;

import java.net.URI;
import java.util.List;

import br.com.hidrovigia.api.dto.PontoDto;
import br.com.hidrovigia.dominio.ponto.PontoMonitoramento;
import br.com.hidrovigia.servico.PontoMonitoramentoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pontos")
@Tag(name = "1. Pontos de monitoramento",
        description = "Cadastro dos locais onde a agua e coletada")
public class PontoMonitoramentoController {

    private final PontoMonitoramentoService servico;

    public PontoMonitoramentoController(PontoMonitoramentoService servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Cadastra um ponto de monitoramento")
    public ResponseEntity<PontoDto.Saida> cadastrar(@Valid @RequestBody PontoDto.Entrada entrada) {
        PontoMonitoramento ponto = servico.cadastrar(
                entrada.codigo(), entrada.nome(), entrada.tipoFonte(), entrada.populacaoAtendida(),
                entrada.localizacao().paraDominio(), entrada.responsavel().paraDominio());

        return ResponseEntity
                .created(URI.create("/api/pontos/" + ponto.getId()))
                .body(PontoDto.Saida.de(ponto));
    }

    @GetMapping
    @Operation(summary = "Lista pontos, opcionalmente filtrando por municipio")
    public List<PontoDto.Saida> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos,
            @RequestParam(required = false) String municipio) {

        List<PontoMonitoramento> pontos = municipio == null || municipio.isBlank()
                ? servico.listar(apenasAtivos)
                : servico.listarPorMunicipio(municipio);

        return pontos.stream().map(PontoDto.Saida::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um ponto pelo identificador")
    public PontoDto.Saida buscar(@PathVariable String id) {
        return PontoDto.Saida.de(servico.buscarPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    @Operation(summary = "Busca um ponto pelo codigo")
    public PontoDto.Saida buscarPorCodigo(@PathVariable String codigo) {
        return PontoDto.Saida.de(servico.buscarPorCodigo(codigo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados cadastrais de um ponto")
    public PontoDto.Saida atualizar(@PathVariable String id,
                                    @Valid @RequestBody PontoDto.Entrada entrada) {
        return PontoDto.Saida.de(servico.atualizar(id,
                entrada.nome(), entrada.tipoFonte(), entrada.populacaoAtendida(),
                entrada.localizacao().paraDominio(), entrada.responsavel().paraDominio()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa um ponto sem apagar o historico de analises")
    public PontoDto.Saida desativar(@PathVariable String id) {
        return PontoDto.Saida.de(servico.desativar(id));
    }

    @PostMapping("/{id}/reativacao")
    @Operation(summary = "Reativa um ponto desativado")
    public PontoDto.Saida reativar(@PathVariable String id) {
        return PontoDto.Saida.de(servico.reativar(id));
    }
}
