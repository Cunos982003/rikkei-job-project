import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { BarChart3, AlertTriangle, TrendingUp, CheckCircle2, RefreshCw, PenLine } from 'lucide-react';
import { candidateKeys } from '@/api/queryKeys';
import { selfAssessmentApi, type ComparisonFlag } from '@/api/endpoints/self-assessment.api';
import { profileApi } from '@/api/endpoints/profile.api';

interface ComparisonDisplayItem {
    id: string;
    name: string;
    measured: number | null;
    self: number | null;
    flag: ComparisonFlag;
}

const MOCK_COMPARISON: ComparisonDisplayItem[] = [
    { id: "s1", name: "Java Core", measured: 85, self: 90, flag: 'MATCH' },
    { id: "s2", name: "Spring Boot", measured: 60, self: 85, flag: 'OVER' },
    { id: "s3", name: "ReactJS", measured: 80, self: 50, flag: 'UNDER' },
    { id: "s4", name: "Database (SQL)", measured: null, self: 75, flag: 'NO_DATA' },
    { id: "s5", name: "Git & CI/CD", measured: 75, self: 75, flag: 'MATCH' },
];

function computeFlag(measured: number | null, self: number | null): ComparisonFlag {
    if (self === null || self === undefined) return 'NO_DATA';
    if (measured === null || measured === undefined) return 'NO_DATA';
    const diff = self - measured;
    if (diff > 15) return 'OVER';
    if (diff < -15) return 'UNDER';
    return 'MATCH';
}

export default function SkillComparisonPage() {
    const navigate = useNavigate();

    const { data: comparisonData, isLoading, refetch, isFetching } = useQuery<ComparisonDisplayItem[]>({
        queryKey: candidateKeys.selfAssessment.comparison(),
        queryFn: async (): Promise<ComparisonDisplayItem[]> => {
            // 1. Thử gọi API so sánh chuyên dụng từ selfAssessmentApi
            try {
                const res = await selfAssessmentApi.getComparison();
                if (res.data && res.data.length > 0) {
                    return res.data.map((item) => ({
                        id: item.skillId,
                        name: item.skillName,
                        measured: item.measured,
                        self: item.self,
                        flag: item.flag,
                    }));
                }
            } catch (err) {
                console.warn("selfAssessmentApi.getComparison chưa khả dụng, tính toán từ profile skills:", err);
            }

            // 2. Tính toán đối chiếu từ profileApi.getMySkills() thực tế của profile-service
            try {
                const mySkillsRes = await profileApi.getMySkills();
                if (mySkillsRes.data && mySkillsRes.data.length > 0) {
                    return mySkillsRes.data.map((s) => ({
                        id: s.skillId,
                        name: s.skillName,
                        measured: s.verifiedScore,
                        self: s.selfScore,
                        flag: computeFlag(s.verifiedScore, s.selfScore),
                    }));
                }
            } catch (err) {
                console.warn("Không thể tải mySkills cho so sánh:", err);
            }

            // 3. Fallback mặc định
            return MOCK_COMPARISON;
        },
        staleTime: 30 * 1000,
    });

    const items = comparisonData || MOCK_COMPARISON;

    return (
        <div className="container mx-auto py-8 px-4 max-w-4xl space-y-6 animate-in fade-in duration-500">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b pb-4">
                <div className="flex items-center gap-3">
                    <div className="bg-red-100 p-2.5 rounded-xl">
                        <BarChart3 className="w-6 h-6 text-red-600" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">So sánh năng lực</h1>
                        <p className="text-gray-500 text-sm">Đối chiếu giữa Tự đánh giá và Kết quả đo lường thực tế.</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="px-3.5 py-2 border border-gray-200 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 flex items-center gap-1.5 transition-colors"
                    >
                        <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
                        Làm mới
                    </button>
                    <button
                        type="button"
                        onClick={() => navigate('/profile/self-assessment')}
                        className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium flex items-center gap-1.5 transition-colors shadow-2xs"
                    >
                        <PenLine className="w-4 h-4" />
                        Tự đánh giá lại
                    </button>
                </div>
            </div>

            {isLoading ? (
                <div className="space-y-4">
                    <Skeleton className="h-40 w-full rounded-2xl" />
                    <Skeleton className="h-40 w-full rounded-2xl" />
                </div>
            ) : (
                <Card className="shadow-sm border-gray-200">
                    <CardContent className="space-y-10 pt-6 pb-8 px-6 md:px-8">
                        {items.map((item) => (
                            <div key={item.id} className="space-y-4">
                                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                                    <span className="font-bold text-lg text-gray-800">{item.name}</span>
                                    <div className="flex items-center gap-2">
                                        {item.flag === 'OVER' && (
                                            <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200 gap-1 py-1 px-3">
                                                <TrendingUp className="w-3.5 h-3.5" /> Tự đánh giá cao hơn
                                            </Badge>
                                        )}
                                        {item.flag === 'UNDER' && (
                                            <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200 gap-1 py-1 px-3">
                                                <AlertTriangle className="w-3.5 h-3.5" /> Tự đánh giá thấp hơn
                                            </Badge>
                                        )}
                                        {item.flag === 'MATCH' && (
                                            <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200 gap-1 py-1 px-3">
                                                <CheckCircle2 className="w-3.5 h-3.5" /> Đánh giá chính xác
                                            </Badge>
                                        )}
                                        {item.flag === 'NO_DATA' && (
                                            <Badge variant="outline" className="bg-gray-50 text-gray-500 border-gray-200 py-1 px-3">
                                                Chưa có dữ liệu đo
                                            </Badge>
                                        )}
                                    </div>
                                </div>

                                <div className="relative pt-6 pb-2">
                                    <div className="flex justify-between text-xs font-medium text-gray-400 mb-2">
                                        <span>0</span><span>25</span><span>50</span><span>75</span><span>100</span>
                                    </div>
                                    <div className="relative h-4 w-full bg-gray-100 rounded-full">
                                        {item.measured !== null && item.measured !== undefined && (
                                            <div
                                                className="absolute top-0 left-0 h-full bg-red-500 rounded-full opacity-30"
                                                style={{ width: `${Math.min(100, Math.max(0, item.measured))}%` }}
                                            />
                                        )}
                                        {item.self !== null && item.self !== undefined && (
                                            <div
                                                className="absolute top-[-6px] bottom-[-6px] w-1.5 bg-red-600 rounded-full z-10 shadow-sm"
                                                style={{ left: `calc(${Math.min(100, Math.max(0, item.self))}% - 3px)` }}
                                            >
                                                <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-900 text-white text-xs py-1 px-2 rounded whitespace-nowrap">
                                                    Tự: {item.self}
                                                </div>
                                            </div>
                                        )}
                                        {item.measured !== null && item.measured !== undefined && (
                                            <div
                                                className="absolute top-[-2px] bottom-[-2px] w-1 bg-gray-800 rounded-full z-10 shadow-sm"
                                                style={{ left: `calc(${Math.min(100, Math.max(0, item.measured))}% - 2px)` }}
                                            >
                                                <div className="absolute -bottom-7 left-1/2 -translate-x-1/2 bg-white border text-gray-800 text-xs py-1 px-2 rounded whitespace-nowrap shadow-sm">
                                                    Đo: {item.measured}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div className="flex justify-end text-xs text-gray-500 gap-4 mt-6">
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-3 h-3 bg-red-600 rounded-sm"></div> Tự đánh giá
                                    </div>
                                    {item.measured !== null && item.measured !== undefined && (
                                        <div className="flex items-center gap-1.5">
                                            <div className="w-3 h-3 bg-gray-800 rounded-sm"></div> Kết quả đo
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}

            <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 flex flex-col sm:flex-row gap-4 sm:gap-6 text-sm text-gray-600">
                <p className="flex items-center gap-2">
                    <TrendingUp className="w-4 h-4 text-red-500"/> Lệch {'>'} 15 điểm: Đánh giá cao hơn
                </p>
                <p className="flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-yellow-500"/> Lệch {'<'} -15 điểm: Đánh giá thấp hơn
                </p>
            </div>
        </div>
    );
}
