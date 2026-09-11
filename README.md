<div align="center">

# 💧 HidroVigia

**Vigilância da qualidade da água em sistemas de abastecimento de pequeno porte**

Prova de Conceito — AEP 2026.2 · 6º Semestre · Engenharia de Software

[![ODS 6](https://img.shields.io/badge/ODS%206-Água%20Potável%20e%20Saneamento-26BDE2?style=flat-square)](https://brasil.un.org/pt-br/sdgs/6)
[![Cobertura](https://img.shields.io/badge/cobertura%20mínima-70%25-blue?style=flat-square)](#testes-e-cobertura)
[![Licença](https://img.shields.io/badge/licença-MIT-lightgrey?style=flat-square)](LICENSE)

</div>

---

## Sumário

1. [Identificação](#identificação)
2. [O problema](#o-problema)
3. [ODS 6](#ods-6)
4. [A solução](#a-solução)
5. [Tecnologias](#tecnologias)
6. [Como executar](#como-executar)
7. [Testes e cobertura](#testes-e-cobertura)
8. [Banco de dados NoSQL](#banco-de-dados-nosql)
9. [Arquitetura e orientação a objetos](#arquitetura-e-orientação-a-objetos)
10. [API](#api)
11. [Estrutura do repositório](#estrutura-do-repositório)
12. [Equipe](#equipe)
13. [Critérios da 1ª entrega](#critérios-da-1ª-entrega)
14. [Próximos passos](#próximos-passos)

---

## Identificação

|  |  |
|---|---|
| **Curso** | Engenharia de Software |
| **Série** | 6º Semestre |
| **Atividade** | AEP — Atividade de Estudo Programada · 2026.2 · 1ª entrega |
| **Título da PoC** | HidroVigia — vigilância da qualidade da água em sistemas de abastecimento de pequeno porte |
| **ODS atendido** | ODS 6 — Água Potável e Saneamento |

### Acadêmicos

| RA | Nome | GitHub |
|---|---|---|
| 24000732-2 | Victor Sgobbi | [@VictorSgobbi](https://github.com/VictorSgobbi) |
| 24021297-2 | Bruno | [@BrundoCJ](https://github.com/BrundoCJ) |
| 24015988-2 | Leonardo | [@Leocm123](https://github.com/Leocm123) |

### Links da entrega

| | |
|---|---|
| **Repositório** | https://github.com/VictorSgobbi/aep-hidrovigia |
| **Vídeo de demonstração** | https://www.youtube.com/watch?v=ym-w1ppuR4E |
| **Versão da 1ª entrega** | tag [`v1.0-entrega1`](https://github.com/VictorSgobbi/aep-hidrovigia/releases/tag/v1.0-entrega1) |

---

## O problema

Municípios pequenos e sistemas comunitários — poços de escolas rurais, associações,
cisternas, distritos afastados — são obrigados a monitorar a potabilidade da água que
distribuem. Na prática, isso é feito em planilha e caderno de campo: uma análise fora
do padrão pode levar dias para virar uma ação concreta, e nesse intervalo a água
continua sendo distribuída.

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
| **6.1** — Acesso universal e equitativo a água potável segura | Torna verificável, ponto a ponto, se a água distribuída atende ao padrão legal |
| **6.3** — Melhorar a qualidade da água | Reduz o tempo entre detectar um desvio e agir sobre ele, com prazo por gravidade |

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
> portaria antes de defender o trabalho.

---

## A solução

1. Cadastra-se um ponto de monitoramento (poço, cisterna, manancial, reservatório...).
2. O técnico registra a coleta com os parâmetros medidos.
3. O sistema confronta cada valor com o limite da norma.
4. Se está tudo conforme, a análise é só gravada. Se algo reprova, o sistema classifica
   a gravidade e abre uma ocorrência com prazo automaticamente.
5. A vigilância registra as tratativas até resolver a pendência.
6. O painel mostra conformidade, pendências e prazos vencidos em tempo real.

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

## Tecnologias

| Camada | Tecnologia | Papel |
|---|---|---|
| Interface | React 19 + Vite + TypeScript | Telas que consomem a API |
| Estado remoto | TanStack Query | Cache e invalidação cruzada entre telas |
| Linguagem | Java 21 | Programação orientada a objetos |
| Framework | Spring Boot 3.3.5 | Injeção de dependências, REST, configuração |
| Banco de dados | MongoDB 7 | NoSQL orientado a documentos |
| Persistência | Spring Data MongoDB | Repositórios e mapeamento objeto-documento |
| Validação | Jakarta Bean Validation | Contratos de entrada da API |
| Documentação da API | springdoc-openapi | Swagger UI interativo |
| Testes | JUnit 5 · Mockito · AssertJ | Unidade, aplicação e web |
| Testes de integração | Testcontainers + MongoDB 7 | Banco real e efêmero |
| Cobertura | JaCoCo | Relatório e trava de 70% no build |
| Integração contínua | GitHub Actions | Testes e cobertura a cada push |
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
de Java. Maven também não precisa ser instalado: o repositório traz o Maven Wrapper
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
ao `docker compose down`.

### 3. Rode a aplicação

```bash
./mvnw -Pfrontend spring-boot:run
```

Um comando serve a interface, a API e o Swagger na porta 8080. Sem o perfil
(`./mvnw spring-boot:run`) sobe só a API, o que já é suficiente para trabalhar no
backend.

Na primeira subida a base é populada com 3 pontos e 5 análises de demonstração,
cobrindo os quatro desfechos possíveis (conforme, média, alta e crítica). Para
desligar a carga:

```bash
HIDROVIGIA_CARGA_DEMONSTRACAO=false ./mvnw spring-boot:run
```

### 4. Abra a interface

**http://localhost:8080/**

Quatro telas: Painel de conformidade, Pontos de monitoramento, Coletas e a fila de
Ocorrências.

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

Compila, roda a suíte inteira, gera o relatório de cobertura e reprova o build se a
cobertura de linhas ficar abaixo de 70%.

O relatório fica em `target/site/jacoco/index.html`:

```bash
start target/site/jacoco/index.html
```

Para gerar o relatório sem a trava de cobertura, `./mvnw clean test`.

Ficam fora da medição apenas a classe de bootstrap (`HidroVigiaApplication`) e o
pacote `config`, que contêm declaração de beans e carga de demonstração — nenhuma
regra de negócio.

### O que é testado

**188 testes de backend** em 18 classes e **59 de frontend** em 10 arquivos. Os dois
números medem coisas diferentes e não se somam.

| Nível | Ferramenta | O que cobre |
|---|---|---|
| Domínio | JUnit 5 + AssertJ | Limites da norma (conforme, no limite e violado por parâmetro), invariantes dos agregados, ciclo de vida da ocorrência |
| Aplicação | Mockito | Orquestração dos serviços com repositórios simulados |
| Web | MockMvc | Contratos HTTP (201/400/404/409/422), header `Location` e formato do JSON |
| Integração | Testcontainers + MongoDB 7 | Round-trip dos subdocumentos, consultas por campo aninhado e os índices únicos |

Nenhuma data de teste vem de `Instant.now()`: todas derivam de `Fixtures.REFERENCIA`,
um instante fixo, e quem precisa de "agora" recebe um `Clock` parado — evita o clássico
teste que falha uma vez a cada dez por depender do relógio da máquina.

Os testes de integração se desabilitam sozinhos em máquina sem Docker
(`@Testcontainers(disabledWithoutDocker = true)`), o que é conveniente localmente mas
arriscado em CI — por isso o workflow lê o relatório do Surefire e reprova o build se
essa classe tiver sido pulada.

Frontend:

```bash
cd frontend && npm run verificar    # tipos + lint + testes com cobertura
```

O esforço de teste do frontend está concentrado onde a falha é silenciosa: normalização
dos dois formatos de erro da API, distribuição das mensagens de validação pelos campos,
conversões de data e decimal, e a prévia de conformidade. Layout não é testado por
unidade, de propósito — isso apareceria na primeira vez que alguém abrisse a tela.

### Cobertura atual

| Medição | Ferramenta | Escopo | Mínimo travado | Atual |
|---|---|---|:---:|:---:|
| Backend | JaCoCo (linhas) | `src/main/java`, menos bootstrap e `config/` | 70% | ~99% |
| Lógica do frontend | Vitest (v8) | `frontend/src/api` e `frontend/src/dominio` | 90% | 100% |

### Integração contínua

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda a suíte de
backend e reprova o build se a cobertura ficar abaixo de 70% ou se o teste de
integração tiver sido pulado.

---

## Banco de dados NoSQL

Três coleções, com relacionamento por identificador e subdocumentos aninhados em
todas elas.

| Coleção | Subdocumentos aninhados | Referencia | Índices |
|---|---|---|---|
| `pontos_monitoramento` | `localizacao`, `responsavel` | — | `codigo` (único) |
| `analises` | `parametros[]`, `resultado` | `pontoId` | `pontoId`, `coletadoEm` |
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

### Duas decisões de modelagem

**O limite aplicado fica gravado dentro de cada parâmetro.** Poderia ser lido do
catálogo na hora de exibir, mas a norma muda: uma análise de 2026 precisa continuar
mostrando o limite que valia em 2026.

**`pontoCodigo` é repetido em `analises` e `ocorrencias`.** Desnormalização
deliberada — a fila de pendências mostra o código do ponto em cada linha, e repetir 7
caracteres evita uma consulta extra por linha. O código do ponto é imutável, então não
há risco de divergência.

Detalhamento completo em [docs/modelagem-nosql.md](docs/modelagem-nosql.md).

---

## Arquitetura e orientação a objetos

```
api/          Controllers REST, DTOs e tradução de erros para HTTP
   ↓
servico/      Casos de uso, orquestração e regras de aplicação
   ↓
repositorio/  Interfaces Spring Data MongoDB
dominio/      Entidades, objetos de valor e regras de negócio
config/       Beans de infraestrutura e carga de demonstração
```

A dependência aponta sempre para dentro. O pacote `dominio` não depende do Spring — só
usa `@Document`/`@Id` nas três raízes de agregado, que é mapeamento de persistência,
não acoplamento de framework. Por isso as regras de potabilidade rodam em teste de
unidade puro, sem subir contexto.

| Padrão | Onde | Problema que resolve |
|---|---|---|
| Template Method | `ParametroPotabilidade.avaliar()` (`final`) | Fixa o formato do veredito; subclasse decide só o limite |
| Polimorfismo | `ParametroFaixa` · `ParametroMaximo` · `ParametroAusencia` | Elimina o `switch` por código de parâmetro |
| Strategy | `ClassificadorGravidade` (2 implementações) | Trocar a política de urgência sem tocar no serviço |
| State | `StatusOcorrencia` | Transições válidas declaradas no enum; impede reabrir resolvida |
| Objeto de Valor | `ResultadoParametro`, `Localizacao`, `Tratativa`... | Não existe instância inválida em memória |
| Agregado fechado | `Analise` (sem setter) | Histórico imutável — corrigir é registrar outra análise |
| Injeção de `Clock` | `PainelConformidadeService` | Torna o cálculo de prazo vencido testável |

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

---

## Estrutura do repositório

```
aep-hidrovigia/
├── .github/workflows/ci.yml          Testes e cobertura a cada push
├── docs/
│   ├── arquitetura.md                Camadas, padrões e estratégia de testes
│   ├── modelagem-nosql.md            As três coleções, com justificativas
│   └── roteiro-video-entrega1.txt    Roteiro do vídeo de demonstração
├── src/
│   ├── main/
│   │   ├── java/br/com/hidrovigia/
│   │   │   ├── api/                  Controllers, DTOs, tratador de erros
│   │   │   ├── config/               Beans de domínio, Swagger, carga demo
│   │   │   ├── dominio/
│   │   │   │   ├── analise/          Analise, LeituraParametro, ResultadoAnalise
│   │   │   │   ├── gravidade/        Gravidade e as duas Strategies
│   │   │   │   ├── ocorrencia/       Ocorrencia, StatusOcorrencia, Tratativa
│   │   │   │   ├── parametro/        Hierarquia de parâmetros e catálogo
│   │   │   │   └── ponto/            PontoMonitoramento, Localizacao, Responsavel
│   │   │   ├── repositorio/          3 repositórios MongoDB
│   │   │   └── servico/              4 serviços de aplicação
│   │   └── resources/application.yml
│   └── test/java/br/com/hidrovigia/  18 classes de teste + Fixtures
├── frontend/                         Interface React + Vite + TypeScript
│   └── src/
│       ├── api/                      Cliente tipado e normalizacao de erro
│       ├── componentes/              Selos, campos, prazo, estados
│       ├── dominio/                  Rotulos, prazos, limites, formatos
│       └── paginas/                  painel · pontos · coletas · ocorrencias
├── docker-compose.yml                MongoDB 7 + Mongo Express opcional
├── mvnw · mvnw.cmd · .mvn/           Maven Wrapper — dispensa instalar Maven
├── pom.xml                           Dependências e trava de cobertura
├── CONTRIBUTING.md                   Convenção de commits e fluxo de trabalho
├── LICENSE                           MIT
└── README.md
```

---

## Equipe

| Integrante | GitHub | Responsável por |
|---|---|---|
| Victor Sgobbi | [@VictorSgobbi](https://github.com/VictorSgobbi) | Entidades, objetos de valor, Strategies, testes de unidade do núcleo |
| Bruno | [@BrundoCJ](https://github.com/BrundoCJ) | Modelagem das coleções, repositórios, seed, testes de integração |
| Leonardo | [@Leocm123](https://github.com/Leocm123) | Controllers, DTOs, Swagger, JaCoCo, CI, README, roteiro do vídeo |

Todos revisam os pull requests uns dos outros. Convenção de commits e fluxo de
trabalho completos em [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Critérios da 1ª entrega

Como o repositório atende a cada critério da correção.

| Critério | Pts | Evidência | Situação |
|---|:---:|---|:---:|
| Problema e alinhamento ao ODS | 0,1 | [O problema](#o-problema) e [ODS 6](#ods-6) — público-alvo identificado e limites vindos da Portaria GM/MS 888/2021 | ✅ |
| Primeira versão funcional da PoC | 0,1 | Fluxo principal executável pela interface em `localhost:8080`, com carga de demonstração automática; contrato inspecionável no Swagger UI | ✅ |
| Banco de dados NoSQL | 0,1 | [Banco de dados NoSQL](#banco-de-dados-nosql) — 3 coleções, relacionamento por identificador e subdocumentos aninhados em todas | ✅ |
| Programação orientada a objetos e organização do código | 0,1 | [Arquitetura](#arquitetura-e-orientação-a-objetos) — camadas, hierarquia polimórfica, Strategy, State, objetos de valor | ✅ |
| GitHub e versionamento | 0,1 | Repositório público, histórico em Conventional Commits, PRs revisados entre a equipe | ✅ |
| Testes automatizados | 0,1 | 188 testes de backend em 4 níveis (`./mvnw clean verify`) e 59 de frontend (`npm run verificar`) | ✅ |
| Cobertura de testes ≥ 70% | 0,1 | JaCoCo trava o build abaixo de 70%; cobertura atual ~99% — relatório em `target/site/jacoco/index.html` | ✅ |
| Vídeo de demonstração | 0,3 | Publicado no YouTube, link na seção [Identificação](#identificação); roteiro em [docs/roteiro-video-entrega1.txt](docs/roteiro-video-entrega1.txt) | ✅ |

Os oito critérios estão cobertos e verificáveis com os comandos listados neste README.

---

## Próximos passos

Itens de backend que ficam para depois desta entrega, sem compromisso de prazo ainda:

- Série histórica por parâmetro, com agregações do MongoDB (`$group`, `$bucket`)
- Alerta de análise vencida, comparando a última coleta com a frequência mínima
  exigida por tipo de fonte
- Índice de perdas por macromedição versus micromedição
- Relatório de conformidade exportável por ponto e período
- Índice composto `{ pontoId: 1, coletadoEm: -1 }` para a consulta mais frequente

---

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

<div align="center">

---

**HidroVigia** · AEP 2026.2 · 6º Semestre · Engenharia de Software
Victor Sgobbi · Bruno · Leonardo

</div>
