import type { MentorEvaluation } from '@/api/types/evaluation';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import { Separator } from '@/components/ui/separator';
import { Calendar, CheckCircle2, MessageSquareQuote } from 'lucide-react';

interface Props {
    evaluation: MentorEvaluation | null;
    onClose: () => void;
}

export function EvaluationDetailDialog({ evaluation, onClose }: Props) {
    return (
        <Dialog open={!!evaluation} onOpenChange={(open) => !open && onClose()}>
            <DialogContent aria-describedby={undefined} className="max-h-[85vh] max-w-2xl overflow-y-auto p-0 gap-0">
                {evaluation && (
                    <>
                        {/* Header của Popup */}
                        <DialogHeader className="p-6 pb-4 border-b bg-gray-50/50">
                            <DialogTitle className="text-xl font-bold text-gray-900 flex items-center gap-2">
                                {evaluation.templateName}
                            </DialogTitle>
                            <DialogDescription className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                <span className="flex items-center gap-1.5">
                  <span className="font-medium text-gray-700">Mentor:</span> {evaluation.mentorName}
                </span>
                                <span className="flex items-center gap-1.5">
                  <Calendar className="w-4 h-4" />
                                    {new Date(evaluation.submittedAt).toLocaleDateString('vi-VN')}
                </span>
                            </DialogDescription>
                        </DialogHeader>

                        <div className="p-6 space-y-6">
                            {/* Danh sách tiêu chí đánh giá */}
                            <div className="space-y-5">
                                {evaluation.scores.map((s) => (
                                    <div key={s.criterionId} className="space-y-2.5">
                                        <div className="flex items-center justify-between gap-4">
                                            <div>
                                                <span className="font-semibold text-gray-900">{s.skillName}</span>
                                                <p className="text-xs text-gray-500 mt-0.5">{s.description}</p>
                                            </div>
                                            <div className="flex items-baseline gap-1">
                                                <span className="text-xl font-bold text-blue-600">{s.score}</span>
                                                <span className="text-xs text-gray-400">/100</span>
                                            </div>
                                        </div>

                                        {/* Thanh tiến trình hiển thị điểm */}
                                        <Progress
                                            value={s.score}
                                            className={`h-2.5 ${
                                                s.score >= 80 ? 'bg-green-100 [&>div]:bg-green-500' :
                                                    s.score >= 60 ? 'bg-blue-100 [&>div]:bg-blue-500' :
                                                        s.score >= 40 ? 'bg-yellow-100 [&>div]:bg-yellow-500' :
                                                            'bg-red-100 [&>div]:bg-red-500'
                                            }`}
                                        />

                                        {/* Ô nhận xét của tiêu chí đó */}
                                        {s.comment && (
                                            <div className="bg-gray-50 rounded-lg p-3 text-sm text-gray-700 border border-gray-100 mt-2 flex items-start gap-2">
                                                <MessageSquareQuote className="w-4 h-4 text-gray-400 shrink-0 mt-0.5" />
                                                <p className="whitespace-pre-wrap leading-relaxed">{s.comment}</p>
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>

                            {/* Phần nhận xét chung */}
                            {evaluation.overallComment && (
                                <>
                                    <Separator />
                                    <div className="bg-blue-50/50 border border-blue-100 rounded-xl p-4">
                                        <h4 className="flex items-center gap-2 text-sm font-bold text-blue-900 mb-2">
                                            <CheckCircle2 className="w-4 h-4 text-blue-600" />
                                            Nhận xét chung từ Mentor
                                        </h4>
                                        <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
                                            {evaluation.overallComment}
                                        </p>
                                    </div>
                                </>
                            )}
                        </div>
                    </>
                )}
            </DialogContent>
        </Dialog>
    );
}