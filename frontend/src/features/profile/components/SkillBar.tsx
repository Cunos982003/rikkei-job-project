import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

interface SkillBarProps {
  value: number;
  max?: number;
  className?: string;
  showLabel?: boolean;
}

export function SkillBar({ value, max = 100, className, showLabel = false }: SkillBarProps) {
  const percentage = Math.min(Math.max((value / max) * 100, 0), 100);

  // Color based on score
  let colorClass = "bg-red-500";
  if (percentage >= 80) colorClass = "bg-green-500";
  else if (percentage >= 60) colorClass = "bg-yellow-500";
  else if (percentage >= 40) colorClass = "bg-orange-500";

  return (
    <div className={cn("w-full", className)}>
      <Progress
        value={percentage}
        max={100}
        className="h-2.5"
      >
        <div className={`${colorClass} h-full rounded-full transition-all duration-500`} style={{ width: `${percentage}%` }} />
      </Progress>
      {showLabel && (
        <div className="flex justify-between text-xs text-muted-foreground mt-1">
          <span>0</span>
          <span>{max}</span>
        </div>
      )}
    </div>
  );
}