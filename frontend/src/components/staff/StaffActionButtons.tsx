import { useState } from "react";
import { Check, X, Edit2, Loader2, AlertCircle, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/common/Button";
import { Modal } from "@/components/common/Modal";
import { Input } from "@/components/common/Input";
import type { StaffEditSelfDeclaredRequest } from "@/api/endpoints/profile.api";

interface StaffActionButtonsProps {
  currentVisibility: string;
  onApprove: () => void;
  onReject: (reason: string) => void;
  onEditSelfDeclared: (data: StaffEditSelfDeclaredRequest) => void;
  isApproving?: boolean;
  isRejecting?: boolean;
  isEditing?: boolean;
  onNextProfile?: () => void;
}

const FIELD_OPTIONS = [
  { value: "HEADLINE", label: "Tiêu đề hồ sơ (Headline)" },
  { value: "SUMMARY", label: "Giới thiệu bản thân (Summary)" },
  { value: "SELF_SKILLS", label: "Kỹ năng tự khai (Self Skills - JSON)" },
] as const;

export function StaffActionButtons({
  currentVisibility,
  onApprove,
  onReject,
  onEditSelfDeclared,
  isApproving = false,
  isRejecting = false,
  isEditing = false,
  onNextProfile,
}: StaffActionButtonsProps) {
  const [rejectReason, setRejectReason] = useState("");
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [editField, setEditField] = useState<"HEADLINE" | "SUMMARY" | "SELF_SKILLS">("HEADLINE");
  const [editValue, setEditValue] = useState("");
  const [editReason, setEditReason] = useState("");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const canApprove = currentVisibility === "PENDING";
  const canReject = currentVisibility === "PENDING";
  const canEdit = currentVisibility === "PENDING" || currentVisibility === "PUBLIC";

  const handleRejectSubmit = () => {
    if (!rejectReason.trim()) return;
    onReject(rejectReason.trim());
    setRejectReason("");
    setIsRejectModalOpen(false);
    if (onNextProfile) {
      setTimeout(onNextProfile, 500);
    }
  };

  const handleEditSubmit = () => {
    if (!editValue.trim() || !editReason.trim()) return;
    onEditSelfDeclared({ field: editField, value: editValue.trim(), reason: editReason.trim() });
    setEditValue("");
    setEditReason("");
    setIsEditModalOpen(false);
  };

  const openRejectModal = () => {
    setRejectReason("");
    setIsRejectModalOpen(true);
  };

  const openEditModal = (field: "HEADLINE" | "SUMMARY" | "SELF_SKILLS" = "HEADLINE") => {
    setEditField(field);
    setEditValue("");
    setEditReason("");
    setIsEditModalOpen(true);
  };

  return (
    <div className="flex flex-col sm:flex-row gap-3 p-4 bg-slate-50 rounded-xl border border-border-subtle">
      {/* Approve Button */}
      <Button
        variant="primary"
        size="lg"
        className="flex-1 flex items-center justify-center gap-2"
        onClick={() => {
          onApprove();
          if (onNextProfile) {
            setTimeout(onNextProfile, 500);
          }
        }}
        disabled={!canApprove || isApproving || isRejecting || isEditing}
        isLoading={isApproving}
        leftIcon={isApproving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
      >
        Duyệt hồ sơ
      </Button>

      {/* Reject Button */}
      <Button
        variant="danger"
        size="lg"
        className="flex-1 flex items-center justify-center gap-2"
        onClick={openRejectModal}
        disabled={!canReject || isApproving || isRejecting || isEditing}
        isLoading={isRejecting}
        leftIcon={<X className="h-4 w-4" />}
      >
        Trả lại
      </Button>

      {/* Edit Self-Declared Button */}
      {canEdit && (
        <Button
          variant="outline"
          size="lg"
          className="flex-1 flex items-center justify-center gap-2 border-info text-info hover:bg-info/10"
          onClick={() => openEditModal("HEADLINE")}
          disabled={isApproving || isRejecting || isEditing}
          isLoading={isEditing}
          leftIcon={<Edit2 className="h-4 w-4" />}
        >
          Sửa tự khai
        </Button>
      )}

      {/* Reject Modal */}
      <Modal
        isOpen={isRejectModalOpen}
        onClose={() => setIsRejectModalOpen(false)}
        title="Trả lại hồ sơ"
        size="md"
      >
        <div className="space-y-4">
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg">
            <p className="flex items-center gap-2 text-xs text-rose-800">
              <AlertCircle className="h-3.5 w-3.5 text-rose-600" />
              <span>Hành động này sẽ chuyển hồ sơ về trạng thái <strong>REJECTED</strong>. Ứng viên sẽ được thông báo và có thể gửi duyệt lại sau khi chỉnh sửa.</span>
            </p>
          </div>
          <div className="space-y-2">
            <label htmlFor="reject-reason" className="block text-sm font-medium text-foreground">
              Lý do trả lại <span className="text-danger">*</span>
            </label>
            <textarea
              id="reject-reason"
              rows={4}
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Nhập lý do chi tiết để ứng viên biết cần cải thiện gì..."
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none"
            />
            <p className="text-xs text-muted">{rejectReason.length}/500 ký tự</p>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={() => setIsRejectModalOpen(false)} disabled={isRejecting}>
              Hủy
            </Button>
            <Button
              variant="danger"
              onClick={handleRejectSubmit}
              disabled={!rejectReason.trim() || isRejecting}
              isLoading={isRejecting}
              leftIcon={<X className="h-4 w-4" />}
            >
              Xác nhận trả lại
            </Button>
          </div>
        </div>
      </Modal>

      {/* Edit Self-Declared Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Sửa phần tự khai"
        size="md"
      >
        <div className="space-y-4">
          <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="flex items-center gap-2 text-xs text-blue-800">
              <CheckCircle2 className="h-3.5 w-3.5 text-blue-600" />
              <span>Chỉ được sửa các phần <strong>tự khai</strong>: Headline, Summary, Self Skills. Các thông tin đã xác thực (LMS, Exam, PM) không thể thay đổi.</span>
            </p>
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-medium text-foreground">
              Trường cần sửa <span className="text-danger">*</span>
            </label>
            <select
              value={editField}
              onChange={(e) => setEditField(e.target.value as "HEADLINE" | "SUMMARY" | "SELF_SKILLS")}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            >
              {FIELD_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-medium text-foreground">
              Giá trị mới <span className="text-danger">*</span>
            </label>
            {editField === "SELF_SKILLS" ? (
              <textarea
                rows={6}
                value={editValue}
                onChange={(e) => setEditValue(e.target.value)}
                placeholder='[{"skillId": "uuid", "selfScore": 80}, {"skillId": "uuid", "selfScore": 75}]'
                className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm font-mono text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none"
              />
            ) : (
              <Input
                value={editValue}
                onChange={(e) => setEditValue(e.target.value)}
                placeholder={editField === "HEADLINE" ? "Ví dụ: Java Backend Developer" : "Mô tả ngắn về kinh nghiệm, thế mạnh..."}
                className="font-mono"
              />
            )}
            <p className="text-xs text-muted">
              {editField === "SELF_SKILLS"
                ? "Định dạng JSON array: [{skillId, selfScore}, ...]"
                : "Tối đa 500 ký tự"}
            </p>
          </div>

          <div className="space-y-2">
            <label htmlFor="edit-reason" className="block text-sm font-medium text-foreground">
              Lý do sửa <span className="text-danger">*</span>
            </label>
            <textarea
              id="edit-reason"
              rows={3}
              value={editReason}
              onChange={(e) => setEditReason(e.target.value)}
              placeholder="Nhập lý do chỉnh sửa (ví dụ: cập nhật thông tin mới từ LMS, sửa lỗi chính tả...)"
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={() => setIsEditModalOpen(false)} disabled={isEditing}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={handleEditSubmit}
              disabled={!editValue.trim() || !editReason.trim() || isEditing}
              isLoading={isEditing}
              leftIcon={<Edit2 className="h-4 w-4" />}
            >
              Lưu thay đổi
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}