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
  "tipoFonte": "POCO_ARTESIANO",              // define a frequência mínima de coleta
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

**Índices:** composto `{ pontoId: 1, coletadoEm: -1 }` e simples em `coletadoEm`.

O composto atende a consulta mais frequente — o histórico de um ponto, da coleta mais
recente para a mais antiga — sem ordenar em memória. A última coleta de um ponto, que o
alerta de coleta vencida busca para cada ponto ativo
(`findFirstByPontoIdOrderByColetadoEmDesc`), é o primeiro item desse índice. Pelo
prefixo, ele também serve as contagens por ponto (`countByPontoId...`), por isso
`pontoId` não tem índice próprio. `coletadoEm` mantém o simples porque a listagem por
período não filtra por ponto.

**Valor medido gravado como texto.** O Spring Data persiste `BigDecimal` como string
(`"valor": "14"`), o que preserva as casas decimais exatamente como foram medidas. O
custo aparece na agregação, que precisa converter antes de comparar — ver abaixo.

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

## Agregação — série histórica por parâmetro

A série histórica de um parâmetro num ponto é calculada pelo próprio MongoDB, em
`AnaliseRepositoryCustomImpl`. O pipeline é escrito em documentos BSON, na mesma forma
em que se escreveria no shell:

```js
db.analises.aggregate([
  { $match: { pontoId: "68b...",
              coletadoEm: { $gte: ISODate("2026-08-14"), $lte: ISODate("2026-11-12") } } },
  { $unwind: "$parametros" },
  { $match: { "parametros.codigo": "CRL" } },
  { $group: {
      _id: { $dateTrunc: { date: "$coletadoEm", unit: "day" } },
      minimo: { $min: { $toDecimal: "$parametros.valor" } },
      media:  { $avg: { $toDecimal: "$parametros.valor" } },
      maximo: { $max: { $toDecimal: "$parametros.valor" } },
      leituras: { $sum: 1 },
      foraDoLimite: { $sum: { $cond: ["$parametros.conforme", 0, 1] } }
  } },
  { $sort: { _id: 1 } }
])
```

| Estágio | Por quê |
|---|---|
| `$match` primeiro | Filtra por ponto e período **antes** de desdobrar o array, usando o índice composto |
| `$unwind` | Transforma o array `parametros` em um documento por leitura |
| `$match` pelo código | Só depois do `$unwind` dá para filtrar um parâmetro dentro do array |
| `$toDecimal` | O valor é gravado como texto; comparado como texto, `"10.5"` seria menor que `"9.0"` |
| `$dateTrunc` | Agrupa por dia (UTC), não por instante de coleta |
| `foraDoLimite` | Usa o veredito **gravado** em cada leitura (`parametros.conforme`), que aplicou o limite em vigor na data da coleta |

O teste de integração `RepositoriosMongoIntegracaoTest.agregaSerieDiaria` roda esse
pipeline contra um MongoDB 7 real (Testcontainers) e cobre justamente o caso de
`"10.5"` e `"9.0"` no mesmo dia.

## O que mudou na 2ª entrega

| Item | Situação |
|---|---|
| Índice composto `{ pontoId: 1, coletadoEm: -1 }` | ✅ Entregue — substituiu o índice simples de `pontoId` |
| Agregação para a série histórica | ✅ Entregue — `$group` por dia; o `$bucket` previsto não foi necessário, porque o gráfico pede faixas de tempo, não de valor |
| Frequência mínima de coleta por tipo de fonte | ✅ Entregue como atributo do enum `TipoFonte`, **não** como a coleção `frequencias_minimas` prevista — ver abaixo |
| Índice TTL em ocorrências resolvidas | ⏸ Fora do escopo — arquivar histórico de vigilância é decisão de política de retenção, não de PoC |

**Por que a frequência mínima ficou no enum, e não numa coleção.** A frequência é
uma regra da norma associada ao tipo de fonte, e não muda em tempo de execução. Numa
coleção, ela exigiria carga inicial, uma consulta a mais por cálculo e um caminho de
erro para "tipo de fonte sem frequência cadastrada" — que no enum é impossível, porque
o compilador obriga cada constante a declarar a sua. A coleção passaria a valer a pena
se a frequência fosse configurável por município, o que a PoC não faz.
