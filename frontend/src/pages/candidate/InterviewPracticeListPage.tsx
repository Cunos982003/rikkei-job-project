import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  MessageSquare,
  Plus,
  Calendar,
  CheckCircle2,
  Clock,
  Briefcase,
  ChevronRight,
  Sparkles,
  Trash2,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { jobsApi, type JobPostingItem } from "../../api/endpoints/jobs.api";
import {
  useInterviewPracticeList,
  useCreatePractice,
  useDeletePractice,
} from "../../features/interview-practice/hooks/useInterviewPractice";
import type { InterviewPracticeItem } from "../../api/endpoints/interviewPractice.api";
import { Card, CardContent } from "../../components/common/Card";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";
import { Modal } from "../../components/common/Modal";

export const InterviewPracticeListPage: React.FC = () => {
  const navigate = useNavigate();
  const [isSelectJobOpen, setIsSelectJobOpen] = useState(false);
  const [selectedJobId, setSelectedJobId] = useState<string>("");

  const { data: practices, isLoading: isPracticesLoading } = useInterviewPracticeList();
  const createPracticeMutation = useCreatePractice();
  const deletePracticeMutation = useDeletePractice();

  const { data: jobsRes, isLoading: isJobsLoading } = useQuery({
    queryKey: ["approvedJobsForInterview"],
    queryFn: async () => {
      try {
        return await jobsApi.getJobs({ status: "APPROVED" });
      } catch {
        return null;
      }
    },
    enabled: isSelectJobOpen,
  });

  const jobs: JobPostingItem[] = React.useMemo(() => {
    if (jobsRes?.data) {
      if (Array.isArray(jobsRes.data)) return jobsRes.data;
      if ("items" in jobsRes.data && Array.isArray(jobsRes.data.items)) return jobsRes.data.items;
    }
    return [];
  }, [jobsRes]);

  const handleStartPractice = () => {
    if (!selectedJobId) return;
    createPracticeMutation.mutate(
      { jobId: selectedJobId },
      {
        onSuccess: (res) => {
          setIsSelectJobOpen(false);
          setSelectedJobId("");
          if (res?.data?.id) {
            navigate(`/candidate/career/interviews/${res.data.id}`);
          }
        },
      }
    );
  };

  const getScoreBadge = (score?: number | null) => {
    if (score === null || score === undefined) return null;
    if (score >= 8) {
      return (
        <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
          {score} / 10
        </span>
      );
    }
    if (score >= 5) {
      return (
        <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200">
          {score} / 10
        </span>
      );
    }
    return (
      <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200">
        {score} / 10
      </span>
    );
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-lg bg-primary-light text-primary">
              <MessageSquare className="h-6 w-6" />
            </span>
            <div>
              <h1 className="text-2xl font-bold text-foreground">
                Luyện Phỏng Vấn Với AI
              </h1>
              <p className="text-xs text-muted mt-0.5">
                Mô phỏng phỏng vấn thực chiến theo JD, nhận điểm và nhận xét chi tiết từng câu từ AI
              </p>
            </div>
          </div>
        </div>

        <div>
          <Button
            variant="primary"
            size="sm"
            onClick={() => setIsSelectJobOpen(true)}
            leftIcon={<Plus className="h-4 w-4" />}
          >
            Bắt đầu buổi luyện mới
          </Button>
        </div>
      </div>

      {/* List of practices */}
      {isPracticesLoading ? (
        <div className="space-y-4 animate-pulse">
          <div className="h-28 bg-slate-200/80 rounded-xl" />
          <div className="h-28 bg-slate-200/80 rounded-xl" />
        </div>
      ) : !practices || practices.length === 0 ? (
        <Card className="border-dashed border-2 border-border p-10 text-center">
          <div className="inline-flex p-3 rounded-full bg-primary-light text-primary mb-3">
            <Sparkles className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-semibold text-foreground">
            Chưa có buổi luyện phỏng vấn nào
          </h3>
          <p className="text-xs text-muted mt-1 max-w-sm mx-auto">
            Hãy chọn một tin tuyển dụng (JD) phù hợp để AI sinh 5 câu hỏi phỏng vấn và bắt đầu luyện tập ngay!
          </p>
          <div className="mt-4">
            <Button
              variant="primary"
              size="sm"
              onClick={() => setIsSelectJobOpen(true)}
              leftIcon={<Plus className="h-4 w-4" />}
            >
              Tạo phiên đầu tiên
            </Button>
          </div>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {practices.map((item: InterviewPracticeItem) => (
            <Card
              key={item.id}
              hoverable
              className="border-border cursor-pointer transition-all"
              onClick={() => navigate(`/candidate/career/interviews/${item.id}`)}
            >
              <CardContent className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-2 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <Badge variant={item.status === "DONE" ? "verified" : "default"}>
                      {item.status === "DONE" ? (
                        <span className="flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600" /> Hoàn thành
                        </span>
                      ) : (
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3 text-amber-600" /> Đang luyện tập
                        </span>
                      )}
                    </Badge>
                    {getScoreBadge(item.overallScore)}
                    <span className="text-[11px] text-muted flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {new Date(item.createdAt).toLocaleDateString("vi-VN")}
                    </span>
                  </div>

                  <h3 className="text-base font-bold text-foreground hover:text-primary transition-colors">
                    {item.jobTitle}
                  </h3>

                  {item.summary && (
                    <p className="text-xs text-muted line-clamp-1 italic">
                      &quot;{item.summary}&quot;
                    </p>
                  )}
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <Button
                    variant="outline"
                    size="sm"
                    rightIcon={<ChevronRight className="w-4 h-4" />}
                  >
                    {item.status === "DONE" ? "Xem kết quả" : "Tiếp tục làm"}
                  </Button>
                  <button
                    type="button"
                    title="Xóa buổi luyện phỏng vấn"
                    aria-label="Xóa buổi luyện phỏng vấn"
                    disabled={deletePracticeMutation.isPending}
                    onClick={(e) => {
                      e.stopPropagation();
                      if (window.confirm(`Bạn có chắc muốn xóa buổi luyện phỏng vấn "${item.jobTitle}"?`)) {
                        deletePracticeMutation.mutate(item.id);
                      }
                    }}
                    className="rounded-lg p-2 text-muted transition-colors hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Modal Choose JD */}
      <Modal
        isOpen={isSelectJobOpen}
        onClose={() => setIsSelectJobOpen(false)}
        title="Chọn Vị Trí Để Luyện Phỏng Vấn"
        description="AI sẽ phân tích JD bạn chọn để tạo 3 câu hỏi chuyên môn và 2 câu hỏi hành vi"
        size="md"
        footer={
          <div className="flex items-center justify-end gap-2 w-full">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsSelectJobOpen(false)}
              disabled={createPracticeMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={createPracticeMutation.isPending}
              disabled={!selectedJobId || createPracticeMutation.isPending}
              onClick={handleStartPractice}
              leftIcon={<Sparkles className="w-4 h-4" />}
            >
              Sinh câu hỏi & Bắt đầu
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          {isJobsLoading ? (
            <div className="p-4 text-center text-xs text-muted animate-pulse">
              Đang tải danh sách việc làm...
            </div>
          ) : jobs.length === 0 ? (
            <div className="p-4 text-center text-xs text-muted">
              Hiện chưa có tin tuyển dụng nào được duyệt.
            </div>
          ) : (
            <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
              {jobs.map((j) => (
                <div
                  key={j.id}
                  onClick={() => setSelectedJobId(j.id)}
                  className={`p-3 rounded-xl border text-xs cursor-pointer transition-all flex items-center justify-between gap-3 ${
                    selectedJobId === j.id
                      ? "border-primary bg-primary-light/50 font-medium text-foreground"
                      : "border-border hover:bg-slate-50 text-slate-700"
                  }`}
                >
                  <div className="space-y-1 flex-1">
                    <p className="font-semibold text-foreground text-sm">{j.title}</p>
                    <p className="text-muted flex items-center gap-2">
                      <Briefcase className="w-3 h-3" />
                      {j.companyName} · {j.location}
                    </p>
                  </div>
                  <input
                    type="radio"
                    name="selectedJob"
                    checked={selectedJobId === j.id}
                    onChange={() => setSelectedJobId(j.id)}
                    className="accent-primary"
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};

export default InterviewPracticeListPage;
