import react from '@vitejs/plugin-react'
// defineConfig vem de vitest/config (e nao de vite) para que a chave `test`
// abaixo seja tipada.
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    // 8080 e a API, 8081 e o mongo-express, 27017 e o Mongo.
    port: 5173,
    proxy: {
      // Mesma origem em desenvolvimento: o navegador so conhece :5173, entao
      // nao existe CORS para configurar (o backend nao tem nenhum). O front
      // usa caminhos relativos ("/api/pontos") tanto aqui quanto dentro do
      // jar, onde o proprio Spring serve os arquivos estaticos.
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      // O Swagger continua acessivel a partir do dev server.
      '/v3/api-docs': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },

  // O Maven copia daqui para target/classes/static no perfil -Pfrontend.
  // Nunca gerar direto em src/main/resources/static: o `clean` nao limparia e
  // um bundle velho continuaria sendo servido.
  build: { outDir: 'dist', emptyOutDir: true },

  test: {
    environment: 'jsdom',
    // Restrito a src/: os cenarios do Playwright ficam em e2e/ e sao rodados
    // por ele, com um browser de verdade. Sem isto o Vitest tentaria executa-los.
    include: ['src/**/*.test.{ts,tsx}'],
    setupFiles: ['./src/testes/setup.ts'],
    // Fuso fixo. paraInstante() converte o valor de um
    // <input type="datetime-local"> (hora local, sem offset) para Instant UTC.
    // Sem fixar, o teste passa no notebook em UTC-3 e falha no runner em UTC.
    // Aqui, e nao no script do npm, porque `TZ=x vitest` nao funciona no cmd
    // do Windows.
    env: { TZ: 'America/Sao_Paulo' },
    // Suite que nao roda e pior que suite que falha: o build fica verde.
    passWithNoTests: false,
    // `.only` esquecido deixaria o resto da suite sem rodar.
    allowOnly: false,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      // Medido: so a logica cuja falha e silenciosa.
      //
      // Fora de proposito: tipos.ts (nao tem codigo em runtime), recursos.ts
      // (18 wrappers de uma linha sobre requisitar), chaves.ts (dados) e
      // queryClient.ts (configuracao). Exigir 90% deles seria comprar
      // asseracoes de que uma linha continua sendo uma linha; a garantia real
      // deles e o tsc.
      include: ['src/api/erros.ts', 'src/api/cliente.ts', 'src/dominio/**/*.ts'],
      exclude: ['src/**/*.test.{ts,tsx}'],
      // Sem minimo global: a camada de apresentacao nao e medida, e isso e
      // proposital — ela e verificada abrindo a tela. O que sobra aqui e
      // funcao pura sem I/O, onde 90% e mais facil de sustentar que 70%.
      thresholds: {
        lines: 90,
        functions: 90,
        branches: 85,
      },
    },
  },
})
