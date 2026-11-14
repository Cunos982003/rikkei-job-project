import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

// Hardcode proxy targets để tránh phụ thuộc .env load
const proxyTarget = 'http://localhost:9001';
const careerProxyTarget = 'http://localhost:8090';

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
      '/lms': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/staff': {
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
