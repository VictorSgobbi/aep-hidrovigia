<div align="center">

# 💧 HidroVigia

**Vigilância da qualidade da água em sistemas de abastecimento de pequeno porte**

Prova de Conceito — AEP 2026.2 · 6º Semestre · Engenharia de Software

[![ODS 6](https://img.shields.io/badge/ODS%206-Água%20Potável%20e%20Saneamento-26BDE2?style=flat-square)](https://brasil.un.org/pt-br/sdgs/6)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square)](https://www.mongodb.com/)
[![Cobertura](https://img.shields.io/badge/cobertura%20mínima-70%25-blue?style=flat-square)](#-testes-e-cobertura)
[![Licença](https://img.shields.io/badge/licença-MIT-lightgrey?style=flat-square)](LICENSE)

</div>

---

## 📑 Sumário

1. [Identificação](#-identificação)
2. [O problema](#-o-problema)
3. [Objetivo de Desenvolvimento Sustentável](#-objetivo-de-desenvolvimento-sustentável)
4. [A solução](#-a-solução)
5. [Tecnologias](#-tecnologias)
6. [Como executar](#-como-executar)
7. [Testes e cobertura](#-testes-e-cobertura)
8. [Estrutura do banco NoSQL](#-estrutura-do-banco-nosql)
9. [Arquitetura e orientação a objetos](#-arquitetura-e-orientação-a-objetos)
10. [API](#-api)
11. [Estrutura do repositório](#-estrutura-do-repositório)
12. [Equipe e metodologia](#-equipe-e-metodologia)
13. [Entregas e versionamento](#-entregas-e-versionamento)
14. [Rastreabilidade dos critérios de avaliação](#-rastreabilidade-dos-critérios-de-avaliação)
15. [Evolução prevista](#-evolução-prevista)

---

## 🎓 Identificação

|  |  |
|---|---|
| **Curso** | Engenharia de Software |
| **Série** | 6º Semestre |
| **Atividade** | AEP — Atividade de Estudo Programada · 2026.2 |
| **Título da PoC** | HidroVigia — vigilância da qualidade da água em sistemas de abastecimento de pequeno porte |
| **ODS atendido** | ODS 6 — Água Potável e Saneamento |

### Acadêmicos

| RA | Nome | GitHub |
|---|---|---|
| *a preencher* | Victor Sgobbi | [@VictorSgobbi](https://github.com/VictorSgobbi) |
| *a preencher* | Bruno | [@BrundoCJ](https://github.com/BrundoCJ) |
| *a preencher* | Leonardo | [@Leocm123](https://github.com/Leocm123) |

### Links da entrega

| | |
|---|---|
| **Repositório** | https://github.com/VictorSgobbi/aep-hidrovigia |
| **Vídeo de demonstração (1ª entrega)** | *a preencher — YouTube, 2 a 3 minutos* |
| **Versão da 1ª entrega** | tag [`v1.0-entrega1`](https://github.com/VictorSgobbi/aep-hidrovigia/releases/tag/v1.0-entrega1) |

---

## 🚱 O problema

Municípios pequenos e sistemas comunitários — poços de escolas rurais, associações,
cisternas, distritos afastados — são obrigados a monitorar a potabilidade da água que
distribuem. Na prática, esse controle é feito em planilhas e cadernos de campo.

O efeito é sempre o mesmo: **uma análise fora do padrão demora dias para virar ação.**
O laudo chega, é arquivado, e a água continua sendo distribuída enquanto ninguém cruza
aquele resultado com o limite legal nem cobra uma providência de alguém com nome e prazo.

O intervalo entre *detectar* e *agir* é exatamente onde mora o risco sanitário — e é
esse intervalo que o HidroVigia fecha.

### Quem usa

| Público | Para quê |
|---|---|
| Operadores de sistemas de pequeno porte | Registrar coletas e acompanhar pendências |
| Vigilância sanitária municipal | Fiscalizar conformidade por ponto e por período |
| Associações comunitárias rurais | Comprovar que a água distribuída atende ao padrão |

---

## 🎯 Objetivo de Desenvolvimento Sustentável

<img src="https://img.shields.io/badge/6-ÁGUA%20POTÁVEL%20E%20SANEAMENTO-26BDE2?style=for-the-badge" alt="ODS 6" />

> **Garantir a disponibilidade e a gestão sustentável da água e saneamento para todos.**

| Meta | Como a PoC contribui |
|---|---|
| **6.1** — Acesso universal e equitativo a água potável segura | Torna verificável, ponto a ponto, se a água distribuída atende ao padrão legal |
| **6.3** — Melhorar a qualidade da água | Reduz o tempo entre detectar um desvio e agir sobre ele, com prazo por gravidade |
| **6.4** — Aumentar a eficiência no uso da água | Base de indicadores por ponto, ponto de partida para acompanhar perdas (2ª entrega) |

### Por que o alinhamento é objetivo, e não retórico

**As regras de negócio do sistema são a própria norma.** Os limites aplicados vêm da
**Portaria GM/MS nº 888/2021**, que alterou o Anexo XX da Portaria de Consolidação nº 5/2017
e define o padrão de potabilidade da água para consumo humano no Brasil.

Não há critério inventado pela equipe: o que reprova uma amostra aqui é o que reprova
uma amostra na vigilância sanitária.

| Código | Parâmetro | Limite aplicado | Tipo de limite | Risco |
|---|---|---|---|---|
| `ECOLI` | *Escherichia coli* | ausência | Ausência | 🔴 Microbiológico |
| `CTOT` | Coliformes totais | ausência | Ausência | 🔴 Microbiológico |
| `CRL` | Cloro residual livre | 0,2 – 2,0 mg/L | Faixa | 🟠 Desinfecção |
| `PH` | pH | 6,0 – 9,0 | Faixa | 🟡 Físico-químico |
| `TURB` | Turbidez | ≤ 5,0 uT | Máximo | 🟡 Físico-químico |
| `NITRATO` | Nitrato (como N) | ≤ 10,0 mg/L | Máximo | 🟡 Físico-químico |
| `FLUOR` | Fluoreto | ≤ 1,5 mg/L | Máximo | 🟡 Físico-químico |
| `COR` | Cor aparente | ≤ 15,0 uH | Máximo | 🔵 Organoléptico |

> ⚠️ **Verificar antes da apresentação.** Os valores em
> [`CatalogoParametros`](src/main/java/br/com/hidrovigia/dominio/parametro/CatalogoParametros.java)
> seguem o padrão brasileiro, mas **confira cada limite contra o texto oficial da portaria**
> antes de defender o trabalho. A PoC não substitui a norma. O mesmo critério vale para
> qualquer estatística que entrar nesta documentação: só entra com fonte e ano.

---

## 💡 A solução

### Fluxo principal

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Cadastrar ponto de monitoramento                            │
│     poço, cisterna, manancial, reservatório…                    │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Técnico registra a coleta com N parâmetros medidos          │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Sistema confronta cada valor com o limite da norma          │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
                    ┌────────┴────────┐
              conforme            reprovou
                    │                 │
                    ▼                 ▼
        ┌───────────────────┐  ┌──────────────────────────────────┐
        │ Análise gravada   │  │ 4. Classifica a gravidade        │
        │ como conforme     │  │ 5. Abre ocorrência com prazo     │
        └───────────────────┘  └──────────────┬───────────────────┘
                                              ▼
                               ┌──────────────────────────────────┐
                               │ 6. Vigilância registra tratativas│
                               │    até resolver a pendência      │
                               └──────────────┬───────────────────┘
                                              ▼
                               ┌──────────────────────────────────┐
                               │ 7. Painel: conformidade,         │
                               │    pendências e prazos vencidos  │
                               └──────────────────────────────────┘
```

### Gravidade e prazo de resposta

A política padrão classifica pelo **pior risco encontrado na amostra**:

| Gravidade | Prazo | Dispara quando |
|:---:|:---:|---|
| 🔴 **Crítica** | **24 h** | Detecção microbiológica (*E. coli* ou coliformes totais) |
| 🟠 **Alta** | **72 h** | Falha de desinfecção ou desvio físico-químico |
| 🟡 **Média** | **168 h** | Desvio apenas organoléptico |

Dez desvios de cor continuam sendo menos urgentes que uma única detecção de *E. coli* —
por isso a política olha o **risco**, não a contagem. Uma segunda política
(`ClassificadorPorQuantidade`) está implementada para demonstrar que essa decisão é um
ponto de extensão real, e a ocorrência grava **qual política a classificou**.

---

## 🛠 Tecnologias

| Camada | Tecnologia | Papel |
|---|---|---|
| Linguagem | **Java 21** | Programação orientada a objetos |
| Framework | **Spring Boot 3.3.5** | Injeção de dependências, REST, configuração |
| Banco de dados | **MongoDB 7** | **NoSQL orientado a documentos** |
| Persistência | Spring Data MongoDB | Repositórios e mapeamento objeto-documento |
| Validação | Jakarta Bean Validation | Contratos de entrada da API |
| Documentação da API | springdoc-openapi | Swagger UI interativo |
| Testes | JUnit 5 · Mockito · AssertJ | Unidade, aplicação e web |
| Testes de integração | Testcontainers + MongoDB 7 | Banco real e efêmero |
| Cobertura | **JaCoCo** | Relatório e trava de 70% no build |
| Integração contínua | GitHub Actions | Testes e cobertura a cada push |
| Infraestrutura local | Docker Compose | MongoDB e Mongo Express |

---

## 🚀 Como executar

### Pré-requisitos

| Ferramenta | Versão | Onde obter |
|---|---|---|
| JDK | 21 | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org/download.cgi) |
| Docker | com Compose v2 | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |

No Windows, os dois primeiros saem em um comando:

```bash
winget install EclipseAdoptium.Temurin.21.JDK Apache.Maven
```

### 1️⃣ Clone o repositório

```bash
git clone https://github.com/VictorSgobbi/aep-hidrovigia.git
```

```bash
cd aep-hidrovigia
```

### 2️⃣ Suba o MongoDB

```bash
docker compose up -d
```

Banco em `localhost:27017`, base `hidrovigia`, com volume nomeado — os dados sobrevivem
ao `docker compose down`.

### 3️⃣ Rode a aplicação

```bash
mvn spring-boot:run
```

Na primeira subida a base é populada com **3 pontos e 5 análises de demonstração**,
cobrindo os quatro desfechos possíveis (conforme, média, alta e crítica). Para desligar
a carga:

```bash
HIDROVIGIA_CARGA_DEMONSTRACAO=false mvn spring-boot:run
```

### 4️⃣ Abra a documentação interativa

### 👉 **http://localhost:8080/swagger-ui.html**

É por essa tela que a PoC é demonstrada — todo o fluxo roda ali, sem front-end próprio.

### 5️⃣ (Opcional) Inspecione as coleções

```bash
docker compose --profile ferramentas up -d
```

Mongo Express em **http://localhost:8081** — útil para mostrar os documentos aninhados
durante a gravação do vídeo.

### Parar tudo

```bash
docker compose --profile ferramentas down
```

---

## 🧪 Testes e cobertura

### Comando único e reproduzível

```bash
mvn clean verify
```

Este comando **compila, roda a suíte inteira, gera o relatório de cobertura e reprova o
build se a cobertura de linhas ficar abaixo de 70%**. É a evidência reproduzível que o
edital exige.

O relatório fica em `target/site/jacoco/index.html`:

```bash
start target/site/jacoco/index.html
```

Para gerar o relatório sem a trava de cobertura:

```bash
mvn clean test
```

### Como a trava está configurada

```xml
<rule>
  <element>BUNDLE</element>
  <limits>
    <limit>
      <counter>LINE</counter>
      <value>COVEREDRATIO</value>
      <minimum>0.70</minimum>
    </limit>
  </limits>
</rule>
```

Ficam fora da medição apenas a classe de bootstrap (`HidroVigiaApplication`) e o pacote
`config`, que contêm declaração de beans e carga de demonstração — nenhuma regra de negócio.

### O que é testado

| Nível | Ferramenta | Classes | O que cobre |
|---|---|:---:|---|
| **Domínio** | JUnit 5 + AssertJ | 5 | Limites da norma (conforme, no limite e violado por parâmetro), invariantes dos agregados, ciclo de vida da ocorrência |
| **Aplicação** | Mockito | 4 | Orquestração dos serviços com repositórios simulados |
| **Web** | MockMvc | 4 | Contratos HTTP (201/400/404/409/422) e formato do JSON |
| **Integração** | Testcontainers + MongoDB 7 | 1 | Round-trip dos subdocumentos e consultas por campo aninhado |

Os testes de integração são **ignorados automaticamente em máquina sem Docker**
(`@Testcontainers(disabledWithoutDocker = true)`), então o build continua verde. A
cobertura mínima é sustentada pelos testes de unidade, que não dependem de
infraestrutura nenhuma.

### Integração contínua

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda `mvn clean verify`
a cada push e pull request na `main`, e publica o relatório JaCoCo como artefato do
workflow. O runner do GitHub tem Docker, então os testes de integração também executam lá.

---

## 🗄 Estrutura do banco NoSQL

**Três coleções**, com **relacionamento por identificador** e **subdocumentos aninhados
em todas elas**.

| Coleção | Subdocumentos aninhados | Referencia | Índices |
|---|---|---|---|
| `pontos_monitoramento` | `localizacao`, `responsavel` | — | `codigo` (único) |
| `analises` | `parametros[]`, `resultado` | `pontoId` | `pontoId`, `coletadoEm` |
| `ocorrencias` | `parametrosViolados[]`, `tratativas[]` | `analiseId`, `pontoId` | `analiseId`, `pontoId` |

### Relacionamentos

```
pontos_monitoramento (1) ──────< (N) analises
          │                            │
          │                            │ 1 : 0..1
          └──────────< (N) ocorrencias ┘
```

Uma análise reprovada gera **exatamente uma** ocorrência. Uma análise conforme não gera
nenhuma.

### Exemplo de documento — `analises`

```json
{
  "_id": ObjectId("66f1a2b3c4d5e6f7a8b9c0d1"),
  "pontoId": "66f1a2b3c4d5e6f7a8b9c0aa",
  "pontoCodigo": "PMA-001",
  "coletadoEm": ISODate("2026-09-01T10:00:00Z"),
  "coletor": "Tecnico Bruno",
  "parametros": [
    {
      "codigo": "ECOLI",
      "nome": "Escherichia coli",
      "valor": "14",
      "unidade": "UFC/100mL",
      "limiteMinimo": null,
      "limiteMaximo": "0",
      "conforme": false,
      "mensagem": "Escherichia coli detectado na amostra: 14 UFC/100mL (exigida ausencia)",
      "risco": "MICROBIOLOGICO",
      "referenciaLegal": "Portaria GM/MS n. 888/2021"
    },
    {
      "codigo": "CRL",
      "nome": "Cloro residual livre",
      "valor": "0.05",
      "unidade": "mg/L",
      "limiteMinimo": "0.2",
      "limiteMaximo": "2.0",
      "conforme": false,
      "mensagem": "Cloro residual livre abaixo do minimo: 0.05 mg/L (esperado entre 0.2 e 2.0 mg/L)",
      "risco": "DESINFECCAO",
      "referenciaLegal": "Portaria GM/MS n. 888/2021"
    }
  ],
  "resultado": {
    "conforme": false,
    "qtdParametros": 5,
    "qtdNaoConformidades": 2
  },
  "registradoEm": ISODate("2026-09-01T11:30:00Z")
}
```

### Exemplo de documento — `ocorrencias`

```json
{
  "_id": ObjectId("66f1a2b3c4d5e6f7a8b9c0e2"),
  "analiseId": "66f1a2b3c4d5e6f7a8b9c0d1",
  "pontoId": "66f1a2b3c4d5e6f7a8b9c0aa",
  "pontoCodigo": "PMA-001",
  "gravidade": "CRITICA",
  "politicaClassificacao": "risco-sanitario",
  "status": "EM_TRATATIVA",
  "abertaEm": ISODate("2026-09-01T11:30:00Z"),
  "prazoLimite": ISODate("2026-09-02T11:30:00Z"),
  "parametrosViolados": [
    {
      "codigo": "ECOLI",
      "nome": "Escherichia coli",
      "valorMedido": "14",
      "unidade": "UFC/100mL",
      "limiteAplicado": "ausencia (UFC/100mL)",
      "risco": "MICROBIOLOGICO",
      "mensagem": "Escherichia coli detectado na amostra: 14 UFC/100mL (exigida ausencia)"
    }
  ],
  "tratativas": [
    {
      "acao": "Ponto isolado da rede de distribuicao",
      "por": "Leonardo",
      "em": ISODate("2026-09-01T13:10:00Z"),
      "statusResultante": "EM_TRATATIVA"
    }
  ]
}
```

### Duas decisões de modelagem que valem explicação

**1. O limite aplicado fica gravado dentro de cada parâmetro.**
Poderia ser lido do catálogo na hora de exibir, mas norma muda. Uma análise de 2026
precisa continuar mostrando o limite que valia em 2026, não o que passou a valer depois.
Sem isso, o histórico de vigilância deixa de ser auditável.

**2. `pontoCodigo` é repetido em `analises` e `ocorrencias`.**
Desnormalização deliberada. A fila de pendências mostra o código do ponto em cada linha,
e repetir 7 caracteres evita uma consulta extra por linha. O código do ponto é imutável,
então não há risco de divergência.

📄 Detalhamento completo em **[docs/modelagem-nosql.md](docs/modelagem-nosql.md)**.

---

## 🏗 Arquitetura e orientação a objetos

### Camadas

```
api/          Controllers REST, DTOs e tradução de erros para HTTP
   ↓
servico/      Casos de uso, orquestração e regras de aplicação
   ↓
repositorio/  Interfaces Spring Data MongoDB
dominio/      Entidades, objetos de valor e regras de negócio
config/       Beans de infraestrutura e carga de demonstração
```

A dependência aponta sempre para dentro. **O pacote `dominio` não depende do Spring** —
só usa `@Document`/`@Id` nas três raízes de agregado, que é mapeamento de persistência,
não acoplamento de framework. Por isso as regras de potabilidade rodam em teste de
unidade puro, sem subir contexto.

### Padrões aplicados, e o que cada um resolve

| Padrão | Onde | Problema que resolve |
|---|---|---|
| **Template Method** | `ParametroPotabilidade.avaliar()` (`final`) | Fixa o formato do veredito; subclasse decide só o limite |
| **Polimorfismo** | `ParametroFaixa` · `ParametroMaximo` · `ParametroAusencia` | Elimina o `switch` por código de parâmetro |
| **Strategy** | `ClassificadorGravidade` (2 implementações) | Trocar a política de urgência sem tocar no serviço |
| **State** | `StatusOcorrencia` | Transições válidas declaradas no enum; impede reabrir resolvida |
| **Objeto de Valor** | `ResultadoParametro`, `Localizacao`, `Tratativa`… | Não existe instância inválida em memória |
| **Agregado fechado** | `Analise` (sem setter) | Histórico imutável — corrigir é registrar outra análise |
| **Injeção de `Clock`** | `PainelConformidadeService` | Torna o cálculo de prazo vencido testável |

### A hierarquia que sustenta o domínio

```
                ParametroPotabilidade  «abstract»
                  + avaliar(valor) : ResultadoParametro   «final»
                  # dentroDoLimite(valor) : boolean       «abstract»
                  # descreverViolacao(valor) : String     «abstract»
                             ▲
        ┌────────────────────┼────────────────────┐
        │                    │                    │
 ParametroFaixa       ParametroMaximo      ParametroAusencia
 pH 6,0–9,0           turbidez ≤ 5,0 uT    E. coli: contagem 0
 cloro 0,2–2,0 mg/L   nitrato ≤ 10 mg/L    coliformes: contagem 0
```

Adicionar um parâmetro novo da norma é **adicionar uma linha no catálogo** — nenhuma
classe existente muda.

### Ciclo de vida da ocorrência

```
   ABERTA ─────────► EM_TRATATIVA ─────────► RESOLVIDA
      │                                          ▲
      └──────────────────────────────────────────┘
```

`RESOLVIDA` não tem saída. Não existe caminho no código para resolver duas vezes nem
para reabrir.

📄 Detalhamento completo em **[docs/arquitetura.md](docs/arquitetura.md)**.

---

## 🔌 API

### Endpoints

| Método | Rota | O que faz |
|:---:|---|---|
| `POST` | `/api/pontos` | Cadastra um ponto de monitoramento |
| `GET` | `/api/pontos` | Lista pontos — filtros `apenasAtivos` e `municipio` |
| `GET` | `/api/pontos/{id}` | Busca ponto pelo identificador |
| `GET` | `/api/pontos/codigo/{codigo}` | Busca ponto pelo código |
| `PUT` | `/api/pontos/{id}` | Atualiza dados cadastrais |
| `DELETE` | `/api/pontos/{id}` | Desativa sem apagar o histórico |
| `POST` | `/api/pontos/{id}/reativacao` | Reativa um ponto desativado |
| 🔷 `POST` | **`/api/analises`** | **Registra a coleta e abre ocorrência se reprovar** |
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

### Códigos de resposta

| Código | Significado | Exemplo |
|:---:|---|---|
| `201` | Criado | Ponto ou análise registrada |
| `400` | Requisição inválida | Campo obrigatório ausente |
| `404` | Não encontrado | Código de ponto inexistente |
| `409` | Regra de negócio violada | Ponto desativado, código duplicado, ocorrência já resolvida |
| `422` | Não processável | Parâmetro fora do catálogo da norma |

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

Retorna `201` com o veredito de **cada** parâmetro e abre uma ocorrência **crítica** com
prazo de 24 horas, imediatamente visível em:

```bash
curl http://localhost:8080/api/ocorrencias
```

---

## 📁 Estrutura do repositório

```
aep-hidrovigia/
├── .github/workflows/ci.yml          Testes e cobertura a cada push
├── docs/
│   ├── arquitetura.md                Camadas, padrões e estratégia de testes
│   └── modelagem-nosql.md            As três coleções, com justificativas
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
│   └── test/java/br/com/hidrovigia/  12 classes de teste + Fixtures
├── docker-compose.yml                MongoDB 7 + Mongo Express opcional
├── pom.xml                           Dependências e trava de cobertura
├── CONTRIBUTING.md                   Convenção de commits e fluxo de trabalho
├── LICENSE                           MIT
└── README.md
```

---

## 👥 Equipe e metodologia

| Integrante | GitHub | Trilha | Responsável por |
|---|---|---|---|
| **Victor Sgobbi** | [@VictorSgobbi](https://github.com/VictorSgobbi) | Domínio e regras | Entidades, objetos de valor, Strategies, testes de unidade do núcleo |
| **Bruno** | [@BrundoCJ](https://github.com/BrundoCJ) | Persistência e NoSQL | Modelagem das coleções, repositórios, seed, testes de integração |
| **Leonardo** | [@Leocm123](https://github.com/Leocm123) | API, qualidade e docs | Controllers, DTOs, Swagger, JaCoCo, CI, README, roteiro do vídeo |

Todos revisam os pull requests dos outros.

### Convenção de commits

O projeto segue [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
<tipo>(<escopo>): <descrição no imperativo>
```

| Tipo | Uso | Escopos |
|---|---|---|
| `feat` | Nova funcionalidade | `dominio` |
| `fix` | Correção de defeito | `persistencia` |
| `test` | Testes | `aplicacao` |
| `docs` | Documentação | `api` |
| `refactor` | Reestruturação sem mudar comportamento | `infra` |
| `build` | Dependências e empacotamento | `seed` |
| `ci` | Integração contínua | `readme` |
| `chore` | Manutenção | `build` · `repo` |

Exemplos reais do histórico:

```
feat(dominio): modela parametros de potabilidade com hierarquia polimorfica
test(dominio): cobre avaliacao de parametros de faixa, maximo e ausencia
feat(persistencia): adiciona repositorios das tres colecoes MongoDB
ci(github-actions): executa testes e publica relatorio de cobertura
```

📄 Regras completas em **[CONTRIBUTING.md](CONTRIBUTING.md)**.

---

## 🏷 Entregas e versionamento

| Marco | Tag | Conteúdo |
|---|---|---|
| **1ª entrega** | `v1.0-entrega1` | Fluxo principal funcional, três coleções com aninhamento, testes com cobertura ≥ 70%, vídeo de 2 a 3 min |
| **2ª entrega** | *a definir* | Evolução da solução, documentação técnica completa, quadro de tarefas, vídeo de 3 a 5 min |

A versão de cada entrega é identificável pela **tag** correspondente:

```bash
git checkout v1.0-entrega1
```

---

## ✅ Rastreabilidade dos critérios de avaliação

Onde encontrar a evidência de cada critério da 1ª entrega.

| Critério | Pts | Evidência neste repositório |
|---|:---:|---|
| Problema e alinhamento ao ODS | 0,1 | [O problema](#-o-problema) e [ODS](#-objetivo-de-desenvolvimento-sustentável) — limites vindos da Portaria GM/MS 888/2021, com público-alvo identificado |
| Primeira versão funcional da PoC | 0,1 | Fluxo principal executável via Swagger UI, com carga de demonstração automática |
| Banco de dados NoSQL | 0,1 | [Estrutura do banco](#-estrutura-do-banco-nosql) — 3 coleções, relacionamento e subdocumentos aninhados em todas |
| POO e organização do código | 0,1 | [Arquitetura](#-arquitetura-e-orientação-a-objetos) — Template Method, polimorfismo, Strategy, State, objetos de valor |
| GitHub e versionamento | 0,1 | Histórico em Conventional Commits, tag `v1.0-entrega1`, CI verde |
| Testes automatizados | 0,1 | 12 classes de teste em 4 níveis — `mvn clean verify` |
| **Cobertura ≥ 70%** | 0,1 | JaCoCo travando o build; relatório em `target/site/jacoco/index.html` |
| Vídeo de demonstração | 0,3 | Link em [Identificação](#-identificação) |

### Requisitos técnicos obrigatórios

| Requisito | Situação |
|---|:---:|
| Utilização efetiva de banco NoSQL | ✅ MongoDB 7, três coleções |
| Linguagem OO com aplicação efetiva do paradigma | ✅ Java 21, hierarquia polimórfica e padrões |
| Código versionado em repositório GitHub acessível | ✅ Público |
| Testes automatizados executáveis | ✅ `mvn clean verify` |
| Cobertura mínima de 70% com evidência reproduzível | ✅ JaCoCo com trava no build |
| Documentação técnica suficiente | ✅ README + `docs/` |
| PoC executável | ✅ Docker Compose + Spring Boot |

### Nível de complexidade do banco

O edital descreve dois níveis. Esta PoC já entrega o **mais alto**, que satisfaz o outro:

| Requisito | Nível 1 | Nível 2 | HidroVigia |
|---|:---:|:---:|:---:|
| Coleção única, objetos homogêneos, CRUD | ✅ | — | ✅ |
| Múltiplas coleções | — | ✅ | ✅ 3 coleções |
| Relacionamento entre coleções | — | ✅ | ✅ por identificador |
| Coleção com documentos aninhados ou listas de subdocumentos | — | ✅ | ✅ nas três |

---

## 🔭 Evolução prevista

Escopo mapeado para a 2ª entrega:

- **Série histórica por parâmetro**, com agregações do MongoDB (`$group`, `$bucket`)
- **Alerta de análise vencida**, comparando a última coleta com a frequência mínima
  exigida por tipo de fonte
- **Índice de perdas** por macromedição versus micromedição (meta 6.4)
- **Relatório de conformidade** exportável por ponto e período
- **Índice composto** `{ pontoId: 1, coletadoEm: -1 }` para a consulta mais frequente
- **Quadro de tarefas** no GitHub Projects como evidência de metodologia

---

## 📄 Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

<div align="center">

---

**HidroVigia** · AEP 2026.2 · 6º Semestre · Engenharia de Software
Victor Sgobbi · Bruno · Leonardo

</div>
