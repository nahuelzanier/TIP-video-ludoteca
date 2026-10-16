import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'node',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: ['src/services/**/*.ts'],
      thresholds: {
        statements: 70,
        branches: 50,
        functions: 85,
        lines: 70,
      },
    },
  },
})
