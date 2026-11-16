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
        // 1. Phân luồng đăng nhập & xác thực -> auth-service (:8081) hoặc Gateway
        '/api/v1/auth': {
          target: env.VITE_AUTH_SERVICE_URL || env.VITE_BACKEND_URL || 'http://localhost:8081',
          changeOrigin: true
        },
        // 2. Phân luồng quản lý công ty, tin tuyển dụng -> job-service (:8085) hoặc Gateway
        '/api/v1': {
          target: env.VITE_JOB_SERVICE_URL || env.VITE_BACKEND_URL || 'http://localhost:8085',
          changeOrigin: true,
          rewrite: (path) => env.VITE_BACKEND_URL ? path : path.replace(/^\/api\/v1/, '')
        },
        // 3. Phân luồng xem ảnh logo tải lên -> job-service (:8085)
        '/uploads': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        // 4. Mock LMS / WireMock
        '/lms': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              if (env.LMS_SERVICE_KEY) {
                proxyReq.setHeader('X-Service-Key', env.LMS_SERVICE_KEY)
              }
            })
          }
        },
        // 5. AI Career
        '/ai-career': {
          target: env.VITE_AI_CAREER_URL || 'http://localhost:8090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/ai-career(?=\/|$)/, '')
        }
      }
    }
  }
})
