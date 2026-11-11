import { useState } from 'react'
import { useProfileEvaluations } from '@/api/hooks/useProfileEvaluations'
import type { MentorEvaluation } from '@/api/types/evaluation'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { EvaluationDetailDialog } from '../EvaluationDetailDialog'

const CONTEXT_LABEL: Record<string, string> = {
  INTERNSHIP: 'Thực tập',
  INTERVIEW: 'Phỏng vấn',
}

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(iso))
}

export function MentorEvaluationsBlock({ userId }: { userId: string | undefined }) {
  const { data, isLoading, isError } = useProfileEvaluations(userId)
  const [selected, setSelected] = useState<MentorEvaluation | null>(null)

  return (
      <Card>
        <CardHeader>
          <CardTitle>Đánh giá từ mentor</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {isLoading && (
              <>
                <Skeleton className="h-14 w-full" />
                <Skeleton className="h-14 w-full" />
              </>
          )}

          {isError && (
              <p className="text-sm text-destructive">Không tải được đánh giá từ mentor.</p>
          )}

          {data && data.length === 0 && (
              <p className="text-sm text-muted-foreground">Chưa có đánh giá nào từ mentor.</p>
          )}

          {data?.map((e) => (
              <button
                  key={e.id}
                  type="button"
                  onClick={() => setSelected(e)}
                  className="flex w-full items-center justify-between gap-4 rounded-md border p-3 text-left transition-colors hover:bg-accent"
              >
            <span className="space-y-1">
              <span className="block font-medium">{e.templateName}</span>
              <span className="block text-sm text-muted-foreground">Mentor {e.mentorName}</span>
            </span>
                <span className="flex flex-col items-end gap-1">
              <Badge variant="secondary">{CONTEXT_LABEL[e.context] ?? e.context}</Badge>
              <span className="text-sm text-muted-foreground">{formatDate(e.submittedAt)}</span>
            </span>
              </button>
          ))}
        </CardContent>

        <EvaluationDetailDialog evaluation={selected} onClose={() => setSelected(null)} />
      </Card>
  )
}