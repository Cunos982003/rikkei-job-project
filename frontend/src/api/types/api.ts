/**
 * Chuẩn định dạng API Response theo tài liệu docs/quy-chuan-api-response.md
 * Áp dụng thống nhất cho toàn bộ hệ sinh thái Rikkei Jobs.
 */

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiErrorDetail {
  errors?: FieldError[];
  [key: string]: unknown;
}

/**
 * Envelope Pattern chuẩn cho toàn bộ API Response.
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  timestamp: string;
}

/**
 * Cấu trúc phân trang chuẩn (0-indexed theo Spring Data Page).
 */
export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Envelope phân trang chuẩn: data chứa danh sách items và meta phân trang.
 */
export interface PaginatedData<T> {
  items: T[];
  pagination: PaginationMeta;
}

/**
 * Tham số truy vấn phân trang gửi lên (0-indexed).
 */
export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
  [key: string]: unknown;
}

/**
 * Custom options có thể truyền vào Axios Request Config.
 */
export interface CustomRequestConfig {
  /**
   * Bật silentError = true nếu không muốn tự động hiện Toast thông báo lỗi toàn cục.
   */
  silentError?: boolean;
}

