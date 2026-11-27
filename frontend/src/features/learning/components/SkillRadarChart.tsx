import React, { useState, useMemo } from "react";
import { Award, Info } from "lucide-react";
import { Card, CardHeader, CardTitle, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import type { GapItem, LearningPathStep } from "../../../api/endpoints/learning.api";

interface SkillRadarChartProps {
  steps?: LearningPathStep[];
  radarSkills?: GapItem[];
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
  steps = [],
  radarSkills,
  title = "Biểu đồ so sánh kỹ năng (Radar Chart)",
}) => {
  const [hoveredSkill, setHoveredSkill] = useState<RadarSkillItem | null>(null);

  // Ưu tiên radarSkills (chứa đầy đủ tất cả kỹ năng của mục tiêu: đã đạt + còn thiếu), fallback sang steps
  const skills: RadarSkillItem[] = useMemo(() => {
    if (radarSkills && radarSkills.length > 0) {
      const map = new Map<string, RadarSkillItem>();
      radarSkills.forEach((item) => {
        const key = item.skillId || item.skillName;
        if (!map.has(key)) {
          map.set(key, {
            skillId: item.skillId,
            skillName: item.skillName || item.skill,
            currentScore: Math.min(100, Math.max(0, item.current ?? 0)),
            requiredScore: Math.min(100, Math.max(0, item.required ?? 0)),
            gap: Math.max(0, item.gap ?? 0),
            isMandatory: Boolean(item.isMandatory),
          });
        }
      });
      return Array.from(map.values());
    }

    if (!steps || steps.length === 0) return [];

    const map = new Map<string, RadarSkillItem>();
    steps.forEach((step) => {
      if (!map.has(step.skillId)) {
        map.set(step.skillId, {
          skillId: step.skillId,
          skillName: step.skillName,
          currentScore: Math.min(100, Math.max(0, step.currentScore ?? 0)),
          requiredScore: Math.min(100, Math.max(0, step.requiredScore ?? 0)),
          gap: Math.max(0, step.gap ?? 0),
          isMandatory: Boolean(step.isMandatory),
        });
      }
    });

    return Array.from(map.values());
  }, [radarSkills, steps]);

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
    const angle = -Math.PI / 2 + (index * 2 * Math.PI) / Math.max(total, 1);
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
        ) : (
          /* Đồng bộ tất cả ứng viên đều dùng Pure SVG Radar Grid */
          <div className="flex flex-col items-center">
            <div className="relative w-full max-w-[420px] aspect-square flex items-center justify-center">
              <svg
                viewBox={`0 0 ${size} ${size}`}
                className="w-full h-full overflow-visible select-none"
              >
                {/* 1. Concentric Web Rings (Levels 20, 40, 60, 80, 100) */}
                {levels.map((level) => {
                  const { y } = getCoordinates(level, 0, numSkills);
                  const r = (level / 100) * radius;

                  return (
                    <g key={level}>
                      {numSkills >= 3 ? (
                        <polygon
                          points={generateGridPolygon(level)}
                          fill={level === 100 ? "#f8fafc" : "none"}
                          stroke="#e2e8f0"
                          strokeWidth="1"
                        />
                      ) : (
                        <circle
                          cx={cx}
                          cy={cy}
                          r={r}
                          fill={level === 100 ? "#f8fafc" : "none"}
                          stroke="#e2e8f0"
                          strokeWidth="1"
                        />
                      )}
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
                  const labelRadius = radius + 22;
                  const labelX = cx + labelRadius * Math.cos(angle);
                  const labelY = cy + labelRadius * Math.sin(angle);

                  const cos = Math.cos(angle);
                  const textAnchor = cos > 0.3 ? "start" : cos < -0.3 ? "end" : "middle";

                  return (
                    <g key={skill.skillId || skill.skillName}>
                      <line
                        x1={cx}
                        y1={cy}
                        x2={outerX}
                        y2={outerY}
                        stroke="#cbd5e1"
                        strokeWidth="1"
                        strokeDasharray="2,2"
                      />

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

                {/* 3. Layer 1: Required Score Polygon/Line (Rose / Red) */}
                {numSkills >= 3 ? (
                  <polygon
                    points={generatePolygonPoints("required")}
                    fill="rgba(244, 63, 94, 0.12)"
                    stroke="#f43f5e"
                    strokeWidth="2"
                    strokeDasharray="4,3"
                  />
                ) : numSkills === 2 ? (
                  <line
                    x1={getCoordinates(skills[0].requiredScore, 0, numSkills).x}
                    y1={getCoordinates(skills[0].requiredScore, 0, numSkills).y}
                    x2={getCoordinates(skills[1].requiredScore, 1, numSkills).x}
                    y2={getCoordinates(skills[1].requiredScore, 1, numSkills).y}
                    stroke="#f43f5e"
                    strokeWidth="2.5"
                    strokeDasharray="4,3"
                  />
                ) : null}

                {/* Required score markers (visible when < 3 axes) */}
                {numSkills < 3 &&
                  skills.map((skill, idx) => {
                    const reqCoord = getCoordinates(skill.requiredScore, idx, numSkills);
                    return (
                      <circle
                        key={`req-${skill.skillId || idx}`}
                        cx={reqCoord.x}
                        cy={reqCoord.y}
                        r={5}
                        fill="#f43f5e"
                        stroke="#ffffff"
                        strokeWidth="1.5"
                      />
                    );
                  })}

                {/* 4. Layer 2: Current Score Polygon/Line (Sky Blue) */}
                {numSkills >= 3 ? (
                  <polygon
                    points={generatePolygonPoints("current")}
                    fill="rgba(14, 165, 233, 0.35)"
                    stroke="#0284c7"
                    strokeWidth="2.5"
                  />
                ) : numSkills === 2 ? (
                  <line
                    x1={getCoordinates(skills[0].currentScore, 0, numSkills).x}
                    y1={getCoordinates(skills[0].currentScore, 0, numSkills).y}
                    x2={getCoordinates(skills[1].currentScore, 1, numSkills).x}
                    y2={getCoordinates(skills[1].currentScore, 1, numSkills).y}
                    stroke="#0284c7"
                    strokeWidth="3"
                  />
                ) : null}

                {/* 5. Vertex interactive markers for Current Score */}
                {skills.map((skill, idx) => {
                  const currentCoord = getCoordinates(skill.currentScore, idx, numSkills);
                  const isHovered = hoveredSkill?.skillId === skill.skillId;

                  return (
                    <g
                      key={`dot-${skill.skillId || idx}`}
                      onMouseEnter={() => setHoveredSkill(skill)}
                      onMouseLeave={() => setHoveredSkill(null)}
                      onClick={() => setHoveredSkill(skill)}
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
                    <span
                      className={`px-2 py-0.5 rounded font-bold border shadow-2xs ${
                        hoveredSkill.gap > 0
                          ? "bg-white text-rose-700 border-rose-200"
                          : "bg-emerald-50 text-emerald-700 border-emerald-200"
                      }`}
                    >
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
