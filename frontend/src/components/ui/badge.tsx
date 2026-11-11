import * as React from "react";
import { Badge as CommonBadge, type BadgeProps as CommonBadgeProps } from "../common/Badge";

export interface BadgeProps extends Omit<CommonBadgeProps, "variant"> {
  variant?: CommonBadgeProps["variant"] | "secondary";
  children?: React.ReactNode;
}

export function Badge({ variant = "default", ...props }: BadgeProps) {
  const mappedVariant = variant === "secondary" ? "default" : variant;
  return <CommonBadge variant={mappedVariant} {...props} />;
}

export default Badge;
