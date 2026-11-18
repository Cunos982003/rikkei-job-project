import React, { useState, useMemo } from "react";
import { Award, Info } from "lucide-react";
import { Card, CardHeader, CardTitle, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import type { LearningPathStep } from "../../../api/endpoints/learning.api";

interface SkillRadarChartProps {
  steps: LearningPathStep[];
  title?: string;
}

interface RadarSkillItem {
  skillId: string;
  skillName: string;
  currentScore: number;
  requiredScore: number;
  gap: number;
  isMandatory: boolean;
}

export const SkillRadarChart: React.FC<SkillRadarChartProps> = ({
  steps,
  title = "Biểu đồ so sánh kỹ năng (Radar Chart)",
}) => {
  const [hoveredSkill, setHoveredSkill] = useState<RadarSkillItem | null>(null);

  // Group steps by unique skillId and extract metrics
  const skills: RadarSkillItem[] = useMemo(() => {
    if (!steps || steps.length === 0) return [];

    const map = new Map<string, RadarSkillItem>();
    steps.forEach((step) => {
      if (!map.has(step.skillId)) {
        map.set(step.skillId, {
          skillId: step.skillId,
          skillName: step.skillName,
          currentScore: Math.min(100, Math.max(0, step.currentScore)),
          requiredScore: Math.min(100, Math.max(0, step.requiredScore)),
          gap: Math.max(0, step.gap),
          isMandatory: step.isMandatory,
        });
      }
    });

    return Array.from(map.values());
  }, [steps]);

  // Dimensions for SVG Radar
  const size = 380;
  const cx = size / 2;
  const cy = size / 2;
  const radius = 125;
  const levels = [20, 40, 60, 80, 100];
  const numSkills = skills.length;

  // Calculate coordinates for a given score & angle index
  const getCoordinates = (score: number, index: number, total: number) => {
    // Start from -PI / 2 (12 o'clock)
    const angle = -Math.PI / 2 + (index * 2 * Math.PI) / total;
    const r = (score / 100) * radius;
    const x = cx + r * Math.cos(angle);
    const y = cy + r * Math.sin(angle);
    return { x, y, angle };
  };

  // Generate SVG polygon points string
  const generatePolygonPoints = (type: "required" | "current") => {
    if (numSkills < 3) return "";
    return skills
      .map((item, idx) => {
        const score = type === "required" ? item.requiredScore : item.currentScore;
        const { x, y } = getCoordinates(score, idx, numSkills);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(" ");
  };

  // Generate web grid level polygons
  const generateGridPolygon = (level: number) => {
    return skills
      .map((_, idx) => {
        const { x, y } = getCoordinates(level, idx, numSkills);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(" ");
  };

  return (
    <Card className="border-border shadow-xs">
      <CardHeader className="pb-2 border-b border-border/60">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <CardTitle className="text-base font-bold text-foreground flex items-center gap-2">
            <Award className="h-4 w-4 text-primary shrink-0" />
            <span>{title}</span>
          </CardTitle>

          {/* Legend */}
          <div className="flex items-center gap-4 text-xs">
            <div className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded-full bg-sky-500 border border-sky-600 inline-block" />
              <span className="font-medium text-slate-700">Điểm hiện tại</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded-full bg-rose-500/30 border border-rose-500 inline-block" />
              <span className="font-medium text-slate-700">Yêu cầu vị trí</span>
            </div>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-4 sm:p-6">
        {skills.length === 0 ? (
          <div className="py-12 text-center text-muted text-sm">
            Chưa có thông tin kỹ năng để hiển thị biểu đồ.
          </div>
        ) : skills.length < 3 ? (
          /* Fallback when skills count < 3 (Render clean Bar Comparison) */
          <div className="space-y-4 py-3">
            <p className="text-xs text-muted mb-2">
              So sánh trực tiếp điểm hiện tại và mức sàn yêu cầu cho các kỹ năng mục tiêu:
            </p>
            {skills.map((skill) => (
              <div
                key={skill.skillId}
                className="p-3.5 rounded-xl border border-border/80 bg-slate-50/50 space-y-2"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-foreground">{skill.skillName}</span>
                    {skill.isMandatory && (
                      <Badge variant="danger" size="sm">
                        Bắt buộc
                      </Badge>
                    )}
                  </div>
                  <span className="text-xs font-semibold text-rose-700">
                    {skill.gap > 0 ? `Thiếu ${skill.gap} điểm` : "Đã đạt chuẩn"}
                  </span>
                </div>

                <div className="space-y-1.5 text-xs">
                  {/* Current Score Bar */}
                  <div className="flex items-center gap-2">
                    <span className="w-20 text-muted">Hiện tại:</span>
                    <div className="flex-1 bg-slate-200 rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-sky-500 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${skill.currentScore}%` }}
                      />
                    </div>
                    <span className="w-10 text-right font-bold text-sky-700">
                      {skill.currentScore}
                    </span>
                  </div>

                  {/* Required Score Bar */}
                  <div className="flex items-center gap-2">
                    <span className="w-20 text-muted">Yêu cầu:</span>
                    <div className="flex-1 bg-slate-200 rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-rose-500 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${skill.requiredScore}%` }}
                      />
                    </div>
                    <span className="w-10 text-right font-bold text-rose-700">
                      {skill.requiredScore}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          /* Pure SVG Radar Chart (numSkills >= 3) */
          <div className="flex flex-col items-center">
            <div className="relative w-full max-w-[420px] aspect-square flex items-center justify-center">
              <svg
                viewBox={`0 0 ${size} ${size}`}
                className="w-full h-full overflow-visible select-none"
              >
                {/* 1. Concentric Web Rings (Levels 20, 40, 60, 80, 100) */}
                {levels.map((level) => {
                  const points = generateGridPolygon(level);
                  const { y } = getCoordinates(level, 0, numSkills);
                  return (
                    <g key={level}>
                      <polygon
                        points={points}
                        fill={level === 100 ? "#f8fafc" : "none"}
                        stroke="#e2e8f0"
                        strokeWidth="1"
                      />
                      <text
                        x={cx + 4}
                        y={y + 10}
                        fontSize="9"
                        fill="#94a3b8"
                        fontWeight="600"
                      >
                        {level}
                      </text>
                    </g>
                  );
                })}

                {/* 2. Radiating Axes from Center to Vertices */}
                {skills.map((skill, idx) => {
                  const { x: outerX, y: outerY, angle } = getCoordinates(100, idx, numSkills);
                  // Label position slightly outside max radius
                  const labelRadius = radius + 22;
                  const labelX = cx + labelRadius * Math.cos(angle);
                  const labelY = cy + labelRadius * Math.sin(angle);

                  const cos = Math.cos(angle);
                  const textAnchor = cos > 0.3 ? "start" : cos < -0.3 ? "end" : "middle";

                  return (
                    <g key={skill.skillId}>
                      {/* Axis Line */}
                      <line
                        x1={cx}
                        y1={cy}
                        x2={outerX}
                        y2={outerY}
                        stroke="#cbd5e1"
                        strokeWidth="1"
                        strokeDasharray="2,2"
                      />

                      {/* Axis Skill Name Label */}
                      <text
                        x={labelX}
                        y={labelY + 4}
                        textAnchor={textAnchor}
                        fontSize="11"
                        fontWeight="700"
                        fill={skill.isMandatory ? "#0f172a" : "#475569"}
                        className="cursor-pointer transition-colors hover:fill-primary"
                        onClick={() => setHoveredSkill(skill)}
                      >
                        {skill.skillName}
                        {skill.isMandatory && "*"}
                      </text>
                    </g>
                  );
                })}

                {/* 3. Layer 1: Required Score Polygon (Rose / Red Outline & Tint) */}
                <polygon
                  points={generatePolygonPoints("required")}
                  fill="rgba(244, 63, 94, 0.12)"
                  stroke="#f43f5e"
                  strokeWidth="2"
                  strokeDasharray="4,3"
                />

                {/* 4. Layer 2: Current Score Polygon (Sky Blue / Solid) */}
                <polygon
                  points={generatePolygonPoints("current")}
                  fill="rgba(14, 165, 233, 0.35)"
                  stroke="#0284c7"
                  strokeWidth="2.5"
                />

                {/* 5. Vertex interactive markers for Current Score */}
                {skills.map((skill, idx) => {
                  const currentCoord = getCoordinates(skill.currentScore, idx, numSkills);
                  const isHovered = hoveredSkill?.skillId === skill.skillId;

                  return (
                    <g
                      key={`dot-${skill.skillId}`}
                      onMouseEnter={() => setHoveredSkill(skill)}
                      onMouseLeave={() => setHoveredSkill(null)}
                      className="cursor-pointer"
                    >
                      <circle
                        cx={currentCoord.x}
                        cy={currentCoord.y}
                        r={isHovered ? 7 : 4.5}
                        fill="#0284c7"
                        stroke="#ffffff"
                        strokeWidth="2"
                        className="transition-all duration-200"
                      />
                    </g>
                  );
                })}
              </svg>
            </div>

            {/* Interactive Tooltip Card underneath */}
            <div className="w-full mt-4 min-h-[64px]">
              {hoveredSkill ? (
                <div className="p-3 rounded-xl border border-sky-200 bg-sky-50/70 flex flex-wrap items-center justify-between gap-3 text-xs animate-in fade-in duration-200">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-slate-900">
                      {hoveredSkill.skillName}
                    </span>
                    {hoveredSkill.isMandatory ? (
                      <Badge variant="danger" size="sm">
                        Bắt buộc
                      </Badge>
                    ) : (
                      <Badge variant="default" size="sm">
                        Khuyến nghị
                      </Badge>
                    )}
                  </div>

                  <div className="flex items-center gap-4">
                    <span className="text-slate-600">
                      Điểm hiện tại:{" "}
                      <strong className="text-sky-700 font-bold">
                        {hoveredSkill.currentScore}
                      </strong>
                    </span>
                    <span className="text-slate-600">
                      Yêu cầu:{" "}
                      <strong className="text-rose-700 font-bold">
                        {hoveredSkill.requiredScore}
                      </strong>
                    </span>
                    <span className="px-2 py-0.5 rounded font-bold bg-white text-rose-700 border border-rose-200 shadow-2xs">
                      {hoveredSkill.gap > 0
                        ? `Thiếu -${hoveredSkill.gap} điểm`
                        : "Đã đạt chuẩn"}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="p-2.5 rounded-lg border border-dashed border-border bg-slate-50/50 flex items-center justify-center gap-2 text-xs text-muted">
                  <Info className="h-3.5 w-3.5" />
                  <span>
                    Rê chuột vào các điểm đỉnh trên biểu đồ để xem chi tiết từng kỹ năng (* là kỹ năng bắt buộc).
                  </span>
                </div>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
