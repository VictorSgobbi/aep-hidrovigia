# Como contribuir

Guia interno da equipe. O histórico de commits é avaliado na AEP, então vale
seguir o mesmo padrão desde o primeiro dia.

## Antes do primeiro commit

Cada integrante commita com a **própria conta do GitHub**. Configure uma vez:

```bash
git config user.name "Seu Nome"
```

```bash
git config user.email "email-da-sua-conta@github.com"
```

Use o e-mail cadastrado no GitHub — senão os commits não são atribuídos ao seu
perfil e a sua contribuição não aparece no gráfico do repositório.

## Convenção de commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/pt-br/).

```
<tipo>(<escopo>): <descrição no imperativo, minúscula, sem ponto final>

<corpo opcional explicando o porquê, não o quê>
```

### Tipos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de defeito |
| `test` | Adição ou ajuste de testes |
| `docs` | Documentação, README, comentários |
| `refactor` | Mudança de estrutura sem alterar comportamento |
| `build` | `pom.xml`, dependências, empacotamento |
| `ci` | GitHub Actions |
| `chore` | Tarefas de manutenção que não entram nas anteriores |
| `perf` | Melhoria de desempenho |

### Escopos do projeto

`dominio` · `persistencia` · `aplicacao` · `api` · `infra` · `seed` · `readme` · `build` · `repo`

### Exemplos reais deste repositório

```
feat(dominio): modela parametros de potabilidade com hierarquia polimorfica
test(dominio): cobre avaliacao de parametros de faixa, maximo e ausencia
feat(persistencia): adiciona repositorios das tres colecoes MongoDB
ci(github-actions): executa testes e publica relatorio de cobertura
docs(readme): documenta problema, ODS 6, execucao e cobertura
```

### Regras práticas

- **Um commit, um assunto.** Se a mensagem precisa de "e", provavelmente são dois
  commits.
- **Imperativo, não passado.** `adiciona`, não `adicionado`.
- **Sem acento na primeira linha.** Evita problema de encoding entre Windows e
  Linux no histórico.
- **Código e teste podem vir juntos** quando o teste é a prova da mudança; mas
  uma leva grande de testes merece commit próprio.
- **Nunca commite** com o build quebrado ou a cobertura abaixo de 70%.

## Fluxo de trabalho

1. Crie um branch a partir da `main`:
   ```bash
   git checkout -b feat/nome-curto-da-tarefa
   ```
2. Faça commits pequenos e frequentes.
3. Antes de abrir o PR, rode a verificação completa:
   ```bash
   ./mvnw clean verify
   ```
   Se você tocou em `frontend/`, rode também:
   ```bash
   cd frontend && npm run verificar
   ```
   E se você mexeu em algo que a interface exercita ponta a ponta — rota, formulário,
   fluxo de ocorrência — vale rodar os cenários end-to-end antes de o revisor descobrir:
   ```bash
   ./mvnw -Pfrontend clean package -DskipTests
   cd frontend && npm run e2e        # npm run e2e:ui para depurar passo a passo
   ```
   Eles precisam do MongoDB no ar (`docker compose up -d`) e, na primeira vez, do
   navegador: `npx playwright install chromium`.
   Se você só tocou em Java, `./mvnw clean verify` continua bastando — e continua
   **não exigindo Node**.
4. Abra o Pull Request e peça revisão de pelo menos um dos outros dois.
5. Merge na `main` só com CI verde.

### Nomes de branch

| Prefixo | Uso |
|---|---|
| `feat/` | Nova funcionalidade |
| `fix/` | Correção |
| `test/` | Só testes |
| `docs/` | Só documentação |

Para commits que mexem na interface, o escopo é `frontend`:
`feat(frontend): ...`, `build(frontend): ...`.

## Divisão de trabalho

| Integrante | Trilha | Responsável por |
|---|---|---|
| **Victor** | Domínio e regras | Entidades, objetos de valor, Strategies, testes de unidade do núcleo |
| **Bruno** | Persistência e NoSQL | Modelagem das coleções, repositories, seed, testes de integração |
| **Leonardo** | API, qualidade e docs | Controllers, DTOs, Swagger, JaCoCo, GitHub Actions, README, roteiro do vídeo |

Todos revisam os PRs dos outros. Isso já deixa o critério "metodologia de
trabalho / quadro de tarefas" da 2ª entrega evidenciado desde agora.

## Cobertura de testes

O `./mvnw verify` reprova o build abaixo de **70% de linhas**. Se o seu PR derrubou
a cobertura, o caminho não é baixar o mínimo no `pom.xml` — é escrever o teste
que faltou.

O frontend tem uma medição **separada**: o Vitest trava em 90% sobre `src/api` e
`src/dominio`, que é onde a lógica de falha silenciosa mora. A camada de apresentação
não é medida de propósito. Os dois números não se somam, e o JaCoCo não tem opinião
sobre TypeScript — não adicione `frontend/` aos `<excludes>` dele, seria um no-op que
documenta um mal-entendido.

### Os checks obrigatórios da CI

A CI tem três jobs, e o nome de cada um é o nome do status check:

| Job | Quando reprova |
|---|---|
| `Testes e cobertura` | teste de backend falhando, cobertura < 70%, ou teste de integração pulado |
| `Frontend (lint, tipos e testes)` | erro de tipo, lint, teste falhando, ou suíte pulada |
| `Interface no jar (empacotamento e e2e)` | a SPA não entrou no jar, ou um cenário do Playwright falhou |

Renomear qualquer um desses jobs derruba a regra de proteção correspondente — **e a CI
continua verde, o que é pior**. Renomear job e regra é a mesma tarefa.

⚠️ Hoje a proteção da `main` exige apenas `Testes e cobertura`. Os outros dois só passam
a bloquear quando forem acrescentados à regra, o que depende de permissão de admin no
repositório.

Para ver o que ficou descoberto:

```bash
./mvnw clean test
```

Depois abra `target/site/jacoco/index.html` e navegue até a classe.
