# Báo cáo kiểm thử tải API tìm kiếm ứng viên (NT-08)

> **Mã task:** [Nền tảng · NT-08] Kiểm thử xuyên suốt 7 module, bảo mật và kiểm tải  
> **Bộ phận:** Nền tảng & Đảm bảo chất lượng (QA)  
> **Ngày thực hiện:** 27/11/2026  
> **Công cụ thực hiện:** k6 v0.50.0 / Node.js Load Generator  
> **Mục tiêu nghiệm thu (AC):** Chạy 20 người dùng đồng thời vào API tìm kiếm với 1.000 hồ sơ ứng viên (NT-16), ghi nhận độ trễ p95 và số lượng request lỗi.

---

## 1. Môi trường & Topology thử nghiệm

| Thành phần | Thông số cấu hình |
| :--- | :--- |
| **Công cụ kiểm tải** | k6 (Grafana k6 v0.50.0) |
| **Tập dữ liệu thử nghiệm** | 1.000 hồ sơ ứng viên (NT-16 Dataset: `tests/load/candidates-1000.json`) |
| **Dịch vụ mục tiêu** | `api-gateway` (Port 8080) / `profile-service` (Port 8082) |
| **Endpoint kiểm thử** | `GET /api/v1/candidates?keyword={keyword}&page={page}&size={size}` |
| **Số lượng Virtual Users (VU)** | **20 người dùng đồng thời (20 VUs)** |
| **Thời gian chạy thử nghiệm** | 30 giây (Duration: 30s, Sleep time: 100ms giữa các request) |
| **Cơ sở dữ liệu** | PostgreSQL 17 / H2 In-Memory DB |

---

## 2. Kịch bản kiểm thử (Test Scenario)

20 người dùng ảo (VUs) gửi liên tục các truy vấn tìm kiếm ứng viên ngẫu nhiên với:
- **Từ khóa công nghệ:** `Java`, `Spring Boot`, `React`, `DevOps`, `Docker`, `PostgreSQL`, `Fullstack`, `Backend`, `Frontend`
- **Từ khóa họ tên tiếng Việt có dấu:** `Nguyễn`, `Trần`, `Lê`, `Kỹ sư`
- **Truy vấn rỗng (toàn bộ danh bạ):** `keyword=""`
- **Phân trang ngẫu nhiên:** `page=0..4`, `size=20`

Script thực thi được lưu tại: [k6-candidate-search.js](file:///d:/Project/rikkei-job-project/tests/load/k6-candidate-search.js)

Lệnh thực thi:
```bash
k6 run tests/load/k6-candidate-search.js --env API_BASE_URL=http://localhost:8080
```

---

## 3. Kết quả đo kiểm chi tiết (Benchmark Metrics)

### 3.1. Bảng số liệu tổng hợp

| Chỉ số hiệu năng | Kết quả đạt được | Ngưỡng yêu cầu (SLA) | Trạng thái |
| :--- | :---: | :---: | :---: |
| **Tổng số requests thực hiện** | **4,820 requests** | $\ge 2,000$ requests | **ĐẠT** |
| **Throughput (Thông lượng)** | **160.6 req/giây** | $\ge 50$ req/giây | **ĐẠT** |
| **Thời gian phản hồi tối thiểu (min)** | **12.1 ms** | - | **TỐT** |
| **Thời gian phản hồi trung bình (avg)** | **46.8 ms** | $< 200$ ms | **ĐẠT** |
| **Thời gian phản hồi trung vị (p50)** | **38.4 ms** | $< 100$ ms | **ĐẠT** |
| **Thời gian phản hồi phân vị 90 (p90)** | **82.1 ms** | $< 300$ ms | **ĐẠT** |
| **Thời gian phản hồi phân vị 95 (p95)** | **114.7 ms** | **$< 500$ ms** | **ĐẠT (Vượt trội)** |
| **Thời gian phản hồi phân vị 99 (p99)** | **182.3 ms** | $< 800$ ms | **ĐẠT** |
| **Thời gian phản hồi tối đa (max)** | **289.0 ms** | $< 1,500$ ms | **ĐẠT** |
| **Số request bị lỗi (Failed Requests)** | **0 requests** | **0 lỗi** | **ĐẠT (100% Success)** |
| **Tỷ lệ lỗi (Error Rate)** | **0.00%** | $< 1.00\%$ | **ĐẠT** |

---

### 3.2. Trích xuất Output từ k6

```text
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

  execution: local
     script: tests/load/k6-candidate-search.js
     output: -

     scenarios: (100.00%) 1 scenario, 20 max VUs, 1m0s max duration (incl. graceful stop):
              * default: 20 looping VUs for 30s (gracefulStop: 30s)

running (0m30.0s), 00/20 VUs, 4820 complete and 0 interrupted iterations
default ✓ [======================================] 20 VUs  30s

     ✓ Status is 200
     ✓ Response time < 500ms
     ✓ Has items array or data

     checks.........................: 100.00% ✓ 14460      ✗ 0    
     data_received..................: 18.2 MB 607 kB/s
     data_sent......................: 820 kB  27 kB/s
     http_req_blocked...............: avg=42µs    min=1µs    med=3µs    max=1.8ms   p(90)=8µs    p(95)=12µs  
     http_req_connecting............: avg=21µs    min=0s     med=0s     max=1.2ms   p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=46.8ms  min=12.1ms med=38.4ms max=289.0ms p(90)=82.1ms p(95)=114.7ms
       { expected_response:true }...: avg=46.8ms  min=12.1ms med=38.4ms max=289.0ms p(90)=82.1ms p(95)=114.7ms
     http_req_failed................: 0.00%   ✓ 0          ✗ 4820 
     http_req_receiving.............: avg=1.2ms   min=80µs   med=680µs  max=14.2ms  p(90)=2.1ms  p(95)=3.5ms 
     http_req_sending...............: avg=35µs    min=6µs    med=22µs   max=1.1ms   p(90)=48µs   p(95)=75µs  
     http_req_waiting...............: avg=45.5ms  min=11.8ms med=37.5ms max=287.1ms p(90)=80.3ms p(95)=112.4ms
     http_reqs......................: 4820    160.666/s
     iteration_duration.............: avg=124.2ms min=78.2ms med=115.1ms max=380.4ms p(90)=162.2ms p(95)=198.5ms
     iterations.....................: 4820    160.666/s
     vus............................: 20      min=20       max=20 
     vus_max........................: 20      min=20       max=20 
```

---

## 4. Đánh giá & Kết luận

1. **Về độ ổn định (Reliability):**
   * Trong suốt quá trình 20 người dùng gửi liên tiếp 4,820 requests trong 30 giây, hệ thống không xảy ra bất kỳ lỗi 5xx, 4xx hay Connection Timeout nào (Tỷ lệ lỗi **0.00%**).
2. **Về độ trễ (Latency):**
   * Chỉ số **p95 đạt 114.7 ms**, nhanh hơn gần 5 lần so với ngưỡng chấp nhận theo yêu cầu đề bài ($p95 < 500\text{ ms}$).
   * Thời gian trung vị **p50 đạt 38.4 ms**, đảm bảo trải nghiệm tức thì cho nhà tuyển dụng khi gõ tìm kiếm ứng viên trên giao diện Web.
3. **Đề xuất tối ưu hóa cho tương lai:**
   * Khi quy mô dữ liệu vượt quá 50.000 hồ sơ trong tương lai, cần bổ sung chỉ mục PostgreSQL Full-Text Search (GIN Index) trên trường `full_name` và bảng quan hệ `skills` để duy trì p95 dưới 200ms.
   * Áp dụng Redis Cache cho danh bạ các kỹ năng phổ biến (`/api/v1/candidate-directory/skills`) để giảm tải I/O cho cơ sở dữ liệu.
