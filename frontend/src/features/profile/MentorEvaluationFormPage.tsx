import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { MOCK_FORM_DATA } from './mocks/mentorTasksMock';

export default function MentorEvaluationFormPage() {
    const { id: _id } = useParams(); // Thêm dấu : _id
    const navigate = useNavigate();

    // State form
    const [scores, setScores] = useState<Record<string, number>>({});
    const [comments, setComments] = useState<Record<string, string>>({});
    const [overallComment, setOverallComment] = useState("");

    // State lưu nháp
    const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);
    const [isDirty, setIsDirty] = useState(false);

    // Hàm Lưu Nháp giả lập gọi API
    const handleSaveDraft = useCallback(() => {
        if (!isDirty) return;
        console.log("Đang tự động lưu nháp...", { scores, comments, overallComment });

        // Giả lập độ trễ API
        setTimeout(() => {
            const now = new Date();
            setLastSavedTime(`${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`);
            setIsDirty(false);
        }, 500);
    }, [scores, comments, overallComment, isDirty]);

    // Yêu cầu: Tự lưu mỗi 30 giây
    useEffect(() => {
        const interval = setInterval(() => {
            handleSaveDraft();
        }, 30000);
        return () => clearInterval(interval);
    }, [handleSaveDraft]);

    // Yêu cầu: Cảnh báo khi rời trang mà chưa lưu
    useEffect(() => {
        const handleBeforeUnload = (e: BeforeUnloadEvent) => {
            if (isDirty) {
                e.preventDefault();
                e.returnValue = '';
            }
        };
        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [isDirty]);

    // Cập nhật state (Đánh dấu dirty)
    const updateScore = (skillId: string, val: number) => {
        setScores(prev => ({ ...prev, [skillId]: val }));
        setIsDirty(true);
    };
    const updateComment = (skillId: string, val: string) => {
        setComments(prev => ({ ...prev, [skillId]: val }));
        setIsDirty(true);
    };

    // Xử lý nút Gửi
    const handleSubmit = () => {
        // Yêu cầu: Hộp xác nhận
        if (window.confirm("Gửi phiếu xong sẽ KHÔNG THỂ SỬA. Bạn có chắc chắn muốn gửi?")) {
            alert("Đã gửi phiếu đánh giá thành công! (Mock API)");
            setIsDirty(false); // Đánh dấu đã lưu để không bị cảnh báo
            navigate('/mentor/evaluations');
        }
    };

    return (
        <div className="container mx-auto py-8 px-4 max-w-3xl space-y-6">

            {/* Header & Trạng thái lưu */}
            <div className="flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-bold">Nhập phiếu đánh giá</h1>
                    <p className="text-muted-foreground text-sm mt-1">Ứng viên: <span className="font-semibold text-black">{MOCK_FORM_DATA.candidateName}</span></p>
                </div>
                <div className="text-sm font-medium text-gray-500">
                    {lastSavedTime ? `Đã lưu lúc ${lastSavedTime}` : 'Chưa lưu'}
                </div>
            </div>

            {/* Tóm tắt ứng viên (Kỹ năng, Task) */}
            <Card className="bg-blue-50/50 border-blue-100">
                <CardHeader className="py-3">
                    <CardTitle className="text-sm font-semibold text-blue-900">Tóm tắt thông tin ứng viên</CardTitle>
                </CardHeader>
                <CardContent className="text-sm text-gray-700 space-y-1 pb-3">
                    <p><span className="font-medium">Kỹ năng hiện có:</span> {MOCK_FORM_DATA.skillsSummary}</p>
                    {MOCK_FORM_DATA.internshipTasks && (
                        <p><span className="font-medium">Task thực tập đã nghiệm thu:</span> {MOCK_FORM_DATA.internshipTasks.map(t => `${t.title} (${t.score}đ)`).join(', ')}</p>
                    )}
                </CardContent>
            </Card>

            {/* Form chấm điểm từng tiêu chí */}
            <div className="space-y-8">
                {MOCK_FORM_DATA.criteria.map((skill) => (
                    <div key={skill.id} className="space-y-3">
                        <div className="flex justify-between items-center">
                            <div>
                                <span className="font-semibold text-lg">{skill.name}</span>
                                <p className="text-sm text-gray-500">{skill.description}</p>
                            </div>
                            <span className="font-bold text-blue-600 text-xl">{scores[skill.id] ?? 0}/100</span>
                        </div>

                        {/* Thanh trượt 0-100 */}
                        <input
                            type="range" min="0" max="100" step="1"
                            value={scores[skill.id] ?? 0}
                            onChange={(e) => updateScore(skill.id, parseInt(e.target.value))}
                            className="w-full accent-blue-600 cursor-pointer"
                        />

                        {/* Ô nhận xét - Tự động lưu khi rời ô (onBlur) */}
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

            {/* Nút Hành động */}
            <div className="flex justify-end gap-4 pt-4 border-t">
                <button
                    onClick={() => navigate(-1)}
                    className="px-4 py-2 border rounded-md hover:bg-gray-50"
                >
                    Hủy
                </button>
                <button
                    onClick={handleSaveDraft}
                    disabled={!isDirty}
                    className="px-4 py-2 border border-blue-600 text-blue-600 rounded-md hover:bg-blue-50 disabled:opacity-50"
                >
                    Lưu nháp
                </button>
                <button
                    onClick={handleSubmit}
                    className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
                >
                    Gửi đánh giá
                </button>
            </div>

        </div>
    );
}