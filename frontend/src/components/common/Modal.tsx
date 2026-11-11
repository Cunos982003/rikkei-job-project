import { useEffect, type ReactNode } from "react";
import { X } from "lucide-react";
import { cn } from "../../utils/cn";

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
  closeOnOverlayClick?: boolean;
}

export function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer,
  size = "md",
  className,
  closeOnOverlayClick = true,
}: ModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };

    if (isOpen) {
      document.body.style.overflow = "hidden";
      window.addEventListener("keydown", handleKeyDown);
    } else {
      document.body.style.overflow = "unset";
    }

    return () => {
      document.body.style.overflow = "unset";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const sizeStyles = {
    sm: "max-w-md",
    md: "max-w-lg",
    lg: "max-w-2xl",
    xl: "max-w-4xl",
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
      {/* Backdrop overlay */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity animate-in fade-in duration-200"
        onClick={() => closeOnOverlayClick && onClose()}
      />

      {/* Modal Dialog Content */}
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          "relative w-full rounded-2xl bg-surface border border-border shadow-xl z-10 overflow-hidden",
          "animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]",
          sizeStyles[size],
          className
        )}
      >
        {/* Header */}
        {(title || description) && (
          <div className="flex items-start justify-between p-5 pb-4 border-b border-border-subtle shrink-0">
            <div>
              {title && <h3 className="text-lg font-semibold text-foreground">{title}</h3>}
              {description && <p className="text-xs text-muted mt-1 leading-relaxed">{description}</p>}
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="Đóng cửa sổ"
              className="p-1.5 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg transition-colors cursor-pointer ml-3 shrink-0"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        {/* Body Content */}
        <div className="p-6 overflow-y-auto text-sm text-foreground flex-1">
          {children}
        </div>

        {/* Footer */}
        {footer && (
          <div className="p-4 px-6 bg-slate-50 border-t border-border-subtle flex items-center justify-end gap-3 shrink-0">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

