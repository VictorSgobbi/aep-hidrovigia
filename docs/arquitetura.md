# Arquitetura

## Camadas

```
api/          Controllers REST, DTOs e tradução de erros para HTTP
  └── depende de ↓
servico/      Casos de uso, orquestração e regras de aplicação
  └── depende de ↓
repositorio/  Interfaces Spring Data MongoDB
dominio/      Entidades, objetos de valor e regras de negócio
config/       Beans de infraestrutura e carga de demonstração
```

A dependência aponta sempre para dentro. O pacote `dominio` **não importa nada
do Spring** — nem anotação de injeção, nem `@Component`. Só usa
`@Document`/`@Id` do Spring Data nas três raízes de agregado, que é mapeamento
de persistência, não acoplamento de framework.

Consequência prática: as regras de potabilidade rodam em teste de unidade puro,
sem subir contexto. É por isso que a suíte inteira leva segundos.

## Onde cada padrão foi usado, e por quê

### Template Method — `ParametroPotabilidade.avaliar()`

O método é `final`. Ele fixa o formato do veredito (`ResultadoParametro`, sempre
com o limite aplicado registrado) e delega às subclasses só a decisão de limite.
Sem isso, cada subclasse poderia montar o resultado de um jeito, e o documento
gravado ficaria irregular entre parâmetros.

### Polimorfismo — as três formas de limite

```
ParametroPotabilidade (abstract)
├── ParametroFaixa      pH 6,0–9,0 · cloro residual 0,2–2,0 mg/L
├── ParametroMaximo     turbidez ≤ 5,0 uT · nitrato ≤ 10 mg/L
└── ParametroAusencia   E. coli e coliformes: contagem zero
```

A alternativa seria um `switch` por código de parâmetro dentro do serviço.
Cada novo parâmetro da norma exigiria editar esse `switch`. Com a hierarquia,
adicionar um parâmetro é adicionar uma linha no catálogo.

### Strategy — `ClassificadorGravidade`

Duas políticas implementadas: `ClassificadorPorRiscoSanitario` (padrão — o pior
risco dita a urgência) e `ClassificadorPorQuantidade` (acúmulo de desvios).
Trocar a política em vigor é trocar o `@Bean` em `DominioConfig`. A ocorrência
grava qual política a classificou, então a decisão fica rastreável.

### State — `StatusOcorrencia`

As transições válidas são declaradas no próprio enum:

```
ABERTA ──> EM_TRATATIVA ──> RESOLVIDA
   └──────────────────────────┘
```

`RESOLVIDA` não tem saída. O agregado recusa qualquer transição que o enum não
autorize, então não existe caminho no código para resolver duas vezes a mesma
ocorrência nem para reabrir uma resolvida.

### Objetos de valor imutáveis

`ResultadoParametro`, `Localizacao`, `Responsavel`, `ParametroViolado`,
`Tratativa` e `ResultadoAnalise` são `record` com validação no construtor
compacto. Não existe instância inválida desses tipos em memória.

### Agregados fechados

`Analise` não tem setter. Depois de registrada, nenhum parâmetro entra ou sai —
corrigir uma coleta significa registrar outra análise. O histórico de vigilância
precisa ser imutável para servir de prova.

## Fluxo principal

```
POST /api/analises
   │
   ├─ PontoMonitoramentoService.buscarPorCodigo()  → 404 se não existe
   ├─ ponto desativado?                            → 409
   ├─ código repetido na mesma análise?            → 409
   │
   ├─ para cada leitura:
   │     CatalogoParametros.buscar(codigo)         → 422 se fora do catálogo
   │     parametro.avaliar(valor)                  → ResultadoParametro
   │
   ├─ Analise.registrar(...)                       → consolida o veredito
   ├─ AnaliseRepository.save()
   │
   └─ se reprovou:
         ClassificadorGravidade.classificar()      → CRITICA | ALTA | MEDIA
         Ocorrencia.abrir()                        → prazo = 24h | 72h | 168h
         OcorrenciaRepository.save()
```

## Injeção de `Clock`

`PainelConformidadeService` recebe um `java.time.Clock` em vez de chamar
`Instant.now()`. O cálculo de ocorrências vencidas depende do instante atual, e
sem relógio injetável esse trecho seria intestável — o teste teria de esperar 24
horas ou aceitar não cobri-lo. Com `Clock.fixed()`, o cenário é determinístico.

## Tradução de erros

Centralizada em `TratadorDeErros` (`@RestControllerAdvice`). Nenhum controller
precisa saber qual status HTTP corresponde a qual falha.

| Exceção | HTTP | Quando |
|---|---|---|
| `RecursoNaoEncontradoException` | 404 | Identificador ou código não existe |
| `RegraNegocioException` | 409 | Ponto desativado, código duplicado, transição inválida |
| `ParametroDesconhecidoException` | 422 | Código fora do catálogo da norma |
| `MethodArgumentNotValidException` | 400 | Falha de validação do corpo da requisição |

## Estratégia de testes

| Nível | Ferramenta | O que cobre |
|---|---|---|
| Unidade — domínio | JUnit 5 + AssertJ | Limites da norma, invariantes, ciclo de vida |
| Unidade — aplicação | Mockito | Orquestração dos serviços, tradução de exceções |
| Web | MockMvc | Contratos HTTP e formato do JSON |
| Integração | Testcontainers + MongoDB 7 | Round-trip dos subdocumentos e consultas derivadas |

Os testes de integração são ignorados automaticamente em máquina sem Docker
(`@Testcontainers(disabledWithoutDocker = true)`). A cobertura mínima exigida é
sustentada pelos testes de unidade, que não dependem de infraestrutura nenhuma.
