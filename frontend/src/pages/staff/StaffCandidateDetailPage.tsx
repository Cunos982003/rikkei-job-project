import { useState, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ChevronLeft,
  CheckCircle2,
  XCircle,
  AlertCircle,
  User,
  Building2,
  Award,
  Sparkles,
  Eye,
  RefreshCw,
  ArrowLeft,
  ArrowRight,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/common/Button";
import { Badge } from "@/components/common/Badge";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/common/Card";
import { Skeleton } from "@/components/feedback/Skeleton";
import { StaffCandidatePreview } from "@/components/staff/StaffCandidatePreview";
import { StaffActionButtons } from "@/components/staff/StaffActionButtons";
import { profileApi, type StaffPreviewProfile, type StaffPendingReview } from "@/api/endpoints/profile.api";
import { useStaffProfileReviews } from "@/features/profile/hooks/useStaffProfileReviews";
import type { StaffEditSelfDeclaredRequest } from "@/api/endpoints/profile.api";

export function StaffCandidateDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Guard: if userId is not provided, redirect to list
  const effectiveUserId = userId ?? "";

  // Get the list of pending reviews to enable auto-advance
  const { data: reviewsResponse } = useStaffProfileReviews({
    status: "PENDING",
    page: 0,
    size: 100, // Get a larger list for navigation
  });

  const pendingCandidates = reviewsResponse?.data?.content ?? [];

  // Find current index and next/prev candidates
  const currentIndex = useMemo(() =>
    pendingCandidates.findIndex((c: StaffPendingReview) => c.userId === effectiveUserId), [pendingCandidates, effectiveUserId]);

  const nextCandidate = currentIndex >= 0 && currentIndex < pendingCandidates.length - 1
    ? pendingCandidates[currentIndex + 1]
    : null;

  const prevCandidate = currentIndex > 0
    ? pendingCandidates[currentIndex - 1]
    : null;

  // State for actions
  const [isApproving, setIsApproving] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Fetch preview profile
  const {
    data: previewResponse,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["staff", "preview-profile", effectiveUserId],
    queryFn: async () => {
      if (!effectiveUserId) throw new Error("User ID không hợp lệ");
      return (await profileApi.getStaffPreviewProfile(effectiveUserId)).data;
    },
    enabled: Boolean(effectiveUserId),
    staleTime: 30 * 1000,
  });

  const profile: StaffPreviewProfile | undefined = previewResponse;

  // Mutations
  const approveMutation = useMutation({
    mutationFn: (userId: string) => profileApi.approveProfile(userId),
    onMutate: () => { setIsApproving(true); },
    onSuccess: (_, userId) => {
      toast.success("Đã duyệt hồ sơ thành công");
      queryClient.invalidateQueries({ queryKey: ["staff", "profile-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["staff", "preview-profile", userId] });
      // Auto-advance to next candidate
      if (nextCandidate) {
        setTimeout(() => navigate(`/staff/candidates/${nextCandidate.userId}`), 500);
      } else {
        setTimeout(() => navigate("/staff/candidates"), 500);
      }
    },
    onError: (err) => {
      toast.error("Duyệt hồ sơ thất bại: " + (err as Error).message);
    },
    onSettled: () => { setIsApproving(false); },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason: string }) =>
      profileApi.rejectProfile(userId, reason),
    onMutate: () => { setIsRejecting(true); },
    onSuccess: (_, { userId }) => {
      toast.success("Đã trả lại hồ sơ");
      queryClient.invalidateQueries({ queryKey: ["staff", "profile-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["staff", "preview-profile", userId] });
      // Auto-advance to next candidate
      if (nextCandidate) {
        setTimeout(() => navigate(`/staff/candidates/${nextCandidate.userId}`), 500);
      } else {
        setTimeout(() => navigate("/staff/candidates"), 500);
      }
    },
    onError: (err) => {
      toast.error("Trả lại hồ sơ thất bại: " + (err as Error).message);
    },
    onSettled: () => { setIsRejecting(false); },
  });

  const editMutation = useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: StaffEditSelfDeclaredRequest }) =>
      profileApi.editStaffSelfDeclared(userId, data),
    onMutate: () => { setIsEditing(true); },
    onSuccess: (_, { userId }) => {
      toast.success("Đã cập nhật phần tự khai");
      queryClient.invalidateQueries({ queryKey: ["staff", "preview-profile", userId] });
    },
    onError: (err) => {
      toast.error("Cập nhật thất bại: " + (err as Error).message);
    },
    onSettled: () => { setIsEditing(false); },
  });

  // Handlers
  const handleApprove = () => {
    approveMutation.mutate(effectiveUserId);
  };

  const handleReject = (reason: string) => {
    rejectMutation.mutate({ userId: effectiveUserId, reason });
  };

  const handleEditSelfDeclared = (data: StaffEditSelfDeclaredRequest) => {
    editMutation.mutate({ userId: effectiveUserId, data });
  };

  const formatDateTime = (dateStr?: string): string => {
    if (!dateStr) return "—";
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return dateStr;
    return d.toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<ChevronLeft className="h-4 w-4" />}
            onClick={() => navigate("/staff/candidates")}
          >
            Quay lại danh sách
          </Button>
          <Skeleton className="h-9 w-64" />
        </div>
        <Card>
          <CardContent className="p-6 space-y-4">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <Button
          variant="outline"
          size="sm"
          leftIcon={<ChevronLeft className="h-4 w-4" />}
          onClick={() => navigate("/staff/candidates")}
        >
          Quay lại danh sách
        </Button>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12 text-center">
            <div className="rounded-full bg-danger-light p-3 text-danger mb-3">
              <AlertCircle className="h-6 w-6" />
            </div>
            <h2 className="text-base font-semibold text-foreground">Không tìm thấy hồ sơ</h2>
            <p className="mt-1 text-sm text-muted max-w-md">
              Hồ sơ không tồn tại hoặc đã bị xóa khỏi hệ thống.
            </p>
            <Button
              variant="outline"
              size="sm"
              className="mt-4"
              leftIcon={<RefreshCw className="h-4 w-4" />}
              onClick={() => void refetch()}
            >
              Thử lại
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const getStatusBadge = () => {
    switch (profile.visibility) {
      case "PUBLIC":
        return <Badge variant="verified" size="md"><CheckCircle2 className="h-3 w-3" /> Đã duyệt (PUBLIC)</Badge>;
      case "PENDING":
        return <Badge variant="warning" size="md"><Sparkles className="h-3 w-3" /> Chờ duyệt (PENDING)</Badge>;
      case "REJECTED":
        return <Badge variant="danger" size="md"><XCircle className="h-3 w-3" /> Đã trả lại (REJECTED)</Badge>;
      case "PRIVATE":
        return <Badge variant="default" size="md">Riêng tư (PRIVATE)</Badge>;
      case "RECRUITER":
        return <Badge variant="outline" size="md">Chỉ doanh nghiệp (RECRUITER)</Badge>;
      default:
        return <Badge variant="default" size="md">{profile.visibility}</Badge>;
    }
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Header with back button and status */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<ChevronLeft className="h-4 w-4" />}
            onClick={() => navigate("/staff/candidates")}
          >
            Quay lại danh sách
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{profile.fullName || "Ứng viên"}</h1>
            <p className="mt-1 text-sm text-muted flex items-center gap-2">
              <span className="font-mono">{profile.userId}</span>
              {profile.classCode && (
                <>
                  <span>•</span>
                  <Badge variant="outline" size="sm" className="gap-1">
                    <Building2 className="h-2.5 w-2.5" />
                    {profile.classCode}
                  </Badge>
                </>
              )}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {getStatusBadge()}
        </div>
      </div>

      {/* Navigation between candidates (if in queue) */}
      {(prevCandidate || nextCandidate) && (
        <Card className="border-info/30">
          <CardContent className="p-3">
            <div className="flex items-center justify-between">
              <Button
                variant="outline"
                size="sm"
                leftIcon={<ArrowLeft className="h-4 w-4" />}
                disabled={!prevCandidate}
                onClick={prevCandidate ? () => navigate(`/staff/candidates/${prevCandidate.userId}`) : undefined}
              >
                Hồ sơ trước
              </Button>
              <div className="flex items-center gap-4 text-sm text-muted">
                {prevCandidate && (
                  <span className="flex items-center gap-1">
                    <User className="h-3.5 w-3.5" />
                    {prevCandidate.fullName}
                  </span>
                )}
                <span className="font-medium text-foreground">
                  {profile.fullName || "Ứng viên"}
                </span>
                {nextCandidate && (
                  <span className="flex items-center gap-1">
                    <User className="h-3.5 w-3.5" />
                    {nextCandidate.fullName}
                  </span>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                rightIcon={<ArrowRight className="h-4 w-4" />}
                disabled={!nextCandidate}
                onClick={nextCandidate ? () => navigate(`/staff/candidates/${nextCandidate.userId}`) : undefined}
              >
                Hồ sơ sau
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Action Bar - Fixed at top for easy access */}
      <Card className="sticky top-4 z-10 shadow-sm">
        <CardContent className="p-4">
          <StaffActionButtons
            currentVisibility={profile.visibility ?? "UNKNOWN"}
            onApprove={handleApprove}
            onReject={handleReject}
            onEditSelfDeclared={handleEditSelfDeclared}
            isApproving={isApproving}
            isRejecting={isRejecting}
            isEditing={isEditing}
            onNextProfile={() => {
              if (nextCandidate) {
                navigate(`/staff/candidates/${nextCandidate.userId}`);
              } else {
                navigate("/staff/candidates");
              }
            }}
          />
        </CardContent>
      </Card>

      {/* Profile Preview */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Eye className="h-5 w-5 text-primary" />
            Xem trước hồ sơ (chế độ nhân sự)
          </CardTitle>
          <CardDescription>
            Thông tin liên hệ (email, điện thoại) đã được che giấu theo quy định bảo mật.
            Chỉ hiển thị đầy đủ khi hồ sơ chuyển sang trạng thái PUBLIC hoặc RECRUITER.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <StaffCandidatePreview profile={profile} isLoading={false} />
        </CardContent>
      </Card>

      {/* Additional Info Cards */}
      <div className="grid gap-4 md:grid-cols-2">
        {/* Basic Info */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5 text-info" />
              Thông tin cơ bản
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Chương trình</label>
                <p className="text-foreground">{profile.program || "—"}</p>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Khóa học</label>
                <p className="text-foreground">{profile.courseName || "—"}</p>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Mã lớp</label>
                <p className="text-foreground">{profile.classCode || "—"}</p>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Email (đã che)</label>
                <p className="font-mono text-sm text-foreground">{profile.maskedEmail || profile.email || "—"}</p>
              </div>
              <div className="space-y-1 sm:col-span-2">
                <label className="text-xs font-medium text-muted">Điện thoại (đã che)</label>
                <p className="font-mono text-sm text-foreground">{profile.maskedPhone || profile.phone || "—"}</p>
              </div>
              <div className="space-y-1 sm:col-span-2">
                <label className="text-xs font-medium text-muted">Tiêu đề hồ sơ</label>
                <p className="text-foreground">{profile.headline || "—"}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Visibility & Timestamps */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Award className="h-5 w-5 text-warning" />
              Trạng thái & Thời gian
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Trạng thái hiện tại</label>
                <div className="flex items-center gap-2">
                  {getStatusBadge()}
                </div>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Số lần bị trả lại</label>
                <p className="text-foreground font-medium">{profile.rejectionCount ?? 0}</p>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Ngày gửi duyệt</label>
                <p className="text-foreground">—</p>
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium text-muted">Ngày cập nhật</label>
                <p className="text-foreground">{formatDateTime(profile.updatedAt)}</p>
              </div>
              {profile.consentAt && (
                <div className="space-y-1">
                  <label className="text-xs font-medium text-muted">Ngày đồng ý hiển thị</label>
                  <p className="text-success flex items-center gap-1">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {formatDateTime(profile.consentAt)}
                  </p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}