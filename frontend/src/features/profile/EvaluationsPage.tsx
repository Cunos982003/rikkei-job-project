import { useParams } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';
import { MentorEvaluationsBlock } from './components/MentorEvaluations';

export default function EvaluationsPage() {
    const { userId: paramUserId } = useParams<{ userId?: string }>();
    const { user } = useAuthStore();
    const currentUserId = paramUserId || user?.id;

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
                <MentorEvaluationsBlock userId={currentUserId} />
            </div>

        </div>
    );
}