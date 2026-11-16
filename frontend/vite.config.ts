import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

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
      // 1. Phân luồng đăng nhập & xác thực -> auth-service (:8081)
      '/api/v1/auth': {
        target: process.env.VITE_AUTH_SERVICE_URL || 'http://localhost:8081',
        changeOrigin: true
      },
      // 2. Phân luồng quản lý công ty, tin tuyển dụng -> job-service (:8085)
      '/api/v1': {
        target: process.env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/v1/, '')
      },
      // 3. Phân luồng xem ảnh logo tải lên -> job-service (:8085)
      '/uploads': {
        target: process.env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
        changeOrigin: true
      }
    }
  }
})
