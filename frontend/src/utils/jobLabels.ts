export const LEVEL_LABELS: Record<string, string> = {
  INTERN: "Thực tập",
  FRESHER: "Fresher",
  JUNIOR: "Junior",
  MIDDLE: "Middle",
  SENIOR: "Senior",
};

export const TYPE_LABELS: Record<string, string> = {
  FULLTIME: "Toàn thời gian",
  FULL_TIME: "Toàn thời gian",
  PARTTIME: "Bán thời gian",
  PART_TIME: "Bán thời gian",
  INTERNSHIP: "Thực tập",
  CONTRACT: "Hợp đồng",
};

export const WORK_MODE_LABELS: Record<string, string> = {
  ONSITE: "Tại văn phòng",
  REMOTE: "Từ xa",
  HYBRID: "Kết hợp",
};

export function getJobLevelLabel(level?: string | null): string {
  if (!level) return "";
  return LEVEL_LABELS[level] || level;
}

export function getJobTypeLabel(type?: string | null): string {
  if (!type) return "";
  return TYPE_LABELS[type] || type;
}

export function getWorkModeLabel(workMode?: string | null): string {
  if (!workMode) return "";
  return WORK_MODE_LABELS[workMode] || workMode;
}

export function formatDate(dateStr?: string | null): string {
  if (!dateStr) return "";
  try {
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
      const [y, m, d] = dateStr.split("-");
      return `${d}/${m}/${y}`;
    }
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    const day = String(d.getDate()).padStart(2, "0");
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  } catch {
    return dateStr;
  }
}

export function formatSalary(min?: number | null, max?: number | null): string {
  if (!min && !max) return "Thoả thuận";
  if (min && max) {
    return `${(min / 1000000).toFixed(0)} - ${(max / 1000000).toFixed(0)} triệu đ`;
  }
  return `${(((min || max)!) / 1000000).toFixed(0)} triệu đ`;
}

export function formatSalaryVND(min?: number | null, max?: number | null): string {
  if (min == null && max == null) return "Thỏa thuận";
  if (min != null && max != null) {
    return `${Number(min).toLocaleString("vi-VN")} - ${Number(max).toLocaleString("vi-VN")} đ`;
  }
  if (min != null) return `Từ ${Number(min).toLocaleString("vi-VN")} đ`;
  return `Lên đến ${Number(max).toLocaleString("vi-VN")} đ`;
}


