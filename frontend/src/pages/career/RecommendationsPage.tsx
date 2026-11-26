import { zodResolver } from '@hookform/resolvers/zod'
import { BriefcaseBusiness, RefreshCw, Search } from 'lucide-react'
import { useMemo } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { Badge } from '../../components/common/Badge'
import { Button } from '../../components/common/Button'
import { Card, CardContent } from '../../components/common/Card'
import { Input } from '../../components/common/Input'
import { DataPanel, EmptyRecommendations, PageIntro, RecommendationList, RequestState } from '../../features/career/components/CareerShared'
import { useCareerData } from '../../features/career/careerUtils'

const searchSchema = z.object({ search: z.string() })
type SearchForm = z.infer<typeof searchSchema>

export function RecommendationsPage() {
  const { recommendations, isLoading, isSuccess, refetch } = useCareerData()
  const { register, control } = useForm<SearchForm>({
    resolver: zodResolver(searchSchema),
    defaultValues: { search: '' },
  })
  const search = useWatch({ control, name: 'search', defaultValue: '' })
  const filtered = useMemo(() => {
    const needle = search.trim().toLocaleLowerCase('vi')
    if (!needle) return recommendations
    return recommendations.filter((item) =>
      [item.jobId, item.jobTitle || '', item.companyName || '', item.explanation, ...item.skillGaps.map((skill) => skill.skillName)]
        .join(' ')
        .toLocaleLowerCase('vi')
        .includes(needle),
    )
  }, [recommendations, search])

  return (
    <>
      <PageIntro
        eyebrow="Cơ hội dành cho bạn"
        title="Việc làm phù hợp"
        description="Thứ tự đề xuất dựa trên mức độ đáp ứng các kỹ năng yêu cầu của từng vị trí."
        action={
          <div className="flex items-center gap-2">
            <Badge variant="default"><BriefcaseBusiness size={13} /> {recommendations.length} vị trí</Badge>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={refetch}
              disabled={isLoading}
              leftIcon={<RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />}
            >
              Làm mới
            </Button>
          </div>
        }
      />
      <RequestState />
      {recommendations.length === 0 && isSuccess && <EmptyRecommendations />}
      {recommendations.length > 0 && (
        <>
          <Card className="mb-4">
            <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
              <label className="relative block w-full sm:max-w-md">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <Input
                  aria-label="Tìm trong đề xuất"
                  className="pl-9"
                  placeholder="Tìm theo tên vị trí, kỹ năng, công ty..."
                  {...register('search')}
                />
              </label>
            </CardContent>
          </Card>
          <p className="mb-3 text-xs text-slate-500">
            {search.trim() ? `${filtered.length} kết quả phù hợp` : 'Ưu tiên vị trí có mức độ phù hợp cao'}
          </p>
          <DataPanel><RecommendationList items={filtered} /></DataPanel>
        </>
      )}
    </>
  )
}
