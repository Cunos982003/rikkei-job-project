/**
 * Formatters for currency, dates, scores, and skill levels according to Rikkei Jobs standards.
 */

/**
 * Format currency to Vietnamese Dong (e.g. 15.000.000 đ)
 */
export function formatCurrency(amount: number | null | undefined): string {
  if (amount == null) return "Thoả thuận";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

/**
 * Format salary range (e.g. "12.000.000 - 20.000.000 đ" or "Từ 15.000.000 đ")
 */
export function formatSalaryRange(min?: number | null, max?: number | null): string {
  if (min == null && max == null) return "Thoả thuận";
  if (min != null && max != null) {
    return `${min.toLocaleString("vi-VN")} - ${max.toLocaleString("vi-VN")} đ`;
  }
  if (min != null) return `Từ ${min.toLocaleString("vi-VN")} đ`;
  return `Lên đến ${max!.toLocaleString("vi-VN")} đ`;
}

/**
 * Format date to Vietnamese format DD/MM/YYYY
 */
export function formatDate(dateString?: string | null): string {
  if (!dateString) return "—";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "—";
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }).format(date);
  } catch {
    return "—";
  }
}

/**
 * Format date time to DD/MM/YYYY HH:mm
 */
export function formatDateTime(dateString?: string | null): string {
  if (!dateString) return "—";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "—";
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(date);
  } catch {
    return "—";
  }
}

/**
 * Map skill score (0-100) to level name according to conventions.md:
 * 0 - 39: Cơ bản
 * 40 - 69: Khá
 * 70 - 84: Tốt
 * 85 - 100: Xuất sắc
 */
export function getSkillLevel(score: number): {
  level: "Cơ bản" | "Khá" | "Tốt" | "Xuất sắc";
  colorClass: string;
  bgLightClass: string;
} {
  if (score >= 85) {
    return {
      level: "Xuất sắc",
      colorClass: "text-primary",
      bgLightClass: "bg-primary-light text-primary",
    };
  }
  if (score >= 70) {
    return {
      level: "Tốt",
      colorClass: "text-success",
      bgLightClass: "bg-emerald-50 text-success",
    };
  }
  if (score >= 40) {
    return {
      level: "Khá",
      colorClass: "text-warning",
      bgLightClass: "bg-amber-50 text-warning",
    };
  }
  return {
    level: "Cơ bản",
    colorClass: "text-danger",
    bgLightClass: "bg-rose-50 text-danger",
  };
}

/**
 * Mask PII phone number (e.g. 098***1234)
 */
export function maskPhone(phone?: string | null): string {
  if (!phone) return "—";
  const cleaned = phone.trim();
  if (cleaned.length < 7) return "***";
  return `${cleaned.slice(0, 3)}***${cleaned.slice(-4)}`;
}

/**
 * Mask PII email (e.g. ng***@rikkei.vn)
 */
export function maskEmail(email?: string | null): string {
  if (!email) return "—";
  const [localPart, domain] = email.split("@");
  if (!domain) return "***";
  const visible = localPart.slice(0, 2);
  return `${visible}***@${domain}`;
}

