# HidroVigia

**Vigilância da qualidade da água em sistemas de abastecimento de pequeno porte.**

Prova de Conceito da AEP 2026.2 — 6S — Engenharia de Software.
ODS 6: Água Potável e Saneamento.

---

## O problema

Municípios pequenos e sistemas comunitários — poços de escolas rurais,
associações, cisternas, distritos afastados — são obrigados a monitorar a
potabilidade da água que distribuem. Na prática, esse controle é feito em
planilhas e cadernos de campo.

O efeito é sempre o mesmo: **uma análise fora do padrão demora dias para virar
ação.** O laudo chega, é arquivado, e a água continua sendo distribuída enquanto
ninguém cruza aquele resultado com o limite legal nem cobra uma providência de
alguém com nome e prazo.

O HidroVigia fecha esse intervalo. Cada coleta registrada é confrontada
automaticamente com o padrão de potabilidade brasileiro, e todo desvio abre uma
ocorrência com gravidade e prazo de resposta.

### Público-alvo

Operadores de sistemas de abastecimento de pequeno porte, vigilância sanitária
municipal e associações comunitárias rurais.

---

## Alinhamento com o ODS 6

| Meta | Como a PoC contribui |
|---|---|
| **6.1** Acesso universal a água potável segura | Torna verificável se a água distribuída atende ao padrão legal |
| **6.3** Melhorar a qualidade da água | Reduz o tempo entre detectar um desvio e agir sobre ele |
| **6.4** Eficiência no uso da água | Base para acompanhar perdas e desempenho por ponto (evolução prevista) |

O alinhamento não é retórico: **as regras de negócio do sistema são a própria
norma.** Os limites aplicados vêm da Portaria GM/MS nº 888/2021, que alterou o
Anexo XX da Portaria de Consolidação nº 5/2017 e define o padrão de potabilidade
brasileiro.

> ⚠️ **Antes da entrega final:** confira cada limite implementado contra o texto
> oficial da portaria. Os valores em `CatalogoParametros` seguem o padrão
> brasileiro, mas a PoC não substitui a norma. O mesmo vale para qualquer
> estatística que entrar nesta documentação — só entra com fonte e ano.

### Parâmetros implementados

| Código | Parâmetro | Limite | Risco |
|---|---|---|---|
| `ECOLI` | *Escherichia coli* | ausência (UFC/100 mL) | Microbiológico |
| `CTOT` | Coliformes totais | ausência (UFC/100 mL) | Microbiológico |
| `CRL` | Cloro residual livre | 0,2 – 2,0 mg/L | Desinfecção |
| `PH` | pH | 6,0 – 9,0 | Físico-químico |
| `TURB` | Turbidez | ≤ 5,0 uT | Físico-químico |
| `NITRATO` | Nitrato (como N) | ≤ 10,0 mg/L | Físico-químico |
| `FLUOR` | Fluoreto | ≤ 1,5 mg/L | Físico-químico |
| `COR` | Cor aparente | ≤ 15,0 uH | Organoléptico |

---

## Fluxo principal

```
Cadastrar ponto de monitoramento
        ↓
Técnico registra uma análise com N parâmetros medidos
        ↓
Sistema confronta cada valor com o limite legal
        ↓
Algum parâmetro reprovou?
        ├── Não → análise gravada como conforme
        └── Sim → classifica a gravidade e abre ocorrência com prazo
                     ↓
              Vigilância registra tratativas até resolver
                     ↓
              Painel mostra conformidade e pendências vencidas
```

### Gravidade e prazo

A política padrão classifica pelo pior risco encontrado na amostra:

| Gravidade | Prazo | Dispara quando |
|---|---|---|
| **Crítica** | 24 h | Detecção microbiológica (*E. coli* ou coliformes) |
| **Alta** | 72 h | Falha de desinfecção ou desvio físico-químico |
| **Média** | 168 h | Desvio apenas organoléptico |

Dez desvios de cor continuam sendo menos urgentes que uma única detecção de
*E. coli* — por isso a política olha o risco, não a contagem.

---

## Tecnologias

| Camada | Escolha |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Banco de dados | **MongoDB 7** (NoSQL, orientado a documentos) |
| Persistência | Spring Data MongoDB |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ |
| Testes de integração | Testcontainers + MongoDB 7 |
| Cobertura | JaCoCo, mínimo obrigatório de 70% |
| Integração contínua | GitHub Actions |

---

## Como executar

### Pré-requisitos

- **JDK 21** ([Temurin](https://adoptium.net/temurin/releases/?version=21))
- **Maven 3.9+**
- **Docker** e Docker Compose

### 1. Suba o MongoDB

```bash
docker compose up -d
```

O banco fica em `localhost:27017`, base `hidrovigia`, com volume nomeado para os
dados sobreviverem ao `docker compose down`.

### 2. Rode a aplicação

```bash
mvn spring-boot:run
```

Na primeira subida a base é populada com 3 pontos e 5 análises de demonstração,
cobrindo os quatro desfechos possíveis. Para desligar a carga:

```bash
HIDROVIGIA_CARGA_DEMONSTRACAO=false mvn spring-boot:run
```

### 3. Abra a documentação interativa

**http://localhost:8080/swagger-ui.html**

É por essa tela que a PoC é demonstrada — todo o fluxo pode ser executado ali,
sem front-end próprio.

### Inspecionar as coleções (opcional)

```bash
docker compose --profile ferramentas up -d
```

Mongo Express em **http://localhost:8081**.

---

## Testes e cobertura

Um único comando roda a suíte, gera o relatório e **reprova o build se a
cobertura ficar abaixo de 70%**:

```bash
mvn clean verify
```

Relatório em `target/site/jacoco/index.html`.

```bash
start target/site/jacoco/index.html
```

Só o relatório, sem a trava de cobertura:

```bash
mvn clean test
```

### O que é testado

| Nível | O que cobre |
|---|---|
| Domínio | Limites da norma (valor conforme, no limite e violado por parâmetro), invariantes dos agregados, ciclo de vida da ocorrência |
| Aplicação | Orquestração dos serviços com repositórios mockados |
| Web | Contratos HTTP e formato do JSON, via MockMvc |
| Integração | Round-trip dos subdocumentos em MongoDB real, via Testcontainers |

Os testes de integração são **ignorados automaticamente** em máquina sem Docker.
A cobertura mínima é sustentada pelos testes de unidade, que não dependem de
infraestrutura nenhuma.

---

## Estrutura do banco

Três coleções, relacionamento por identificador e subdocumentos aninhados em
todas elas.

| Coleção | Aninhamento | Referencia |
|---|---|---|
| `pontos_monitoramento` | `localizacao`, `responsavel` | — |
| `analises` | `parametros[]`, `resultado` | `pontoId` |
| `ocorrencias` | `parametrosViolados[]`, `tratativas[]` | `analiseId`, `pontoId` |

Exemplo de documento em `analises`:

```json
{
  "pontoId": "68b...",
  "pontoCodigo": "PMA-001",
  "coletadoEm": "2026-09-01T10:00:00Z",
  "coletor": "Tecnico Bruno",
  "parametros": [
    { "codigo": "ECOLI", "valor": "14", "unidade": "UFC/100mL",
      "limiteMaximo": "0", "conforme": false, "risco": "MICROBIOLOGICO",
      "referenciaLegal": "Portaria GM/MS n. 888/2021" }
  ],
  "resultado": { "conforme": false, "qtdParametros": 5, "qtdNaoConformidades": 2 }
}
```

Detalhes e justificativas das decisões de modelagem em
**[docs/modelagem-nosql.md](docs/modelagem-nosql.md)**.

---

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/api/pontos` | Cadastra um ponto de monitoramento |
| `GET` | `/api/pontos` | Lista pontos (filtros: `apenasAtivos`, `municipio`) |
| `GET` | `/api/pontos/codigo/{codigo}` | Busca ponto pelo código |
| `PUT` | `/api/pontos/{id}` | Atualiza dados cadastrais |
| `DELETE` | `/api/pontos/{id}` | Desativa sem apagar o histórico |
| `POST` | **`/api/analises`** | **Registra coleta e abre ocorrência se reprovar** |
| `GET` | `/api/analises/ponto/{codigo}` | Histórico de um ponto |
| `GET` | `/api/ocorrencias` | Fila de pendências por prazo |
| `POST` | `/api/ocorrencias/{id}/tratativas` | Registra uma ação |
| `POST` | `/api/ocorrencias/{id}/resolucao` | Encerra a ocorrência |
| `GET` | `/api/painel/conformidade` | Indicadores consolidados |
| `GET` | `/api/painel/parametros` | Catálogo da norma aplicado pela PoC |

### Exemplo — registrar uma coleta contaminada

```bash
curl -X POST http://localhost:8080/api/analises \
  -H "Content-Type: application/json" \
  -d '{
    "codigoPonto": "PMA-001",
    "coletor": "Tecnico Bruno",
    "coletadoEm": "2026-09-01T10:00:00Z",
    "leituras": [
      { "codigo": "ECOLI", "valor": 14 },
      { "codigo": "CRL",   "valor": 0.05 },
      { "codigo": "PH",    "valor": 7.1 },
      { "codigo": "TURB",  "valor": 6.2 }
    ]
  }'
```

Retorna `201` com o veredito de cada parâmetro e abre uma ocorrência **crítica**
com prazo de 24 horas, visível em `GET /api/ocorrencias`.

---

## Arquitetura

```
api/          Controllers REST, DTOs, tradução de erros
servico/      Casos de uso e orquestração
repositorio/  Interfaces Spring Data MongoDB
dominio/      Entidades, objetos de valor e regras de negócio
config/       Beans de infraestrutura e carga de demonstração
```

O pacote `dominio` não depende do Spring, o que permite testar as regras de
potabilidade sem subir contexto.

Padrões aplicados: **Template Method** em `ParametroPotabilidade.avaliar()`,
**polimorfismo** nas três formas de limite, **Strategy** na classificação de
gravidade, **State** no ciclo de vida da ocorrência e **objetos de valor
imutáveis** em todos os subdocumentos.

Detalhamento em **[docs/arquitetura.md](docs/arquitetura.md)**.

---

## Equipe

| Integrante | Trilha |
|---|---|
| Victor Sgobbi | Domínio e regras de negócio |
| Bruno | Persistência e modelagem NoSQL |
| Leonardo | API, qualidade e documentação |

Convenção de commits e fluxo de trabalho em **[CONTRIBUTING.md](CONTRIBUTING.md)**.

---

## Entregas

| Marco | Versão | Conteúdo |
|---|---|---|
| 1ª entrega | `v1.0-entrega1` | Fluxo principal funcional, três coleções, testes com cobertura ≥ 70% |
| 2ª entrega | *a definir* | Série histórica com agregações, alerta de análise vencida, relatório de conformidade |

## Licença

MIT — ver [LICENSE](LICENSE).
