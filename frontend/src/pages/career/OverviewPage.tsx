import { ArrowRight, BriefcaseBusiness, ChartNoAxesColumnIncreasing, CircleCheck, ShieldCheck, Target, TrendingUp } from 'lucide-react'
import { lazy, Suspense } from 'react'
import { Link } from 'react-router-dom'
import { Badge } from '../../components/common/Badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/common/Card'
import { ChartLoading, EmptyRecommendations, formatScore, getSkillGaps, MetricCard, PageIntro, RecommendationCard, RequestState, useCareerData } from '../../features/career/components/CareerShared'

const ScoreChart = lazy(() => import('../../features/career/charts').then((module) => ({ default: module.ScoreChart })))

export function OverviewPage() {
  const { recommendations, isLoading, isSuccess } = useCareerData()
  const gaps = getSkillGaps(recommendations)
  const missingRequired = new Set(
    gaps.filter((gap) => gap.required && gap.gap > 0).map((gap) => gap.skillName),
  ).size
  const average = recommendations.length
    ? recommendations.reduce((sum, item) => sum + item.score, 0) / recommendations.length
    : 0

  return (
    <>
      <PageIntro
        eyebrow="AI career assistant"
        title="Chào bạn, sẵn sàng cho bước tiếp theo?"
        description="Khám phá cơ hội phù hợp với năng lực đã được xác thực và xem kỹ năng nào sẽ giúp bạn tiến xa hơn."
        action={
          <Link to="/candidate/career/recommendations" className="inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium hover:bg-slate-50">
            Khám phá việc làm <ArrowRight size={15} />
          </Link>
        }
      />
      <RequestState />
      {isLoading ? null : recommendations.length === 0 ? (
        isSuccess && <EmptyRecommendations />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard label="Cơ hội phù hợp" value={String(recommendations.length)} caption="Vị trí được đề xuất cho bạn" icon={BriefcaseBusiness} tone="violet" />
            <MetricCard label="Điểm phù hợp trung bình" value={formatScore(average)} caption="Dựa trên kỹ năng đã xác thực" icon={TrendingUp} tone="blue" />
            <MetricCard label="Kỹ năng được đối chiếu" value={String(new Set(gaps.map((gap) => gap.skillName)).size)} caption="So sánh theo yêu cầu công việc" icon={ChartNoAxesColumnIncreasing} tone="emerald" />
            <MetricCard label="Kỹ năng cần bồi đắp" value={String(missingRequired)} caption="Khoảng trống ở kỹ năng bắt buộc" icon={Target} tone="amber" />
          </div>

          <div className="mt-5 grid gap-5 xl:grid-cols-[1.4fr_0.8fr]">
            <Card>
              <CardHeader className="flex-row items-start justify-between">
                <div>
                  <CardTitle>Điểm phù hợp theo vị trí</CardTitle>
                  <CardDescription className="mt-1">Mức độ đáp ứng kỹ năng cho từng cơ hội</CardDescription>
                </div>
                <Badge><TrendingUp size={13} /> DN-06</Badge>
              </CardHeader>
              <CardContent>
                <Suspense fallback={<ChartLoading />}>
                  <ScoreChart recommendations={recommendations} />
                </Suspense>
              </CardContent>
            </Card>
            <Card className="overflow-hidden bg-gradient-to-br from-[#fdf2f2] to-white">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <div className="grid size-8 place-items-center rounded-lg bg-primary-100 text-primary-700"><ShieldCheck size={16} /></div>
                  <CardTitle>Điểm số đáng tin cậy</CardTitle>
                </div>
                <CardDescription className="mt-2">Đề xuất dựa trên bằng chứng kỹ năng đã được xác thực.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                {[
                  'Kỹ năng tự khai không cộng vào điểm chính thức.',
                  'Thiếu kỹ năng bắt buộc vẫn được hiển thị rõ ràng.',
                  'Điểm phù hợp được tính từ yêu cầu của từng vị trí.',
                ].map((text) => (
                  <div key={text} className="flex gap-2.5 text-xs leading-5 text-slate-600">
                    <CircleCheck className="mt-0.5 shrink-0 text-emerald-500" size={15} />
                    {text}
                  </div>
                ))}
                <Link to="/candidate/career/skills" className="inline-flex items-center gap-1 pt-1 text-xs font-semibold text-primary-700 hover:text-primary-800">
                  Xem phân tích kỹ năng <ArrowRight size={14} />
                </Link>
              </CardContent>
            </Card>
          </div>

          <div className="mb-3 mt-8 flex items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold text-slate-900">Cơ hội nổi bật</h2>
              <p className="mt-1 text-xs text-slate-500">Những vị trí có điểm phù hợp cao nhất</p>
            </div>
            <Link to="/candidate/career/recommendations" className="inline-flex items-center gap-1 text-xs font-semibold text-primary-700 hover:text-primary-800">
              Xem tất cả <ArrowRight size={14} />
            </Link>
          </div>
          <div className="space-y-3">
            {recommendations.slice(0, 3).map((item) => <RecommendationCard key={item.jobId} item={item} />)}
          </div>
        </>
      )}
    </>
  )
}
