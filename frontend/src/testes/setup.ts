import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// O fuso e fixado em vite.config.ts (test.env.TZ), nao aqui: este arquivo e
// type-checado pelo tsconfig do app, que nao carrega os tipos do Node.

afterEach(() => {
  cleanup()
})
