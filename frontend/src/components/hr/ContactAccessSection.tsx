import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Mail, Phone, Lock, Unlock, AlertCircle, Eye } from "lucide-react";
import { Button } from "@/components/common/Button";
import { Modal } from "@/components/common/Modal";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/common/Card";
import { contactApi, type ContactResponse, type CreateContactRequestRequest } from "@/api/endpoints/contact.api";

interface ContactAccessSectionProps {
  candidateId: string;
  hasApplied: boolean;
  initialContactStatus: "locked" | "unlocked" | "pending";
}

export function ContactAccessSection({ candidateId, hasApplied, initialContactStatus }: ContactAccessSectionProps) {
  const queryClient = useQueryClient();
  const [showRequestModal, setShowRequestModal] = useState(false);
  const [showContactModal, setShowContactModal] = useState(false);
  const [contactData, setContactData] = useState<ContactResponse | null>(null);
  const [requestReason, setRequestReason] = useState("");

  const requestMutation = useMutation({
    mutationFn: (data: CreateContactRequestRequest) => contactApi.createContactRequest(candidateId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["candidate", candidateId] });
      setShowRequestModal(false);
      setRequestReason("");
    },
  });

  const viewContactMutation = useMutation({
    mutationFn: () => contactApi.getContact(candidateId),
    onSuccess: (res) => {
      setContactData(res.data);
      setShowContactModal(true);
    },
  });

  const handleRequestAccess = () => {
    if (hasApplied) {
      viewContactMutation.mutate();
    } else {
      setShowRequestModal(true);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Mail className="h-5 w-5" />
          Thông tin liên hệ
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {initialContactStatus === "unlocked" && contactData && (
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-muted" />
              <span className="text-sm">{contactData.email}</span>
            </div>
            <div className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-muted" />
              <span className="text-sm">{contactData.phone}</span>
            </div>
          </div>
        )}

        {initialContactStatus === "locked" && (
          <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
            <Lock className="h-5 w-5 text-muted flex-shrink-0" />
            <div className="flex-1">
              <p className="text-sm font-medium text-foreground">Liên hệ đã ẩn</p>
              <p className="text-xs text-muted">
                {hasApplied
                  ? "Ứng viên đã ứng tuyển. Nhấn Xem liên hệ để hiển thị."
                  : "Ứng viên chưa ứng tuyển công ty bạn. Cần xin mở liên hệ."}
              </p>
            </div>
            <Button
              variant={hasApplied ? "primary" : "outline"}
              size="sm"
              onClick={handleRequestAccess}
              isLoading={requestMutation.isPending || viewContactMutation.isPending}
              leftIcon={hasApplied ? <Eye className="h-3.5 w-3.5" /> : <Unlock className="h-3.5 w-3.5" />}
            >
              {hasApplied ? "Xem liên hệ" : "Xin mở liên hệ"}
            </Button>
          </div>
        )}

        {initialContactStatus === "pending" && (
          <div className="flex items-center gap-3 p-3 bg-amber-50 rounded-lg">
            <AlertCircle className="h-5 w-5 text-amber-600 flex-shrink-0" />
            <p className="text-sm text-amber-800">Yêu cầu mở liên hệ đang chờ duyệt</p>
          </div>
        )}

        {/* Request Access Modal */}
        <Modal isOpen={showRequestModal} onClose={() => setShowRequestModal(false)} title="Xin mở liên hệ" size="md">
          <div className="space-y-4">
            <p className="text-sm">Nhập lý do cần xem thông tin liên hệ:</p>
            <textarea
              value={requestReason}
              onChange={(e) => setRequestReason(e.target.value)}
              placeholder="Lý do xin mở liên hệ..."
              rows={3}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted transition-colors outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none"
            />
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowRequestModal(false)}>Hủy</Button>
              <Button variant="primary" onClick={() => requestMutation.mutate({ reason: requestReason })} isLoading={requestMutation.isPending} disabled={!requestReason.trim()}>
                <Mail className="h-3.5 w-3.5" /> Gửi yêu cầu
              </Button>
            </div>
          </div>
        </Modal>

        {/* View Contact Modal */}
        <Modal isOpen={showContactModal} onClose={() => setShowContactModal(false)} title="Thông tin liên hệ" size="md">
          {contactData && (
            <div className="space-y-3">
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <Mail className="h-5 w-5 text-muted" />
                <div>
                  <p className="text-xs text-muted">Email</p>
                  <p className="text-sm font-medium">{contactData.email}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
                <Phone className="h-5 w-5 text-muted" />
                <div>
                  <p className="text-xs text-muted">Số điện thoại</p>
                  <p className="text-sm font-medium">{contactData.phone}</p>
                </div>
              </div>
              <div className="flex justify-end">
                <Button variant="outline" onClick={() => setShowContactModal(false)}>Đóng</Button>
              </div>
            </div>
          )}
        </Modal>
      </CardContent>
    </Card>
  );
}