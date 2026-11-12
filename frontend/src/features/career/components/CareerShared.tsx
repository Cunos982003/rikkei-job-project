import { AlertCircle, BriefcaseBusiness, CircleCheck, Clock3, RefreshCw, Search, Target } from 'lucide-react'
import type { ReactNode } from 'react'
import { useOutletContext } from 'react-router-dom'
import { getCareerApiError, type JobRecommendation } from '../../../api/endpoints/career.api'
import { Badge } from '../../../components/common/Badge'
import { Button } from '../../../components/common/Button'
import { Card, CardContent } from '../../../components/common/Card'
import { formatDate } from '../../../utils/formatters'
import type { CareerOutletContext } from '../CareerLayout'

export const formatScore = (score: number) => `${Math.round(score)}%`

export function ChartLoading() {
  return <div className="h-[245px] w-full animate-pulse rounded-xl bg-slate-50" aria-label="Đang tải biểu đồ" />
}

export function useCareerData() {
  return useOutletContext<CareerOutletContext>()
}

export function PageIntro({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary">{eyebrow}</p>
        <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 sm:text-[30px]">{title}</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">{description}</p>
      </div>
      {action}
    </div>
  )
}

export function RequestState() {
  const { error, isLoading, refetch } = useCareerData()
  const apiError = error ? getCareerApiError(error) : undefined

  if (isLoading) {
    return (
      <Card className="overflow-hidden">
        <div className="h-1 animate-pulse bg-primary-300" />
        <CardContent className="flex items-center gap-4 py-8">
          <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-primary-50 text-primary">
            <RefreshCw className="animate-spin" size={19} />
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-800">Đang tải gợi ý việc làm</p>
            <p className="mt-1 text-xs text-slate-500">Đang lấy dữ liệu phù hợp từ AI Career Service…</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (!apiError) return null

  const isAuthError = apiError.status === 401
  const isUnavailable = apiError.status === 503

  return (
    <Card className="border-amber-200/80 bg-white">
      <CardContent className="flex flex-col gap-4 py-6 sm:flex-row sm:items-start">
        <div className={`grid size-11 shrink-0 place-items-center rounded-xl ${isUnavailable ? 'bg-orange-50 text-orange-600' : 'bg-amber-50 text-amber-600'}`}>
          <AlertCircle size={20} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-slate-900">
            {isAuthError
              ? 'Cần xác thực để xem đề xuất'
              : isUnavailable
                ? 'Dữ liệu gợi ý chưa sẵn sàng'
                : 'Chưa thể tải dữ liệu từ máy chủ'}
          </p>
          <p className="mt-1 text-sm leading-6 text-slate-500">{apiError.message}</p>
          {isAuthError && (
            <p className="mt-2 text-xs leading-5 text-slate-400">
              Vui lòng đăng nhập lại để tiếp tục.
            </p>
          )}
          {isUnavailable && (
            <p className="mt-2 text-xs leading-5 text-slate-400">
              Adapter hồ sơ và việc làm cần được kết nối ở backend trước khi API trả về dữ liệu.
            </p>
          )}
        </div>
        <Button className="shrink-0" onClick={refetch} variant="outline">
          <RefreshCw size={15} />
          Thử lại
        </Button>
      </CardContent>
    </Card>
  )
}

export function EmptyRecommendations() {
  return (
    <Card>
      <CardContent className="py-12 text-center">
        <div className="mx-auto grid size-12 place-items-center rounded-2xl bg-primary-50 text-primary">
          <BriefcaseBusiness size={20} />
        </div>
        <p className="mt-4 text-sm font-semibold text-slate-800">Chưa có việc làm phù hợp</p>
        <p className="mx-auto mt-1 max-w-md text-xs leading-5 text-slate-500">
          API đã phản hồi thành công nhưng chưa tìm thấy vị trí phù hợp. Hãy quay lại sau khi hồ sơ hoặc dữ liệu việc làm được cập nhật.
        </p>
      </CardContent>
    </Card>
  )
}

export function DataPanel({ children }: { children: ReactNode }) {
  const { recommendations } = useCareerData()
  return recommendations.length === 0 ? <RequestState /> : <>{children}</>
}

export function MetricCard({
  label,
  value,
  caption,
  icon: Icon,
  tone,
}: {
  label: string
  value: string
  caption: string
  icon: typeof BriefcaseBusiness
  tone: 'violet' | 'blue' | 'amber' | 'emerald'
}) {
  const tones = {
    violet: 'bg-primary-50 text-primary',
    blue: 'bg-sky-50 text-sky-600',
    amber: 'bg-amber-50 text-amber-600',
    emerald: 'bg-emerald-50 text-emerald-600',
  }

  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium text-slate-500">{label}</p>
          <p className="mt-3 text-2xl font-bold tracking-tight text-slate-900">{value}</p>
        </div>
        <div className={`grid size-10 place-items-center rounded-xl ${tones[tone]}`}>
          <Icon size={19} />
        </div>
      </div>
      <p className="mt-3 text-xs text-slate-400">{caption}</p>
    </Card>
  )
}

export function getSkillGaps(recommendations: JobRecommendation[]) {
  return recommendations.flatMap((recommendation) =>
    recommendation.skillGaps.map((gap) => ({ ...gap, jobId: recommendation.jobId })),
  )
}

export function RecommendationCard({ item }: { item: JobRecommendation }) {
  const missing = item.missingRequiredSkills.length
  return (
    <Card className="group transition-all hover:-translate-y-0.5 hover:border-primary-200 hover:shadow-md hover:shadow-primary-100/60">
      <CardContent className="p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
          <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-primary-50 to-primary-50 text-primary">
            <BriefcaseBusiness size={19} />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-sm font-semibold text-slate-900">Cơ hội việc làm</h3>
              {missing === 0 ? (
                <Badge variant="verified"><CircleCheck size={12} /> Đủ kỹ năng bắt buộc</Badge>
              ) : (
                <Badge variant="warning"><AlertCircle size={12} /> Thiếu {missing} kỹ năng bắt buộc</Badge>
              )}
            </div>
            <p className="mt-1 text-xs text-slate-400">Mã vị trí: {item.jobId}</p>
            <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">{item.explanation}</p>
            <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-400">
              <span className="inline-flex items-center gap-1.5"><Clock3 size={13} /> Cập nhật {formatDate(item.createdAt)}</span>
              {item.skillGaps.length > 0 && (
                <span className="inline-flex items-center gap-1.5"><Target size={13} /> {item.skillGaps.length} kỹ năng được đối chiếu</span>
              )}
            </div>
            {item.skillGaps.length > 0 && (
              <div className="mt-4 flex flex-wrap gap-2">
                {item.skillGaps.slice(0, 4).map((skill) => (
                  <span key={skill.skillName} className="rounded-lg bg-slate-50 px-2.5 py-1.5 text-[11px] text-slate-600">
                    {skill.skillName}
                  </span>
                ))}
                {item.skillGaps.length > 4 && (
                  <span className="rounded-lg bg-slate-50 px-2.5 py-1.5 text-[11px] font-medium text-slate-500">
                    +{item.skillGaps.length - 4} kỹ năng
                  </span>
                )}
              </div>
            )}
          </div>
          <div className="flex shrink-0 items-center gap-3 sm:flex-col sm:items-end sm:gap-1">
            <div className="text-2xl font-bold tracking-tight text-primary-700">{formatScore(item.score)}</div>
            <div className="text-[11px] font-medium text-slate-400">độ phù hợp</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

export function RecommendationList({ items }: { items: JobRecommendation[] }) {
  if (!items.length) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <div className="mx-auto grid size-12 place-items-center rounded-2xl bg-slate-100 text-slate-400"><Search size={20} /></div>
          <p className="mt-4 text-sm font-semibold text-slate-800">Không tìm thấy kết quả</p>
          <p className="mt-1 text-xs text-slate-500">Thử một từ khóa khác để tìm trong mã vị trí, kỹ năng hoặc giải thích.</p>
        </CardContent>
      </Card>
    )
  }

  return <div className="space-y-3">{items.map((item) => <RecommendationCard key={item.jobId} item={item} />)}</div>
}
