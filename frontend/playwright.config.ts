import { defineConfig, devices } from '@playwright/test'

/**
 * Cenários end-to-end contra o sistema real.
 *
 * O alvo é o **jar empacotado**, e não o dev server: é ali que a interface e a
 * API ficam na mesma origem, servidas pelo mesmo processo, que é exatamente
 * como a demonstração é gravada. Nenhum outro teste do projeto cobre isso — os
 * do Vitest usam MSW, e os de backend não sabem que existe interface.
 *
 * Pré-requisitos, nesta ordem:
 *   docker compose up -d
 *   ./mvnw -Pfrontend clean package -DskipTests
 *   npm run e2e
 *
 * O jar é iniciado pelo próprio Playwright (`webServer` abaixo).
 */
export default defineConfig({
  testDir: './e2e',

  // Os cenários mexem no banco (registram coleta, encerram ocorrência), então
  // não podem rodar em paralelo entre si.
  fullyParallel: false,
  workers: 1,

  // Nenhuma tolerância a `.only` esquecido na CI.
  forbidOnly: Boolean(process.env.CI),
  // Uma retentativa na CI: o alvo é um sistema com banco, e a alternativa a
  // isto é uma falha ocasional que ninguém consegue reproduzir.
  retries: process.env.CI ? 1 : 0,

  reporter: process.env.CI
    ? [['list'], ['json', { outputFile: 'playwright-report.json' }]]
    : [['list']],

  use: {
    baseURL: 'http://localhost:8080',
    // Rastro só da tentativa que falhou: é o que se abre para entender.
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: {
    command: 'java -jar ../target/hidrovigia-1.0.0.jar',
    // Espera a API responder, e não só a porta abrir: o Spring leva alguns
    // segundos entre ligar o socket e conseguir consultar o Mongo.
    url: 'http://localhost:8080/api/painel/conformidade',
    timeout: 120_000,
    // Localmente reaproveita o que já estiver rodando; na CI sempre sobe do
    // zero, para o teste não depender de estado de máquina.
    reuseExistingServer: !process.env.CI,
    // O log de inicializacao do Spring nao interessa quando passa; o erro,
    // sim — sem stderr um jar que nao sobe falharia sem dizer por que.
    stdout: 'ignore',
    stderr: 'pipe',
  },
})
