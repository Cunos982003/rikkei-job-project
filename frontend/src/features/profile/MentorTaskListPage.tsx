import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { MOCK_MENTOR_TASKS } from './mocks/mentorTasksMock';

export default function MentorTaskListPage() {
    const [tab, setTab] = useState<'INTERNSHIP' | 'INTERVIEW'>('INTERNSHIP');

    // Lọc dữ liệu theo Tab (Đã bỏ khai báo kiểu EvaluationTask[] vì đã xóa import thừa)
    const tasks = MOCK_MENTOR_TASKS.filter((t) => t.context === tab);
    const pendingCount = tasks.filter((t) => t.status === 'PENDING').length;

    const renderStatus = (status: string) => {
        switch (status) {
            case 'PENDING':
                return (
                    <Badge variant="outline" className="text-red-600 border-red-200 bg-red-50">
                        Chưa làm
                    </Badge>
                );
            case 'DRAFT':
                return (
                    <Badge variant="outline" className="text-yellow-600 border-yellow-200 bg-yellow-50">
                        Nháp
                    </Badge>
                );
            case 'SUBMITTED':
                return (
                    <Badge variant="outline" className="text-green-600 border-green-200 bg-green-50">
                        Đã gửi
                    </Badge>
                );
            default:
                return null;
        }
    };

    return (
        <div className="container mx-auto py-8 px-4 max-w-5xl space-y-6">
            <div>
                <h1 className="text-2xl font-bold">Phiếu đánh giá cần làm</h1>
                <p className="text-muted-foreground text-sm mt-1">
                    Bạn có <strong className="text-red-600">{pendingCount}</strong> phiếu chưa hoàn thành trong mục này.
                </p>
            </div>

            {/* Tabs */}
            <div className="flex gap-4 border-b">
                <button
                    onClick={() => setTab('INTERNSHIP')}
                    className={`pb-2 px-2 text-sm font-medium border-b-2 transition-colors ${
                        tab === 'INTERNSHIP'
                            ? 'border-primary text-primary'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    Thực tập (IOC)
                </button>
                <button
                    onClick={() => setTab('INTERVIEW')}
                    className={`pb-2 px-2 text-sm font-medium border-b-2 transition-colors ${
                        tab === 'INTERVIEW'
                            ? 'border-primary text-primary'
                            : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                >
                    Phỏng vấn
                </button>
            </div>

            {/* Bảng danh sách */}
            <Card>
                <CardContent className="p-0">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 border-b">
                        <tr>
                            <th className="p-4 font-medium">Tên ứng viên</th>
                            <th className="p-4 font-medium">
                                {tab === 'INTERNSHIP' ? 'Đợt thực tập' : 'Vòng phỏng vấn'}
                            </th>
                            <th className="p-4 font-medium">Hạn chót</th>
                            <th className="p-4 font-medium">Trạng thái</th>
                            <th className="p-4 font-medium text-right">Hành động</th>
                        </tr>
                        </thead>
                        <tbody>
                        {tasks.length === 0 && (
                            <tr>
                                <td colSpan={5} className="p-8 text-center text-muted-foreground">
                                    Không có phiếu nào cần đánh giá.
                                </td>
                            </tr>
                        )}
                        {tasks.map((task) => (
                            <tr key={task.id} className="border-b last:border-0 hover:bg-gray-50">
                                <td className="p-4 font-medium">{task.candidateName}</td>
                                <td className="p-4 text-gray-600">{task.batchOrRoundName}</td>
                                <td className="p-4 text-gray-600">
                                    {new Date(task.deadline).toLocaleDateString('vi-VN')}
                                </td>
                                <td className="p-4">{renderStatus(task.status)}</td>
                                <td className="p-4 text-right">
                                    <Link
                                        to={`/mentor/evaluations/${task.id}`}
                                        className="text-blue-600 font-medium hover:underline"
                                    >
                                        {task.status === 'PENDING' ? 'Nhập phiếu' : 'Xem/Sửa'}
                                    </Link>
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