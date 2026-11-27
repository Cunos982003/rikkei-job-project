import { useState, useEffect } from 'react';
import { format, subDays } from 'date-fns';
import { getReport, exportReport } from '../api/recruitmentReport';
import { XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, FunnelChart, Funnel, LabelList } from 'recharts';

export default function RecruitmentReportPage() {
    const [loading, setLoading] = useState(false);
    const [report, setReport] = useState<any>(null);

    // State cho Bộ lọc
    const [fromDate, setFromDate] = useState(format(subDays(new Date(), 30), 'yyyy-MM-dd'));
    const [toDate, setToDate] = useState(format(new Date(), 'yyyy-MM-dd'));
    const [companyId, setCompanyId] = useState('');
    const [jobId, setJobId] = useState('');
    const [courseName, setCourseName] = useState('');

    // Gọi API lấy dữ liệu
    const fetchReport = async () => {
        setLoading(true);
        try {
            const fromIso = new Date(fromDate).toISOString();
            const toIso = new Date(toDate + 'T23:59:59').toISOString();
            const data = await getReport({
                from: fromIso,
                to: toIso,
                companyId: companyId || undefined,
                jobId: jobId || undefined,
                courseName: courseName || undefined
            });
            setReport(data);
        } catch (error) {
            console.error('Lỗi lấy báo cáo', error);
            setReport(null); // Đảm bảo clear dữ liệu cũ nếu lỗi
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReport();
    }, []);

    const handleExport = async () => {
        try {
            const fromIso = new Date(fromDate).toISOString();
            const toIso = new Date(toDate + 'T23:59:59').toISOString();
            await exportReport({
                from: fromIso,
                to: toIso,
                companyId: companyId || undefined,
                jobId: jobId || undefined,
                courseName: courseName || undefined
            });
        } catch (error) {
            console.error("Lỗi xuất Excel", error);
            alert("Có lỗi xảy ra khi xuất file Excel");
        }
    };

    if (loading && !report) return <div className="p-8 text-center mt-10">Đang tải báo cáo...</div>;

    // Nếu không có dữ liệu (null) thì báo lỗi nhẹ nhàng thay vì crash
    if (!report) return (
        <div className="p-8 text-center mt-10 bg-white rounded-xl shadow-sm border border-slate-200 max-w-lg mx-auto">
            <p className="text-red-500 font-medium">Không thể tải dữ liệu báo cáo.</p>
            <p className="text-slate-500 text-sm mt-2">Vui lòng kiểm tra lại kết nối Backend hoặc dữ liệu.</p>
            <button onClick={fetchReport} className="mt-4 bg-blue-600 text-white px-4 py-2 rounded-lg">Thử lại</button>
        </div>
    );

    // Lấy dữ liệu an toàn (Fallback về mảng rỗng hoặc số 0 nếu API trả thiếu)
    const totals = report.totals || { views: 0, applications: 0, interviews: 0, offers: 0, hires: 0 };
    const funnelData = report.funnel || [];
    const dailyData = report.byDay || [];
    const jobData = report.byJob || [];

    return (
        <div className="p-8 space-y-6 bg-slate-50 min-h-screen">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900">Báo cáo tuyển dụng</h1>
                    <p className="text-slate-500 text-sm mt-1">Thống kê hiệu quả tuyển dụng</p>
                </div>
                <button onClick={handleExport} className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg font-medium transition-colors">
                    Xuất Excel
                </button>
            </div>

            {/* Bộ lọc */}
            <div className="flex flex-wrap gap-4 bg-white p-4 rounded-xl shadow-sm border border-slate-200">
                <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Từ ngày</label>
                    <input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} className="border border-slate-300 p-2 rounded-lg text-sm outline-none focus:border-blue-500" />
                </div>
                <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Đến ngày</label>
                    <input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} className="border border-slate-300 p-2 rounded-lg text-sm outline-none focus:border-blue-500" />
                </div>
                <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Công ty</label>
                    <select value={companyId} onChange={(e) => setCompanyId(e.target.value)} className="border border-slate-300 p-2 rounded-lg text-sm outline-none focus:border-blue-500 min-w-[150px]">
                        <option value="">Tất cả công ty</option>
                        {/* Hardcode ID công ty mẫu theo seed data của bạn */}
                        <option value="00000000-0000-0000-0000-000000000001">Công ty A</option>
                        <option value="00000000-0000-0000-0000-000000000002">Công ty B</option>
                    </select>
                </div>
                <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">JD (Tin tuyển dụng)</label>
                    <select value={jobId} onChange={(e) => setJobId(e.target.value)} className="border border-slate-300 p-2 rounded-lg text-sm outline-none focus:border-blue-500 min-w-[150px]">
                        <option value="">Tất cả JD</option>
                        <option value="00000000-0000-0000-0000-000000000011">Java Backend</option>
                        <option value="00000000-0000-0000-0000-000000000012">React Frontend</option>
                    </select>
                </div>
                <div>
                    <label className="block text-xs font-medium text-slate-700 mb-1">Khoá học</label>
                    <select value={courseName} onChange={(e) => setCourseName(e.target.value)} className="border border-slate-300 p-2 rounded-lg text-sm outline-none focus:border-blue-500 min-w-[150px]">
                        <option value="">Tất cả khoá</option>
                        <option value="Java Backend">Java Backend</option>
                        <option value="React Frontend">React Frontend</option>
                    </select>
                </div>
                <div className="flex items-end">
                    <button onClick={fetchReport} className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition-colors">
                        Lọc dữ liệu
                    </button>
                </div>
            </div>

            {/* Thẻ số tổng - Đã bọc Optional Chaining */}
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                <SummaryCard title="Lượt xem JD" value={totals.views} color="bg-blue-50 text-blue-700 border-blue-200" />
                <SummaryCard title="Ứng tuyển" value={totals.applications} color="bg-green-50 text-green-700 border-green-200" />
                <SummaryCard title="Phỏng vấn" value={totals.interviews} color="bg-yellow-50 text-yellow-700 border-yellow-200" />
                <SummaryCard title="Offer" value={totals.offers} color="bg-orange-50 text-orange-700 border-orange-200" />
                <SummaryCard title="Nhận việc" value={totals.hires} color="bg-purple-50 text-purple-700 border-purple-200" />
            </div>

            {/* Biểu đồ - Bọc fallback mảng rỗng */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Phễu */}
                <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200">
                    <h2 className="font-semibold text-slate-800 mb-4">Tỉ lệ chuyển đổi (Phễu)</h2>
                    <ResponsiveContainer width="100%" height={300}>
                        <FunnelChart>
                            <Tooltip formatter={(value: number) => [`${value}`, 'Số lượng']} />
                            <Funnel dataKey="count" data={funnelData} isAnimationActive>
                                <LabelList position="right" fill="#000" stroke="none" dataKey="step" />
                            </Funnel>
                        </FunnelChart>
                    </ResponsiveContainer>
                </div>

                {/* Biểu đồ theo ngày */}
                <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200">
                    <h2 className="font-semibold text-slate-800 mb-4">Xu hướng theo ngày</h2>
                    <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={dailyData}>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} />
                            <XAxis dataKey="date" tick={{fontSize: 12}} />
                            <YAxis tick={{fontSize: 12}} />
                            <Tooltip />
                            <Bar dataKey="views" fill="#3b82f6" name="Lượt xem" radius={[4, 4, 0, 0]} />
                            <Bar dataKey="applications" fill="#22c55e" name="Ứng tuyển" radius={[4, 4, 0, 0]} />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Bảng theo JD */}
            <div className="bg-white p-5 rounded-xl shadow-sm border border-slate-200">
                <h2 className="font-semibold text-slate-800 mb-4">Thống kê theo JD</h2>
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead className="bg-slate-50 text-slate-600 text-sm">
                        <tr>
                            <th className="p-3 border-b border-slate-200 font-medium">Tên JD</th>
                            <th className="p-3 border-b border-slate-200 font-medium text-center">Lượt xem</th>
                            <th className="p-3 border-b border-slate-200 font-medium text-center">Ứng tuyển</th>
                        </tr>
                        </thead>
                        <tbody>
                        {jobData.map((job: any) => (
                            <tr key={job.jobId} className="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                                <td className="p-3 font-medium text-slate-800">{job.jobTitle || "Chưa có tên"}</td>
                                <td className="p-3 text-center text-slate-600">{job.views || 0}</td>
                                <td className="p-3 text-center text-slate-600">{job.applications || 0}</td>
                            </tr>
                        ))}
                        {jobData.length === 0 && (
                            <tr>
                                <td colSpan={3} className="p-4 text-center text-slate-500">Không có dữ liệu trong khoảng thời gian này</td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

function SummaryCard({ title, value, color }: { title: string; value: number; color: string }) {
    return (
        <div className={`p-5 rounded-xl border ${color} shadow-sm flex flex-col justify-center`}>
            <p className="text-sm font-medium opacity-80 mb-1">{title}</p>
            <p className="text-3xl font-bold">{value || 0}</p>
        </div>
    );
}