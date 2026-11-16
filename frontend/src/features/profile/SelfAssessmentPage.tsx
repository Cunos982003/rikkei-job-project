import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { ClipboardCheck, ArrowRight, Save } from 'lucide-react';

const MOCK_TEMPLATE = {
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
    const [scores, setScores] = useState<Record<string, number>>({});
    const [notes, setNotes] = useState<Record<string, string>>({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = () => {
        setIsSubmitting(true);
        setTimeout(() => {
            alert("Đã gửi bản tự đánh giá thành công! (Mock API)");
            setIsSubmitting(false);
            navigate('/profile/skill-comparison');
        }, 800);
    };

    return (
        <div className="container mx-auto py-8 px-4 max-w-4xl space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="bg-gradient-to-r from-red-700 to-red-600 p-6 rounded-2xl text-white shadow-md relative overflow-hidden">
                <div className="absolute -right-10 -top-10 w-40 h-40 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
                <div className="relative z-10 flex items-center gap-4">
                    <div className="bg-white/20 p-3 rounded-xl backdrop-blur-sm">
                        <ClipboardCheck className="w-8 h-8 text-white" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold">Tự đánh giá năng lực</h1>
                        <p className="text-red-100 mt-1 text-sm">Khung: {MOCK_TEMPLATE.name}</p>
                    </div>
                </div>
            </div>

            <Card className="border-gray-200 shadow-sm">
                <CardContent className="p-6 md:p-8 space-y-8">
                    {MOCK_TEMPLATE.criteria.map((skill, index) => (
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
                                    <span className="text-3xl font-black text-red-600">{scores[skill.skillId] ?? 0}</span>
                                    <span className="text-sm font-medium text-gray-400">/100</span>
                                </div>
                            </div>

                            <div className="space-y-3">
                                <input type="range" min="0" max="100" step="5" value={scores[skill.skillId] ?? 0}
                                       onChange={(e) => setScores(prev => ({ ...prev, [skill.skillId]: parseInt(e.target.value) }))}
                                       className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-red-600"
                                />
                                <input type="text" placeholder="Nhập ghi chú hoặc minh chứng..."
                                       className="w-full p-3 border border-gray-200 rounded-lg text-sm bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500 transition-all"
                                       value={notes[skill.skillId] ?? ''}
                                       onChange={(e) => setNotes(prev => ({ ...prev, [skill.skillId]: e.target.value }))}
                                />
                            </div>
                            {index !== MOCK_TEMPLATE.criteria.length - 1 && <Separator className="mt-8" />}
                        </div>
                    ))}
                </CardContent>
            </Card>

            <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-4">
                <button onClick={() => navigate(-1)} className="px-6 py-2.5 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-50 w-full sm:w-auto">Hủy bỏ</button>
                <button onClick={handleSubmit} disabled={isSubmitting} className="px-6 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium flex items-center justify-center gap-2 disabled:opacity-50 w-full sm:w-auto shadow-sm">
                    {isSubmitting ? <Save className="w-4 h-4 animate-pulse" /> : <ArrowRight className="w-4 h-4" />}
                    {isSubmitting ? 'Đang gửi...' : 'Gửi tự đánh giá'}
                </button>
            </div>
        </div>
    );
}