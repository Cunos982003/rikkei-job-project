import React, { forwardRef } from "react";
import { cn } from "../../utils/cn";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, leftIcon, rightIcon, id, required, disabled, ...props }, ref) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-foreground">
            {label}
            {required && <span className="text-danger ml-1">*</span>}
          </label>
        )}

        <div className="relative flex items-center">
          {leftIcon && (
            <div className="absolute left-3 text-muted pointer-events-none flex items-center">
              {leftIcon}
            </div>
          )}

          <input
            ref={ref}
            id={inputId}
            disabled={disabled}
            className={cn(
              "w-full rounded-lg border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted transition-colors outline-none",
              "border-border focus:border-primary focus:ring-2 focus:ring-primary/20",
              leftIcon && "pl-10",
              rightIcon && "pr-10",
              error && "border-danger focus:border-danger focus:ring-danger/20",
              disabled && "opacity-50 cursor-not-allowed bg-slate-50",
              className
            )}
            {...props}
          />

          {rightIcon && (
            <div className="absolute right-3 text-muted flex items-center">
              {rightIcon}
            </div>
          )}
        </div>

        {error && <p className="text-xs text-danger font-medium">{error}</p>}
        {hint && !error && <p className="text-xs text-muted">{hint}</p>}
      </div>
    );
  }
);

Input.displayName = "Input";

