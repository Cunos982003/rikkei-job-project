import React, { forwardRef } from "react";
import { CheckCircle2, User, AlertTriangle, ShieldAlert } from "lucide-react";
import { cn } from "../../utils/cn";

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: "verified" | "self" | "primary" | "warning" | "danger" | "outline" | "default";
  size?: "sm" | "md";
  showIcon?: boolean;
}

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  (
    {
      className,
      variant = "default",
      size = "md",
      showIcon = false,
      children,
      ...props
    },
    ref
  ) => {
    const sizeStyles = {
      sm: "text-[11px] px-2 py-0.5 gap-1 font-medium",
      md: "text-xs px-2.5 py-1 gap-1.5 font-medium",
    };

    const variantStyles = {
      default: "bg-slate-100 text-slate-700 border border-slate-200",
      primary: "bg-primary-light text-primary border border-primary-border font-semibold",
      verified: "bg-emerald-50 text-success border border-emerald-200 font-semibold",
      self: "bg-blue-50 text-info border border-blue-200",
      warning: "bg-amber-50 text-warning border border-amber-200",
      danger: "bg-rose-50 text-danger border border-rose-200",
      outline: "bg-transparent text-foreground border border-border",
    };

    const icons = {
      verified: <CheckCircle2 className="h-3 w-3 shrink-0 text-success" />,
      self: <User className="h-3 w-3 shrink-0 text-info" />,
      warning: <AlertTriangle className="h-3 w-3 shrink-0 text-warning" />,
      danger: <ShieldAlert className="h-3 w-3 shrink-0 text-danger" />,
      primary: null,
      default: null,
      outline: null,
    };

    return (
      <span
        ref={ref}
        className={cn(
          "inline-flex items-center rounded-md transition-colors select-none",
          sizeStyles[size],
          variantStyles[variant],
          className
        )}
        {...props}
      >
        {showIcon && icons[variant]}
        <span>{children}</span>
      </span>
    );
  }
);

Badge.displayName = "Badge";

