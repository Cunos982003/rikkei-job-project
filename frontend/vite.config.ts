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
        // 1. Mock LMS / WireMock (hệ thống bên ngoài)
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
        // 2. Phân luồng đăng nhập & xác thực -> Mock Server (:9000) hoặc Auth Service (:8081)
        // Set VITE_USE_MOCK_AUTH=true để dùng Mock Server, mặc định dùng Auth Service
        '/api/v1/auth': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_AUTH_SERVICE_URL || env.VITE_GATEWAY_URL || env.VITE_BACKEND_URL || 'http://localhost:8081'),
          changeOrigin: true
        },
        // 3. Phân luồng hồ sơ ứng viên & duyệt hồ sơ -> Mock Server (:9000) khi VITE_USE_MOCK_AUTH=true, fallback sang profile-service
        '/api/v1/profiles': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_PROFILE_SERVICE_URL || env.VITE_BACKEND_URL || 'http://localhost:8082'),
          changeOrigin: true
        },
        '/api/v1/staff/profile-reviews': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_PROFILE_SERVICE_URL || env.VITE_BACKEND_URL || 'http://localhost:8082'),
          changeOrigin: true
        },
        '/api/v1/staff/profiles': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_PROFILE_SERVICE_URL || env.VITE_BACKEND_URL || 'http://localhost:8082'),
          changeOrigin: true
        },
        // Danh mục kỹ năng chuẩn (DN-02) -> Mock Server khi VITE_USE_MOCK_AUTH=true, fallback sang profile-service
        '/api/v1/skills': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_PROFILE_SERVICE_URL || env.VITE_BACKEND_URL || 'http://localhost:8082'),
          changeOrigin: true
        },
        // 4. Phân luồng Quản lý Doanh nghiệp & HR -> Mock Server (:9000) khi VITE_USE_MOCK_AUTH=true, fallback sang job-service (:8085)
        '/api/v1/staff/jobs': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8085'),
          changeOrigin: true,
          rewrite: (path) => env.VITE_USE_MOCK_AUTH === 'true' ? path : path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/staff/companies': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8085'),
          changeOrigin: true,
          rewrite: (path) => env.VITE_USE_MOCK_AUTH === 'true' ? path : path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/staff/hr-accounts': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8085'),
          changeOrigin: true,
          rewrite: (path) => env.VITE_USE_MOCK_AUTH === 'true' ? path : path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/companies': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8085'),
          changeOrigin: true,
          rewrite: (path) => env.VITE_USE_MOCK_AUTH === 'true' ? path : path.replace(/^\/api\/v1/, '')
        },
        // Tin tuyển dụng (DN-02) -> job-service
        '/api/v1/jobs': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8085'),
          changeOrigin: true,
          rewrite: (path) => env.VITE_USE_MOCK_AUTH === 'true' ? path : path.replace(/^\/api\/v1/, '')
        },
        // 5. Tuyến chung /api/v1 (fallback) -> Mock Server (:9000) khi VITE_USE_MOCK_AUTH=true
        '/api/v1': {
          target: env.VITE_USE_MOCK_AUTH === 'true'
            ? (env.VITE_MOCK_API_URL || 'http://localhost:9000')
            : (env.VITE_GATEWAY_URL || env.VITE_BACKEND_URL || 'http://localhost:9000'),
          changeOrigin: true
        },
        // 6. Phân luồng xem ảnh logo tải lên -> job-service (:8085)
        '/uploads': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        // 7. AI Career
        '/ai-career': {
          target: env.VITE_AI_CAREER_URL || 'http://localhost:8090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/ai-career(?=\/|$)/, '')
        }
      }
    }
  }
})