import axios from "axios";
import { getAccessToken, getCurrentUserId, getCurrentUserRole } from "../client";
import type { ApiResponse } from "../types/api";

export type ConversationMode = "CAREER" | "INTERVIEW";

export interface ConversationItem {
  id: string;
  title: string;
  mode: ConversationMode;
  jobId?: string | null;
  practiceId?: string | null;
  createdAt: string;
}

export interface ChatMessageItem {
  id: string;
  conversationId: string;
  role: "USER" | "ASSISTANT";
  content: string;
  tokens: number;
  createdAt: string;
}

export interface AiQuota {
  used: number;
  limit: number;
  remaining: number;
  resetAt: string;
}

export interface CreateConversationPayload {
  mode: ConversationMode;
  jobId?: string;
  title?: string;
}

export interface SendMessageResponse {
  userMessage: ChatMessageItem;
  assistantMessage: ChatMessageItem;
  quota: AiQuota;
}

const AI_CAREER_BASE = import.meta.env.VITE_AI_CAREER_BASE_URL || "/ai-career";

const chatAxios = axios.create({
  baseURL: `${AI_CAREER_BASE}/me/conversations`,
  timeout: 45000,
  headers: {
    "Content-Type": "application/json",
  },
});

chatAxios.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const userId = getCurrentUserId();
  if (userId) {
    config.headers["X-User-Id"] = userId;
  }
  const role = getCurrentUserRole();
  if (role) {
    config.headers["X-User-Role"] = role;
  }
  return config;
});

chatAxios.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
);

export const careerChatApi = {
  listConversations: async (): Promise<ApiResponse<ConversationItem[]>> => {
    return chatAxios.get("");
  },

  createConversation: async (
    payload: CreateConversationPayload
  ): Promise<ApiResponse<ConversationItem>> => {
    return chatAxios.post("", payload);
  },

  getMessages: async (
    conversationId: string
  ): Promise<ApiResponse<ChatMessageItem[]>> => {
    return chatAxios.get(`/${conversationId}/messages`);
  },

  getQuota: async (): Promise<ApiResponse<AiQuota>> => {
    return chatAxios.get("/quota");
  },

  deleteConversation: async (
    conversationId: string
  ): Promise<ApiResponse<void>> => {
    return chatAxios.delete(`/${conversationId}`);
  },

  streamMessage: async (
    conversationId: string,
    content: string,
    handlers: {
      onUserMessage?: (msg: ChatMessageItem) => void;
      onDelta: (chunk: string) => void;
      onDone: (payload: SendMessageResponse) => void;
      onQuotaExceeded?: (message: string, quota?: AiQuota) => void;
      onError: (message: string) => void;
    },
    signal?: AbortSignal
  ): Promise<void> => {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    };
    const token = getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;
    const userId = getCurrentUserId();
    if (userId) headers["X-User-Id"] = userId;
    const role = getCurrentUserRole();
    if (role) headers["X-User-Role"] = role;

    const response = await fetch(
      `${AI_CAREER_BASE}/me/conversations/${conversationId}/messages`,
      {
        method: "POST",
        headers,
        body: JSON.stringify({ content }),
        signal,
      }
    );

    if (response.status === 429) {
      let msg = "Bạn đã hết lượt sử dụng AI hôm nay.";
      let quota: AiQuota | undefined;
      try {
        const body = (await response.json()) as ApiResponse<AiQuota>;
        if (body.message) msg = body.message;
        if (body.data) quota = body.data;
      } catch {
        // ignore parse error
      }
      handlers.onQuotaExceeded?.(msg, quota);
      return;
    }

    if (!response.ok || !response.body) {
      let msg = `Yêu cầu thất bại (HTTP ${response.status})`;
      try {
        const body = (await response.json()) as { message?: string };
        if (body.message) msg = body.message;
      } catch {
        // ignore
      }
      handlers.onError(msg);
      return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let receivedDone = false;

    const processEventBlock = (block: string) => {
      const lines = block.split(/\r?\n/);
      let eventName = "message";
      const dataLines: string[] = [];

      for (const line of lines) {
        if (line.startsWith("event:")) {
          eventName = line.slice(6).trim();
        } else if (line.startsWith("data:")) {
          dataLines.push(line.slice(5).trim());
        }
      }

      if (dataLines.length === 0) return;
      const rawData = dataLines.join("\n");

      try {
        const parsed = JSON.parse(rawData);
        if (eventName === "user_message") {
          handlers.onUserMessage?.(parsed as ChatMessageItem);
        } else if (eventName === "delta" && typeof parsed.content === "string") {
          handlers.onDelta(parsed.content);
        } else if (eventName === "done") {
          receivedDone = true;
          handlers.onDone(parsed as SendMessageResponse);
        } else if (eventName === "error") {
          receivedDone = true;
          handlers.onError(parsed.message || "Lỗi khi tạo phản hồi AI.");
        }
      } catch {
        // ignore malformed chunk
      }
    };

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer = (buffer + decoder.decode(value, { stream: true })).replace(/\r\n/g, "\n");

      let sepIndex: number;
      while ((sepIndex = buffer.indexOf("\n\n")) !== -1) {
        const block = buffer.slice(0, sepIndex).trim();
        buffer = buffer.slice(sepIndex + 2);
        if (block) {
          processEventBlock(block);
        }
      }
    }

    if (buffer.trim()) {
      processEventBlock(buffer.trim());
    }

    if (!receivedDone) {
      handlers.onError("Luồng phản hồi bị gián đoạn trước khi hoàn tất.");
    }
  },
};
