import { forwardRef } from "react";
import { SkillBar } from "./SkillBar";
import { EvidenceModal } from "./EvidenceModal";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ChevronDown, ChevronUp, Award, User } from "lucide-react";
import { useState } from "react";
import type { SkillProfileResponse } from "@/api/types/skill";

interface SkillGroupCardProps {
  skill: SkillProfileResponse;
}

export const SkillGroupCard = forwardRef<HTMLDivElement, SkillGroupCardProps>(
  ({ skill }, ref) => {
    const [showEvidences, setShowEvidences] = useState(false);
    const displayScore = skill.verifiedScore ?? skill.selfScore ?? 0;
    const isSelfOnly = skill.verifiedScore === null && skill.selfScore !== null;

    return (
      <Card ref={ref} className="overflow-hidden transition-shadow hover:shadow-md">
        <CardHeader className="pb-2">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0">
              <h3 className="font-semibold text-foreground truncate">{skill.skillName}</h3>
              <p className="text-sm text-muted-foreground">{skill.skillCode}</p>
            </div>
            <div className="flex items-center gap-1.5">
              {skill.verifiedScore !== null && (
                <Badge variant="default" className="text-xs gap-1">
                  <Award className="h-3 w-3" />
                  Xác thực
                </Badge>
              )}
              {isSelfOnly && (
                <Badge variant="secondary" className="text-xs gap-1">
                  <User className="h-3 w-3" />
                  Tự khai
                </Badge>
              )}
              <button
                onClick={() => setShowEvidences(!showEvidences)}
                className="p-1 hover:bg-accent rounded transition-colors"
                aria-label={showEvidences ? "Ẩn bằng chứng" : "Hiện bằng chứng"}
              >
                {showEvidences ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
              </button>
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-3 pb-3">
          <div className="flex items-end justify-between gap-2">
            <SkillBar value={displayScore} max={100} className="flex-1" />
            <span className="text-lg font-bold text-foreground whitespace-nowrap">
              {displayScore}
            </span>
          </div>

          <div className="flex items-center justify-between">
            <Badge variant="outline" className="text-xs">
              {skill.level}
            </Badge>
            {skill.evidences.length > 0 && (
              <button
                onClick={() => setShowEvidences(!showEvidences)}
                className="text-xs text-primary hover:underline"
              >
                {showEvidences ? "Ẩn bằng chứng" : `${skill.evidences.length} bằng chứng`}
              </button>
            )}
          </div>

          {showEvidences && skill.evidences.length > 0 && (
            <>
              <Separator />
              <EvidenceModal evidences={skill.evidences} onClose={() => setShowEvidences(false)} />
            </>
          )}
        </CardContent>
      </Card>
    );
  }
);

SkillGroupCard.displayName = "SkillGroupCard";