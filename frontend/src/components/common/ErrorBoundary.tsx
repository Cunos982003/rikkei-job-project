import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "./Button";

interface Props {
  children?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error in component tree:", error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-[400px] flex flex-col items-center justify-center p-8 text-center bg-surface rounded-xl border border-border">
          <div className="rounded-full bg-danger-light p-4 text-danger mb-4">
            <AlertCircle className="h-8 w-8" />
          </div>
          <h2 className="text-lg font-bold text-foreground">Đã xảy ra sự cố hiển thị</h2>
          <p className="mt-2 text-sm text-muted max-w-md">
            Giao diện gặp lỗi trong quá trình xử lý dữ liệu. Bạn có thể tải lại trang hoặc thử lại thao tác vừa rồi.
          </p>
          <div className="mt-6 flex items-center gap-3">
            <Button
              variant="outline"
              size="sm"
              onClick={() => this.setState({ hasError: false })}
            >
              Thử lại
            </Button>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<RefreshCw className="h-4 w-4" />}
              onClick={() => window.location.reload()}
            >
              Tải lại trang
            </Button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

