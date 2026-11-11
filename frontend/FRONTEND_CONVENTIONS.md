# Quy Chuẩn Thiết Kế Frontend & UI/UX - Rikkei Jobs

> **Trạng thái**: Đã thống nhất  
> **Áp dụng cho**: Toàn bộ lập trình viên Frontend (React 19 + TypeScript + Vite + Tailwind CSS v4)  
> **Mục tiêu**: Đồng bộ trải nghiệm người dùng (UX), ngôn ngữ thiết kế (UI), cấu trúc mã nguồn và thẩm mỹ sản phẩm theo chuẩn hiện đại.

---

## 1. Triết Lý Thiết Kế (Design Philosophy)

Hệ thống **Rikkei Jobs** được định vị là nền tảng tuyển dụng công nghệ gắn liền với giáo dục chất lượng cao của Rikkei Education. Thiết kế giao diện phải đảm bảo:

1. **Tin cậy & Minh bạch (Trust & Transparency)**: Làm nổi bật tính xác thực của dữ liệu (bằng chứng từ LMS, Khảo thí, IOC); phân định rõ ràng giữa dữ liệu đã kiểm định và dữ liệu tự khai.
2. **Hiện đại, Chuyên nghiệp (Modern Enterprise)**: Tinh giản, sạch sẽ, bố cục dạng dashboard/portal chuẩn mực, không rườm rà.
3. **Phản hồi tức thì & Thấu hiểu người dùng (Responsive & Human-centric)**: Mọi thao tác tải dữ liệu, gửi form hay lỗi hệ thống đều có trạng thái hiển thị rõ ràng (Skeleton, Toast tiếng Việt, Empty state, Inline error).
4. **Bảo mật giao diện**: Tuân thủ nguyên tắc che thông tin cá nhân (PII Masking), không để lộ số điện thoại, email ứng viên ở trang duyệt công khai.

---

## 2. Hệ Thống Màu Sắc (Color Palette)

### 2.1. Màu sắc chủ đạo (Brand Primary Color - Rikkei Red)
* **Mã màu chính**: `#ab1f24` (Màu đỏ thương hiệu Rikkei)
* Màu chủ đạo được sử dụng cho các thành phần chính: Primary Button, Active Nav item, Logo accent, Progress bar quan trọng, Điểm nhấn thương hiệu.

Bảng thang màu chi tiết cho biến thể Tailwind CSS:

| Tên biến thể | Mã Hex | Vai trò / Ngữ cảnh sử dụng |
|:---|:---:|:---|
| `primary-50` | `#fdf2f2` | Nền phụ, thẻ highlight nhẹ, hover hàng trong bảng |
| `primary-100` | `#fde8e8` | Viền khung thông báo quan trọng, badge nền nhạt |
| `primary-200` | `#fbd5d5` | Đường viền khi focus trạng thái nhẹ |
| `primary-500` | `#ab1f24` | **Màu thương hiệu chính (Base Primary)** |
| `primary-600` | `#8c191d` | Trạng thái Hover của Primary Button |
| `primary-700` | `#6e1317` | Trạng thái Active / Pressed của nút bấm |
| `primary-900` | `#450b0e` | Tiêu đề hoặc nền tối điểm nhấn |

### 2.2. Màu trung tính (Neutrals)
Sử dụng thang màu xám lạnh (Slate/Zinc) để tạo độ sâu, tương phản cao và hiện đại:
* **Background (`bg-background`)**: `#f8fafc` (Nền trang tổng thể) hoặc `#ffffff` (Nền card/modal).
* **Card / Surface (`bg-card`)**: `#ffffff` (Có bo góc và viền mỏng).
* **Text Primary (`text-foreground`)**: `#0f172a` (Tiêu đề, chữ chính - tương phản cao).
* **Text Secondary (`text-muted-foreground`)**: `#64748b` (Mô tả, nhãn phụ, ngày tháng).
* **Border (`border-border`)**: `#e2e8f0` (Đường kẻ phân cách, viền input, viền card).

### 2.3. Màu ngữ nghĩa (Semantic Colors)

| Loại trạng thái | Mã Hex | Màu nền nhạt | Áp dụng trong dự án Rikkei Jobs |
|:---|:---:|:---:|:---|
| **Success (Thành công)** | `#16a34a` (Green-600) | `#f0fdf4` | Huy hiệu **Đã xác thực**, Nộp đơn thành công, Điểm thi tốt, Offer được duyệt |
| **Warning (Cảnh báo)** | `#d97706` (Amber-600) | `#fffbeb` | Hồ sơ chờ duyệt, Cảnh báo **Thiếu kỹ năng bắt buộc**, Sắp hết hạn offer |
| **Danger / Destructive** | `#dc2626` (Red-600) | `#fef2f2` | Thao tác hủy đơn, Xóa chứng chỉ, Báo sai dữ liệu, Rút công khai hồ sơ |
| **Info / Self-reported** | `#2563eb` (Blue-600) | `#eff6ff` | Kỹ năng **Tự khai**, Thông báo lịch phỏng vấn, Gợi ý học tập LMS |

### 2.4. Khai báo Tailwind CSS v4 (`src/index.css`)
Cấu hình CSS variables và `@theme` cho toàn bộ dự án:

```css
@import "tailwindcss";

:root {
  --font-sans: "Inter", "Be Vietnam Pro", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;

  /* Theme Brand Colors */
  --color-primary: #ab1f24;
  --color-primary-hover: #8c191d;
  --color-primary-active: #6e1317;
  --color-primary-light: #fdf2f2;
  --color-primary-border: #fde8e8;

  /* Neutrals & Surfaces */
  --color-background: #f8fafc;
  --color-surface: #ffffff;
  --color-foreground: #0f172a;
  --color-muted: #64748b;
  --color-border: #e2e8f0;

  /* Semantic */
  --color-success: #16a34a;
  --color-warning: #d97706;
  --color-danger: #dc2626;
  --color-info: #2563eb;
}

@theme {
  --font-sans: var(--font-sans);
  --color-primary: var(--color-primary);
  --color-primary-hover: var(--color-primary-hover);
  --color-primary-active: var(--color-primary-active);
  --color-primary-light: var(--color-primary-light);
  --color-primary-border: var(--color-primary-border);

  --color-background: var(--color-background);
  --color-surface: var(--color-surface);
  --color-foreground: var(--color-foreground);
  --color-muted: var(--color-muted);
  --color-border: var(--color-border);
}

body {
  font-family: var(--font-sans);
  background-color: var(--color-background);
  color: var(--color-foreground);
  -webkit-font-smoothing: antialiased;
}
```

---

## 3. Hệ Thống Typography (Chữ Viết & Phông Chữ)

### 3.1. Phông chữ chuẩn
* **Phông chữ quy định**: **Inter** hoặc **Be Vietnam Pro**.
* **Lý do**: Đây là hai họ phông chữ hình học trung tính, hỗ trợ tiếng Việt có dấu hoàn hảo, kerning cân đối và hiển thị sắc nét trên cả màn hình retina và màn hình phổ thông.

### 3.2. Cấp bậc kiểu chữ (Typography Hierarchy)

| Cấp độ | Cỡ chữ / Line-height | Font-weight | Tailwind Class | Trường hợp sử dụng |
|:---|:---:|:---:|:---|:---|
| **Display / H1** | 30px / 36px (`1.875rem`) | Bold (`700`) | `text-2xl sm:text-3xl font-bold` | Tiêu đề trang chính (Dashboard, Hồ sơ năng lực) |
| **Heading 2 (H2)** | 24px / 32px (`1.5rem`) | SemiBold (`600`) | `text-xl sm:text-2xl font-semibold` | Tiêu đề phân mục lớn (Nhóm kỹ năng, Vòng phỏng vấn) |
| **Heading 3 (H3)** | 18px / 28px (`1.125rem`) | SemiBold (`600`) | `text-lg font-semibold` | Tiêu đề thẻ Card, Tên vị trí JD, Modal Title |
| **Heading 4 (H4)** | 15px / 22px (`0.9375rem`) | Medium (`500`) | `text-base font-medium` | Tiêu đề con, Tên kỹ năng thành phần |
| **Body (Chính)** | 14px / 20px (`0.875rem`) | Normal (`400`) | `text-sm font-normal` | Văn bản nội dung, dữ liệu trong bảng, form input |
| **Body Small** | 13px / 18px (`0.8125rem`) | Normal / Medium | `text-xs sm:text-sm` | Nhận xét của Mentor, gợi ý form, helper text |
| **Caption / Badge** | 12px / 16px (`0.75rem`) | Medium / SemiBold | `text-xs font-semibold` | Tag, Badge trạng thái, Timestamp, Metadata |

---

## 4. Bố Cục, Spacing & Breakpoints (Layout & Grid)

### 4.1. Hệ thống lưới Spacing (4px Base Grid)
Tuân thủ nghiêm ngặt thang đo 4px của Tailwind:
* `p-1` (4px), `p-2` (8px), `p-3` (12px), `p-4` (16px), `p-6` (24px), `p-8` (32px).
* Khoảng cách giữa các khối (Page Sections): `gap-6` hoặc `space-y-6` (24px).
* Khoảng cách giữa các phần tử trong form: `space-y-4` (16px).
* Khoảng cách giữa các inline buttons/tags: `gap-2` (8px) hoặc `gap-3` (12px).

### 4.2. Breakpoints chuẩn Responsive (Mobile-First)
* **Mobile**: `< 640px` (Thanh toán, modal full màn hình, sidebar chuyển thành Drawer/Sheet).
* **Tablet (`sm` - `md`)**: `640px` đến `1024px` (Lưới 2 cột cho card kỹ năng và JD).
* **Desktop (`lg` - `xl`)**: `1024px` đến `1280px` (Bố cục 3 cột, hiển thị sidebar cố định).
* **Màn hình lớn (`2xl`)**: `> 1280px` (Đóng khung `max-w-7xl mx-auto` để không tràn vô tận).

### 4.3. Bo góc (Border Radius) & Bóng đổ (Box Shadow)
* **Bo góc**:
  * Nút bấm, Badge, Input: `rounded-md` (6px) hoặc `rounded-lg` (8px).
  * Card, Modal, Dropdown Panel: `rounded-xl` (12px).
  * Avatar, Progress Pill: `rounded-full`.
* **Bóng đổ**:
  * Card ở trạng thái nghỉ: `shadow-sm border border-border` (ưu tiên border mỏng hơn bóng đậm).
  * Card khi Hover: `hover:shadow-md transition-shadow duration-200`.
  * Modal, Popup: `shadow-xl`.

---

## 5. Kiến Trúc Thư Mục Frontend Chuẩn (`src/`)

Thư mục `frontend/src` được phân chia module hoá theo kiến trúc hiện đại:

```
frontend/src/
├── api/                   # Tầng giao tiếp HTTP & Endpoints
│   ├── client.ts          # Cấu hình Axios instance, interceptors JWT Bearer
│   ├── endpoints/         # Định nghĩa hàm gọi API (auth.api.ts, profile.api.ts, job.api.ts)
│   └── types/             # Typescript interfaces cho request / response (chuẩn ApiResponse tiếng Việt)
│
├── components/            # Các UI Component tái sử dụng
│   ├── common/            # Nút bấm, Input, Modal, Badge, Table, Tooltip, Dropdown
│   ├── feedback/          # EmptyState, ErrorState, LoadingSkeleton, ConfirmDialog
│   └── layout/            # Navbar, Sidebar theo từng role, Footer, PageHeader
│
├── layouts/               # Layout wrapper cấp trang
│   ├── MainLayout.tsx     # Layout chính cho người dùng đã đăng nhập (Sidebar + Header + Body)
│   ├── AuthLayout.tsx     # Layout cho màn đăng nhập, quên mật khẩu, đổi mật khẩu
│   └── AppShell.tsx       # Khung điều hướng tổng thể & Route Guard
│
├── pages/                 # Màn hình chính theo vai trò nghiệp vụ
│   ├── auth/              # Đăng nhập HR, SSO callback, Đổi mật khẩu
│   ├── candidate/         # Hồ sơ kỹ năng, CV chuẩn, Quản lý đơn ứng tuyển, Học bổ sung
│   ├── hr/                # Quản lý hồ sơ công ty, Đăng JD, Tìm ứng viên, Phỏng vấn, Offer
│   ├── mentor/            # Danh sách sinh viên thực tập, Phiếu đánh giá
│   ├── staff/             # Duyệt JD, Ghép đợt IOC, Mở khóa liên hệ ứng viên
│   └── admin/             # Quản lý danh mục kỹ năng, Trọng số, Báo sai bằng chứng, Audit
│
├── service/               # Tầng xử lý logic nghiệp vụ phía client (nếu có tính toán độc lập)
│   ├── matchingService.ts # Logic tính toán hiển thị điểm khớp, skill gap
│   └── authService.ts     # Xử lý token, phân quyền vai trò
│
├── store/                 # State management toàn cục bằng Zustand
│   ├── useAuthStore.ts    # Lưu thông tin phiên đăng nhập, vai trò hiện tại, quyền hạn
│   └── useUiStore.ts      # Quản lý trạng thái mở sidebar, theme, modal
│
├── utils/                 # Các hàm tiện ích dùng chung
│   ├── cn.ts              # Helper kết hợp class Tailwind
│   ├── formatters.ts      # Định dạng tiền tệ (VND), ngày tháng (DD/MM/YYYY theo chuẩn vi-VN)
│   └── storage.ts         # Wrapper an toàn cho bộ nhớ phiên làm việc
│
├── lib/                   # Khởi tạo thư viện bên thứ 3
│   ├── queryClient.ts     # Cấu hình TanStack Query (staleTime 5m, retry policy)
│   └── sonner.ts          # Helper cấu hình hiển thị Toast thông báo
│
├── App.tsx                # Khai báo React Router v7 & QueryClientProvider
├── main.tsx               # Entry point
└── index.css              # Khởi tạo Tailwind CSS v4 & Biến màu toàn cục
```

---

## 6. Quy Chuẩn Kỹ Thuật (Frontend Technical Standards)

### 6.1. Quản lý trạng thái server với TanStack Query v5
* **Query Key**: Bắt buộc là mảng phân cấp rõ ràng:
  ```ts
  queryKey: ["profile", "skills", userId ?? "me"]
  queryKey: ["jobs", { page, size, status }]
  ```
* **Thời gian cache**: Mặc định `staleTime: 1000 * 60 * 5` (5 phút) để tránh gọi lại API liên tục.
* **Mutations**: Sau khi thêm/sửa/xóa thành công (ví dụ: gửi phiếu đánh giá, duyệt JD), bắt buộc gọi `queryClient.invalidateQueries(...)` để tự động làm mới dữ liệu.

### 6.2. Quản lý biểu mẫu (Forms) với React Hook Form & Zod
* Mọi form nhập liệu đều phải được định nghĩa bằng **Zod schema**:
  * Kiểm tra đầy đủ tin báo lỗi tiếng Việt (ví dụ: `"Tiêu đề không được để trống"`, `"Mức lương tối thiểu phải lớn hơn 0"`).
  * Hiển thị lỗi **ngay bên dưới ô nhập (Inline error)** với chữ đỏ `text-danger text-xs mt-1`.
  * Trạng thái đang gửi (isSubmitting): Nút Submit phải hiển thị Spinner và bị `disabled` để chống click đúp.

### 6.3. Thông báo Toast (Sonner)
* Vị trí xuất hiện thống nhất: `top-right` trên Desktop và `bottom-center` trên Mobile.
* 100% nội dung thông báo bằng **Tiếng Việt thân thiện, rõ nghĩa**:
  ```ts
  toast.success("Cập nhật hồ sơ năng lực thành công");
  toast.error("Không thể kết nối đến máy chủ. Vui lòng thử lại sau");
  ```

### 6.4. State Management với Zustand
* Chỉ dùng Zustand cho trạng thái UI toàn cục (`sidebarOpen`) và trạng thái xác thực (`user`, `isAuthenticated`).
* Dữ liệu từ API **bắt buộc quản lý qua TanStack Query**, không copy thủ công vào Zustand.

---

## 7. Quy Chuẩn UI/UX Đặc Thù Nghiệp Vụ Rikkei Jobs

### 7.1. Trực quan hóa Skill Profile & Bằng chứng xác thực
1. **Phân biệt nguồn dữ liệu**:
   * **Bằng chứng xác thực (LMS, Khảo thí, Thực tập IOC)**: Hiển thị kèm Badge màu xanh lá (`#16a34a`) có icon `CheckCircle` hoặc `Award`.
   * **Dữ liệu tự khai (Ứng viên tự nhập)**: Bắt buộc gắn Badge màu xanh dương hoặc xám kèm nhãn rõ ràng: `Tự khai`. Điểm tự khai không gộp chung vào thanh điểm chính thức mà vẽ thành vạch nét đứt hoặc thanh phụ để đối chiếu.
2. **Thang điểm 0–100 & Phân bậc kỹ năng**:
   * `0 – 39` (Cơ bản): Thanh tiến độ màu Đỏ/Cam (`#ef4444`).
   * `40 – 69` (Khá): Thanh tiến độ màu Vàng/Amber (`#f59e0b`).
   * `70 – 84` (Tốt/Vững): Thanh tiến độ màu Xanh lá (`#10b981`).
   * `85 – 100` (Xuất sắc): Thanh tiến độ màu Đỏ thương hiệu (`#ab1f24`) hoặc Tím/Xanh đậm.

### 7.2. Bảo mật thông tin định danh cá nhân (PII Masking)
* Trên màn hình tìm kiếm ứng viên của Doanh nghiệp (HR):
  * Số điện thoại phải che dạng: `098***1234`.
  * Email phải che dạng: `ng***@rikkei.vn`.
  * Chỉ khi Cán bộ tuyển dụng (Staff) phê duyệt mở khóa hoặc khi ứng viên nộp đơn vào JD cụ thể của HR đó thì thông tin này mới được giải mã hiển thị.

### 7.3. Trực quan hóa Điểm khớp (Matching Score) & Khoảng thiếu hụt (Skill Gap)
* Khi hiển thị chi tiết JD cho ứng viên:
  * Điểm khớp tổng thể hiển thị dạng vòng tròn tiến độ (Circular Progress) hoặc thanh phần trăm lớn với màu chủ đạo `#ab1f24`.
  * Danh sách **Kỹ năng bắt buộc**: Nếu ứng viên chưa đạt điểm chuẩn, hiển thị badge cảnh báo màu vàng/cam kèm nút bấm một chạm: **"Học bổ sung trên LMS"**.

### 7.4. Hộp thoại xác nhận thao tác quan trọng (Confirmation Modals)
* Các hành động có tác động lớn: *Rút công khai hồ sơ, Từ chối ứng viên, Gửi Offer, Báo sai bằng chứng*:
  * Bắt buộc mở Modal xác nhận, nêu rõ hệ quả (Ví dụ: *"Khi rút công khai, hồ sơ của bạn sẽ biến mất khỏi kết quả tìm kiếm của doanh nghiệp ngay lập tức"*).
  * Đối với các hành động từ chối/báo sai, bắt buộc có trường Textarea để nhập **Lý do**.

---

## 8. Nguyên Tắc Trải Nghiệm Người Dùng (UX Checklist)

Trước khi nghiệm thu một màn hình Frontend, lập trình viên cần đối chiếu:

- [ ] **Màu chủ đạo**: Sử dụng đúng màu thương hiệu `#ab1f24` cho các nút bấm hành động chính (Primary CTA).
- [ ] **Phông chữ**: Hiển thị sắc nét, không bị lỗi font tiếng Việt trên các trình duyệt Chrome, Edge, Safari, Firefox.
- [ ] **Loading State**: Không để màn hình trắng khi đang tải; sử dụng `Skeleton` đúng với hình dáng nội dung sẽ hiển thị.
- [ ] **Empty State**: Khi danh sách rỗng (chưa có đơn ứng tuyển, chưa có JD nào), hiển thị hình minh họa nhẹ, dòng thông báo hướng dẫn và nút CTA tạo mới.
- [ ] **Error Handling**: Khi mất kết nối hoặc API trả lỗi 4xx/5xx, hiển thị khối thông báo có nút **"Thử lại" (Retry)**, không làm trắng trang.
- [ ] **Format dữ liệu**: Tiền tệ viết dạng `15.000.000 đ`, ngày tháng viết dạng `15/10/2026`, thời gian chuẩn múi giờ Việt Nam (UTC+7).
- [ ] **Keyboard & Accessibility**: Có thể dùng phím `Tab` để duyệt form, phím `Enter` để gửi form, phím `Esc` để đóng Modal.
- [ ] **Mobile-friendly**: Mọi bảng dữ liệu lớn (Table) trên màn hình nhỏ phải hỗ trợ cuộn ngang hoặc tự chuyển đổi thành dạng danh sách thẻ (Card list).

---
*Tài liệu này là quy chuẩn bắt buộc áp dụng khi phát triển giao diện người dùng cho toàn bộ hệ sinh thái Rikkei Jobs.*

