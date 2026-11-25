import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Users, Star, Mail, Briefcase, ChevronRight } from 'lucide-react';

// Bổ sung rất nhiều dữ liệu mẫu
const MOCK_COMPANY_INTERNS = [
    {
        id: "int-1",
        candidateName: "Nguyễn Văn A",
        batchName: "Đợt thực tập Java K20",
        mentorEmail: "mentor1@rikkeijobs.test",
        acceptedTasksCount: 8,
        currentScore: 8.5,
        status: "ACTIVE"
    },
    {
        id: "int-2",
        candidateName: "Trần Thị B",
        batchName: "Đợt thực tập Java K20",
        mentorEmail: "mentor2@rikkeijobs.test",
        acceptedTasksCount: 10,
        currentScore: 9.0,
        status: "ACTIVE"
    },
    {
        id: "int-3",
        candidateName: "Lê Hoàng Cường",
        batchName: "Đợt thực tập Frontend K21",
        mentorEmail: "mentor3@rikkeijobs.test",
        acceptedTasksCount: 6,
        currentScore: 7.8,
        status: "ACTIVE"
    },
    {
        id: "int-4",
        candidateName: "Phạm Minh Dũng",
        batchName: "Đợt thực tập Frontend K21",
        mentorEmail: "mentor1@rikkeijobs.test",
        acceptedTasksCount: 5,
        currentScore: 7.0,
        status: "WARNING"
    },
    {
        id: "int-5",
        candidateName: "Vũ Thị Hương",
        batchName: "Đợt thực tập DevOps K20",
        mentorEmail: "mentor2@rikkeijobs.test",
        acceptedTasksCount: 9,
        currentScore: 8.8,
        status: "ACTIVE"
    },
    {
        id: "int-6",
        candidateName: "Hoàng Anh Tuấn",
        batchName: "Đợt thực tập DevOps K20",
        mentorEmail: "mentor4@rikkeijobs.test",
        acceptedTasksCount: 7,
        currentScore: 8.2,
        status: "ACTIVE"
    }
];

export default function HrInternsPage() {
    return (
        <div className="container mx-auto py-8 px-4 max-w-6xl space-y-6">
            {/* Header */}
            <div className="flex items-center gap-3 border-b pb-4">
                <div className="bg-red-100 p-2 rounded-lg">
                    <Users className="w-6 h-6 text-red-600" />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Thực tập sinh của tôi</h1>
                    <p className="text-muted-foreground text-sm mt-1">Theo dõi tiến độ và kết quả thực tập của sinh viên tại công ty.</p>
                </div>
            </div>

            <Card className="shadow-sm border-gray-200 overflow-hidden">
                {/* Khung đỏ cho Header của Card */}
                <div className="bg-gradient-to-r from-red-700 to-red-600 p-5 text-white">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-bold flex items-center gap-2">
                            <Briefcase className="w-5 h-5 text-red-200" /> Danh sách thực tập sinh
                        </h2>
                        <Badge className="bg-white/20 text-white border-0 hover:bg-white/30 text-sm">
                            Tổng: {MOCK_COMPANY_INTERNS.length} sinh viên
                        </Badge>
                    </div>
                </div>

                <CardContent className="p-0">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 border-b">
                        <tr>
                            <th className="p-4 font-semibold text-gray-700">Tên thực tập sinh</th>
                            <th className="p-4 font-semibold text-gray-700">Đợt thực tập</th>
                            <th className="p-4 font-semibold text-gray-700">Mentor</th>
                            <th className="p-4 font-semibold text-gray-700 text-center">Task nghiệm thu</th>
                            <th className="p-4 font-semibold text-gray-700 text-center">Điểm tạm</th>
                            <th className="p-4 font-semibold text-gray-700 text-right"></th>
                        </tr>
                        </thead>
                        <tbody>
                        {MOCK_COMPANY_INTERNS.map((intern) => (
                            <tr key={intern.id} className="border-b last:border-0 hover:bg-gray-50 transition-colors">
                                <td className="p-4">
                                    <div className="flex items-center gap-2">
                                        <div className="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center text-red-700 font-bold text-xs">
                                            {intern.candidateName.charAt(0)}
                                        </div>
                                        <span className="font-medium text-gray-900">{intern.candidateName}</span>
                                    </div>
                                </td>
                                <td className="p-4 text-gray-600">{intern.batchName}</td>
                                <td className="p-4 text-gray-600 flex items-center gap-1.5">
                                    <Mail className="w-3.5 h-3.5 text-gray-400" />
                                    {intern.mentorEmail}
                                </td>
                                <td className="p-4 text-center">
                                    <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                                        {intern.acceptedTasksCount} tasks
                                    </Badge>
                                </td>
                                <td className="p-4 text-center">
                                    <div className="flex items-center justify-center gap-1 font-bold text-amber-600 text-lg">
                                        {intern.currentScore} <Star className="w-4 h-4 fill-amber-500 text-amber-500" />
                                    </div>
                                </td>
                                <td className="p-4 text-right">
                                    <button className="text-gray-400 hover:text-red-600 transition-colors">
                                        <ChevronRight className="w-5 h-5" />
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </CardContent>
            </Card>
        </div>
    );
}