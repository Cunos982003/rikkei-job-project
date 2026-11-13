import { api } from "../client";
import type { ApiResponse } from "../types/api";

export type Role = "CANDIDATE" | "HR" | "MENTOR" | "STAFF" | "ADMIN";

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  avatarUrl?: string;
  companyId?: string | number;
  mustChangePassword?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SsoLoginRequest {
  ticket: string;
}

export interface AuthResponseData {
  accessToken: string;
  tokenType?: string;
  expiresIn?: number;
  user: UserProfile;
}

export interface DemoAccount {
  role: Role;
  email: string;
  label: string;
}

export const DEMO_ACCOUNTS: DemoAccount[] = [
  { role: "CANDIDATE", email: "candidate1@rikkeijobs.test", label: "Ứng viên (Nguyễn Văn An)" },
  { role: "HR", email: "hr1@rikkeijobs.test", label: "Doanh nghiệp HR (Rikkeisoft)" },
  { role: "MENTOR", email: "mentor1@rikkeijobs.test", label: "Mentor (Phạm Minh Đức)" },
  { role: "STAFF", email: "staff1@rikkeijobs.test", label: "Cán bộ tuyển dụng" },
  { role: "ADMIN", email: "admin1@rikkeijobs.test", label: "Quản trị hệ thống" },
];

/**
 * Endpoints xác thực người dùng (auth-service)
 */
export const authApi = {
  /**
   * Lấy danh sách tài khoản demo phục vụ kiểm thử nhanh
   */
  getDemoAccounts(): Promise<ApiResponse<DemoAccount[]>> {
    return api.get<DemoAccount[]>("/auth/demo-accounts", { silentError: true });
  },

  /**
   * Đăng nhập HR / Cán bộ bằng email & mật khẩu
   */
  login(payload: LoginRequest): Promise<ApiResponse<AuthResponseData>> {
    return api.post<AuthResponseData>("/auth/login", payload);
  },

  /**
   * Đăng nhập SSO bằng vé dùng một lần từ Rikkei LMS
   */
  ssoLogin(payload: SsoLoginRequest): Promise<ApiResponse<AuthResponseData>> {
    return api.post<AuthResponseData>("/auth/sso", payload);
  },

  /**
   * Lấy thông tin người dùng đang đăng nhập
   */
  getMe(): Promise<ApiResponse<UserProfile>> {
    return api.get<UserProfile>("/auth/me");
  },

  /**
   * Quên mật khẩu (gửi link reset qua email)
   */
  forgotPassword(email: string): Promise<ApiResponse<null>> {
    return api.post<null>("/auth/forgot-password", { email });
  },

  /**
   * Đổi mật khẩu
   */
  changePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<null>> {
    return api.post<null>("/auth/change-password", { currentPassword, newPassword });
  },
};

