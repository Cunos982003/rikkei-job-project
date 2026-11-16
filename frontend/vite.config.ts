import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = {
    ...loadEnv(mode, fileURLToPath(new URL('..', import.meta.url)), ''),
    ...loadEnv(mode, fileURLToPath(new URL('.', import.meta.url)), ''),
  }

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_BACKEND_URL || 'http://localhost:9001',
          changeOrigin: true
        },
        '/lms': {
          target: env.VITE_BACKEND_URL || 'http://localhost:9001',
          changeOrigin: true
        },
        '/staff': {
          target: env.VITE_BACKEND_URL || 'http://localhost:9001',
          changeOrigin: true
        },
        '/ai-career': {
          target: env.VITE_AI_CAREER_URL || 'http://localhost:8090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/ai-career(?=\/|$)/, '')
        }
      }
    }
  }
})
