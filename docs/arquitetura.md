# Arquitetura

## Camadas

```
frontend/     React + TypeScript — fala com o backend só pela API REST
  └── HTTP ↓
api/          Controllers REST, DTOs e tradução de erros para HTTP
  └── depende de ↓
servico/      Casos de uso, orquestração e regras de aplicação
  └── depende de ↓
repositorio/  Interfaces Spring Data MongoDB e a agregação da série histórica
dominio/      Entidades, objetos de valor e regras de negócio
config/       Beans de infraestrutura, rotas da SPA e carga de demonstração
```

O frontend é empacotado dentro do mesmo jar (perfil `-Pfrontend`) e servido pelo
Spring na mesma origem da API; `RotasSpaConfig` devolve o `index.html` para as rotas
da interface, para que um F5 em `/ocorrencias` não vire 404.

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
                  ┌── nova tratativa ──┐
                  │                    │
                  ▼                    │
ABERTA ──> EM_TRATATIVA ───────────────┘
   │             │
   └─────────────┴──> RESOLVIDA
```

`EM_TRATATIVA` transiciona para si mesmo: a vigilância registra quantas ações
forem necessárias antes de encerrar, e cada ação grava uma tratativa. É o único
laço do ciclo — `ABERTA` não o tem, porque a primeira ação é o que tira a
ocorrência da fila de não-iniciadas.

`RESOLVIDA` não tem saída. O agregado recusa qualquer transição que o enum não
autorize, então não existe caminho no código para resolver duas vezes a mesma
ocorrência nem para reabrir uma resolvida.

### Enum com comportamento — `TipoFonte`

Cada tipo de fonte declara, no próprio construtor da constante, a frequência mínima
de coleta, e sabe calcular até quando a próxima coleta precisa acontecer
(`proximaColetaAte`). A alternativa — um mapa `TipoFonte → Duration` num serviço —
deixaria possível esquecer um tipo novo; no enum, o compilador obriga cada constante a
declarar a sua.

### Objetos de valor imutáveis

`ResultadoParametro`, `Localizacao`, `Responsavel`, `ParametroViolado`,
`Tratativa` e `ResultadoAnalise` são `record` com validação no construtor
compacto. Não existe instância inválida desses tipos em memória. `SituacaoColeta`,
`SerieHistorica` e `LeituraDiaria` são `record` de leitura, que levam os resultados
dos serviços até o JSON sem estado mutável no caminho.

### Fragmento de repositório — `AnaliseRepositoryCustom`

A série histórica é uma agregação, que não cabe em método derivado do Spring Data.
Em vez de um serviço receber `MongoTemplate` e misturar orquestração com BSON, a
agregação fica num fragmento (`AnaliseRepositoryCustom` + `AnaliseRepositoryCustomImpl`)
que o `AnaliseRepository` estende. Para quem usa, é mais um método do repositório —
e o serviço continua testável com o repositório simulado.

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

### Alerta de coleta vencida

```
GET /api/painel/coletas
   │
   └─ MonitoramentoColetaService.situacaoDosPontosAtivos()
         para cada ponto ativo:
           AnaliseRepository.findFirstByPontoIdOrderByColetadoEmDesc()  ← índice composto
           TipoFonte.proximaColetaAte(ultimaColeta)
           vencida = agora > limite   (ponto sem coleta: vencido)
         ordena: nunca coletados, depois os mais atrasados, depois os em dia
```

### Série histórica

```
GET /api/painel/serie/{codigo}?parametro=&inicio=&fim=
   │
   ├─ CatalogoParametros.buscar(parametro)        → 422 se fora do catálogo
   ├─ PontoMonitoramentoService.buscarPorCodigo() → 404 se não existe
   ├─ período: padrão de 90 dias até agora        → 400 se invertido
   │
   └─ AnaliseRepository.serieDiaria()             → pipeline de agregação no MongoDB
```

O pipeline está documentado em [modelagem-nosql.md](modelagem-nosql.md#agregação--série-histórica-por-parâmetro).

## Injeção de `Clock`

`PainelConformidadeService`, `MonitoramentoColetaService` e `SerieHistoricaService`
recebem um `java.time.Clock` em vez de chamar `Instant.now()`. Prazo vencido, coleta
vencida e o período padrão da série dependem do instante atual, e sem relógio injetável
esses trechos seriam intestáveis — o teste teria de esperar dias ou aceitar não
cobri-los. Com `Clock.fixed()`, cada cenário é determinístico, inclusive o limite
exato ("vence no instante seguinte ao prazo, e não no próprio prazo").

## Tradução de erros

Centralizada em `TratadorDeErros` (`@RestControllerAdvice`). Nenhum controller
precisa saber qual status HTTP corresponde a qual falha.

| Exceção | HTTP | Quando |
|---|---|---|
| `RecursoNaoEncontradoException` | 404 | Identificador ou código não existe |
| `RegraNegocioException` | 409 | Ponto desativado, código duplicado, transição inválida |
| `ParametroDesconhecidoException` | 422 | Código fora do catálogo da norma |
| `MethodArgumentNotValidException` | 400 | Falha de validação do corpo da requisição |
| `IllegalArgumentException` | 400 | Invariante violada, como período com início depois do fim |

## Estratégia de testes

| Nível | Ferramenta | O que cobre |
|---|---|---|
| Unidade — domínio | JUnit 5 + AssertJ | Limites da norma, invariantes, ciclo de vida, frequência por tipo de fonte |
| Unidade — aplicação | Mockito | Orquestração dos serviços, coleta vencida, período da série, tradução de exceções |
| Web | MockMvc | Contratos HTTP e formato do JSON |
| Integração | Testcontainers + MongoDB 7 | Round-trip dos subdocumentos, consultas derivadas, índices e o pipeline de agregação |
| Frontend | Vitest + Testing Library + MSW | Cliente da API, erros, formatos, prévia de conformidade, escala do gráfico e as telas com HTTP simulado |
| End-to-end | Playwright | O fluxo da demonstração num navegador, contra o jar empacotado e um MongoDB real |

Os testes de integração são ignorados automaticamente em máquina sem Docker
(`@Testcontainers(disabledWithoutDocker = true)`). A cobertura mínima exigida é
sustentada pelos testes de unidade, que não dependem de infraestrutura nenhuma — mas
a CI reprova o build se a suíte de integração tiver sido pulada, porque foi assim que
um defeito real passou despercebido.

### Por que o frontend testa lógica, e não layout

A cobertura do frontend é medida só em `src/api` e `src/dominio`, onde a falha é
silenciosa: um erro da API que vira mensagem genérica, uma data convertida no fuso
errado, um ponto do gráfico na posição errada. Layout aparece na primeira vez que
alguém abre a tela; snapshot de JSX ensinaria a equipe a atualizar o snapshot por
reflexo. As telas são exercitadas pelos testes com HTTP simulado e pelos cenários
end-to-end.
