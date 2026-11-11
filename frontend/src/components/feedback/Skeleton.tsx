import React from "react";
import { cn } from "../../utils/cn";

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "rounded" | "circle" | "text";
}

export function Skeleton({ className, variant = "rounded", ...props }: SkeletonProps) {
  const variantStyles = {
    rounded: "rounded-lg",
    circle: "rounded-full",
    text: "rounded-md h-4 w-full",
  };

  return (
    <div
      className={cn("animate-pulse bg-slate-200/80 dark:bg-slate-700/50", variantStyles[variant], className)}
      {...props}
    />
  );
}

