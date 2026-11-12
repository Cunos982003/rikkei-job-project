import { ArrowDownRight, BriefcaseBusiness, ChartNoAxesColumnIncreasing, CircleCheck, ShieldCheck, Target, TrendingUp } from 'lucide-react'
import { lazy, Suspense, useMemo, useState } from 'react'
import { Badge } from '../../components/common/Badge'
import { Button } from '../../components/common/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/common/Card'
import { ChartLoading, EmptyRecommendations, getSkillGaps, MetricCard, PageIntro, RequestState, useCareerData } from '../../features/career/components/CareerShared'

const SkillComparisonChart = lazy(() => import('../../features/career/charts').then((module) => ({ default: module.SkillComparisonChart })))

export function SkillsPage() {
  const { recommendations, isSuccess } = useCareerData()
  const [requiredOnly, setRequiredOnly] = useState(false)
  const allGaps = getSkillGaps(recommendations)
  const skillRows = useMemo(() => {
    const groups = new Map<string, { skillName: string; verifiedIndex: number | null; requiredIndex: number; gap: number; required: boolean; jobs: Set<string> }>()
    for (const gap of allGaps) {
      const current = groups.get(gap.skillName)
      if (!current) {
        groups.set(gap.skillName, {
          skillName: gap.skillName,
          verifiedIndex: gap.verifiedIndex,
          requiredIndex: gap.requiredIndex,
          gap: gap.gap,
          required: gap.required,
          jobs: new Set([gap.jobId]),
        })
      } else {
        current.requiredIndex = Math.max(current.requiredIndex, gap.requiredIndex)
        current.gap = Math.max(current.gap, gap.gap)
        current.required = current.required || gap.required
        current.jobs.add(gap.jobId)
        if (gap.verifiedIndex !== null && (current.verifiedIndex === null || gap.verifiedIndex > current.verifiedIndex)) {
          current.verifiedIndex = gap.verifiedIndex
        }
      }
    }
    return Array.from(groups.values()).sort((first, second) => second.gap - first.gap)
  }, [allGaps])
  const visibleRows = requiredOnly ? skillRows.filter((skill) => skill.required) : skillRows
  const chartData = visibleRows.slice(0, 10).map((skill) => ({
    ...skill,
    verified: skill.verifiedIndex ?? 0,
    chartLabel: skill.skillName.length > 18 ? `${skill.skillName.slice(0, 16)}…` : skill.skillName,
  }))
  const withGaps = skillRows.filter((skill) => skill.gap > 0).length

  return (
    <>
      <PageIntro
        eyebrow="Phát triển năng lực"
        title="Bản đồ kỹ năng của bạn"
        description="So sánh chỉ số kỹ năng đã xác thực với yêu cầu từ những công việc được đề xuất."
        action={
          <Button
            onClick={() => setRequiredOnly((only) => !only)}
            variant={requiredOnly ? 'secondary' : 'outline'}
          >
            <Target size={15} />
            {requiredOnly ? 'Đang xem kỹ năng bắt buộc' : 'Lọc kỹ năng bắt buộc'}
          </Button>
        }
      />
      <RequestState />
      {recommendations.length === 0 && isSuccess && <EmptyRecommendations />}
      {recommendations.length > 0 && (
        <>
          <div className="mb-5 grid gap-4 sm:grid-cols-3">
            <MetricCard label="Kỹ năng được phân tích" value={String(skillRows.length)} caption="Tổng hợp từ các vị trí phù hợp" icon={ChartNoAxesColumnIncreasing} tone="violet" />
            <MetricCard label="Khoảng trống kỹ năng" value={String(withGaps)} caption="Chỉ số hiện tại thấp hơn yêu cầu" icon={ArrowDownRight} tone="amber" />
            <MetricCard label="Vị trí được so sánh" value={String(recommendations.length)} caption="Dựa trên đề xuất hiện tại" icon={BriefcaseBusiness} tone="blue" />
          </div>
          {skillRows.length === 0 ? (
            <Card>
              <CardContent className="py-12 text-center">
                <div className="mx-auto grid size-12 place-items-center rounded-2xl bg-primary-50 text-primary"><CircleCheck size={21} /></div>
                <p className="mt-4 text-sm font-semibold text-slate-800">Chưa có dữ liệu kỹ năng để so sánh</p>
                <p className="mt-1 text-xs text-slate-500">Kỹ năng sẽ xuất hiện khi API trả về thông tin yêu cầu của vị trí.</p>
              </CardContent>
            </Card>
          ) : (
            <>
              <Card>
                <CardHeader className="flex-row items-start justify-between">
                  <div>
                    <CardTitle>Chỉ số hiện tại và mức yêu cầu</CardTitle>
                    <CardDescription className="mt-1">Kỹ năng chưa có chỉ số xác thực được thể hiện ở mức 0 trên biểu đồ</CardDescription>
                  </div>
                  <Badge variant="default"><TrendingUp size={13} /> Thang điểm 0–100</Badge>
                </CardHeader>
                <CardContent>
                  <Suspense fallback={<ChartLoading />}>
                    <SkillComparisonChart data={chartData} />
                  </Suspense>
                </CardContent>
              </Card>
              <Card className="mt-5 overflow-hidden">
                <CardHeader className="border-b border-slate-100">
                  <CardTitle>Chi tiết khoảng trống</CardTitle>
                  <CardDescription>Kỹ năng cần ưu tiên dựa trên khoảng cách tới mức yêu cầu cao nhất.</CardDescription>
                </CardHeader>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[620px] text-left text-xs">
                    <thead className="bg-slate-50/80 text-[10px] uppercase tracking-wider text-slate-400">
                      <tr>
                        <th className="px-6 py-3 font-semibold">Kỹ năng</th>
                        <th className="px-4 py-3 font-semibold">Chỉ số xác thực</th>
                        <th className="px-4 py-3 font-semibold">Mức yêu cầu</th>
                        <th className="px-4 py-3 font-semibold">Khoảng thiếu</th>
                        <th className="px-6 py-3 font-semibold">Loại</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {visibleRows.map((skill) => (
                        <tr key={skill.skillName} className="hover:bg-slate-50/70">
                          <td className="px-6 py-4 font-medium text-slate-800">{skill.skillName}</td>
                          <td className="px-4 py-4 text-slate-600">{skill.verifiedIndex === null ? <span className="text-slate-400">Chưa có dữ liệu</span> : `${skill.verifiedIndex}/100`}</td>
                          <td className="px-4 py-4 text-slate-600">{skill.requiredIndex}/100</td>
                          <td className="px-4 py-4">
                            <span className={`font-semibold ${skill.gap > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
                              {skill.gap > 0 ? `-${skill.gap}` : 'Đạt'}
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <Badge variant={skill.required ? 'warning' : 'default'}>{skill.required ? 'Bắt buộc' : 'Ưu tiên'}</Badge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <CardContent className="flex items-start gap-2 border-t border-slate-100 py-4 text-[11px] leading-5 text-slate-400">
                  <ShieldCheck className="mt-0.5 shrink-0" size={14} />
                  Chỉ số tự khai không được cộng vào năng lực chính thức. Một kỹ năng có thể xuất hiện ở nhiều vị trí; bảng tổng hợp mức yêu cầu cao nhất.
                </CardContent>
              </Card>
            </>
          )}
        </>
      )}
    </>
  )
}
