import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { UserPlus, ChevronDown, Building2, CalendarDays, Users } from 'lucide-react';

// Bổ sung rất nhiều dữ liệu mẫu
const MOCK_DEMANDS = [
    {
        jobId: "job-1",
        jobTitle: "Thực tập sinh Java Backend",
        companyName: "Công ty Rikkei Software",
        preferredBatch: "Đợt thực tập Java K20",
        durationWeeks: 12,
        hiredCandidates: [
            { userId: "u-1", candidateName: "Nguyễn Văn A", applicationId: "app-1" },
            { userId: "u-2", candidateName: "Trần Thị B", applicationId: "app-2" },
            { userId: "u-3", candidateName: "Lê Hoàng Cường", applicationId: "app-3" },
            { userId: "u-4", candidateName: "Phạm Minh Dũng", applicationId: "app-4" }
        ]
    },
    {
        jobId: "job-2",
        jobTitle: "Thực tập sinh Frontend React",
        companyName: "Công ty Rikkei Software",
        preferredBatch: "Đợt thực tập FE K21",
        durationWeeks: 10,
        hiredCandidates: [
            { userId: "u-5", candidateName: "Vũ Thị Hương", applicationId: "app-5" },
            { userId: "u-6", candidateName: "Hoàng Anh Tuấn", applicationId: "app-6" }
        ]
    },
    {
        jobId: "job-3",
        jobTitle: "Thực tập sinh DevOps",
        companyName: "Công ty ABC Tech",
        preferredBatch: "Đợt thực tập DevOps K20",
        durationWeeks: 8,
        hiredCandidates: [
            { userId: "u-7", candidateName: "Đặng Thu Thảo", applicationId: "app-7" },
            { userId: "u-8", candidateName: "Bùi Văn Nam", applicationId: "app-8" }
        ]
    },
    {
        jobId: "job-4",
        jobTitle: "Thực tập sinh Data Analyst",
        companyName: "Công ty XYZ Data",
        preferredBatch: "Đợt thực tập Data K22",
        durationWeeks: 14,
        hiredCandidates: [
            { userId: "u-9", candidateName: "Ngô Thị Mai", applicationId: "app-9" },
            { userId: "u-10", candidateName: "Đỗ Quang Huy", applicationId: "app-10" }
        ]
    }
];

export default function StaffPlacementPage() {
    const [selections, setSelections] = useState<Record<string, string>>({});

    const handlePlace = (userId: string) => {
        if (!selections[userId]) {
            alert("Vui lòng chọn đợt thực tập trước khi ghép!");
            return;
        }
        alert(`Đã ghép thành công ứng viên ${userId} vào đợt ${selections[userId]}! (Mock API)`);
    };

    return (
        <div className="container mx-auto py-8 px-4 max-w-6xl space-y-6">
            {/* Header */}
            <div className="flex items-center gap-3 border-b pb-4">
                <div className="bg-red-100 p-2 rounded-lg">
                    <UserPlus className="w-6 h-6 text-red-600" />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Ghép thực tập (IOC)</h1>
                    <p className="text-muted-foreground text-sm mt-1">Ghép ứng viên đã trúng tuyển vào các đợt thực tập.</p>
                </div>
            </div>

            <div className="space-y-6">
                {MOCK_DEMANDS.map((demand) => (
                    <Card key={demand.jobId} className="shadow-sm border-gray-200 overflow-hidden">
                        {/* Khung đỏ cho Header của Card */}
                        <CardHeader className="bg-gradient-to-r from-red-700 to-red-600 text-white p-5">
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                                <div>
                                    <CardTitle className="text-lg font-bold flex items-center gap-2">
                                        <Building2 className="w-5 h-5 text-red-200" />
                                        {demand.jobTitle}
                                    </CardTitle>
                                    <p className="text-sm text-red-100 mt-1 flex items-center gap-2">
                                        {demand.companyName} • <CalendarDays className="w-4 h-4" /> {demand.durationWeeks} tuần
                                    </p>
                                </div>
                                <div className="bg-white/20 px-3 py-1.5 rounded-lg text-sm font-medium backdrop-blur-sm flex items-center gap-2">
                                    <Users className="w-4 h-4" />
                                    {demand.hiredCandidates.length} ứng viên chờ
                                </div>
                            </div>
                        </CardHeader>

                        <CardContent className="p-0">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 border-b">
                                <tr>
                                    <th className="p-4 font-semibold text-gray-700">Tên ứng viên</th>
                                    <th className="p-4 font-semibold text-gray-700">Mã đơn</th>
                                    <th className="p-4 font-semibold text-gray-700">Chọn đợt thực tập</th>
                                    <th className="p-4 font-semibold text-gray-700 text-right">Hành động</th>
                                </tr>
                                </thead>
                                <tbody>
                                {demand.hiredCandidates.map((candidate) => (
                                    <tr key={candidate.userId} className="border-b last:border-0 hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-medium text-gray-900">{candidate.candidateName}</td>
                                        <td className="p-4 text-gray-500 font-mono text-xs">{candidate.applicationId}</td>
                                        <td className="p-4">
                                            <div className="relative w-full max-w-[280px]">
                                                <select
                                                    className="w-full appearance-none bg-white border border-gray-300 rounded-lg p-2.5 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500 transition-all cursor-pointer"
                                                    value={selections[candidate.userId] || ""}
                                                    onChange={(e) => setSelections(prev => ({ ...prev, [candidate.userId]: e.target.value }))}
                                                >
                                                    <option value="">-- Chọn đợt thực tập --</option>
                                                    <option value={demand.preferredBatch}>{demand.preferredBatch}</option>
                                                    <option value="Đợt thực tập dự phòng">Đợt thực tập dự phòng</option>
                                                </select>
                                                <ChevronDown className="absolute right-2.5 top-3 w-4 h-4 text-gray-500 pointer-events-none" />
                                            </div>
                                        </td>
                                        <td className="p-4 text-right">
                                            <button
                                                onClick={() => handlePlace(candidate.userId)}
                                                className="px-5 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors shadow-sm"
                                            >
                                                Ghép
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </CardContent>
                    </Card>
                ))}
            </div>
        </div>
    );
}