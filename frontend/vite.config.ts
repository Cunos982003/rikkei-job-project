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
      strictPort: true,
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
        // 2.1 Đánh giá Mentor & Tự đánh giá -> Mock Server (:9000)
        '^/api/v1/profiles/[^/]+/evaluations': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        '/api/v1/profiles/me/evaluations': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        '/api/v1/me/self-assessment': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        '/api/v1/me/self-assessments': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        '/api/v1/me/skill-comparison': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
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
          target: env.VITE_SKILLS_SERVICE_URL || env.VITE_PROFILE_SERVICE_URL || env.VITE_MOCK_API_URL || 'http://localhost:9000',
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
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/staff/sync': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/staff/sync-runs': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        '/api/v1/staff/external-records': {
          target: env.VITE_PROFILE_SERVICE_URL || 'http://localhost:8082',
          changeOrigin: true
        },
        // 4. Lịch phỏng vấn doanh nghiệp -> API Gateway (:8080) -> recruitment-service (:8086)
        '/api/v1/companies/me/interviews': {
          target: env.VITE_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true
        },
        // 5. Quản lý Doanh nghiệp & HR -> job-service (:8085)
        '/api/v1/staff/jobs': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        '/api/v1/staff/companies': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        '/api/v1/staff/hr-accounts': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        '/api/v1/companies': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        '^/api/v1/jobs(?!/[^/?]+/(?:recruit|apply))(/|\\?|$)': {
          target: env.VITE_JOB_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true
        },
        '/api/v1/candidate/saved-jobs': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        // 5.1. Phân hệ Đào tạo bổ sung LMS -> learning-service (:8089) hoặc API Gateway (:8080)
        '/api/v1/learning': {
          target: env.VITE_LEARNING_SERVICE_URL || env.VITE_GATEWAY_URL || 'http://localhost:8089',
          changeOrigin: true
        },
        '/api/v1/hr/interns': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        '/api/v1/staff/placements': {
          target: env.VITE_MOCK_API_URL || 'http://localhost:9000',
          changeOrigin: true
        },
        // 6. Tuyến chung /api/v1 (jobs, recruitment, interviews, learning...) -> API Gateway (:8080)
        '/api/v1': {
          target: env.VITE_GATEWAY_URL || 'http://localhost:8080',
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
        },
        '/me/conversations': {
          target: env.VITE_AI_CAREER_URL || env.VITE_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})