import { useState } from 'react';
import { useProfileEvaluations } from '../hooks/useProfileEvaluations';
import type { MentorEvaluation } from '@/api/types/evaluation';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { EvaluationDetailDialog } from '../EvaluationDetailDialog';
import { ChevronRight, Calendar, UserCircle2 } from 'lucide-react';

const CONTEXT_LABEL: Record<string, string> = {
    INTERNSHIP: 'Thực tập',
    INTERVIEW: 'Phỏng vấn',
};

function formatDate(iso: string): string {
    return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(iso));
}

export function MentorEvaluationsBlock({ userId }: { userId: string | undefined }) {
    const { data, isLoading, isError } = useProfileEvaluations(userId);
    const [selected, setSelected] = useState<MentorEvaluation | null>(null);

    return (
        <>
            <Card className="shadow-sm border-gray-200 overflow-hidden">
                {/* HEADER: BANNER ĐỎ NỔI BẬT */}
                <CardHeader className="p-6 bg-gradient-to-r from-red-700 to-red-600 relative overflow-hidden">
                    {/* Họa tiết trang trí (vòng tròn mờ) */}
                    <div className="absolute -right-10 -top-10 w-40 h-40 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
                    <div className="absolute -left-10 -bottom-10 w-32 h-32 bg-white/10 rounded-full blur-xl pointer-events-none"></div>

                    <CardTitle className="relative z-10 text-xl flex items-center gap-3 text-white font-bold tracking-wide">
                        <UserCircle2 className="w-6 h-6 text-white" />
                        Đánh giá từ Mentor
                    </CardTitle>
                    <p className="relative z-10 text-red-100 text-sm mt-1 ml-9">
                        Xem nhận xét và điểm số từ giảng viên hướng dẫn
                    </p>
                </CardHeader>

                <CardContent className="p-4 space-y-3 bg-gray-50/50">
                    {isLoading && (
                        <div className="space-y-3">
                            <Skeleton className="h-20 w-full rounded-xl" />
                            <Skeleton className="h-20 w-full rounded-xl" />
                        </div>
                    )}

                    {isError && (
                        <p className="text-sm text-red-500 font-medium text-center py-4">
                            Không tải được đánh giá từ mentor.
                        </p>
                    )}

                    {data && data.length === 0 && (
                        <p className="text-sm text-gray-500 text-center py-6 italic">
                            Chưa có đánh giá nào từ mentor.
                        </p>
                    )}

                    {data?.map((e) => (
                        <button
                            key={e.id}
                            type="button"
                            onClick={() => setSelected(e)}
                            className="group flex w-full flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-xl border border-gray-200 bg-white p-4 text-left transition-all duration-200 hover:border-red-300 hover:shadow-md hover:bg-red-50/20"
                        >
                            <div className="space-y-1.5 flex-1">
                                <div className="flex items-center gap-2">
                  <span className="font-semibold text-gray-900 text-base">
                    {e.templateName}
                  </span>
                                    <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200 font-medium">
                                        {CONTEXT_LABEL[e.context] ?? e.context}
                                    </Badge>
                                </div>
                                <div className="flex items-center gap-2 text-sm text-gray-500">
                                    <span className="font-medium text-gray-700">Mentor:</span>
                                    {e.mentorName}
                                </div>
                            </div>

                            <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-2 w-full sm:w-auto mt-2 sm:mt-0 pt-2 sm:pt-0 border-t sm:border-0 border-gray-100">
                <span className="flex items-center gap-1.5 text-xs font-medium text-gray-500 bg-gray-100 px-2.5 py-1 rounded-md">
                  <Calendar className="w-3.5 h-3.5" />
                    {formatDate(e.submittedAt)}
                </span>
                                <span className="text-sm font-medium text-red-600 flex items-center gap-1 group-hover:underline opacity-80 group-hover:opacity-100 transition-opacity">
                  Xem chi tiết <ChevronRight className="w-4 h-4" />
                </span>
                            </div>
                        </button>
                    ))}
                </CardContent>
            </Card>

            <EvaluationDetailDialog evaluation={selected} onClose={() => setSelected(null)} />
        </>
    );
}