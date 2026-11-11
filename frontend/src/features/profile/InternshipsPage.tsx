import { InternshipBlock } from './components/InternshipBlock';

export default function InternshipsPage() {
    // ID giả lập để kích hoạt gọi Mock Data
    const MOCK_USER_ID = "user-123";

    return (
        <div className="mx-auto max-w-4xl p-6 md:p-8 space-y-6 animate-in fade-in duration-300">

            {/* Header của trang */}
            <div className="flex flex-col space-y-2">
                <h1 className="text-2xl font-bold tracking-tight text-gray-900">
                    Hồ sơ năng lực
                </h1>
                <p className="text-muted-foreground text-sm">
                    Thông tin các đợt thực tập (IOC) và kết quả nghiệm thu của bạn.
                </p>
            </div>

            {/* Khối hiển thị Thực tập IOC */}
            <div className="mt-4">
                <InternshipBlock userId={MOCK_USER_ID} />
            </div>

        </div>
    );
}