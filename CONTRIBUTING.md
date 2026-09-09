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
4. Abra o Pull Request e peça revisão de pelo menos um dos outros dois.
5. Merge na `main` só com CI verde.

### Nomes de branch

| Prefixo | Uso |
|---|---|
| `feat/` | Nova funcionalidade |
| `fix/` | Correção |
| `test/` | Só testes |
| `docs/` | Só documentação |

## Proteção da branch main

O item 5 acima é uma combinação entre nós — o GitHub só o **impõe** com proteção
de branch configurada. Sem ela, o botão de merge fica disponível mesmo com a CI
vermelha, e um `git push` direto na `main` passa sem revisão.

A configuração precisa ser feita por quem tem permissão de **admin** no
repositório (hoje, [@VictorSgobbi](https://github.com/VictorSgobbi)).

### Pelo site

`Settings` → `Branches` → `Add branch ruleset` (ou `Add rule`) na branch `main`:

| Opção | Valor |
|---|---|
| Require a pull request before merging | ✅ |
| Required approvals | 1 |
| Require status checks to pass before merging | ✅ |
| Status check obrigatório | **`Testes e cobertura`** |
| Require branches to be up to date before merging | ✅ |
| Do not allow bypassing the above settings | ✅ (vale também para admins) |

O nome do check é exatamente o `name:` do job em
[`.github/workflows/ci.yml`](.github/workflows/ci.yml). Se o job for renomeado, a
regra para de encontrar o check e **deixa de bloquear** — renomear job e regra
tem que ser feito junto.

### Pela linha de comando

Mesma configuração, para quem preferir o `gh`:

```bash
gh api -X PUT repos/VictorSgobbi/aep-hidrovigia/branches/main/protection \
  --input - <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Testes e cobertura"]
  },
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true
  },
  "enforce_admins": true,
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
```

Para conferir depois:

```bash
gh api repos/VictorSgobbi/aep-hidrovigia/branches/main/protection \
  --jq '{checks: .required_status_checks.contexts, revisoes: .required_pull_request_reviews.required_approving_review_count, admins: .enforce_admins.enabled}'
```

> ⚠️ **Depois de ligar isso, ninguém mais commita direto na `main`** — inclusive
> quem é admin, por causa do `enforce_admins`. Todo trabalho passa a entrar por
> branch + PR, como no fluxo acima. É o comportamento desejado, mas vale avisar
> o time antes de aplicar.

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

Para ver o que ficou descoberto:

```bash
./mvnw clean test
```

Depois abra `target/site/jacoco/index.html` e navegue até a classe.

## Uso de IA

Os commits iniciais deste repositório trazem o trailer
`Co-Authored-By: Claude Opus 5`. Isso é declaração honesta de que a base do
projeto foi escrita com assistência de IA. Não remova o trailer dos commits
existentes — apagar a atribuição transformaria uma divulgação correta em
representação falsa de autoria.

Se o professor pedir uma política diferente, conversem com ele antes de
reescrever o histórico.
