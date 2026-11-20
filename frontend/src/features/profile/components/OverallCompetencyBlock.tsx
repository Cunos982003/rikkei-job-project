import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { MOCK_COMPETENCY } from '../mocks/competencyMock';

export function OverallCompetencyBlock() {
    const [selectedBadge, setSelectedBadge] = useState<any>(null);
    const data = MOCK_COMPETENCY;

    return (
        <div className="space-y-6">
            {/* 1. Chỉ số năng lực chung */}
            <Card className="bg-gradient-to-r from-blue-900 to-indigo-900 text-white shadow-lg border-0">
                <CardContent className="p-8 flex items-center justify-between">
                    <div>
                        <h2 className="text-blue-200 font-medium mb-1 uppercase tracking-wider text-sm">Chỉ số năng lực tổng hợp</h2>
                        <div className="flex items-baseline gap-3">
                            <span className="text-6xl font-bold">{data.overallScore}</span>
                            <span className="text-xl text-blue-200">/ 100</span>
                        </div>
                    </div>
                    <div className="text-right">
                        <span className="inline-block bg-white/20 px-4 py-2 rounded-full text-lg font-semibold backdrop-blur-sm">Bậc: {data.level}</span>
                    </div>
                </CardContent>
            </Card>

            {/* 2. Dải huy hiệu */}
            <Card>
                <CardHeader><CardTitle className="text-lg">Huy hiệu đã xác thực</CardTitle></CardHeader>
                <CardContent>
                    <div className="flex flex-wrap gap-4">
                        {data.badges.map((badge) => (
                            <button key={badge.id} onClick={() => setSelectedBadge(badge)}
                                    className={`flex flex-col items-center justify-center p-4 w-28 h-28 rounded-xl border-2 transition-all hover:scale-105 ${badge.isGranted ? 'border-yellow-400 bg-yellow-50 shadow-md' : 'border-gray-200 bg-gray-50 opacity-50 grayscale'}`}>
                                <span className="text-4xl mb-2">{badge.icon}</span>
                                <span className="text-xs font-semibold text-center leading-tight">{badge.name}</span>
                            </button>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* 3. Chi tiết kỹ năng & đóng góp nguồn */}
            <Card>
                <CardHeader><CardTitle className="text-lg">Chi tiết năng lực</CardTitle></CardHeader>
                <CardContent className="space-y-6">
                    {data.skills.map((skill) => (
                        <div key={skill.skillId} className="space-y-2">
                            <div className="flex justify-between items-end">
                                <div className="flex items-center gap-2">
                                    <span className="font-semibold">{skill.skillName}</span>
                                    <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{skill.level}</span>
                                </div>
                                <span className="font-bold text-blue-600">{skill.verifiedScore}</span>
                            </div>
                            <Progress value={skill.verifiedScore} className="h-2" />
                            <div className="flex flex-wrap gap-1.5 mt-2">
                                {skill.contributions.map((c) => (
                                    <span key={c.source} className="text-xs bg-gray-50 border px-2 py-1 rounded">
                    {c.source}: <span className="font-semibold">{c.score}</span> (x{c.weight}%)
                  </span>
                                ))}
                            </div>
                        </div>
                    ))}
                </CardContent>
            </Card>

            {/* Dialog xem điều kiện huy hiệu */}
            <Dialog open={!!selectedBadge} onOpenChange={() => setSelectedBadge(null)}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2 text-2xl"><span className="text-4xl">{selectedBadge?.icon}</span>{selectedBadge?.name}</DialogTitle>
                        <DialogDescription className="pt-4 text-base"><strong>Điều kiện:</strong><br />{selectedBadge?.ruleDescription}</DialogDescription>
                    </DialogHeader>
                    <div className="mt-4 p-3 bg-gray-50 rounded-lg text-sm">
                        {selectedBadge?.isGranted ? <p className="text-green-600 font-medium">✅ Đã đạt được vào {new Date(selectedBadge.grantedAt).toLocaleDateString('vi-VN')}</p> : <p className="text-gray-500 font-medium">❌ Chưa đạt được</p>}
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
}