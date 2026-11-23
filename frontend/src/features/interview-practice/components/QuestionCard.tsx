import React, { useState } from "react";
import { Send, CheckCircle2, AlertCircle, HelpCircle } from "lucide-react";
import type { PracticeQuestion } from "../../../api/endpoints/interviewPractice.api";
import { Card, CardContent } from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";
import { Badge } from "../../../components/common/Badge";

interface QuestionCardProps {
  question: PracticeQuestion;
  totalQuestions: number;
  onSubmit: (answer: string) => void;
  isSubmitting: boolean;
  onNext?: () => void;
  isLastQuestion?: boolean;
}

export const QuestionCard: React.FC<QuestionCardProps> = ({
  question,
  totalQuestions,
  onSubmit,
  isSubmitting,
  onNext,
  isLastQuestion,
}) => {
  const [answerText, setAnswerText] = useState(question.answer || "");
  const isAnswered = question.score !== null && question.score !== undefined;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!answerText.trim() || isSubmitting) return;
    onSubmit(answerText.trim());
  };

  const getScoreVariant = (score: number) => {
    if (score >= 8) return "bg-emerald-50 text-emerald-700 border-emerald-200";
    if (score >= 5) return "bg-amber-50 text-amber-700 border-amber-200";
    return "bg-rose-50 text-rose-700 border-rose-200";
  };

  return (
    <Card className="border-border shadow-xs">
      <CardContent className="p-6 space-y-5">
        <div className="flex items-center justify-between border-b border-border pb-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold px-2.5 py-1 rounded-md bg-primary-light text-primary border border-primary-border">
              Câu {question.qOrder} / {totalQuestions}
            </span>
            <Badge variant={question.type === "TECH" ? "verified" : "default"}>
              {question.type === "TECH" ? "Kỹ thuật (TECH)" : "Hành vi (BEHAVIOR)"}
            </Badge>
          </div>
          {isAnswered && (
            <div className={`px-3 py-1 rounded-full text-xs font-bold border ${getScoreVariant(question.score!)} flex items-center gap-1`}>
              <CheckCircle2 className="w-3.5 h-3.5" />
              Điểm: {question.score} / 10
            </div>
          )}
        </div>

        <div>
          <h3 className="text-base font-semibold text-foreground leading-relaxed flex items-start gap-2">
            <HelpCircle className="w-5 h-5 text-primary shrink-0 mt-0.5" />
            <span>{question.question}</span>
          </h3>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-muted mb-1.5">
              Câu trả lời của bạn:
            </label>
            <textarea
              rows={5}
              value={answerText}
              onChange={(e) => setAnswerText(e.target.value)}
              disabled={isSubmitting}
              placeholder="Nhập câu trả lời chi tiết và dẫn chứng kinh nghiệm thực tế của bạn..."
              className="w-full text-sm rounded-xl border border-border bg-surface p-3.5 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all disabled:opacity-60 resize-none"
            />
          </div>

          <div className="flex items-center justify-between">
            <span className="text-xs text-muted">
              {answerText.trim().length} ký tự
            </span>
            <div className="flex items-center gap-2">
              <Button
                type="submit"
                variant="primary"
                size="sm"
                isLoading={isSubmitting}
                disabled={!answerText.trim() || isSubmitting}
                leftIcon={<Send className="w-3.5 h-3.5" />}
              >
                {isAnswered ? "Chấm lại câu này" : "Gửi câu trả lời"}
              </Button>
              {isAnswered && onNext && (
                <Button type="button" variant="outline" size="sm" onClick={onNext}>
                  {isLastQuestion ? "Xem tổng kết" : "Câu tiếp theo →"}
                </Button>
              )}
            </div>
          </div>
        </form>

        {isAnswered && (
          <div className="mt-5 pt-4 border-t border-border space-y-3 bg-slate-50/70 -mx-6 -mb-6 p-6 rounded-b-xl">
            <div className="flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-primary shrink-0 mt-0.5" />
              <div>
                <span className="text-xs font-bold text-foreground block mb-0.5">Nhận xét của AI:</span>
                <p className="text-xs text-slate-700 leading-relaxed">{question.feedback}</p>
              </div>
            </div>

            {question.suggestion && (
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-900 leading-relaxed">
                <span className="font-semibold block mb-0.5">💡 Gợi ý cải thiện:</span>
                {question.suggestion}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
