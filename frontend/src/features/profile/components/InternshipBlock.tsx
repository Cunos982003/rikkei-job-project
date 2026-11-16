import { useInternships } from '../hooks/useInternships';
// Sử dụng alias @/ trỏ vào src/components/ui/ (chữ thường)
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';

export function InternshipBlock({ userId }: { userId: string }) {
    const { data, isLoading, isError } = useInternships(userId);

    return (
        <Card className="shadow-sm mt-6">
            <CardHeader>
                <CardTitle className="text-lg">Thực tập IOC</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">

                {isLoading && (
                    <>
                        <Skeleton className="h-28 w-full rounded-xl" />
                        <Skeleton className="h-28 w-full rounded-xl" />
                    </>
                )}

                {isError && (
                    <p className="text-sm text-destructive">Không tải được dữ liệu thực tập.</p>
                )}

                {data && data.length === 0 && (
                    <p className="text-sm text-muted-foreground">Chưa có dữ liệu thực tập.</p>
                )}

                {data?.map((item) => (
                    <div
                        key={item.id}
                        className="rounded-lg border bg-card text-card-foreground shadow-sm p-4 space-y-3"
                    >
                        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-2">
                            <div>
                                <h3 className="font-semibold text-base text-foreground">
                                    {item.topicName}
                                </h3>
                                <p className="text-sm text-muted-foreground mt-0.5">
                                    {item.batchName} • {item.groupName}
                                </p>
                            </div>
                            <Badge variant="secondary" className="w-fit bg-blue-100 text-blue-800 hover:bg-blue-100">
                                Điểm cuối kỳ: {item.finalScore}/10
                            </Badge>
                        </div>

                        <Separator className="my-2" />

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                            <div className="flex items-center gap-2 text-muted-foreground">
                                <span className="font-medium text-foreground">Mentor:</span>
                                {item.mentorEmail}
                            </div>
                            <div className="flex items-center gap-2 text-muted-foreground">
                                <span className="font-medium text-foreground">Task đã nghiệm thu:</span>
                                {item.tasks?.length || 0}
                            </div>
                        </div>
                    </div>
                ))}
            </CardContent>
        </Card>
    );
}