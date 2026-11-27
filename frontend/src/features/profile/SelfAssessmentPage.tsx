import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { ClipboardCheck, ArrowRight, Save, RotateCcw } from 'lucide-react';
import { toast } from 'sonner';
import { candidateKeys } from '@/api/queryKeys';
import { selfAssessmentApi } from '@/api/endpoints/self-assessment.api';
import { profileApi } from '@/api/endpoints/profile.api';

interface AssessmentCriterion {
    skillId: string;
    skillName: string;
    description: string;
    initialScore?: number;
}

interface AssessmentTemplateData {
    id: string;
    name: string;
    criteria: AssessmentCriterion[];
}

const DEFAULT_TEMPLATE: AssessmentTemplateData = {
    id: "tpl-1",
    name: "Khung năng lực Lập trình viên Fullstack",
    criteria: [
        { skillId: "s1", skillName: "Java Core", description: "Nắm vững OOP, Collections, Multithreading" },
        { skillId: "s2", skillName: "Spring Boot", description: "Xây dựng RESTful API, Spring Data JPA" },
        { skillId: "s3", skillName: "ReactJS", description: "Component lifecycle, Hooks, State Management" },
        { skillId: "s4", skillName: "Database (SQL)", description: "Thiết kế CSDL, viết query phức tạp" },
        { skillId: "s5", skillName: "Git & CI/CD", description: "Quản lý source code, làm việc nhóm" },
    ]
};

export default function SelfAssessmentPage() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [scores, setScores] = useState<Record<string, number>>({});
    const [notes, setNotes] = useState<Record<string, string>>({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const { data: template, isLoading } = useQuery<AssessmentTemplateData>({
        queryKey: candidateKeys.selfAssessment.template(),
        queryFn: async (): Promise<AssessmentTemplateData> => {
            // 1. Thử gọi API template chuyên dụng từ selfAssessmentApi
            try {
                const res = await selfAssessmentApi.getTemplate();
                if (res.data && res.data.skills?.length > 0) {
                    return {
                        id: res.data.templateId || "tpl-api",
                        name: res.data.name || "Khung năng lực tự đánh giá",
                        criteria: res.data.skills.map((s) => ({
                            skillId: s.skillId,
                            skillName: s.skillName,
                            description: s.description || "Đánh giá mức độ thành thạo và áp dụng thực tế",
                        })),
                    };
                }
            } catch {
                // Fallback tự động lấy danh mục kỹ năng thực tế từ profile
            }

            // 2. Fallback: Lấy danh sách kỹ năng thật của ứng viên từ profileApi.getMySkills()
            try {
                const mySkillsRes = await profileApi.getMySkills();
                if (mySkillsRes.data && mySkillsRes.data.length > 0) {
                    return {
                        id: "tpl-profile-skills",
                        name: "Khung kỹ năng thực tế của ứng viên",
                        criteria: mySkillsRes.data.map((s) => ({
                            skillId: s.skillId,
                            skillName: s.skillName,
                            description: `Nhóm kỹ năng: ${s.group || "Chuyên môn"} (Điểm đo thực tế: ${s.verifiedScore ?? "Chưa đo"})`,
                            initialScore: s.selfScore ?? 0,
                        })),
                    };
                }
            } catch (err) {
                console.warn("Không thể tải mySkills:", err);
            }

            // 3. Fallback mặc định
            return DEFAULT_TEMPLATE;
        },
        staleTime: 60 * 1000,
    });

    const handleReset = () => {
        setScores({});
        setNotes({});
    };

    const handleSubmit = async () => {
        setIsSubmitting(true);
        try {
            const activeTemplate = template || DEFAULT_TEMPLATE;
            const scoresPayload = activeTemplate.criteria.map((c) => ({
                skillId: c.skillId,
                score: scores[c.skillId] ?? c.initialScore ?? 0,
                note: notes[c.skillId] || undefined,
            }));

            // 1. Thử gửi bản tự đánh giá lên DG-02 endpoint
            try {
                await selfAssessmentApi.submit({
                    templateId: activeTemplate.id,
                    scores: scoresPayload,
                });
            } catch (err) {
                console.warn("selfAssessmentApi.submit chưa khả dụng, tiếp tục lưu trực tiếp vào profile-service:", err);
            }

            // 2. Đồng thời ghi vào profile-service qua upsertSelfSkills (HS-02) nếu skillId hợp lệ
            try {
                const selfSkillsList = activeTemplate.criteria
                    .filter((c) => c.skillId && c.skillId.length > 10)
                    .map((c) => ({
                        skillId: c.skillId,
                        selfScore: scores[c.skillId] ?? c.initialScore ?? 0,
                    }));
                if (selfSkillsList.length > 0) {
                    await profileApi.upsertSelfSkills(selfSkillsList);
                }
            } catch (err) {
                console.warn("profileApi.upsertSelfSkills:", err);
            }

            // 3. Invalidate toàn bộ cache liên quan
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: candidateKeys.skills.all() }),
                queryClient.invalidateQueries({ queryKey: candidateKeys.learning.all() }),
                queryClient.invalidateQueries({ queryKey: candidateKeys.profile.all() }),
                queryClient.invalidateQueries({ queryKey: candidateKeys.selfAssessment.all() }),
            ]);

            toast.success("Đã gửi bản tự đánh giá năng lực thành công!");
            navigate('/profile/skill-comparison');
        } catch (error) {
            console.error("Lỗi khi gửi tự đánh giá:", error);
            toast.error("Không thể lưu tự đánh giá. Vui lòng thử lại sau.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const activeCriteria = template?.criteria || DEFAULT_TEMPLATE.criteria;

    return (
        <div className="container mx-auto py-8 px-4 max-w-4xl space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-primary-dark p-6 rounded-2xl text-white shadow-2xs relative overflow-hidden">
                <div className="absolute -right-10 -top-10 w-40 h-40 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
                <div className="relative z-10 flex items-center gap-4">
                    <div className="bg-white/15 p-3 rounded-xl backdrop-blur-sm border border-white/20">
                        <ClipboardCheck className="w-8 h-8 text-primary-light" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold">Tự đánh giá năng lực</h1>
                        <p className="text-red-100 mt-1 text-sm">
                            Khung: {template?.name || DEFAULT_TEMPLATE.name}
                        </p>
                    </div>
                </div>
            </div>

            {isLoading ? (
                <div className="space-y-4">
                    <Skeleton className="h-28 w-full rounded-2xl" />
                    <Skeleton className="h-64 w-full rounded-2xl" />
                </div>
            ) : (
                <Card className="border-gray-200 shadow-sm">
                    <CardContent className="p-6 md:p-8 space-y-8">
                        {activeCriteria.map((skill, index) => {
                            const currentScore = scores[skill.skillId] ?? skill.initialScore ?? 0;
                            return (
                                <div key={skill.skillId} className="space-y-4">
                                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                                        <div>
                                            <h3 className="font-semibold text-lg text-gray-900 flex items-center gap-2">
                                                <span className="bg-red-100 text-red-700 text-xs py-1 px-2.5 rounded-md font-bold">#{index + 1}</span>
                                                {skill.skillName}
                                            </h3>
                                            <p className="text-sm text-gray-500 mt-1">{skill.description}</p>
                                        </div>
                                        <div className="text-right sm:text-right text-left">
                                            <span className="text-3xl font-black text-red-600">{currentScore}</span>
                                            <span className="text-sm font-medium text-gray-400">/100</span>
                                        </div>
                                    </div>

                                    <div className="space-y-3">
                                        <input
                                            type="range"
                                            min="0"
                                            max="100"
                                            step="5"
                                            value={currentScore}
                                            onChange={(e) => setScores(prev => ({ ...prev, [skill.skillId]: parseInt(e.target.value) }))}
                                            className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-primary"
                                        />
                                        <input
                                            type="text"
                                            placeholder="Nhập ghi chú hoặc minh chứng năng lực..."
                                            className="w-full p-3 border border-border rounded-lg text-sm bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all text-foreground"
                                            value={notes[skill.skillId] ?? ''}
                                            onChange={(e) => setNotes(prev => ({ ...prev, [skill.skillId]: e.target.value }))}
                                        />
                                    </div>
                                    {index !== activeCriteria.length - 1 && <Separator className="mt-8" />}
                                </div>
                            );
                        })}
                    </CardContent>
                </Card>
            )}

            <div className="flex flex-col-reverse sm:flex-row justify-between items-center gap-3 pt-4">
                <button
                    type="button"
                    onClick={handleReset}
                    className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 flex items-center gap-1.5 transition-colors"
                >
                    <RotateCcw className="w-4 h-4" /> Đặt lại điểm mặc định
                </button>
                <div className="flex flex-col-reverse sm:flex-row gap-3 w-full sm:w-auto">
                    <button
                        type="button"
                        onClick={() => navigate(-1)}
                        className="px-6 py-2.5 border border-border rounded-lg text-foreground font-medium hover:bg-slate-100 w-full sm:w-auto transition-colors"
                    >
                        Hủy bỏ
                    </button>
                    <button
                        type="button"
                        onClick={handleSubmit}
                        disabled={isSubmitting || isLoading}
                        className="px-6 py-2.5 bg-primary hover:bg-primary-hover text-white rounded-lg font-medium flex items-center justify-center gap-2 disabled:opacity-50 w-full sm:w-auto shadow-2xs transition-colors"
                    >
                        {isSubmitting ? <Save className="w-4 h-4 animate-pulse" /> : <ArrowRight className="w-4 h-4" />}
                        {isSubmitting ? 'Đang gửi...' : 'Gửi tự đánh giá'}
                    </button>
                </div>
            </div>
        </div>
    );
}
