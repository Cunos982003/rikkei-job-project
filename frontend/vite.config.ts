import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

const proxyTarget = process.env.VITE_BACKEND_URL || 'http://localhost:8080';
const careerProxyTarget = process.env.VITE_AI_CAREER_URL || 'http://localhost:8090';

// https://vite.dev/config/
export default defineConfig({
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
        target: proxyTarget,
        changeOrigin: true
      },
      '/ai-career': {
        target: careerProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai-career(?=\/|$)/, '')
      }
    }
  }
})
