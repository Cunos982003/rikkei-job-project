import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Users } from 'lucide-react';

const MOCK_INTERNS = [
    {
        id: "int-1",
        candidateName: "Nguyễn Văn A",
        batchName: "Đợt thực tập Java K20",
        mentorEmail: "mentor1@rikkeijobs.test",
        acceptedTasksCount: 8,
        currentScore: 8.5
    }
];

export default function HrInternsPage() {
    return (
        <div className="container mx-auto py-8 px-4 max-w-5xl space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                    <Users className="w-6 h-6 text-blue-600" /> Thực tập sinh của tôi
                </h1>
            </div>

            <Card>
                <CardContent className="p-0">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 border-b">
                        <tr>
                            <th className="p-4 font-medium">Tên thực tập sinh</th>
                            <th className="p-4 font-medium">Đợt thực tập</th>
                            <th className="p-4 font-medium">Mentor</th>
                            <th className="p-4 font-medium text-center">Task nghiệm thu</th>
                            <th className="p-4 font-medium text-center">Điểm</th>
                        </tr>
                        </thead>
                        <tbody>
                        {MOCK_INTERNS.map((intern) => (
                            <tr key={intern.id} className="border-b">
                                <td className="p-4 font-medium">{intern.candidateName}</td>
                                <td className="p-4 text-gray-600">{intern.batchName}</td>
                                <td className="p-4 text-gray-600">{intern.mentorEmail}</td>
                                <td className="p-4 text-center">
                                    <Badge variant="outline">{intern.acceptedTasksCount} tasks</Badge>
                                </td>
                                <td className="p-4 text-center font-bold text-amber-600">{intern.currentScore}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </CardContent>
            </Card>
        </div>
    );
}