import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { toast } from "sonner";
import type { ApiResponse, CustomRequestConfig, ApiErrorDetail } from "./types/api";

declare module "axios" {
  export interface AxiosRequestConfig extends CustomRequestConfig {}
  export interface InternalAxiosRequestConfig extends CustomRequestConfig {}
}

/**
 * Token lưu trong bộ nhớ (In-memory) theo chuẩn bảo mật D11,
 * giảm thiểu rủi ro XSS so với localStorage / sessionStorage.
 */
let inMemoryAccessToken: string | null = null;
let onUnauthorizedCallback: (() => void) | null = null;

export const setAccessToken = (token: string | null) => {
  inMemoryAccessToken = token;
};

export const getAccessToken = (): string | null => inMemoryAccessToken;

export const registerUnauthorizedHandler = (handler: () => void) => {
  onUnauthorizedCallback = handler;
};

/**
 * Helper chuẩn hóa URL tài nguyên tĩnh (logo công ty, file upload).
 * - Nếu là URL tuyệt đối (http://, https://, blob:, data:) thì giữ nguyên.
 * - Nếu là đường dẫn tương đối:
 *   + Nếu VITE_API_BASE_URL là URL tuyệt đối: ghép với origin của VITE_API_BASE_URL (bỏ phần path như /api/v1 nếu có).
 *   + Nếu VITE_API_BASE_URL là đường dẫn tương đối (qua proxy/gateway): dùng chính nó.
 *   + Nếu VITE_API_BASE_URL không được cấu hình: mặc định ghép với origin window.location.origin.
 */
export function resolveAssetUrl(path: string | null | undefined): string {
  if (!path) return "";
  if (/^https?:\/\//i.test(path) || path.startsWith("data:") || path.startsWith("blob:")) {
    return path;
  }
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const rawBaseUrl = import.meta.env.VITE_API_BASE_URL;

  if (rawBaseUrl) {
    if (/^https?:\/\//i.test(rawBaseUrl)) {
      try {
        const parsed = new URL(rawBaseUrl);
        return `${parsed.origin}${normalizedPath}`;
      } catch {
        // Fallback below
      }
    } else {
      return normalizedPath;
    }
  }

  const defaultOrigin = typeof window !== "undefined" ? window.location.origin : "";
  return `${defaultOrigin}${normalizedPath}`;
}

/**
 * Khởi tạo Axios Instance chính
 */
export const axiosInstance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/v1",
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

// -------------------------------------------------------------
// REQUEST INTERCEPTOR: Gắn Token & Request Header
// -------------------------------------------------------------
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Nếu gửi FormData (upload file/ảnh), xóa Content-Type mặc định để Axios và trình duyệt tự gán multipart/form-data kèm boundary
    if (config.data instanceof FormData) {
      delete config.headers["Content-Type"];
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// -------------------------------------------------------------
// RESPONSE INTERCEPTOR: Trích xuất Envelope & Bắt lỗi chuẩn tiếng Việt
// -------------------------------------------------------------
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    const resData = response.data;
    // Nếu response đã đóng gói chuẩn Envelope { success: true, data: ... }
    if (resData && typeof resData === "object" && "success" in resData && "data" in resData) {
      return resData;
    }
    // Chuẩn hóa response thô (ví dụ từ Mock Server) về Envelope chuẩn
    return {
      success: true,
      statusCode: response.status,
      message: "Thao tác thành công",
      data: resData,
      timestamp: new Date().toISOString(),
    };
  },
  (error: AxiosError<ApiResponse<ApiErrorDetail>>) => {
    const isSilent = error.config?.silentError === true;
    const status = error.response?.status;
    const errorData = error.response?.data;

    let errorMessage = "Đã xảy ra lỗi không xác định, vui lòng thử lại sau";

    if (!error.response) {
      // Lỗi mất kết nối mạng hoặc không gọi được Gateway/Backend
      errorMessage = "Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền mạng";
      if (!isSilent) toast.error(errorMessage);
      return Promise.reject(error);
    }

    // Lấy message chuẩn từ Backend Envelope nếu có
    if (errorData?.message) {
      errorMessage = errorData.message;
    } else {
      switch (status) {
        case 400:
          errorMessage = "Dữ liệu yêu cầu không hợp lệ hoặc sai định dạng";
          break;
        case 401:
          errorMessage = "Phiên đăng nhập đã hết hạn hoặc bạn chưa được xác thực";
          break;
        case 403:
          errorMessage = "Bạn không có quyền thực hiện thao tác này";
          break;
        case 404:
          errorMessage = "Không tìm thấy dữ liệu yêu cầu hoặc dữ liệu đã bị xóa";
          break;
        case 409:
          errorMessage = "Dữ liệu đã tồn tại hoặc đang bị xung đột trạng thái";
          break;
        case 422:
          errorMessage = "Không thể thực hiện thao tác do vi phạm điều kiện nghiệp vụ";
          break;
        case 429:
          errorMessage = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút";
          break;
        case 500:
        case 502:
        case 503:
          errorMessage = "Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau";
          break;
      }
    }

    // Xử lý riêng mã 401 Unauthorized
    if (status === 401) {
      setAccessToken(null);
      if (onUnauthorizedCallback) {
        onUnauthorizedCallback();
      }
    }

    // Hiển thị Toast tiếng Việt nếu không cấu hình silentError
    if (!isSilent) {
      // Nếu có lỗi validation theo từng field, hiển thị lỗi trường đầu tiên hoặc thông báo chung
      const firstFieldError = errorData?.data?.errors?.[0]?.message;
      toast.error(firstFieldError || errorMessage);
    }

    return Promise.reject(error);
  }
);

// -------------------------------------------------------------
// WRAPPER HELPER: Cung cấp các hàm gọi API chuẩn Type
// -------------------------------------------------------------
export const api = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.get(url, config) as unknown as Promise<ApiResponse<T>>;
  },

  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.post(url, data, config) as unknown as Promise<ApiResponse<T>>;
  },

  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.put(url, data, config) as unknown as Promise<ApiResponse<T>>;
  },

  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.patch(url, data, config) as unknown as Promise<ApiResponse<T>>;
  },

  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return axiosInstance.delete(url, config) as unknown as Promise<ApiResponse<T>>;
  },
};

export default api;

