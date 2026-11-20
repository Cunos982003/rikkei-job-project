import React, { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, CheckCircle2 } from "lucide-react";
import {
  useInterviewPractice,
  useSubmitAnswer,
  useFinishPractice,
} from "../../features/interview-practice/hooks/useInterviewPractice";
import { QuestionCard } from "../../features/interview-practice/components/QuestionCard";
import { PracticeSummary } from "../../features/interview-practice/components/PracticeSummary";
import type { PracticeQuestion } from "../../api/endpoints/interviewPractice.api";
import { Button } from "../../components/common/Button";

export const InterviewPracticePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);

  const { data: practice, isLoading } = useInterviewPractice(id);
  const submitAnswerMutation = useSubmitAnswer(id || "");
  const finishMutation = useFinishPractice(id || "");

  if (isLoading) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto py-8 animate-pulse">
        <div className="h-10 bg-slate-200/80 rounded-lg w-1/3" />
        <div className="h-64 bg-slate-200/80 rounded-xl" />
      </div>
    );
  }

  if (!practice) {
    return (
      <div className="text-center py-12 space-y-3">
        <p className="text-sm font-semibold text-foreground">
          Không tìm thấy buổi luyện phỏng vấn
        </p>
        <Button variant="outline" size="sm" onClick={() => navigate("/candidate/interview-practice")}>
          Quay lại danh sách
        </Button>
      </div>
    );
  }

  const questions: PracticeQuestion[] = practice.questions || [];
  const currentQuestion = questions[activeQuestionIndex];
  const isFinished = practice.status === "DONE";

  const answeredCount = questions.filter((q: PracticeQuestion) => q.score !== null && q.score !== undefined).length;

  const handleSubmitAnswer = (answer: string) => {
    if (!currentQuestion) return;
    submitAnswerMutation.mutate({
      questionId: currentQuestion.id,
      payload: { answer },
    });
  };

  const handleNextQuestion = () => {
    if (activeQuestionIndex < questions.length - 1) {
      setActiveQuestionIndex((prev) => prev + 1);
    } else {
      // Đã tới câu cuối
      handleFinish();
    }
  };

  const handleFinish = () => {
    finishMutation.mutate(undefined, {
      onSuccess: () => {
        // Will refetch and switch to summary view
      },
    });
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-12">
      {/* Top back bar */}
      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate("/candidate/interview-practice")}
          leftIcon={<ArrowLeft className="w-4 h-4" />}
        >
          Quay lại
        </Button>

        {!isFinished && questions.length > 0 && (
          <div className="flex items-center gap-3">
            <span className="text-xs text-muted">
              Đã trả lời: <strong className="text-primary">{answeredCount}</strong> / {questions.length}
            </span>
            <Button
              variant="primary"
              size="sm"
              isLoading={finishMutation.isPending}
              disabled={answeredCount === 0 || finishMutation.isPending}
              onClick={handleFinish}
              leftIcon={<CheckCircle2 className="w-4 h-4" />}
            >
              Hoàn thành luyện tập
            </Button>
          </div>
        )}
      </div>

      {isFinished ? (
        <PracticeSummary practice={practice} onBackToList={() => navigate("/candidate/interview-practice")} />
      ) : (
        <div className="space-y-6">
          {/* Question pagination steps */}
          <div className="flex items-center justify-between gap-1 overflow-x-auto pb-1">
            {questions.map((q: PracticeQuestion, idx: number) => {
              const answered = q.score !== null && q.score !== undefined;
              const isActive = idx === activeQuestionIndex;
              return (
                <button
                  key={q.id}
                  onClick={() => setActiveQuestionIndex(idx)}
                  className={`flex-1 min-w-[50px] py-2 px-1 text-xs font-semibold rounded-lg border transition-all text-center select-none cursor-pointer ${
                    isActive
                      ? "border-primary bg-primary text-white shadow-xs"
                      : answered
                      ? "border-emerald-300 bg-emerald-50 text-emerald-800"
                      : "border-border bg-surface text-muted hover:bg-slate-50"
                  }`}
                >
                  Câu {q.qOrder}
                </button>
              );
            })}
          </div>

          {/* Current Question */}
          {currentQuestion && (
            <QuestionCard
              question={currentQuestion}
              totalQuestions={questions.length}
              onSubmit={handleSubmitAnswer}
              isSubmitting={submitAnswerMutation.isPending}
              onNext={handleNextQuestion}
              isLastQuestion={activeQuestionIndex === questions.length - 1}
            />
          )}
        </div>
      )}
    </div>
  );
};

export default InterviewPracticePage;
