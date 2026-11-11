import { MentorEvaluationsBlock } from './components/MentorEvaluations';

export default function EvaluationsPage() {
    // ID giả lập để kích hoạt gọi Mock Data (khi nào có User thật thì lấy từ Auth Context/Params)
    const MOCK_USER_ID = "user-123";

    return (
        <div className="mx-auto max-w-4xl p-6 md:p-8 space-y-6 animate-in fade-in duration-300">

            {/* Header của trang */}
            <div className="flex flex-col space-y-2">
                <h1 className="text-2xl font-bold tracking-tight text-gray-900">
                    Hồ sơ năng lực
                </h1>
                <p className="text-muted-foreground text-sm">
                    Xem lại các đánh giá và nhận xét từ Mentor dành cho bạn.
                </p>
            </div>

            {/* Khối hiển thị danh sách Đánh giá từ Mentor */}
            <div className="mt-4">
                <MentorEvaluationsBlock userId={MOCK_USER_ID} />
            </div>

        </div>
    );
}