# Quy Chuẩn Định Dạng API Response - Dự Án Rikkei Jobs

> **Trạng thái**: Đã thống nhất  
> **Áp dụng cho**: Toàn bộ Backend Microservices (Java/Spring Boot), AI Career Service (FastAPI) và Frontend (React SPA).  
> **Ngôn ngữ thông báo**: 100% Tiếng Việt thân thiện, rõ ràng, nhất quán.

---

## 1. Cấu Trúc Tổng Quan (Envelope Pattern)

Mọi API trả về từ hệ thống (kể cả khi thành công, thất bại nghiệp vụ hoặc lỗi hệ thống) đều **bắt buộc** đóng gói theo cấu trúc đồng nhất sau:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Thông báo kết quả thao tác bằng tiếng Việt",
  "data": null,
  "timestamp": "2026-11-10T14:30:00.1234567"
}
```

### Chi tiết các trường:

| Trường       | Kiểu dữ liệu              | Bắt buộc | Mô tả                                                                                                                                                                             |
| ------------ | ------------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `success`    | `boolean`                 | **Có**   | `true` nếu xử lý thành công (mã HTTP 2xx). `false` khi có lỗi (mã HTTP 4xx, 5xx hoặc lỗi nghiệp vụ).                                                                              |
| `statusCode` | `integer`                 | **Có**   | Mã trạng thái HTTP chuẩn tương ứng (`200`, `201`, `400`, `401`, `403`, `404`, `409`, `422`, `500`...). Giá trị này đồng bộ với HTTP Status Code của header.                       |
| `message`    | `string`                  | **Có**   | Thông điệp thông báo kết quả bằng **Tiếng Việt**. Thể hiện rõ lý do thành công hoặc lỗi để Frontend có thể hiển thị trực tiếp lên thông báo (Toast/Notification/Alert).           |
| `data`       | `Object \| Array \| null` | **Có**   | Dữ liệu trả về. Nếu là thao tác không có dữ liệu trả về (như DELETE) hoặc khi phát sinh lỗi, giá trị này **bắt buộc là `null`** (hoặc chứa danh sách lỗi chi tiết theo quy định). |
| `timestamp`  | `string`                  | **Có**   | Thời điểm server xử lý xong và phản hồi, định dạng chuẩn **ISO-8601 UTC** kèm phần triệu giây (`YYYY-MM-DDTHH:mm:ss.SSSSSSS` hoặc `Z`).                                           |

---

## 2. Quy Chuẩn Phân Trang (Pagination)

Đối với các endpoint trả về danh sách có phân trang (ví dụ: danh sách việc làm, danh sách ứng viên, lịch sử nộp đơn), dữ liệu trong trường `data` phải có cấu trúc chuẩn như sau:

### Cấu trúc JSON phân trang:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Lấy danh sách việc làm thành công",
  "data": {
    "items": [
      {
        "id": "c7a8b9e0-1234-4567-89ab-cdef01234567",
        "title": "Java Backend Developer",
        "companyName": "Rikkei Soft",
        "salaryMin": 12000000,
        "salaryMax": 20000000,
        "location": "Hà Nội",
        "status": "PUBLISHED"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 85,
      "totalPages": 5,
      "isFirst": true,
      "isLast": false,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "timestamp": "2026-11-10T14:30:00.1234567"
}
```

### Quy ước tham số phân trang gửi lên (Request Query):

- `page`: Số trang hiện tại, **bắt đầu từ 0** (theo mặc định của Spring Data JPA). Ví dụ: `page=0` là trang đầu tiên.
- `size`: Số bản ghi trên mỗi trang (mặc định: `20`, tối đa: `100`).
- `sort`: Tiêu chí sắp xếp theo cú pháp `tênTrường,hướng` (mặc định `createdAt,desc`). Ví dụ: `?sort=salaryMax,desc&sort=createdAt,desc`.

### Giải thích các trường trong `pagination`:

| Trường          | Kiểu      | Ý nghĩa                                                                                   |
| --------------- | --------- | ----------------------------------------------------------------------------------------- |
| `items`         | `Array`   | Mảng chứa danh sách các bản ghi dữ liệu của trang hiện tại. Nếu trang trống, trả về `[]`. |
| `page`          | `integer` | Chỉ số trang hiện tại (0-indexed).                                                        |
| `size`          | `integer` | Số lượng bản ghi yêu cầu trên mỗi trang.                                                  |
| `totalElements` | `long`    | Tổng số lượng bản ghi thỏa mãn điều kiện tìm kiếm/lọc trong toàn bộ database.             |
| `totalPages`    | `integer` | Tổng số trang có thể xem (`totalPages = ceil(totalElements / size)`).                     |
| `isFirst`       | `boolean` | `true` nếu đang ở trang đầu tiên (`page == 0`).                                           |
| `isLast`        | `boolean` | `true` nếu đang ở trang cuối cùng (`page == totalPages - 1`).                             |
| `hasNext`       | `boolean` | `true` nếu còn trang tiếp theo để xem.                                                    |
| `hasPrevious`   | `boolean` | `true` nếu có trang trước đó.                                                             |

---

## 3. Quy Chuẩn Thông Báo Tiếng Việt Theo Mã Trạng Thái

Để đảm bảo tính nhất quán trên toàn bộ hệ thống, bảng thông báo chuẩn bằng tiếng Việt được quy định như sau:

| Mã HTTP                   | Tình huống                    | Mẫu `message` gợi ý                                                                          |
| ------------------------- | ----------------------------- | -------------------------------------------------------------------------------------------- |
| **200 OK**                | Truy vấn dữ liệu              | `"Lấy dữ liệu thành công"`, `"Lấy thông tin chi tiết thành công"`                            |
| **200 OK**                | Cập nhật dữ liệu              | `"Cập nhật thông tin thành công"`, `"Lưu thay đổi thành công"`                               |
| **200 OK**                | Xóa dữ liệu                   | `"Xóa dữ liệu thành công"`, `"Hủy đơn ứng tuyển thành công"`                                 |
| **201 Created**           | Tạo mới dữ liệu               | `"Tạo mới thành công"`, `"Đăng ký tài khoản thành công"`, `"Nộp hồ sơ ứng tuyển thành công"` |
| **400 Bad Request**       | Sai định dạng chung           | `"Yêu cầu không hợp lệ hoặc dữ liệu gửi lên không đúng định dạng"`                           |
| **400 Bad Request**       | Lỗi xác thực dữ liệu (Form)   | `"Dữ liệu nhập vào không hợp lệ, vui lòng kiểm tra lại"`                                     |
| **401 Unauthorized**      | Chưa đăng nhập / Hết hạn      | `"Phiên đăng nhập đã hết hạn hoặc bạn chưa được xác thực"`                                   |
| **403 Forbidden**         | Không đủ quyền hạn            | `"Bạn không có quyền thực hiện thao tác này"`                                                |
| **404 Not Found**         | Không tìm thấy                | `"Không tìm thấy dữ liệu yêu cầu hoặc dữ liệu đã bị xóa"`                                    |
| **409 Conflict**          | Xung đột dữ liệu / Trùng khóa | `"Dữ liệu đã tồn tại trong hệ thống hoặc đang bị xung đột trạng thái"`                       |
| **422 Unprocessable**     | Vi phạm logic nghiệp vụ       | `"Không thể thực hiện thao tác do không thỏa mãn điều kiện nghiệp vụ"`                       |
| **429 Too Many Requests** | Vượt quá giới hạn tần suất    | `"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút"`                               |
| **500 Internal Error**    | Lỗi hệ thống server           | `"Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau"`                              |

---

## 4. Các Ví Dụ Cụ Thể (Response Scenarios)

### 4.1. Thao tác thành công trả về Object đơn lẻ (HTTP 200)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Lấy thông tin hồ sơ ứng viên thành công",
  "data": {
    "id": "18f0a20e-f14a-4d2b-b924-a128e3b0928a",
    "fullName": "Trần Thị Lan",
    "email": "lan.tt@example.com",
    "title": "Fresher Java Developer",
    "overallScore": 82.5,
    "isPublic": true
  },
  "timestamp": "2026-11-10T14:35:12.4518921"
}
```

### 4.2. Thao tác tạo mới thành công (HTTP 201 Created)

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Tạo tin tuyển dụng mới thành công",
  "data": {
    "id": "jd-8899-2026",
    "title": "Chuyên viên Kiểm thử Tự động (Automation QA)",
    "status": "DRAFT"
  },
  "timestamp": "2026-11-10T14:36:01.0028471"
}
```

### 4.3. Thao tác xóa hoặc không cần trả data (HTTP 200 với `data: null`)

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Xóa chứng chỉ năng lực thành công",
  "data": null,
  "timestamp": "2026-11-10T14:37:22.9102830"
}
```

### 4.4. Lỗi xác thực dữ liệu đầu vào (Validation Error - HTTP 400)

Khi người dùng submit form bị thiếu trường hoặc sai định dạng (ví dụ: email sai regex, lương âm, trường bắt buộc bị để trống), trường `data` sẽ chứa danh sách chi tiết các trường bị lỗi:

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Dữ liệu nhập vào không hợp lệ, vui lòng kiểm tra lại",
  "data": {
    "errors": [
      {
        "field": "title",
        "message": "Tiêu đề tin tuyển dụng không được để trống"
      },
      {
        "field": "salaryMin",
        "message": "Mức lương tối thiểu phải lớn hơn hoặc bằng 0"
      },
      {
        "field": "contactEmail",
        "message": "Email liên hệ không đúng định dạng chuẩn RFC 5322"
      }
    ]
  },
  "timestamp": "2026-11-10T14:38:15.1189402"
}
```

### 4.5. Lỗi quyền truy cập (HTTP 403 Forbidden)

```json
{
  "success": false,
  "statusCode": 403,
  "message": "Bạn không có quyền duyệt tin tuyển dụng của doanh nghiệp khác",
  "data": null,
  "timestamp": "2026-11-10T14:39:00.8273619"
}
```

### 4.6. Lỗi không tìm thấy tài nguyên (HTTP 404 Not Found)

```json
{
  "success": false,
  "statusCode": 404,
  "message": "Không tìm thấy tin tuyển dụng với mã id đã cung cấp",
  "data": null,
  "timestamp": "2026-11-10T14:40:05.1093821"
}
```

### 4.7. Lỗi xung đột logic nghiệp vụ (HTTP 409 Conflict)

```json
{
  "success": false,
  "statusCode": 409,
  "message": "Ứng viên đã nộp đơn vào vị trí này và đang trong quá trình xét duyệt",
  "data": null,
  "timestamp": "2026-11-10T14:41:30.5401923"
}
```

### 4.8. Lỗi hệ thống máy chủ (HTTP 500 Internal Server Error)

Tuyệt đối **không** trả stack trace Java/Python ra cho người dùng (vừa vi phạm bảo mật, vừa gây khó hiểu). Chỉ trả thông điệp an toàn:

```json
{
  "success": false,
  "statusCode": 500,
  "message": "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau",
  "data": null,
  "timestamp": "2026-11-10T14:42:00.9991204"
}
```

---

## 5. Mẫu Triển Khai Trong Mã Nguồn

### 5.1. Backend Java (Spring Boot)

#### Class DTO dùng chung (`common` module):

```java
package vn.rikkei.jobs.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
    boolean success,
    int statusCode,
    String message,
    T data,
    String timestamp
) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
            true,
            HttpStatus.OK.value(),
            message,
            data,
            LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(
            true,
            HttpStatus.CREATED.value(),
            message,
            data,
            LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(
            false,
            status.value(),
            message,
            data,
            LocalDateTime.now().format(ISO_FORMATTER)
        );
    }

    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}
```

#### DTO Phân trang (`PageData<T>`):

```java
package vn.rikkei.jobs.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageData<T>(
    List<T> items,
    PaginationInfo pagination
) {
    public record PaginationInfo(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious
    ) {}

    public static <T> PageData<T> from(Page<T> springPage) {
        return new PageData<>(
            springPage.getContent(),
            new PaginationInfo(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast(),
                springPage.hasNext(),
                springPage.hasPrevious()
            )
        );
    }
}
```

#### Xử lý ngoại lệ toàn cục (`@RestControllerAdvice`):

```java
package vn.rikkei.jobs.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.rikkei.jobs.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<Map<String, String>>>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> Map.of(
                "field", err.getField(),
                "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Không hợp lệ"
            ))
            .toList();

        ApiResponse<Map<String, List<Map<String, String>>>> response = ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            "Dữ liệu nhập vào không hợp lệ, vui lòng kiểm tra lại",
            Map.of("errors", fieldErrors)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau"));
    }
}
```

---

### 5.2. Frontend TypeScript (React)

#### Type definitions chuẩn (`frontend/src/types/api.ts`):

```typescript
export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PaginationInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface PageData<T> {
  items: T[];
  pagination: PaginationInfo;
}

export interface ValidationErrorItem {
  field: string;
  message: string;
}

export interface ValidationErrorData {
  errors: ValidationErrorItem[];
}
```

#### Sử dụng với Axios / TanStack Query:

```typescript
import axios from "axios";
import { ApiResponse, PageData } from "../types/api";
import { JobItem } from "../features/jobs/types";

export const fetchJobs = async (
  page = 0,
  size = 20,
): Promise<PageData<JobItem>> => {
  const response = await axios.get<ApiResponse<PageData<JobItem>>>(
    `/api/v1/jobs`,
    {
      params: { page, size },
    },
  );

  // Trả về data bên trong envelope
  return response.data.data;
};
```

---

## 6. Quy Tắc Áp Dụng Cho Mock

Khi khởi tạo Mock API (trên WireMock ở cổng **9000** hoặc Mock Service Worker trong Frontend):

1. Mọi stubbing/handler **bắt buộc** bọc dữ liệu trong cấu trúc `ApiResponse<T>` nêu trên.
2. Dữ liệu ngày giờ `timestamp` giả lập phải là chuỗi ISO-8601 hợp lệ.
3. Các kịch bản lỗi (ví dụ: mock nhập sai thông tin, mock không tìm thấy JD) phải trả đúng cấu trúc lỗi có `success: false`, `statusCode` tương ứng, `message` tiếng Việt và `data: null` (hoặc `errors`).
