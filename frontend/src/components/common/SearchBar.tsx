import { useState, useEffect, useRef } from "react";
import { Search, X } from "lucide-react";
import { cn } from "../../utils/cn";

export interface SearchBarProps {
  placeholder?: string;
  value?: string;
  defaultValue?: string;
  onChange?: (val: string) => void;
  onSearch?: (val: string) => void;
  debounceMs?: number;
  className?: string;
  autoFocus?: boolean;
}

export function SearchBar({
  placeholder = "Tìm kiếm việc làm, kỹ năng, doanh nghiệp...",
  value: controlledValue,
  defaultValue = "",
  onChange,
  onSearch,
  debounceMs = 300,
  className,
  autoFocus,
}: SearchBarProps) {
  const [internalValue, setInternalValue] = useState(defaultValue);
  const isControlled = controlledValue !== undefined;
  const currentValue = isControlled ? controlledValue : internalValue;
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const nextVal = e.target.value;
    if (!isControlled) {
      setInternalValue(nextVal);
    }
    onChange?.(nextVal);

    if (debounceMs > 0 && onSearch) {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        onSearch(nextVal);
      }, debounceMs);
    }
  };

  const handleClear = () => {
    if (!isControlled) {
      setInternalValue("");
    }
    onChange?.("");
    onSearch?.("");
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      if (timerRef.current) clearTimeout(timerRef.current);
      onSearch?.(currentValue);
    }
  };

  return (
    <div className={cn("relative flex items-center w-full", className)}>
      <Search className="absolute left-3.5 h-4 w-4 text-muted pointer-events-none" />

      <input
        type="text"
        autoFocus={autoFocus}
        value={currentValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        className={cn(
          "w-full rounded-xl border border-border bg-surface pl-10 pr-10 py-2.5 text-sm text-foreground",
          "placeholder:text-muted transition-all duration-150 outline-none",
          "focus:border-primary focus:ring-2 focus:ring-primary/20 hover:border-slate-300 shadow-xs"
        )}
      />

      {currentValue && (
        <button
          type="button"
          onClick={handleClear}
          aria-label="Xóa từ khóa tìm kiếm"
          className="absolute right-3 p-1 rounded-full text-muted hover:text-foreground hover:bg-slate-100 transition-colors"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}
