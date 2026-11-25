/**
 * Helper chuẩn hóa mã kỹ năng (Skill Code) theo danh mục hệ thống
 */
export function toCanonicalSkillCode(rawName?: string, rawCodeOrId?: string): string {
  const candidates = [rawCodeOrId, rawName].filter(Boolean) as string[];
  for (const c of candidates) {
    const key = c.trim().toLowerCase();
    if (key === "skl-01" || key === "java" || key === "java_core" || key === "java core") return "JAVA_CORE";
    if (key === "skl-02" || key === "spring_boot" || key === "spring boot") return "SPRING_BOOT";
    if (key === "skl-03" || key === "sql") return "SQL";
    if (key === "skl-04" || key === "git") return "GIT";
    if (key === "skl-05" || key === "rest_api" || key === "rest api") return "DESIGN_PATTERNS";
    if (key === "skl-06" || key === "html_css" || key === "html/css") return "HTML_CSS";
    if (key === "skl-07" || key === "javascript") return "JAVASCRIPT";
    if (key === "skl-08" || key === "react" || key === "reactjs") return "REACT";
    if (key === "skl-09" || key === "typescript") return "REACT";
    if (key === "skl-10" || key === "docker") return "DOCKER";
    if (key === "skl-11" || key === "tiếng anh" || key === "english") return "ENGLISH";
    if (key === "skl-12" || key === "python") return "PYTHON";
    if (key === "skl-13" || key === "microservices") return "DESIGN_PATTERNS";
    if (key === "skl-14" || key === "redis") return "SQL";
    if (key === "skl-15" || key === "apache kafka" || key === "kafka") return "CI_CD";
    if (key === "skl-16" || key === "unit testing" || key === "testing") return "TESTING";
    if (key === "skl-17" || key === "ci/cd" || key === "ci_cd") return "CI_CD";
    if (key === "skl-18" || key === "linux / bash" || key === "linux") return "DOCKER";
    if (key === "skl-19" || key === "tailwind css" || key === "tailwind") return "HTML_CSS";
    if (key === "skl-20" || key === "làm việc nhóm" || key === "teamwork") return "TEAMWORK";
  }

  const primary = rawCodeOrId || rawName || "SKILL";
  return primary.toUpperCase().replace(/[^A-Z0-9]/g, "_");
}

