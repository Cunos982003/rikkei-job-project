# Kết nối AI Career với frontend chung

Module Career đã được nối vào frontend chung. Các thay đổi dùng chung gồm
`src/routes/AppRoutes.tsx`, `src/components/layout/Sidebar.tsx` và `vite.config.ts`;
không có thay đổi trong service khác.

## Route và điều hướng

Các trang Career nằm trong `src/pages/career`; layout, hook và các component đặc thù
vẫn nằm trong `src/features/career`.

Các route sau nằm trong `RoleGuard` dành cho `CANDIDATE`, bên trong `MainLayout`:

```tsx
<Route path="/candidate/career" element={<CareerLayout />}>
  <Route index element={<OverviewPage />} />
  <Route path="recommendations" element={<RecommendationsPage />} />
  <Route path="skills" element={<SkillsPage />} />
</Route>
```

Sidebar của ứng viên có liên kết `/candidate/career`. Module sử dụng phiên đăng nhập
hiện tại và không yêu cầu người dùng nhập JWT thủ công.

## Kết nối API

Module gửi `GET /ai-career/me/job-recommendations?limit=10` kèm token hiện tại.
Endpoint này dùng client riêng vì trả về mảng trực tiếp và lỗi 401 từ service không
được tự động đăng xuất khỏi ứng dụng chung. Vite chuyển tiếp `/ai-career/*` tới
`http://localhost:8090` và bỏ tiền tố `/ai-career`, giữ header `Authorization`.
Đặt `VITE_AI_CAREER_URL` khi khởi chạy Vite để thay đổi đích proxy cục bộ.
Trong môi trường triển khai cần cấu hình reverse proxy tương đương hoặc đặt
`VITE_AI_CAREER_BASE_URL` thành base URL đã được cấu hình an toàn; không gửi token
đến nguồn không tin cậy.

Route proxy không tự tạo principal đã xác thực. AI Career Service hiện có filter
Bearer chỉ chạy với profile `dev`, không kiểm tra chữ ký JWT; tuyệt đối không dùng
filter đó trong môi trường thực tế. Cần cơ chế xác thực hợp lệ tạo principal có
UUID ứng viên cho môi trường triển khai. Backend hiện cũng chưa kết nối nguồn dữ
liệu hồ sơ và việc làm, do đó có thể trả về 503 cho tới khi adapter được triển khai.
