import { api } from "../client";
import type { PaginatedData } from "../types/api";

export interface SyncCounts {
  lms?: number;
  exam?: number;
  pm?: number;
  total?: number;
  candidates?: number;
}

export interface SyncRun {
  id: string;
  trigger: "NIGHTLY" | "ON_OPEN" | "MANUAL" | string;
  userId?: string | null;
  status: "RUNNING" | "SUCCESS" | "FAILED" | string;
  startedAt: string;
  finishedAt?: string | null;
  error?: string | null;
  counts?: SyncCounts | null;
}

export interface ManualOverrideItem {
  id: string;
  recordId: string;
  field: string;
  value: string;
  reason: string;
  staffId: string;
  createdAt: string;
}

export interface ExternalRecordDetail {
  id: string;
  userId: string;
  source: "LMS" | "EXAM" | "PM" | string;
  sourceKey: string;
  payload: Record<string, any>;
  syncedAt: string;
  isOverridden?: boolean;
  overridden?: boolean;
  overrides?: ManualOverrideItem[];
}

export interface SyncRunsParams {
  status?: string;
  userId?: string;
  page?: number;
  size?: number;
}

export interface ExternalRecordsParams {
  userId?: string;
  source?: string;
}

export interface OverridePayload {
  field: string;
  value: string;
  reason: string;
}

export const syncApi = {
  /**
   * Đồng bộ dữ liệu lại cho 1 ứng viên
   */
  syncCandidate: (userId: string) => {
    return api.post<SyncRun>(`/staff/sync/${userId}`);
  },

  /**
   * Chạy thủ công Job đêm cho toàn bộ ứng viên
   */
  triggerNightlySync: () => {
    return api.post<SyncRun>("/staff/sync/trigger-nightly");
  },

  /**
   * Lấy lịch sử các lần chạy đồng bộ
   */
  getSyncRuns: (params?: SyncRunsParams) => {
    return api.get<PaginatedData<SyncRun>>("/staff/sync-runs", {
      params,
    });
  },

  /**
   * Lấy danh sách bản ghi nguồn ngoài của ứng viên
   */
  getExternalRecords: (params?: ExternalRecordsParams) => {
    return api.get<ExternalRecordDetail[]>("/staff/external-records", {
      params,
    });
  },

  /**
   * Cán bộ can thiệp sửa tay một trường bản ghi ngoài
   */
  overrideRecordField: (recordId: string, payload: OverridePayload) => {
    return api.put<ManualOverrideItem>(
      `/staff/external-records/${recordId}/override`,
      payload
    );
  },
};
