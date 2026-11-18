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
        // 1. Mock LMS / hệ thống bên ngoài (chỉ dùng cho cổng LMS & xác thực vé SSO)
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
        // 2. Đăng nhập & xác thực -> auth-service (:8081)
        '/api/v1/auth': {
          target: env.VITE_AUTH_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8081',
          changeOrigin: true
        },
        // 3. Hồ sơ ứng viên, danh mục ứng viên & kỹ năng -> profile-service (:8082)
        '/api/v1/profiles': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/candidates': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/skills': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/files': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/staff/profile-reviews': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/staff/profiles': {
<<<<<<< HEAD
          target: env.VITE_PROFILE_SERVICE_URL || env.VITE_BACKEND_URL || 'http://127.0.0.1:9000',
          changeOrigin: true
        },
        // Danh mục kỹ năng chuẩn (profile-service hoặc mock)
        '/api/v1/skills': {
          target: env.VITE_PROFILE_SERVICE_URL || env.VITE_BACKEND_URL || 'http://127.0.0.1:9000',
          changeOrigin: true
        },
        // 4. Phân luồng Quản lý Doanh nghiệp & HR -> job-service (:8085)
        '/api/v1/staff/jobs': {
          target: env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://127.0.0.1:8085',
          changeOrigin: true,
          rewrite: (path) => env.VITE_JOB_SERVICE_URL?.includes('9000') ? path : path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/staff/companies': {
          target: env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://127.0.0.1:8085',
=======
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        // 4. Lịch phỏng vấn doanh nghiệp -> API Gateway (:8080) -> recruitment-service (:8086)
        '/api/v1/companies/me/interviews': {
          target: env.VITE_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true
        },
        // 5. Quản lý Doanh nghiệp & HR -> job-service (:8085)
        '/api/v1/staff/companies': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
>>>>>>> 56fd545f13801af2a80e6e75cc9226b2f301b5e4
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/staff/hr-accounts': {
<<<<<<< HEAD
          target: env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://127.0.0.1:8085',
=======
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
>>>>>>> 56fd545f13801af2a80e6e75cc9226b2f301b5e4
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/companies': {
<<<<<<< HEAD
          target: env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://127.0.0.1:8085',
          changeOrigin: true,
          rewrite: (path) => env.VITE_JOB_SERVICE_URL?.includes('9000') ? path : path.replace(/^\/api\/v1/, '')
        },
        '/api/v1/jobs': {
          target: env.VITE_JOB_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://127.0.0.1:8085',
=======
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
>>>>>>> 56fd545f13801af2a80e6e75cc9226b2f301b5e4
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/v1/, '')
        },
        // 6. Tuyến chung /api/v1 (jobs, recruitment, interviews, learning...) -> API Gateway (:8080)
        '/api/v1': {
<<<<<<< HEAD
          target: env.VITE_GATEWAY_URL || env.VITE_BACKEND_URL || 'http://127.0.0.1:9000',
=======
          target: env.VITE_GATEWAY_URL || 'http://localhost:8080',
>>>>>>> 56fd545f13801af2a80e6e75cc9226b2f301b5e4
          changeOrigin: true
        },
        // 7. Xem ảnh logo tải lên -> job-service (:8085)
        '/uploads': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        // 8. AI Career -> API Gateway (:8080) -> ai_career_service (:8090)
        '/ai-career': {
          target: env.VITE_AI_CAREER_URL || env.VITE_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true
        },
        '/me/job-recommendations': {
          target: env.VITE_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})
