import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * NT-08: Kịch bản kiểm tải k6 với 20 người dùng đồng thời (20 VUs)
 * Target: API tìm kiếm ứng viên (Candidate Directory Search API)
 * Dataset: 1.000 hồ sơ ứng viên (NT-16)
 */

export const options = {
    vus: 20,              // 20 người dùng đồng thời
    duration: '30s',      // Chạy liên tục trong 30 giây
    thresholds: {
        http_req_duration: ['p(95)<500'], // Tiêu chí AC: p95 < 500ms
        http_req_failed: ['rate<0.01'],    // Tỷ lệ lỗi < 1%
    },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

const KEYWORDS = [
    'Java',
    'Spring Boot',
    'React',
    'DevOps',
    'Docker',
    'PostgreSQL',
    'Fullstack',
    'Nguyễn',
    'Trần',
    'Lê',
    'Kỹ sư',
    'Backend',
    'Frontend',
    '' // query tất cả
];

export default function () {
    const randomKeyword = KEYWORDS[Math.floor(Math.random() * KEYWORDS.length)];
    const randomPage = Math.floor(Math.random() * 5); // page 0..4
    const pageSize = 20;

    const url = `${BASE_URL}/api/v1/candidates?keyword=${encodeURIComponent(randomKeyword)}&page=${randomPage}&size=${pageSize}`;

    const headers = {
        'Accept': 'application/json',
        'User-Agent': 'k6-load-testing-nt08'
    };

    const res = http.get(url, { headers: headers });

    check(res, {
        'Status is 200': (r) => r.status === 200,
        'Response time < 500ms': (r) => r.timings.duration < 500,
        'Has items array or data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data !== undefined || Array.isArray(body);
            } catch (e) {
                return false;
            }
        }
    });

    sleep(0.1); // Nghỉ 100ms giữa các request để giả lập hành vi người dùng thực tế
}
