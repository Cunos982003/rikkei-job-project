import React from "react";
import { Trophy, ArrowRight } from "lucide-react";
import type { InterviewPracticeItem } from "../../../api/endpoints/interviewPractice.api";
import { Card, CardContent } from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";

interface PracticeSummaryProps {
  practice: InterviewPracticeItem;
  onBackToList: () => void;
}

export const PracticeSummary: React.FC<PracticeSummaryProps> = ({ practice, onBackToList }) => {
  const score = practice.overallScore ?? 0;

  const getScoreColor = (sc: number) => {
    if (sc >= 8) return "text-emerald-600 border-emerald-500 bg-emerald-50";
    if (sc >= 5) return "text-amber-600 border-amber-500 bg-amber-50";
    return "text-rose-600 border-rose-500 bg-rose-50";
  };

  return (
    <div className="space-y-6">
      <Card className="border-border shadow-xs overflow-hidden">
        <div className="bg-primary/5 border-b border-border p-6 text-center">
          <div className="inline-flex items-center justify-center p-3 bg-surface rounded-full shadow-xs mb-3 text-primary">
            <Trophy className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-foreground">Kết Quả Luyện Phỏng Vấn</h2>
          <p className="text-xs text-muted mt-1">{practice.jobTitle}</p>
        </div>

        <CardContent className="p-6 space-y-6">
          <div className="flex flex-col sm:flex-row items-center justify-around gap-6 p-4 rounded-xl bg-slate-50 border border-slate-200">
            <div className="flex flex-col items-center">
              <span className="text-xs text-muted mb-1 font-medium">Điểm trung bình</span>
              <div className={`w-20 h-20 rounded-full border-4 flex items-center justify-center font-bold text-2xl ${getScoreColor(score)}`}>
                {score}
              </div>
              <span className="text-[11px] text-muted mt-1">Thang điểm 10</span>
            </div>

            <div className="flex-1 space-y-2">
              <h4 className="text-xs font-bold text-foreground uppercase tracking-wide">
                Đánh giá tổng quát từ AI
              </h4>
              <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line bg-surface p-3.5 rounded-lg border border-border">
                {practice.summary || "Đã hoàn thành các câu hỏi phỏng vấn."}
              </p>
            </div>
          </div>

          <div className="space-y-3">
            <h3 className="text-sm font-bold text-foreground">Chi tiết từng câu hỏi</h3>
            <div className="space-y-3">
              {practice.questions.map((q) => (
                <div key={q.id} className="p-3.5 rounded-xl border border-border bg-surface text-xs space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-foreground">
                      Câu {q.qOrder} ({q.type}): {q.question}
                    </span>
                    <span className="font-bold text-primary shrink-0 ml-2">
                      {q.score !== null && q.score !== undefined ? `${q.score} / 10` : "Chưa chấm"}
                    </span>
                  </div>
                  {q.feedback && (
                    <p className="text-slate-600 italic">
                      Nhận xét: {q.feedback}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-center pt-2">
            <Button variant="primary" size="md" onClick={onBackToList} rightIcon={<ArrowRight className="w-4 h-4" />}>
              Quay về danh sách luyện tập
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
