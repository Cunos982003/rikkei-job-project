import { useInternships } from "../hooks/useInternships";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import {
    CalendarDays,
    CheckCircle2,
    FolderKanban,
    GraduationCap,
    RefreshCw,
    UserRound,
} from "lucide-react";

interface InternshipBlockProps {
    userId?: string;
}

export function InternshipBlock({ userId }: InternshipBlockProps) {
    const {
        data: internships = [],
        isLoading,
        isError,
        refetch,
        isFetching,
    } = useInternships(userId);

    return (
        <Card className="mt-6 shadow-sm">
            <CardHeader>
                <div className="flex items-center justify-between gap-3">
                    <div>
                        <CardTitle className="flex items-center gap-2 text-lg">
                            <GraduationCap className="h-5 w-5 text-primary" />
                            Thực tập IOC
                        </CardTitle>
                        <p className="mt-1 text-sm text-muted-foreground">
                            Lịch sử thực tập, dự án và kết quả nghiệm thu
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm hover:bg-accent disabled:opacity-50"
                    >
                        <RefreshCw
                            className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`}
                        />
                        Làm mới
                    </button>
                </div>
            </CardHeader>

            <CardContent className="space-y-4">
                {isLoading && (
                    <>
                        <Skeleton className="h-32 w-full rounded-xl" />
                        <Skeleton className="h-32 w-full rounded-xl" />
                    </>
                )}

                {isError && (
                    <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-5 text-center">
                        <p className="font-medium text-destructive">
                            Không thể tải dữ liệu thực tập IOC
                        </p>
                        <p className="mt-1 text-sm text-muted-foreground">
                            Vui lòng kiểm tra kết nối Backend rồi thử lại.
                        </p>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            className="mt-3 rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground"
                        >
                            Thử lại
                        </button>
                    </div>
                )}

                {!isLoading && !isError && internships.length === 0 && (
                    <div className="rounded-xl border border-dashed p-8 text-center">
                        <GraduationCap className="mx-auto h-10 w-10 text-muted-foreground" />
                        <p className="mt-3 font-medium">
                            Chưa có dữ liệu thực tập IOC
                        </p>
                        <p className="mt-1 text-sm text-muted-foreground">
                            Kết quả thực tập của bạn sẽ xuất hiện tại đây sau khi được đồng bộ.
                        </p>
                    </div>
                )}

                {!isLoading &&
                    !isError &&
                    internships.map((item) => {
                        const acceptedTasks = item.tasks ?? [];

                        return (
                            <section
                                key={item.id}
                                className="space-y-4 rounded-xl border p-4"
                            >
                                <div className="flex flex-col justify-between gap-3 sm:flex-row">
                                    <div className="min-w-0">
                                        <div className="flex items-center gap-2">
                                            <FolderKanban className="h-5 w-5 shrink-0 text-primary" />
                                            <h3 className="font-semibold">
                                                {/* ĐÃ SỬA LỖI TẠI ĐÂY */}
                                                {item.batchName || "Chưa cập nhật tên đợt"}
                                            </h3>
                                        </div>

                                        <p className="mt-2 text-sm text-muted-foreground">
                                            {item.topicName || "Chưa cập nhật đề tài"}
                                        </p>
                                    </div>

                                    <Badge
                                        variant="secondary"
                                        className="h-fit whitespace-nowrap"
                                    >
                                        Điểm cuối kỳ:{" "}
                                        {item.finalScore != null
                                            ? `${item.finalScore}/10`
                                            : "Chưa có"}
                                    </Badge>
                                </div>

                                <Separator />

                                <div className="grid gap-3 text-sm sm:grid-cols-2">
                                    <div className="flex items-start gap-2">
                                        <UserRound className="mt-0.5 h-4 w-4 text-muted-foreground" />
                                        <div>
                                            <p className="text-muted-foreground">Nhóm thực tập</p>
                                            <p className="font-medium">
                                                {item.groupName || "Chưa cập nhật"}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start gap-2">
                                        <UserRound className="mt-0.5 h-4 w-4 text-muted-foreground" />
                                        <div>
                                            <p className="text-muted-foreground">Mentor</p>
                                            <p className="font-medium">
                                                {item.mentorEmail || "Chưa phân công"}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start gap-2">
                                        <CalendarDays className="mt-0.5 h-4 w-4 text-muted-foreground" />
                                        <div>
                                            <p className="text-muted-foreground">Thời gian</p>
                                            <p className="font-medium">
                                                {item.startDate || "?"} – {item.endDate || "?"}
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start gap-2">
                                        <CheckCircle2 className="mt-0.5 h-4 w-4 text-muted-foreground" />
                                        <div>
                                            <p className="text-muted-foreground">
                                                Task đã nghiệm thu
                                            </p>
                                            <p className="font-medium">
                                                {acceptedTasks.length} task
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                {acceptedTasks.length > 0 && (
                                    <div className="space-y-2">
                                        <p className="text-sm font-semibold">
                                            Danh sách task nghiệm thu
                                        </p>

                                        {acceptedTasks.map((task) => (
                                            <div
                                                key={task.id}
                                                className="flex flex-col justify-between gap-2 rounded-lg bg-muted/40 p-3 sm:flex-row sm:items-center"
                                            >
                                                <div>
                                                    <p className="text-sm font-medium">{task.title}</p>
                                                    {task.acceptedAt && (
                                                        <p className="mt-1 text-xs text-muted-foreground">
                                                            Nghiệm thu:{" "}
                                                            {new Date(task.acceptedAt).toLocaleDateString(
                                                                "vi-VN"
                                                            )}
                                                        </p>
                                                    )}
                                                </div>

                                                <Badge variant="outline">
                                                    {task.acceptedScore != null
                                                        ? `${task.acceptedScore} điểm`
                                                        : "Đã nghiệm thu"}
                                                </Badge>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </section>
                        );
                    })}
            </CardContent>
        </Card>
    );
}