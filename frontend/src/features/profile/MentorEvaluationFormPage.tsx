import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { MOCK_FORM_DATAS, MOCK_MENTOR_TASKS } from './mocks/mentorTasksMock';
import { UserCircle, ChevronDown } from 'lucide-react';

export default function MentorEvaluationFormPage() {
    const { id: _id } = useParams();
    const navigate = useNavigate();

    // State form
    const [scores, setScores] = useState<Record<string, number>>({});
    const [comments, setComments] = useState<Record<string, string>>({});
    const [overallComment, setOverallComment] = useState("");
    const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);
    const [isDirty, setIsDirty] = useState(false);

    // State chọn ứng viên
    const [selectedCandidate, setSelectedCandidate] = useState<string>(MOCK_MENTOR_TASKS[0].candidateName);

    // Lấy dữ liệu form tương ứng với ứng viên đang chọn
    const formData = MOCK_FORM_DATAS[selectedCandidate] || MOCK_FORM_DATAS["Nguyễn Văn A"]; // Fallback

    const handleCandidateChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const newCandidate = e.target.value;
        if (newCandidate === selectedCandidate) return;

        if (isDirty && !window.confirm("Bạn có thay đổi chưa lưu. Chuyển ứng viên sẽ mất dữ liệu hiện tại. Tiếp tục?")) {
            return;
        }

        setSelectedCandidate(newCandidate);
        setScores({}); // Reset điểm
        setComments({}); // Reset nhận xét
        setOverallComment(""); // Reset nhận xét chung
        setIsDirty(false);
    };

    const handleSaveDraft = useCallback(() => {
        if (!isDirty) return;
        setTimeout(() => {
            const now = new Date();
            setLastSavedTime(`${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`);
            setIsDirty(false);
        }, 500);
    }, [isDirty]);

    useEffect(() => {
        const interval = setInterval(handleSaveDraft, 30000);
        return () => clearInterval(interval);
    }, [handleSaveDraft]);

    useEffect(() => {
        const handleBeforeUnload = (e: BeforeUnloadEvent) => {
            if (isDirty) { e.preventDefault(); e.returnValue = ''; }
        };
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [isDirty]);

    const updateScore = (skillId: string, val: number) => {
        setScores(prev => ({ ...prev, [skillId]: val }));
        setIsDirty(true);
    };
    const updateComment = (skillId: string, val: string) => {
        setComments(prev => ({ ...prev, [skillId]: val }));
        setIsDirty(true);
    };

    const handleSubmit = () => {
        if (window.confirm("Gửi phiếu xong sẽ KHÔNG THỂ SỬA. Bạn có chắc chắn muốn gửi?")) {
            alert("Đã gửi phiếu đánh giá thành công! (Mock API)");
            setIsDirty(false);
            navigate('/mentor/evaluations');
        }
    };

    return (
        <div className="container mx-auto py-8 px-4 max-w-3xl space-y-6">
            <div className="flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-bold">Nhập phiếu đánh giá</h1>
                    <p className="text-muted-foreground text-sm mt-1">Ứng viên: <span className="font-semibold text-black">{selectedCandidate}</span></p>
                </div>
                <div className="text-sm font-medium text-gray-500">
                    {lastSavedTime ? `Đã lưu lúc ${lastSavedTime}` : 'Chưa lưu'}
                </div>
            </div>

            {/* Khối chọn ứng viên */}
            <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                    <div className="bg-red-50 p-2 rounded-lg">
                        <UserCircle className="w-5 h-5 text-red-600" />
                    </div>
                    <div>
                        <label className="text-sm font-semibold text-gray-700 block">Chọn ứng viên để đánh giá</label>
                        <p className="text-xs text-gray-500">Chọn từ danh sách thực tập sinh đang chờ đánh giá</p>
                    </div>
                </div>

                <div className="relative w-full sm:w-72">
                    <select
                        value={selectedCandidate}
                        onChange={handleCandidateChange}
                        className="w-full appearance-none bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-red-500 focus:border-red-500 block p-2.5 pr-8 font-medium cursor-pointer"
                    >
                        {MOCK_MENTOR_TASKS.map(task => (
                            <option key={task.id} value={task.candidateName}>
                                {task.candidateName} - {task.batchOrRoundName}
                            </option>
                        ))}
                    </select>
                    <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-500">
                        <ChevronDown className="w-4 h-4" />
                    </div>
                </div>
            </div>

            {/* Tóm tắt ứng viên (ĐÃ CẬP NHẬT ĐỂ LẤY TỪ formData) */}
            <Card className="bg-blue-50/50 border-blue-100">
                <CardHeader className="py-3">
                    <CardTitle className="text-sm font-semibold text-blue-900">Tóm tắt thông tin ứng viên</CardTitle>
                </CardHeader>
                <CardContent className="text-sm text-gray-700 space-y-1 pb-3">
                    <p><span className="font-medium">Kỹ năng hiện có:</span> {formData.skillsSummary}</p>
                    {formData.internshipTasks && (
                        <p><span className="font-medium">Task thực tập đã nghiệm thu:</span> {formData.internshipTasks.map(t => `${t.title} (${t.score}đ)`).join(', ')}</p>
                    )}
                </CardContent>
            </Card>

            {/* Form chấm điểm (Lấy tiêu chí từ formData) */}
            <div className="space-y-8">
                {formData.criteria.map((skill) => (
                    <div key={skill.id} className="space-y-3">
                        <div className="flex justify-between items-center">
                            <div>
                                <span className="font-semibold text-lg">{skill.name}</span>
                                <p className="text-sm text-gray-500">{skill.description}</p>
                            </div>
                            <span className="font-bold text-blue-600 text-xl">{scores[skill.id] ?? 0}/100</span>
                        </div>
                        <input
                            type="range" min="0" max="100" step="1"
                            value={scores[skill.id] ?? 0}
                            onChange={(e) => updateScore(skill.id, parseInt(e.target.value))}
                            className="w-full accent-blue-600 cursor-pointer"
                        />
                        <input
                            type="text" placeholder="Nhận xét cho tiêu chí này..."
                            className="w-full p-2 border rounded-md text-sm focus:outline-blue-500"
                            value={comments[skill.id] ?? ''}
                            onChange={(e) => updateComment(skill.id, e.target.value)}
                            onBlur={handleSaveDraft}
                        />
                        <Separator className="mt-6" />
                    </div>
                ))}
            </div>

            {/* Nhận xét chung */}
            <div className="space-y-2">
                <label className="font-semibold text-lg">Nhận xét chung</label>
                <textarea
                    className="w-full p-3 border rounded-md min-h-[100px] focus:outline-blue-500"
                    placeholder="Đánh giá tổng quan về thái độ, tiềm năng của ứng viên..."
                    value={overallComment}
                    onChange={(e) => { setOverallComment(e.target.value); setIsDirty(true); }}
                    onBlur={handleSaveDraft}
                />
            </div>

            <div className="flex justify-end gap-4 pt-4 border-t">
                <button onClick={() => navigate(-1)} className="px-4 py-2 border rounded-md hover:bg-gray-50">Hủy</button>
                <button onClick={handleSaveDraft} disabled={!isDirty} className="px-4 py-2 border border-blue-600 text-blue-600 rounded-md hover:bg-blue-50 disabled:opacity-50">Lưu nháp</button>
                <button onClick={handleSubmit} className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700">Gửi đánh giá</button>
            </div>
        </div>
    );
}