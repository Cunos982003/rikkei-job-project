import { InternshipBlock } from './components/InternshipBlock';
import { useAuthStore } from '@/store/useAuthStore';

export default function InternshipsPage() {
    const { user } = useAuthStore();
    return (
        <div className="container mx-auto py-6 px-4">
            <div className="mb-6">
                <h1 className="text-2xl font-bold">Thực tập IOC</h1>
                <p className="text-muted-foreground text-sm mt-1">
                    Thông tin các đợt thực tập và kết quả nghiệm thu.
                </p>
            </div>

            <InternshipBlock userId={user?.id} />
        </div>
    );
}
