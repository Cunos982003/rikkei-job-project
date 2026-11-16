import { InternshipBlock } from './components/InternshipBlock';

export default function InternshipsPage() {
    return (
        <div className="container mx-auto py-6 px-4">
            <div className="mb-6">
                <h1 className="text-2xl font-bold">Thực tập IOC</h1>
                <p className="text-muted-foreground text-sm mt-1">
                    Thông tin các đợt thực tập và kết quả nghiệm thu.
                </p>
            </div>

            {/* Truyền userId giả lập để test, sau này thay bằng ID thật */}
            <InternshipBlock userId="user-123" />
        </div>
    );
}