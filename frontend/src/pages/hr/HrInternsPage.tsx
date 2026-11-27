import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, Calendar, CheckCircle2, MessageSquare, BarChart3, Clock } from 'lucide-react';

const MOCK_DETAIL = {
  internName: "Nguyễn Văn A",
  batchName: "Đợt thực tập Java K20",
  mentorEmail: "mentor1@rikkeijobs.test",
  finalScore: null,
  mentorComment: "Ứng viên có thái độ tốt, cần cải thiện thêm kỹ năng SQL. Tiềm năng phát triển thành Fullstack.",
  tasks: [
    { id: 't1', title: 'Thiết kế Database', score: 9, date: '2026-10-01' },
    { id: 't2', title: 'API Đăng nhập', score: 8, date: '2026-10-15' },
    { id: 't3', title: 'Tối ưu Query', score: 7, date: '2026-10-22' },
    { id: 't4', title: 'Viết Unit Test', score: 9, date: '2026-11-01' },
  ],
  chartData: [
    { week: 'Tuần 1', count: 1 },
    { week: 'Tuần 2', count: 1 },
    { week: 'Tuần 3', count: 2 },
    { week: 'Tuần 4', count: 3 },
  ]
};

export default function HrInternDetailPage() {
  const { userId: _userId } = useParams();
  const navigate = useNavigate();
  const data = MOCK_DETAIL;

  return (
      <div className="container mx-auto py-8 px-4 max-w-5xl space-y-6">
        <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-gray-500 hover:text-red-600 transition-colors">
          <ArrowLeft className="w-4 h-4" /> Quay lại danh sách
        </button>

        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-gradient-to-r from-red-700 to-red-600 text-white p-6 rounded-xl shadow-md">
          <div>
            <h1 className="text-2xl font-bold">{data.internName}</h1>
            <p className="text-red-100 flex items-center gap-2 mt-1">
              <Calendar className="w-4 h-4" /> {data.batchName}
            </p>
          </div>
          <div className="text-right">
            <p className="text-sm text-red-200">Mentor: {data.mentorEmail}</p>
            {data.finalScore != null ? (
                <p className="text-2xl font-bold mt-1">Điểm: {data.finalScore}/10</p>
            ) : (
                <Badge className="mt-2 bg-yellow-400 text-yellow-900 hover:bg-yellow-500 gap-1">
                  <Clock className="w-3 h-3"/> Đang thực tập
                </Badge>
            )}
          </div>
        </div>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <BarChart3 className="w-5 h-5 text-red-600"/> Tiến độ task theo tuần
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-end justify-between gap-4 h-48 pt-4 border-b pb-2">
              {data.chartData.map((item, idx) => (
                  <div key={idx} className="flex flex-col items-center flex-1 gap-2 h-full justify-end">
                    <span className="text-sm font-bold text-red-700">{item.count}</span>
                    <div
                        className="w-full max-w-[60px] bg-red-600 rounded-t-md transition-all duration-500 hover:bg-red-700"
                        style={{ height: `${(item.count / 5) * 100}%` }}
                    ></div>
                  </div>
              ))}
            </div>
            <div className="flex justify-between gap-4 mt-2">
              {data.chartData.map((item, idx) => (
                  <span key={idx} className="flex-1 text-center text-xs font-medium text-gray-500">
                {item.week}
              </span>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <MessageSquare className="w-5 h-5 text-red-600"/> Nhận xét từ Mentor
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-gray-700 italic bg-gray-50 p-4 rounded-lg border border-gray-100">
              "{data.mentorComment}"
            </p>
          </CardContent>
        </Card>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <CheckCircle2 className="w-5 h-5 text-red-600"/> Danh sách Task nghiệm thu
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <table className="w-full text-sm text-left">
              <thead className="bg-gray-50 border-b">
              <tr>
                <th className="p-4 font-medium">Tên Task</th>
                <th className="p-4 font-medium text-center">Điểm</th>
                <th className="p-4 font-medium">Ngày nghiệm thu</th>
              </tr>
              </thead>
              <tbody>
              {data.tasks.map(task => (
                  <tr key={task.id} className="border-b last:border-0 hover:bg-gray-50 transition-colors">
                    <td className="p-4 font-medium text-gray-900">{task.title}</td>
                    <td className="p-4 text-center text-blue-600 font-bold">{task.score}</td>
                    <td className="p-4 text-gray-600">{task.date}</td>
                  </tr>
              ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      </div>
  );
}