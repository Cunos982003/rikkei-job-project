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

const TOKEN_STORAGE_KEY = "rikkei_access_token";
const USER_ID_STORAGE_KEY = "rikkei_user_id";

let inMemoryAccessToken: string | null =
  typeof window !== "undefined" ? localStorage.getItem(TOKEN_STORAGE_KEY) : null;
let inMemoryUserId: string | null =
  typeof window !== "undefined" ? localStorage.getItem(USER_ID_STORAGE_KEY) : null;
let onUnauthorizedCallback: (() => void) | null = null;

export const setAccessToken = (token: string | null) => {
  inMemoryAccessToken = token;
  if (typeof window !== "undefined") {
    if (token) {
      try {
        localStorage.setItem(TOKEN_STORAGE_KEY, token);
      } catch {}
    } else {
      try {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      } catch {}
    }
  }
};

export const getAccessToken = (): string | null => {
  if (inMemoryAccessToken) return inMemoryAccessToken;
  if (typeof window !== "undefined") {
    try {
      inMemoryAccessToken = localStorage.getItem(TOKEN_STORAGE_KEY);
      return inMemoryAccessToken;
    } catch {}
  }
  return null;
};

/**
 * Các service nội bộ (profile-service, evidence-service...) xác định danh tính
 * qua header X-User-Id do gateway truyền xuống, không đọc JWT.
 */
export const setCurrentUserId = (userId: string | null) => {
  inMemoryUserId = userId;
  if (typeof window !== "undefined") {
    if (userId) {
      try {
        localStorage.setItem(USER_ID_STORAGE_KEY, userId);
      } catch {}
    } else {
      try {
        localStorage.removeItem(USER_ID_STORAGE_KEY);
      } catch {}
    }
  }
};

export const getCurrentUserId = (): string | null => {
  if (inMemoryUserId) return inMemoryUserId;
  if (typeof window !== "undefined") {
    try {
      inMemoryUserId = localStorage.getItem(USER_ID_STORAGE_KEY);
      return inMemoryUserId;
    } catch {}
  }
  return null;
};

export const registerUnauthorizedHandler = (handler: () => void) => {
  onUnauthorizedCallback = handler;
};

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

    const userId = getCurrentUserId();
    if (userId && !config.headers["X-User-Id"]) {
      config.headers["X-User-Id"] = userId;
    }

    // Để trình duyệt tự sinh boundary cho multipart/form-data
    if (typeof FormData !== "undefined" && config.data instanceof FormData) {
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

