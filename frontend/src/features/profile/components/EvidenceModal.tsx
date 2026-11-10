import { forwardRef } from "react";
import { X, CheckCircle, AlertCircle, Calendar, Award, User } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import type { SkillEvidence } from "@/api/types/skill";

interface EvidenceModalProps {
  evidences: SkillEvidence[];
  onClose: () => void;
}

export const EvidenceModal = forwardRef<HTMLDivElement, EvidenceModalProps>(
  ({ evidences, onClose }, ref) => {
    const getSourceBadge = (source: string) => {
      const variants: Record<string, "default" | "secondary" | "outline" | "destructive"> = {
        LMS: "default",
        EXAM: "default",
        PM: "secondary",
        INTERNSHIP: "secondary",
        MENTOR: "outline",
        SELF: "destructive",
      };
      const icons: Record<string, React.ReactNode> = {
        LMS: <Award className="h-3 w-3" />,
        EXAM: <Award className="h-3 w-3" />,
        PM: <Award className="h-3 w-3" />,
        INTERNSHIP: <User className="h-3 w-3" />,
        MENTOR: <User className="h-3 w-3" />,
        SELF: <User className="h-3 w-3" />,
      };
      return (
        <Badge variant={variants[source] || "outline"} className="gap-1">
          {icons[source]}
          {source}
        </Badge>
      );
    };

    const getVerifiedBadge = (verified: boolean) => {
      if (verified) {
        return (
          <Badge variant="default" className="gap-1">
            <CheckCircle className="h-3 w-3" />
            Đã xác thực
          </Badge>
        );
      }
      return (
        <Badge variant="destructive" className="gap-1">
          <AlertCircle className="h-3 w-3" />
          Chưa xác thực
        </Badge>
      );
    };

    return (
      <Dialog open onOpenChange={onClose}>
        <DialogContent className="sm:max-w-md" ref={ref}>
          <DialogHeader>
            <DialogTitle className="flex items-center justify-between">
              Bằng chứng kỹ năng
              <button onClick={onClose} className="p-1 hover:bg-accent rounded transition-colors">
                <X className="h-4 w-4" />
              </button>
            </DialogTitle>
            <DialogDescription>
              {evidences.length} mục • Nhấn để xem chi tiết
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 max-h-96 overflow-y-auto">
            {evidences.map((evidence, index) => (
              <div
                key={`${evidence.source}-${evidence.sourceRef}-${index}`}
                className="p-3 bg-card border rounded-lg hover:bg-accent/50 transition-colors"
              >
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-foreground truncate">{evidence.title}</h4>
                    <p className="text-sm text-muted-foreground">{evidence.sourceRef}</p>
                  </div>
                  <div className="flex items-center gap-1.5 flex-wrap">
                    {getSourceBadge(evidence.source)}
                    {getVerifiedBadge(evidence.verified)}
                  </div>
                </div>

                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center gap-2">
                    <span className={cn(
                      "font-semibold px-2 py-0.5 rounded",
                      evidence.score >= 80 && "text-green-700 bg-green-100",
                      evidence.score >= 60 && evidence.score < 80 && "text-yellow-700 bg-yellow-100",
                      evidence.score >= 40 && evidence.score < 60 && "text-orange-700 bg-orange-100",
                      evidence.score < 40 && "text-red-700 bg-red-100"
                    )}>
                      {evidence.score}/100
                    </span>
                    <span className="flex items-center gap-1 text-muted-foreground">
                      <Calendar className="h-3 w-3" />
                      {new Date(evidence.recordedAt).toLocaleDateString("vi-VN")}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <Separator className="my-3" />
          <div className="flex justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors"
            >
              Đóng
            </button>
          </div>
        </DialogContent>
      </Dialog>
    );
  }
);

EvidenceModal.displayName = "EvidenceModal";