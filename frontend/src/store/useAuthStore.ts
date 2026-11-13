import { create } from "zustand";
import { setAccessToken, setCurrentUserId, registerUnauthorizedHandler } from "../api/client";
import { authApi, type Role, type UserProfile, type AuthResponseData } from "../api/endpoints/auth.api";

function normalizeRole(role?: string): Role {
  if (!role) return "CANDIDATE";
  return role.replace(/^ROLE_/, "").toUpperCase() as Role;
}

function normalizeUser(user: UserProfile): UserProfile {
  return {
    ...user,
    role: normalizeRole(user.role),
  };
}

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Real API Actions
  login: (email: string, password: string) => Promise<AuthResponseData>;
  ssoLogin: (ticket: string) => Promise<AuthResponseData>;
  fetchMe: () => Promise<void>;
  logout: () => void;
  updateUser: (partial: Partial<UserProfile>) => void;

  // Role Checks
  hasRole: (role: Role | Role[]) => boolean;
  isCandidate: () => boolean;
  isHr: () => boolean;
  isMentor: () => boolean;
  isStaff: () => boolean;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  isLoading: false,

  login: async (email, password) => {
    set({ isLoading: true });
    try {
      const res = await authApi.login({ email, password });
      const data = res.data;
      const token = data.accessToken;
      const user = normalizeUser(data.user);

      setAccessToken(token);
      setCurrentUserId(user.id);
      set({
        user,
        accessToken: token,
        isAuthenticated: true,
        isLoading: false,
      });

      return { ...data, user };
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  ssoLogin: async (ticket) => {
    set({ isLoading: true });
    try {
      const res = await authApi.ssoLogin({ ticket });
      const data = res.data;
      const token = data.accessToken;
      const user = normalizeUser(data.user);

      setAccessToken(token);
      setCurrentUserId(user.id);
      set({
        user,
        accessToken: token,
        isAuthenticated: true,
        isLoading: false,
      });

      return { ...data, user };
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  fetchMe: async () => {
    try {
      const res = await authApi.getMe();
      const user = normalizeUser(res.data);
      setCurrentUserId(user.id);
      set({ user, isAuthenticated: true });
    } catch {
      // Lỗi 401 được interceptor xử lý tự động
    }
  },

  logout: () => {
    setAccessToken(null);
    setCurrentUserId(null);
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  updateUser: (partial) => {
    const currentUser = get().user;
    if (!currentUser) return;
    set({ user: { ...currentUser, ...partial } });
  },

  hasRole: (role) => {
    const currentUser = get().user;
    if (!currentUser) return false;
    const current = normalizeRole(currentUser.role);
    if (Array.isArray(role)) {
      return role.some((r) => normalizeRole(r) === current);
    }
    return normalizeRole(role) === current;
  },

  isCandidate: () => normalizeRole(get().user?.role) === "CANDIDATE",
  isHr: () => normalizeRole(get().user?.role) === "HR",
  isMentor: () => normalizeRole(get().user?.role) === "MENTOR",
  isStaff: () => normalizeRole(get().user?.role) === "STAFF",
  isAdmin: () => normalizeRole(get().user?.role) === "ADMIN",
}));

// Đăng ký tự động logout và chuyển hướng về /login khi nhận mã HTTP 401 từ backend/gateway
registerUnauthorizedHandler(() => {
  useAuthStore.getState().logout();
  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
});
