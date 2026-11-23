/**
 * Tiện ích xử lý URL động cho Cổng LMS và Mock Server.
 * Tránh tình trạng hardcode 'localhost:9000' vào các bundle JavaScript sau khi build production.
 */

declare global {
  interface Window {
    __RUNTIME_CONFIG__?: {
      VITE_LMS_BASE_URL?: string;
      VITE_LMS_PORTAL_URL?: string;
      VITE_MOCK_API_URL?: string;
    };
  }
}

/**
 * Lấy Base URL của hệ thống LMS.
 * Thứ tự ưu tiên:
 * 1. window.__RUNTIME_CONFIG__?.VITE_LMS_BASE_URL (nếu có cấu hình động qua runtime)
 * 2. import.meta.env.VITE_LMS_BASE_URL (biến môi trường Vite khi build)
 * 3. Tự động nhận diện hostname hiện tại của trình duyệt nếu khác localhost/127.0.0.1 (dành cho VPS / mạng LAN)
 * 4. Fallback mặc định 'http://localhost:9000' cho môi trường phát triển local
 */
export const getLmsBaseUrl = (): string => {
  if (typeof window !== "undefined" && window.__RUNTIME_CONFIG__?.VITE_LMS_BASE_URL) {
    return window.__RUNTIME_CONFIG__.VITE_LMS_BASE_URL.replace(/\/+$/, "");
  }

  const envUrl = import.meta.env.VITE_LMS_BASE_URL;
  if (envUrl && typeof envUrl === "string" && envUrl.trim() !== "") {
    return envUrl.trim().replace(/\/+$/, "");
  }

  if (typeof window !== "undefined" && window.location?.hostname) {
    const hostname = window.location.hostname;
    if (hostname !== "localhost" && hostname !== "127.0.0.1") {
      return `${window.location.protocol}//${hostname}:9000`;
    }
  }

  return "http://localhost:9000";
};

/**
 * Lấy URL Cổng Sinh Viên LMS (Portal).
 * Thứ tự ưu tiên:
 * 1. window.__RUNTIME_CONFIG__?.VITE_LMS_PORTAL_URL
 * 2. import.meta.env.VITE_LMS_PORTAL_URL
 * 3. {LMS_BASE_URL}/lms/portal
 */
export const getLmsPortalUrl = (): string => {
  if (typeof window !== "undefined" && window.__RUNTIME_CONFIG__?.VITE_LMS_PORTAL_URL) {
    return window.__RUNTIME_CONFIG__.VITE_LMS_PORTAL_URL;
  }

  const envPortalUrl = import.meta.env.VITE_LMS_PORTAL_URL;
  if (envPortalUrl && typeof envPortalUrl === "string" && envPortalUrl.trim() !== "") {
    return envPortalUrl.trim();
  }

  return `${getLmsBaseUrl()}/lms/portal`;
};

/**
 * Lấy URL endpoint phát hành vé SSO của Mock Server.
 * Thứ tự ưu tiên:
 * 1. window.__RUNTIME_CONFIG__?.VITE_MOCK_API_URL
 * 2. import.meta.env.VITE_MOCK_API_URL
 * 3. import.meta.env.VITE_LMS_BASE_URL
 * 4. Nếu đang chạy trên máy chủ từ xa (VPS / Domain / LAN), trỏ trực tiếp đến cổng 9000 của host đó
 * 5. Mặc định dùng relative path '/lms/sso/tickets' qua Vite / Nginx proxy
 */
export const getMockTicketUrl = (): string => {
  const runtimeMockUrl = typeof window !== "undefined" ? window.__RUNTIME_CONFIG__?.VITE_MOCK_API_URL : undefined;
  if (runtimeMockUrl) {
    return `${runtimeMockUrl.replace(/\/+$/, "")}/lms/sso/tickets`;
  }

  const envMockUrl = import.meta.env.VITE_MOCK_API_URL;
  if (envMockUrl && typeof envMockUrl === "string" && envMockUrl.trim() !== "") {
    return `${envMockUrl.trim().replace(/\/+$/, "")}/lms/sso/tickets`;
  }

  const envLmsUrl = import.meta.env.VITE_LMS_BASE_URL;
  if (envLmsUrl && typeof envLmsUrl === "string" && envLmsUrl.trim() !== "") {
    return `${envLmsUrl.trim().replace(/\/+$/, "")}/lms/sso/tickets`;
  }

  if (typeof window !== "undefined" && window.location?.hostname) {
    const hostname = window.location.hostname;
    if (hostname !== "localhost" && hostname !== "127.0.0.1") {
      return `${window.location.protocol}//${hostname}:9000/lms/sso/tickets`;
    }
  }

  return "/lms/sso/tickets";
};
