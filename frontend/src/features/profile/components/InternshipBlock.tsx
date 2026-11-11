import { useInternships } from '@/api/hooks/useInternships';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';

export function InternshipBlock({ userId }: { userId: string }) {
    const { data, isLoading, isError } = useInternships(userId);

    return (
        <Card className="mt-6 shadow-sm">
            <CardHeader>
                <CardTitle className="text-lg">Thực tập IOC</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
                {isLoading && (
                    <>
                        <Skeleton className="h-24 w-full" />
                        <Skeleton className="h-24 w-full" />
                    </>
                )}

                {isError && <p className="text-destructive text-sm">Không tải được dữ liệu thực tập.</p>}

                {data?.length === 0 && <p className="text-muted-foreground text-sm">Chưa có dữ liệu thực tập.</p>}

                {data?.map((item) => (
                    <div key={item.id} className="border rounded-lg p-4 bg-white shadow-sm flex flex-col gap-2">
                        <div className="flex justify-between items-start">
                            <div>
                                <h3 className="font-semibold text-base text-gray-900">{item.topic_name}</h3>
                                <p className="text-sm text-gray-500 mt-1">
                                    {item.batch_name} • {item.group_name}
                                </p>
                            </div>
                            <Badge variant="outline" className="text-blue-600 border-blue-200 bg-blue-50">
                                Điểm: {item.final_score}
                            </Badge>
                        </div>

                        <div className="text-sm text-gray-600 mt-2 grid grid-cols-2 gap-2">
                            <div>
                                <span className="font-medium">Mentor:</span> {item.mentor_email}
                            </div>
                            <div>
                                <span className="font-medium">Task nghiệm thu:</span> {item.tasks?.length || 0}
                            </div>
                        </div>
                    </div>
                ))}
            </CardContent>
        </Card>
    );
}