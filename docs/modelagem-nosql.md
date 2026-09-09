# Modelagem NoSQL

Banco: **MongoDB 7**. Três coleções, relacionamento por identificador e
subdocumentos aninhados em todas elas.

## Por que três coleções e não uma

O edital pede, no nível mais alto de complexidade, múltiplas coleções com
relacionamento entre elas e ao menos uma contendo objetos complexos. A divisão
aqui não é artificial — ela segue o ciclo de vida real de cada informação:

| Coleção | Muda com que frequência | Por que é separada |
|---|---|---|
| `pontos_monitoramento` | Raramente (cadastro) | Um ponto vive anos e é referenciado por centenas de análises |
| `analises` | A cada coleta | Cresce sem parar; imutável depois de gravada |
| `ocorrencias` | A cada tratativa | Tem ciclo de vida próprio e é consultada por uma fila de trabalho diferente |

Embutir análises dentro do ponto faria o documento crescer indefinidamente e
esbarraria no limite de 16 MB por documento do MongoDB.

## `pontos_monitoramento`

```json
{
  "_id": ObjectId("..."),
  "codigo": "PMA-001",
  "nome": "Poco da Escola Rural Sao Jose",
  "tipoFonte": "POCO_ARTESIANO",
  "populacaoAtendida": 320,
  "localizacao": {
    "municipio": "Maringa",
    "uf": "PR",
    "latitude": -23.4205,
    "longitude": -51.9331
  },
  "responsavel": {
    "nome": "Ana Souza",
    "registro": "CREA-PR 123456",
    "contato": "ana.souza@exemplo.gov.br"
  },
  "ativo": true,
  "criadoEm": ISODate("2026-08-09T12:00:00Z")
}
```

**Aninhamento:** `localizacao` e `responsavel` são subdocumentos. Ficam embutidos
porque não existe consulta que precise deles sem o ponto.

**Índices:** `codigo` é único.

**Consulta que usa o aninhamento:**
`findByLocalizacaoMunicipioIgnoreCase` → `{ "localizacao.municipio": /^maringa$/i }`

## `analises`

```json
{
  "_id": ObjectId("..."),
  "pontoId": "68b...",
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

**Aninhamento:** `parametros` é uma lista de subdocumentos e `resultado` é um
subdocumento consolidado.

**Relacionamento:** `pontoId` referencia `pontos_monitoramento._id`.

### Duas decisões que valem explicação

**1. O limite fica gravado dentro de cada parâmetro.**
Poderia ser lido do catálogo na hora da exibição, mas norma muda. Uma análise de
2026 precisa continuar mostrando o limite que valia em 2026, e não o que passou a
valer depois. Sem isso, o histórico de vigilância deixa de ser auditável.

**2. `pontoCodigo` é repetido aqui.**
Desnormalização deliberada. A tela de ocorrências precisa mostrar o código do
ponto, e repetir 7 caracteres evita uma consulta extra a cada linha da lista.
O código do ponto é imutável, então não há risco de divergência.

**Índices:** `pontoId` e `coletadoEm`.

## `ocorrencias`

```json
{
  "_id": ObjectId("..."),
  "analiseId": "68c...",
  "pontoId": "68b...",
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

**Aninhamento:** dois arrays de subdocumentos no mesmo documento —
`parametrosViolados`, congelado na abertura, e `tratativas`, que cresce conforme
a vigilância age.

**Relacionamento:** `analiseId` e `pontoId`.

**Índices:** `analiseId` (único) e `pontoId`. O índice único é o que garante a
cardinalidade 1:0..1 com `analises` — ver abaixo.

## Mapa dos relacionamentos

```
pontos_monitoramento (1) ──< (N) analises
         │                        │
         │                        │ 1
         └────────< (N) ocorrencias
                              1
```

Uma análise reprovada gera exatamente uma ocorrência. Uma análise conforme não
gera nenhuma.

Essa regra é garantida por **índice único em `analiseId`**, e não apenas por
convenção: `OcorrenciaRepository.findByAnaliseId` devolve `Optional`, então duas
ocorrências para a mesma análise fariam a consulta falhar. O serviço checa antes
e responde `409`; o índice cobre o caso de duas requisições concorrentes.

## Evolução prevista para a 2ª entrega

- Agregações do MongoDB (`$group`, `$bucket`) para a série histórica por parâmetro
- Índice composto `{ pontoId: 1, coletadoEm: -1 }` para a consulta mais frequente
- Índice TTL opcional em ocorrências resolvidas há mais de N anos
- Coleção `frequencias_minimas` com a periodicidade de coleta exigida por tipo de
  fonte, para alertar análise vencida
