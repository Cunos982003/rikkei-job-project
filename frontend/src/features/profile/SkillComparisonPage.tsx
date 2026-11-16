import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart3, AlertTriangle, TrendingUp, CheckCircle2 } from 'lucide-react';

// Bổ sung nhiều dữ liệu mẫu để test các trường hợp
const MOCK_COMPARISON = [
    { id: "s1", name: "Java Core", measured: 85, self: 90, flag: 'MATCH' },
    { id: "s2", name: "Spring Boot", measured: 60, self: 85, flag: 'OVER' },
    { id: "s3", name: "ReactJS", measured: 80, self: 50, flag: 'UNDER' },
    { id: "s4", name: "Database (SQL)", measured: null, self: 75, flag: 'NO_DATA' },
    { id: "s5", name: "Git & CI/CD", measured: 75, self: 75, flag: 'MATCH' },
];

export default function SkillComparisonPage() {
    return (
        <div className="container mx-auto py-8 px-4 max-w-4xl space-y-6 animate-in fade-in duration-500">

            {/* Header */}
            <div className="flex items-center gap-3 border-b pb-4">
                <div className="bg-red-100 p-2 rounded-lg">
                    <BarChart3 className="w-6 h-6 text-red-600" />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">So sánh năng lực</h1>
                    <p className="text-gray-500 text-sm">Đối chiếu giữa Tự đánh giá và Kết quả đo lường thực tế.</p>
                </div>
            </div>

            <Card className="shadow-sm border-gray-200">
                <CardContent className="space-y-10 pt-6 pb-8 px-6 md:px-8">
                    {MOCK_COMPARISON.map((item) => (
                        <div key={item.id} className="space-y-4">

                            {/* Tên kỹ năng & Nhãn trạng thái */}
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                                <span className="font-bold text-lg text-gray-800">{item.name}</span>

                                <div className="flex items-center gap-2">
                                    {item.flag === 'OVER' && (
                                        <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200 gap-1 py-1 px-3">
                                            <TrendingUp className="w-3.5 h-3.5" /> Tự đánh giá cao hơn thực tế
                                        </Badge>
                                    )}
                                    {item.flag === 'UNDER' && (
                                        <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200 gap-1 py-1 px-3">
                                            <AlertTriangle className="w-3.5 h-3.5" /> Tự đánh giá thấp hơn thực tế
                                        </Badge>
                                    )}
                                    {item.flag === 'MATCH' && (
                                        <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200 gap-1 py-1 px-3">
                                            <CheckCircle2 className="w-3.5 h-3.5" /> Đánh giá chính xác
                                        </Badge>
                                    )}
                                    {item.flag === 'NO_DATA' && (
                                        <Badge variant="outline" className="bg-gray-50 text-gray-500 border-gray-200 py-1 px-3">
                                            Chưa có dữ liệu đo
                                        </Badge>
                                    )}
                                </div>
                            </div>

                            {/* Thanh biểu đồ Custom */}
                            <div className="relative pt-6 pb-2">
                                {/* Trục số */}
                                <div className="flex justify-between text-xs font-medium text-gray-400 mb-2">
                                    <span>0</span>
                                    <span>25</span>
                                    <span>50</span>
                                    <span>75</span>
                                    <span>100</span>
                                </div>

                                {/* Thanh nền */}
                                <div className="relative h-4 w-full bg-gray-100 rounded-full">
                                    {/* Vùng kết quả đo (Nền xám) */}
                                    {item.measured !== null && (
                                        <div
                                            className="absolute top-0 left-0 h-full bg-red-500 rounded-full opacity-30"
                                            style={{ width: `${item.measured}%` }}
                                        />
                                    )}

                                    {/* Vạch dọc Tự đánh giá (Màu đỏ) */}
                                    <div
                                        className="absolute top-[-6px] bottom-[-6px] w-1.5 bg-red-600 rounded-full z-10 shadow-sm"
                                        style={{ left: `calc(${item.self}% - 3px)` }}
                                    >
                                        <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-900 text-white text-xs py-1 px-2 rounded whitespace-nowrap">
                                            Tự: {item.self}
                                        </div>
                                    </div>

                                    {/* Vạch dọc Kết quả đo (Màu đen/xám đậm) */}
                                    {item.measured !== null && (
                                        <div
                                            className="absolute top-[-2px] bottom-[-2px] w-1 bg-gray-800 rounded-full z-10 shadow-sm"
                                            style={{ left: `calc(${item.measured}% - 2px)` }}
                                        >
                                            <div className="absolute -bottom-7 left-1/2 -translate-x-1/2 bg-white border text-gray-800 text-xs py-1 px-2 rounded whitespace-nowrap shadow-sm">
                                                Đo: {item.measured}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Chú thích chân trang cho từng item */}
                            <div className="flex justify-end text-xs text-gray-500 gap-4 mt-6">
                                <div className="flex items-center gap-1.5"><div className="w-3 h-3 bg-red-600 rounded-sm"></div> Tự đánh giá</div>
                                {item.measured !== null && <div className="flex items-center gap-1.5"><div className="w-3 h-3 bg-gray-800 rounded-sm"></div> Kết quả đo</div>}
                            </div>

                        </div>
                    ))}
                </CardContent>
            </Card>

            {/* Bảng chú thích chung */}
            <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 flex flex-col sm:flex-row gap-4 sm:gap-6 text-sm text-gray-600">
                <p className="flex items-center gap-2"><TrendingUp className="w-4 h-4 text-red-500"/> Lệch {'>'} 15 điểm: Đánh giá cao hơn</p>
                <p className="flex items-center gap-2"><AlertTriangle className="w-4 h-4 text-yellow-500"/> Lệch {'<'} -15 điểm: Đánh giá thấp hơn</p>
            </div>

        </div>
    );
}