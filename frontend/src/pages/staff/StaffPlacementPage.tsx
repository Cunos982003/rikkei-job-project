import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

// Mock data tạm thời (Sau này thay bằng gọi API)
const MOCK_DEMANDS = [
    {
        jobId: "job-1",
        jobTitle: "Thực tập sinh Java Backend",
        companyName: "Công ty Rikkei Software",
        preferredBatch: "Đợt thực tập Java K20",
        durationWeeks: 12,
        hiredCandidates: [
            { userId: "u-1", candidateName: "Nguyễn Văn A", applicationId: "app-1" },
            { userId: "u-2", candidateName: "Trần Thị B", applicationId: "app-2" }
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
        alert(`Đã ghép thành công! (Mock API)`);
    };

    return (
        <div className="container mx-auto py-8 px-4 max-w-5xl space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900">Ghép thực tập (IOC)</h1>
                <p className="text-muted-foreground text-sm mt-1">Ghép ứng viên đã trúng tuyển vào các đợt thực tập.</p>
            </div>

            <div className="space-y-6">
                {MOCK_DEMANDS.map((demand) => (
                    <Card key={demand.jobId}>
                        <CardHeader className="bg-gray-50 border-b">
                            <CardTitle className="text-lg text-blue-800">{demand.jobTitle}</CardTitle>
                            <p className="text-sm text-gray-500">{demand.companyName} • {demand.durationWeeks} tuần</p>
                        </CardHeader>
                        <CardContent className="p-6">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-gray-50 border-b">
                                <tr>
                                    <th className="p-3 font-medium">Tên ứng viên</th>
                                    <th className="p-3 font-medium">Chọn đợt thực tập</th>
                                    <th className="p-3 font-medium text-right">Hành động</th>
                                </tr>
                                </thead>
                                <tbody>
                                {demand.hiredCandidates.map((candidate) => (
                                    <tr key={candidate.userId} className="border-b">
                                        <td className="p-3 font-medium">{candidate.candidateName}</td>
                                        <td className="p-3">
                                            <select
                                                className="border rounded p-2 text-sm w-full max-w-[250px]"
                                                onChange={(e) => setSelections(prev => ({ ...prev, [candidate.userId]: e.target.value }))}
                                            >
                                                <option value="">-- Chọn đợt --</option>
                                                <option value={demand.preferredBatch}>{demand.preferredBatch}</option>
                                            </select>
                                        </td>
                                        <td className="p-3 text-right">
                                            <button onClick={() => handlePlace(candidate.userId)} className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm">
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