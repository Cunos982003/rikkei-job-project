import type { MentorEvaluation } from '../../api/types/evaluation'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../components/ui/dialog'
import { Progress } from '../../components/ui/progress'
import { Separator } from '../../components/ui/separator'

interface Props {
    evaluation: MentorEvaluation | null
    onClose: () => void
}

export function EvaluationDetailDialog({ evaluation, onClose }: Props) {
    return (
        <Dialog open={!!evaluation} onOpenChange={(open) => !open && onClose()}>
            <DialogContent aria-describedby={undefined} className="max-h-[85vh] max-w-2xl overflow-y-auto">
                {evaluation && (
                    <>
                        <DialogHeader>
                            <DialogTitle>{evaluation.templateName}</DialogTitle>
                        </DialogHeader>

                        <ul className="space-y-4">
                            {evaluation.scores.map((s) => (
                                <li key={s.criterionId} className="space-y-1">
                                    <div className="flex items-center justify-between gap-4">
                                        <span className="font-medium">{s.skillName}</span>
                                        <span className="font-semibold">{s.score}/100</span>
                                    </div>
                                    <p className="text-sm text-muted-foreground">{s.description}</p>
                                    <Progress value={s.score} />
                                    {s.comment && <p className="whitespace-pre-wrap text-sm">{s.comment}</p>}
                                </li>
                            ))}
                        </ul>

                        {evaluation.overallComment && (
                            <>
                                <Separator />
                                <div>
                                    <h4 className="mb-1 text-sm font-semibold">Nhận xét chung</h4>
                                    <p className="whitespace-pre-wrap text-sm">{evaluation.overallComment}</p>
                                </div>
                            </>
                        )}
                    </>
                )}
            </DialogContent>
        </Dialog>
    )
}