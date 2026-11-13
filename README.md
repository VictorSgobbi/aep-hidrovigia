<div align="center">

# 💧 HidroVigia

**Vigilância da qualidade da água em sistemas de abastecimento de pequeno porte**

Prova de Conceito — AEP 2026.2 · 6º Semestre · Engenharia de Software

[![ODS 6](https://img.shields.io/badge/ODS%206-Água%20Potável%20e%20Saneamento-26BDE2?style=flat-square)](https://brasil.un.org/pt-br/sdgs/6)
[![CI](https://github.com/VictorSgobbi/aep-hidrovigia/actions/workflows/ci.yml/badge.svg)](https://github.com/VictorSgobbi/aep-hidrovigia/actions/workflows/ci.yml)
[![Cobertura](https://img.shields.io/badge/cobertura%20mínima-70%25-blue?style=flat-square)](#testes-e-cobertura)
[![Licença](https://img.shields.io/badge/licença-MIT-lightgrey?style=flat-square)](LICENSE)

</div>

---

## Sumário

1. [Identificação](#identificação)
2. [O problema](#o-problema)
3. [ODS 6](#ods-6)
4. [A solução](#a-solução)
5. [Evolução da 1ª para a 2ª entrega](#evolução-da-1ª-para-a-2ª-entrega)
6. [Tecnologias](#tecnologias)
7. [Como executar](#como-executar)
8. [Testes e cobertura](#testes-e-cobertura)
9. [Banco de dados NoSQL](#banco-de-dados-nosql)
10. [Arquitetura e orientação a objetos](#arquitetura-e-orientação-a-objetos)
11. [API](#api)
12. [Estrutura do repositório](#estrutura-do-repositório)
13. [Equipe e metodologia](#equipe-e-metodologia)
14. [Critérios de avaliação](#critérios-de-avaliação)
15. [Próximos passos](#próximos-passos)

---

## Identificação

|  |  |
|---|---|
| **Curso** | Engenharia de Software |
| **Série** | 6º Semestre |
| **Atividade** | AEP — Atividade de Estudo Programada · 2026.2 · 2ª entrega |
| **Título da PoC** | HidroVigia — vigilância da qualidade da água em sistemas de abastecimento de pequeno porte |
| **ODS atendido** | ODS 6 — Água Potável e Saneamento |

### Acadêmicos

| RA | Nome | GitHub |
|---|---|---|
| 24000732-2 | Victor Sgobbi | [@VictorSgobbi](https://github.com/VictorSgobbi) |
| 24021297-2 | Bruno | [@BrundoCJ](https://github.com/BrundoCJ) |
| 24015988-2 | Leonardo | [@Leocm123](https://github.com/Leocm123) |

### Links da entrega

| | 1ª entrega | 2ª entrega |
|---|---|---|
| **Vídeo de demonstração** | https://www.youtube.com/watch?v=ym-w1ppuR4E | *a publicar* |
| **Versão** | tag [`v1.0-entrega1`](https://github.com/VictorSgobbi/aep-hidrovigia/releases/tag/v1.0-entrega1) | tag [`v2.0-entrega2`](https://github.com/VictorSgobbi/aep-hidrovigia/releases/tag/v2.0-entrega2) — versão final |
| **Repositório** | https://github.com/VictorSgobbi/aep-hidrovigia | |
| **Quadro de tarefas** | https://github.com/users/Leocm123/projects/1 | |

---

## O problema

Municípios pequenos e sistemas comunitários — poços de escolas rurais, associações,
cisternas, distritos afastados — são obrigados a monitorar a potabilidade da água que
distribuem. Na prática, isso é feito em planilha e caderno de campo: uma análise fora
do padrão pode levar dias para virar uma ação concreta, e nesse intervalo a água
continua sendo distribuída. E um ponto que simplesmente deixa de ser amostrado nem
chega a gerar alerta — ninguém sabe se aquela água está potável.

É esse intervalo, entre detectar e agir, que o HidroVigia fecha.

| Público | Para quê |
|---|---|
| Operadores de sistemas de pequeno porte | Registrar coletas e acompanhar pendências |
| Vigilância sanitária municipal | Fiscalizar conformidade por ponto e por período |
| Associações comunitárias rurais | Comprovar que a água distribuída atende ao padrão |

---

## ODS 6

<img src="https://img.shields.io/badge/6-ÁGUA%20POTÁVEL%20E%20SANEAMENTO-26BDE2?style=for-the-badge" alt="ODS 6" />

> **Garantir a disponibilidade e a gestão sustentável da água e saneamento para todos.**

| Meta | Como a PoC contribui |
|---|---|
| **6.1** — Acesso universal e equitativo a água potável segura | Torna verificável, ponto a ponto, se a água distribuída atende ao padrão legal — e aponta os pontos que estão sem coleta |
| **6.3** — Melhorar a qualidade da água | Reduz o tempo entre detectar um desvio e agir sobre ele, com prazo por gravidade, e mostra a tendência de cada parâmetro antes do desvio |

As regras de negócio do sistema são a própria norma: os limites aplicados vêm da
**Portaria GM/MS nº 888/2021**, que define o padrão de potabilidade da água para
consumo humano no Brasil. Não há critério inventado pela equipe.

| Código | Parâmetro | Limite aplicado | Risco |
|---|---|---|---|
| `ECOLI` | *Escherichia coli* | ausência | 🔴 Microbiológico |
| `CTOT` | Coliformes totais | ausência | 🔴 Microbiológico |
| `CRL` | Cloro residual livre | 0,2 – 2,0 mg/L | 🟠 Desinfecção |
| `PH` | pH | 6,0 – 9,0 | 🟡 Físico-químico |
| `TURB` | Turbidez | ≤ 5,0 uT | 🟡 Físico-químico |
| `NITRATO` | Nitrato (como N) | ≤ 10,0 mg/L | 🟡 Físico-químico |
| `FLUOR` | Fluoreto | ≤ 1,5 mg/L | 🟡 Físico-químico |
| `COR` | Cor aparente | ≤ 15,0 uH | 🔵 Organoléptico |

> Os valores em [`CatalogoParametros`](src/main/java/br/com/hidrovigia/dominio/parametro/CatalogoParametros.java)
> seguem o padrão brasileiro, mas vale conferir cada limite contra o texto oficial da
> portaria antes de defender o trabalho. O mesmo vale para as frequências mínimas de
> coleta em [`TipoFonte`](src/main/java/br/com/hidrovigia/dominio/ponto/TipoFonte.java),
> que são uma simplificação didática da tabela de amostragem da portaria.

---

## A solução

1. Cadastra-se um ponto de monitoramento (poço, cisterna, manancial, reservatório...).
2. O técnico registra a coleta com os parâmetros medidos.
3. O sistema confronta cada valor com o limite da norma.
4. Se está tudo conforme, a análise é só gravada. Se algo reprova, o sistema classifica
   a gravidade e abre uma ocorrência com prazo automaticamente.
5. A vigilância registra as tratativas até resolver a pendência.
6. O painel mostra conformidade, pendências e prazos vencidos em tempo real.
7. O painel também aponta os **pontos com coleta vencida** — sem amostra dentro da
   frequência mínima do seu tipo de fonte — e, por ponto, a **série histórica** de cada
   parâmetro com a faixa da norma.

### Gravidade e prazo de resposta

A política padrão classifica pelo **pior risco encontrado na amostra**:

| Gravidade | Prazo | Dispara quando |
|:---:|:---:|---|
| 🔴 **Crítica** | **24 h** | Detecção microbiológica (*E. coli* ou coliformes totais) |
| 🟠 **Alta** | **72 h** | Falha de desinfecção ou desvio físico-químico |
| 🟡 **Média** | **168 h** | Desvio apenas organoléptico |

Dez desvios de cor continuam sendo menos urgentes que uma única detecção de *E. coli*,
porque a política olha o risco, não a contagem. Uma segunda política
(`ClassificadorPorQuantidade`) também está implementada, para mostrar que essa decisão
é um ponto de extensão real — e a ocorrência grava qual política a classificou.

---

## Evolução da 1ª para a 2ª entrega

A 1ª entrega mostrou o fluxo principal funcionando: registrar a coleta, abrir a
ocorrência e tratá-la até o fim. A 2ª entrega acrescenta o que a vigilância precisa
para **agir antes** do desvio — enxergar o ponto esquecido e a tendência de cada
parâmetro.

| Área | 1ª entrega (`v1.0-entrega1`) | 2ª entrega (`v2.0-entrega2`) |
|---|---|---|
| Acompanhamento | Só o que foi medido aparece | **Alerta de coleta vencida**: pontos sem amostra dentro da frequência do seu tipo de fonte ([#9](https://github.com/VictorSgobbi/aep-hidrovigia/pull/9)) |
| Análise no tempo | Lista de análises de um ponto | **Série histórica** por parâmetro, com gráfico e a faixa da norma ([#22](https://github.com/VictorSgobbi/aep-hidrovigia/pull/22)) |
| Banco | Índices simples e índice único em `analiseId` | **Índice composto** `{ pontoId: 1, coletadoEm: -1 }` ([#8](https://github.com/VictorSgobbi/aep-hidrovigia/pull/8)) e **pipeline de agregação** calculado no MongoDB |
| Endpoints | 18 | 21 |
| Testes | 188 de backend, 59 de frontend, 2 cenários end-to-end | 205 de backend, 67 de frontend, 4 cenários end-to-end |
| Metodologia | Pull requests revisados | Pull requests ligados a issues de um [quadro de tarefas](https://github.com/users/Leocm123/projects/1) |
| Dados de demonstração | 3 pontos e 5 análises | 4 pontos e 9 análises, com histórico e ponto sem coleta |

A interface React, a integração contínua com três jobs e os cenários end-to-end já
existiam na 1ª entrega e foram estendidos para as funcionalidades novas.

Um defeito de infraestrutura foi achado e corrigido nesta entrega
([#8](https://github.com/VictorSgobbi/aep-hidrovigia/pull/8)): o Docker Engine 29
passou a exigir no mínimo a versão 1.44 da sua API, o projeto pedia a 1.41, e o
Testcontainers leu a recusa como "não há Docker aqui" — os testes de integração passaram
a ser **pulados em silêncio** nas máquinas da equipe, com o build verde. É o mesmo
sintoma que, na 1ª entrega, escondeu a falta de índice único em `analiseId`. Por isso a
CI reprova o build se qualquer suíte for pulada: suíte que não roda parece suíte que
passa.

---

## Tecnologias

| Camada | Tecnologia | Papel |
|---|---|---|
| Interface | React 19 + Vite + TypeScript | Telas que consomem a API |
| Estado remoto | TanStack Query | Cache e invalidação cruzada entre telas |
| Gráficos | SVG próprio | Série histórica sem dependência de biblioteca |
| Linguagem | Java 21 | Programação orientada a objetos |
| Framework | Spring Boot 3.3.5 | Injeção de dependências, REST, configuração |
| Banco de dados | MongoDB 7 | NoSQL orientado a documentos |
| Persistência | Spring Data MongoDB + `MongoTemplate` | Repositórios, mapeamento e pipeline de agregação |
| Validação | Jakarta Bean Validation | Contratos de entrada da API |
| Documentação da API | springdoc-openapi | Swagger UI interativo |
| Testes de backend | JUnit 5 · Mockito · AssertJ · MockMvc | Domínio, aplicação e web |
| Testes de integração | Testcontainers + MongoDB 7 | Banco real e efêmero |
| Testes de frontend | Vitest · Testing Library · MSW | Lógica de API e de domínio, telas com HTTP simulado |
| Testes end-to-end | Playwright | O fluxo da demonstração num navegador, contra o jar |
| Cobertura | JaCoCo · Vitest (v8) | Relatório e trava mínima no build |
| Integração contínua | GitHub Actions | Três jobs a cada push e pull request |
| Infraestrutura local | Docker Compose | MongoDB e Mongo Express |

---

## Como executar

### Pré-requisitos

| Ferramenta | Versão | Onde obter |
|---|---|---|
| JDK | 21 | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) |
| Docker | com Compose v2 | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| Node | 22+ — opcional | [nodejs.org](https://nodejs.org/) |

Node só é necessário para mexer no frontend. Quem trabalha só no backend não precisa
de nada além do JDK e do Docker — `./mvnw clean verify` continua sendo um comando só
de Java, e o perfil `-Pfrontend` baixa o Node sozinho quando precisa empacotar a
interface. Maven também não precisa ser instalado: o repositório traz o Maven Wrapper
(`mvnw` no Linux/macOS, `mvnw.cmd` no Windows), que baixa a versão certa sozinho.

No Windows, o JDK sai em um comando:

```bash
winget install EclipseAdoptium.Temurin.21.JDK
```

### 1. Clone o repositório

```bash
git clone https://github.com/VictorSgobbi/aep-hidrovigia.git
cd aep-hidrovigia
```

### 2. Suba o MongoDB

```bash
docker compose up -d
```

Banco em `localhost:27017`, base `hidrovigia`, com volume nomeado — os dados sobrevivem
ao `docker compose down`. Para recomeçar com a base limpa, `docker compose down -v`.

### 3. Rode a aplicação

```bash
./mvnw -Pfrontend spring-boot:run
```

Um comando serve a interface, a API e o Swagger na porta 8080. Sem o perfil
(`./mvnw spring-boot:run`) sobe só a API, o que já é suficiente para trabalhar no
backend.

Na primeira subida, numa base vazia, a aplicação carrega **4 pontos e 9 análises** de
demonstração:

| Ponto | O que mostra |
|---|---|
| `PMA-001` | Cinco coletas conformes ao longo de dois meses, com o cloro residual caindo até encostar no mínimo — a série histórica |
| `PMA-002` | Uma coleta conforme e um desvio de cor — ocorrência **média** |
| `PMA-003` | Falha de desinfecção e contaminação por *E. coli* — ocorrências **alta** e **crítica** |
| `PMA-004` | Nascente cadastrada e nunca amostrada — **coleta vencida** |

O painel abre em **66,7% de conformidade (6 de 9 análises) e 3 pendências**. Para
desligar a carga:

```bash
HIDROVIGIA_CARGA_DEMONSTRACAO=false ./mvnw spring-boot:run
```

### 4. Abra a interface

**http://localhost:8080/**

| Tela | O que faz |
|---|---|
| **Painel** | Conformidade, pendências e prazos; coletas vencidas; ao escolher um ponto, a série histórica de cada parâmetro |
| **Pontos** | Cadastro, edição, desativação e reativação dos pontos de monitoramento |
| **Coletas** | Formulário montado pelo catálogo da norma, com prévia de conformidade e o veredito do servidor |
| **Ocorrências** | Fila de pendências por prazo, tratativas e encerramento |

A documentação interativa da API fica em **http://localhost:8080/swagger-ui.html**.

### 5. Para desenvolver o frontend

```bash
./mvnw spring-boot:run          # terminal 1: API em :8080
cd frontend && npm install && npm run dev   # terminal 2: interface em :5173
```

O dev server do Vite encaminha `/api` para a porta 8080, então o navegador vê tudo na
mesma origem, sem CORS para configurar.

### 6. (Opcional) Inspecione as coleções

```bash
docker compose --profile ferramentas up -d
```

Mongo Express em **http://localhost:8081**.

### Parar tudo

```bash
docker compose --profile ferramentas down
```

---

## Testes e cobertura

### Comando único e reproduzível

```bash
./mvnw clean verify
```

Compila, roda a suíte inteira, gera o relatório de cobertura e **reprova o build se a
cobertura de linhas ficar abaixo de 70%**.

O relatório fica em `target/site/jacoco/index.html`:

```bash
start target/site/jacoco/index.html
```

Para gerar o relatório sem a trava de cobertura, `./mvnw clean test`.

Ficam fora da medição apenas a classe de bootstrap (`HidroVigiaApplication`) e o
pacote `config`, que contêm declaração de beans e carga de demonstração — nenhuma
regra de negócio.

### Cobertura atual

| Medição | Ferramenta | Escopo | Mínimo travado | Atual |
|---|---|---|:---:|:---:|
| Backend | JaCoCo (linhas) | `src/main/java`, menos bootstrap e `config/` | **70%** | **99%** (722 de 726 linhas) |
| Lógica do frontend | Vitest (v8) | `frontend/src/api` e `frontend/src/dominio` | **90%** | **100%** |

### O que é testado

| Suíte | Quantidade | Comando |
|---|:---:|---|
| Backend | **205 testes** em 20 classes | `./mvnw clean verify` |
| Frontend | **67 testes** em 11 arquivos | `cd frontend && npm run verificar` |
| End-to-end | **4 cenários** no Chromium | ver abaixo |

As três medem coisas diferentes e não se somam.

#### Backend, em quatro níveis

| Nível | Ferramenta | O que cobre |
|---|---|---|
| Domínio | JUnit 5 + AssertJ | Limites da norma (conforme, no limite e violado por parâmetro), invariantes dos agregados, ciclo de vida da ocorrência, frequência de coleta por tipo de fonte |
| Aplicação | Mockito | Orquestração dos serviços com repositórios simulados: abertura de ocorrência, coleta vencida, período da série histórica |
| Web | MockMvc | Contratos HTTP (201/400/404/409/422), header `Location` e formato do JSON |
| Integração | Testcontainers + MongoDB 7 | Round-trip dos subdocumentos, consultas por campo aninhado, índices único e composto, e o **pipeline de agregação** da série histórica |

Nenhuma data de teste vem de `Instant.now()`: todas derivam de `Fixtures.REFERENCIA`,
um instante fixo, e quem precisa de "agora" recebe um `Clock` parado — evita o clássico
teste que falha uma vez a cada dez por depender do relógio da máquina.

O teste de integração da série histórica grava, no mesmo dia, os valores `10.5` e
`9.0`. Comparados como texto, `"10.5"` seria o menor; o teste só passa porque o
pipeline converte com `$toDecimal` antes do `$min`.

#### Frontend

O esforço de teste do frontend está concentrado onde a falha é silenciosa: normalização
dos dois formatos de erro da API, distribuição das mensagens de validação pelos campos,
conversões de data e decimal, a prévia de conformidade e a escala do gráfico da série.
Layout não é testado por unidade, de propósito — isso apareceria na primeira vez que
alguém abrisse a tela.

#### End-to-end

```bash
docker compose up -d
./mvnw -Pfrontend clean package -DskipTests
cd frontend && npx playwright install chromium && npm run e2e
```

Quatro cenários no Playwright contra o **jar empacotado**, que é exatamente como a
demonstração roda: registrar uma coleta contaminada e ver a ocorrência crítica abrir,
ser tratada e encerrada; o alerta de coleta vencida; a série histórica ao escolher um
ponto; e um F5 numa rota da interface carregando a aplicação em vez de um 404. As
asserções são relativas ao estado do banco, então o cenário roda quantas vezes for
preciso durante os ensaios.

### Integração contínua

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda a cada push e
pull request:

| Job | O que faz |
|---|---|
| **Testes e cobertura** | `./mvnw clean verify` e a confirmação de que os testes de integração executaram |
| **Frontend (lint, tipos e testes)** | `npm ci`, checagem de tipos, lint e testes com cobertura |
| **Interface no jar (empacotamento e e2e)** | `-Pfrontend package`, a conferência de que a SPA entrou no jar e os cenários do Playwright contra ele, com MongoDB de serviço |

Os três têm um passo que reprova o build se a suíte tiver sido **pulada**: suíte que não
roda é pior que suíte que falha.

---

## Banco de dados NoSQL

Três coleções, com relacionamento por identificador e subdocumentos aninhados em
todas elas — o nível de complexidade mais alto do edital, que satisfaz também o mais
simples.

| Coleção | Subdocumentos aninhados | Referencia | Índices |
|---|---|---|---|
| `pontos_monitoramento` | `localizacao`, `responsavel` | — | `codigo` (único) |
| `analises` | `parametros[]`, `resultado` | `pontoId` | `{ pontoId: 1, coletadoEm: -1 }` (composto), `coletadoEm` |
| `ocorrencias` | `parametrosViolados[]`, `tratativas[]` | `analiseId`, `pontoId` | `analiseId` (único), `pontoId` |

```
pontos_monitoramento (1) ──────< (N) analises
          │                            │
          │                            │ 1 : 0..1
          └──────────< (N) ocorrencias ┘
```

Uma análise reprovada gera exatamente uma ocorrência; uma análise conforme não gera
nenhuma. `analiseId` tem índice único, e o serviço checa antes e devolve `409` — o
índice é o que segura o caso de duas requisições concorrentes.

### Decisões de modelagem

**O limite aplicado fica gravado dentro de cada parâmetro.** Poderia ser lido do
catálogo na hora de exibir, mas a norma muda: uma análise de 2026 precisa continuar
mostrando o limite que valia em 2026.

**`pontoCodigo` é repetido em `analises` e `ocorrencias`.** Desnormalização
deliberada — a fila de pendências mostra o código do ponto em cada linha, e repetir 7
caracteres evita uma consulta extra por linha. O código do ponto é imutável, então não
há risco de divergência.

**Índice composto `{ pontoId: 1, coletadoEm: -1 }`.** A consulta mais frequente é o
histórico de um ponto, da coleta mais recente para a mais antiga — a última coleta, que
o alerta de coleta vencida busca para cada ponto, é o primeiro item dele. Pelo prefixo,
o mesmo índice atende as contagens por ponto, então `pontoId` dispensou índice próprio.

### Agregação — série histórica

A série histórica é calculada pelo próprio MongoDB, sem trazer os documentos para a
memória:

```
$match   { pontoId, coletadoEm: { $gte, $lte } }     ← usa o índice composto
$unwind  "$parametros"                               ← um documento por leitura
$match   { "parametros.codigo": "CRL" }
$group   _id: $dateTrunc(coletadoEm, "day")
         $min / $avg / $max de $toDecimal(parametros.valor)
         leituras e leituras fora do limite
$sort    { _id: 1 }
```

Detalhamento completo, com exemplos de documento, em
[docs/modelagem-nosql.md](docs/modelagem-nosql.md).

---

## Arquitetura e orientação a objetos

```
frontend/     React — consome só a API REST
   ↓ HTTP
api/          Controllers REST, DTOs e tradução de erros para HTTP
   ↓
servico/      Casos de uso, orquestração e regras de aplicação
   ↓
repositorio/  Interfaces Spring Data MongoDB e a agregação da série histórica
dominio/      Entidades, objetos de valor e regras de negócio
config/       Beans de infraestrutura e carga de demonstração
```

A dependência aponta sempre para dentro. O pacote `dominio` não depende do Spring — só
usa `@Document`/`@Id`/`@CompoundIndex` nas três raízes de agregado, que é mapeamento de
persistência, não acoplamento de framework. Por isso as regras de potabilidade rodam em
teste de unidade puro, sem subir contexto.

| Padrão | Onde | Problema que resolve |
|---|---|---|
| Template Method | `ParametroPotabilidade.avaliar()` (`final`) | Fixa o formato do veredito; subclasse decide só o limite |
| Polimorfismo | `ParametroFaixa` · `ParametroMaximo` · `ParametroAusencia` | Elimina o `switch` por código de parâmetro |
| Strategy | `ClassificadorGravidade` (2 implementações) | Trocar a política de urgência sem tocar no serviço |
| State | `StatusOcorrencia` | Transições válidas declaradas no enum; impede reabrir resolvida |
| Enum com comportamento | `TipoFonte` | Cada tipo de fonte sabe sua frequência mínima de coleta |
| Objeto de Valor | `ResultadoParametro`, `Localizacao`, `Tratativa`, `SituacaoColeta`... | Não existe instância inválida em memória |
| Agregado fechado | `Analise` (sem setter) | Histórico imutável — corrigir é registrar outra análise |
| Fragmento de repositório | `AnaliseRepositoryCustom` + `Impl` | A agregação fica no repositório, atrás da mesma interface dos métodos derivados |
| Injeção de `Clock` | `PainelConformidadeService`, `MonitoramentoColetaService`, `SerieHistoricaService` | Torna testável tudo o que depende de "agora": prazo vencido, coleta vencida, período padrão |

Adicionar um parâmetro novo da norma é adicionar uma linha no catálogo — nenhuma
classe existente muda.

Detalhamento completo em [docs/arquitetura.md](docs/arquitetura.md).

---

## API

| Método | Rota | O que faz |
|:---:|---|---|
| `POST` | `/api/pontos` | Cadastra um ponto de monitoramento |
| `GET` | `/api/pontos` | Lista pontos — filtros `apenasAtivos` e `municipio` |
| `GET` | `/api/pontos/{id}` | Busca ponto pelo identificador |
| `GET` | `/api/pontos/codigo/{codigo}` | Busca ponto pelo código |
| `PUT` | `/api/pontos/{id}` | Atualiza dados cadastrais |
| `DELETE` | `/api/pontos/{id}` | Desativa sem apagar o histórico |
| `POST` | `/api/pontos/{id}/reativacao` | Reativa um ponto desativado |
| `POST` | `/api/analises` | Registra a coleta e abre ocorrência se reprovar |
| `GET` | `/api/analises/{id}` | Busca análise pelo identificador |
| `GET` | `/api/analises/ponto/{codigo}` | Histórico de um ponto |
| `GET` | `/api/analises?inicio=&fim=` | Análises de um período |
| `GET` | `/api/ocorrencias` | Fila de pendências ordenada por prazo |
| `GET` | `/api/ocorrencias/{id}` | Busca ocorrência |
| `POST` | `/api/ocorrencias/{id}/tratativas` | Registra uma ação sem encerrar |
| `POST` | `/api/ocorrencias/{id}/resolucao` | Encerra a ocorrência |
| `GET` | `/api/painel/conformidade` | Indicadores consolidados do sistema |
| `GET` | `/api/painel/conformidade/{codigo}` | Indicadores de um ponto |
| `GET` | `/api/painel/coletas` | 🆕 Rotina de coleta dos pontos ativos, vencidas primeiro |
| `GET` | `/api/painel/serie/{codigo}?parametro=&inicio=&fim=` | 🆕 Série histórica diária de um parâmetro; sem período, os últimos 90 dias |
| `GET` | `/api/painel/parametros` | Catálogo da norma aplicado pela PoC |

Códigos de resposta: `201` criado, `400` requisição inválida, `404` não encontrado,
`409` regra de negócio violada (ponto desativado, código duplicado, ocorrência já
resolvida), `422` parâmetro fora do catálogo da norma.

Exemplo — registrar uma coleta contaminada:

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

Retorna `201` com o veredito de cada parâmetro e abre uma ocorrência crítica com prazo
de 24 horas, imediatamente visível em `GET /api/ocorrencias`.

Exemplo — série histórica do cloro residual em PMA-001:

```bash
curl "http://localhost:8080/api/painel/serie/PMA-001?parametro=CRL"
```

```json
{
  "codigoPonto": "PMA-001",
  "codigoParametro": "CRL",
  "nomeParametro": "Cloro residual livre",
  "unidade": "mg/L",
  "limiteMinimo": 0.2,
  "limiteMaximo": 2.0,
  "dias": [
    { "dia": "2026-09-13", "minimo": 1.4, "media": 1.400, "maximo": 1.4, "leituras": 1, "foraDoLimite": 0 },
    { "dia": "2026-10-22", "minimo": 0.3, "media": 0.300, "maximo": 0.3, "leituras": 1, "foraDoLimite": 0 }
  ]
}
```

---

## Estrutura do repositório

```
aep-hidrovigia/
├── .github/workflows/ci.yml          Três jobs: backend, frontend e e2e no jar
├── docs/
│   ├── arquitetura.md                Camadas, padrões e estratégia de testes
│   ├── modelagem-nosql.md            As três coleções, índices e agregação
│   ├── roteiro-video-entrega1.txt    Roteiro do vídeo da 1ª entrega
│   └── roteiro-video-entrega2.txt    Roteiro do vídeo da 2ª entrega
├── src/
│   ├── main/
│   │   ├── java/br/com/hidrovigia/
│   │   │   ├── api/                  Controllers, DTOs, tratador de erros
│   │   │   ├── config/               Beans de domínio, Swagger, rotas da SPA, carga demo
│   │   │   ├── dominio/
│   │   │   │   ├── analise/          Analise, LeituraParametro, ResultadoAnalise
│   │   │   │   ├── gravidade/        Gravidade e as duas Strategies
│   │   │   │   ├── ocorrencia/       Ocorrencia, StatusOcorrencia, Tratativa
│   │   │   │   ├── parametro/        Hierarquia de parâmetros e catálogo
│   │   │   │   └── ponto/            PontoMonitoramento, TipoFonte, Localizacao, Responsavel
│   │   │   ├── repositorio/          3 repositórios MongoDB + agregação da série
│   │   │   └── servico/              6 serviços de aplicação
│   │   └── resources/application.yml
│   └── test/java/br/com/hidrovigia/  20 classes de teste + Fixtures
├── frontend/                         Interface React + Vite + TypeScript
│   ├── e2e/                          Cenários Playwright contra o jar
│   └── src/
│       ├── api/                      Cliente tipado e normalização de erro
│       ├── componentes/              Selos, campos, prazo, estados
│       ├── dominio/                  Rótulos, prazos, limites, formatos, escala da série
│       └── paginas/                  painel · pontos · coletas · ocorrências
├── docker-compose.yml                MongoDB 7 + Mongo Express opcional
├── mvnw · mvnw.cmd · .mvn/           Maven Wrapper — dispensa instalar Maven
├── pom.xml                           Dependências, perfil -Pfrontend e trava de cobertura
├── CONTRIBUTING.md                   Convenção de commits e fluxo de trabalho
├── LICENSE                           MIT
└── README.md
```

---

## Equipe e metodologia

| Integrante | GitHub | Responsável por |
|---|---|---|
| Victor Sgobbi | [@VictorSgobbi](https://github.com/VictorSgobbi) | Domínio: entidades, objetos de valor, Strategies, regra de coleta vencida e testes de unidade do núcleo |
| Bruno | [@BrundoCJ](https://github.com/BrundoCJ) | Persistência: modelagem das coleções, repositórios, índices, agregação, seed e testes de integração |
| Leonardo | [@Leocm123](https://github.com/Leocm123) | API, interface e qualidade: controllers, frontend, JaCoCo, CI, documentação e roteiros dos vídeos |

### Como o trabalho é organizado

- **Quadro de tarefas** — [GitHub Projects](https://github.com/users/Leocm123/projects/1),
  com os status *A fazer*, *Em andamento*, *Em revisão* e *Feito*. Cada entrega vira
  issues atribuídas pela trilha de cada integrante.
- **Uma branch e um pull request por tarefa**, ligado à sua issue — com `Closes #n`, a
  issue fecha sozinha quando o PR é integrado. A exceção é o
  [#9](https://github.com/VictorSgobbi/aep-hidrovigia/pull/9), integrado sem a referência:
  a [#15](https://github.com/VictorSgobbi/aep-hidrovigia/issues/15) foi fechada à mão e
  aponta para ele.
- **Revisão cruzada**: as evoluções da 2ª entrega ([#8](https://github.com/VictorSgobbi/aep-hidrovigia/pull/8),
  [#9](https://github.com/VictorSgobbi/aep-hidrovigia/pull/9),
  [#22](https://github.com/VictorSgobbi/aep-hidrovigia/pull/22)) foram escritas por um
  integrante e revisadas e integradas por outro.
- **CI em todo pull request**: os três jobs rodam a cada push e PR, e a equipe só integra
  com eles verdes. A branch `main` é protegida contra exclusão; a exigência do check
  não está configurada como regra do GitHub, é acordo da equipe.
- **Conventional Commits** em português, no imperativo — regras completas em
  [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Critérios de avaliação

### 2ª entrega

Como o repositório atende a cada critério da correção.

| Critério | Pts | Evidência |
|---|:---:|---|
| PoC funcional e evolução | 0,1 | Fluxos principais executáveis pela interface em `localhost:8080` e cobertos pelos cenários end-to-end; alerta de coleta vencida, série histórica e agregação como evolução, listados em [Evolução da 1ª para a 2ª entrega](#evolução-da-1ª-para-a-2ª-entrega) e comparável entre as tags `v1.0-entrega1` e `v2.0-entrega2` |
| Banco de dados NoSQL | 0,1 | [Banco de dados NoSQL](#banco-de-dados-nosql) — 3 coleções, relacionamento, subdocumentos em todas, índices único e composto, pipeline de agregação testado contra MongoDB real |
| Programação orientada a objetos e qualidade do código | 0,1 | [Arquitetura](#arquitetura-e-orientação-a-objetos) — camadas com dependência para dentro, domínio sem Spring, hierarquia polimórfica, Strategy, State, objetos de valor, `Clock` injetado |
| Metodologia de trabalho | 0,1 | [Quadro de tarefas](https://github.com/users/Leocm123/projects/1) com issues por integrante, PRs ligados às issues e revisados por outro integrante — ver [Equipe e metodologia](#equipe-e-metodologia) |
| Testes automatizados | 0,1 | 205 testes de backend em 4 níveis, 67 de frontend e 4 cenários end-to-end — ver [O que é testado](#o-que-é-testado) |
| Cobertura de testes ≥ 70% | 0,1 | `./mvnw clean verify` reprova o build abaixo de 70%; cobertura atual de **99%** das linhas, relatório em `target/site/jacoco/index.html` e publicado como artefato em cada execução da CI |
| Documentação técnica | 0,1 | Este README (instalar, executar, testar, tecnologias, estrutura do banco) + [docs/arquitetura.md](docs/arquitetura.md) e [docs/modelagem-nosql.md](docs/modelagem-nosql.md) |
| GitHub e versionamento | 0,1 | Histórico em Conventional Commits, um PR por tarefa, CI com três jobs; versão final na tag [`v2.0-entrega2`](https://github.com/VictorSgobbi/aep-hidrovigia/releases/tag/v2.0-entrega2) |
| Vídeo de demonstração | 0,2 | Link em [Identificação](#identificação); roteiro em [docs/roteiro-video-entrega2.txt](docs/roteiro-video-entrega2.txt) |

### 1ª entrega

<details>
<summary>Critérios da 1ª entrega (tag <code>v1.0-entrega1</code>)</summary>

| Critério | Pts | Evidência |
|---|:---:|---|
| Problema e alinhamento ao ODS | 0,1 | [O problema](#o-problema) e [ODS 6](#ods-6) — público-alvo identificado e limites vindos da Portaria GM/MS 888/2021 |
| Primeira versão funcional da PoC | 0,1 | Fluxo principal executável, com carga de demonstração automática e contrato no Swagger UI |
| Banco de dados NoSQL | 0,1 | 3 coleções, relacionamento por identificador e subdocumentos aninhados em todas |
| Programação orientada a objetos e organização do código | 0,1 | Camadas, hierarquia polimórfica, Strategy, State, objetos de valor |
| GitHub e versionamento | 0,1 | Repositório público, histórico em Conventional Commits, PRs revisados entre a equipe |
| Testes automatizados | 0,1 | 188 testes de backend em 4 níveis e 59 de frontend |
| Cobertura de testes ≥ 70% | 0,1 | JaCoCo trava o build abaixo de 70%; cobertura de ~99% |
| Vídeo de demonstração | 0,3 | https://www.youtube.com/watch?v=ym-w1ppuR4E — roteiro em [docs/roteiro-video-entrega1.txt](docs/roteiro-video-entrega1.txt) |

</details>

---

## Próximos passos

Fora do escopo desta PoC, registrados como evolução possível:

- **Relatório de conformidade exportável** (CSV ou PDF) por ponto e período
  ([#17](https://github.com/VictorSgobbi/aep-hidrovigia/issues/17))
- **Índice de perdas** por macromedição versus micromedição, para a meta 6.4
- **Frequência de coleta pela população atendida**, como na tabela completa da portaria,
  em vez da simplificação por tipo de fonte
- **Índice TTL** para arquivar ocorrências resolvidas há mais de N anos

---

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

<div align="center">

---

**HidroVigia** · AEP 2026.2 · 6º Semestre · Engenharia de Software
Victor Sgobbi · Bruno · Leonardo

</div>
