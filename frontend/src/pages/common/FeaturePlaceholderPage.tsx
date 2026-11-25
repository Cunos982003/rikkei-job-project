import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Clock,
  Construction,
  Layers,
  ShieldCheck,
  KeyRound,
  Sparkles,
  ExternalLink,
} from "lucide-react";
import { Card, CardContent } from "../../components/common/Card";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";

export interface FeaturePlaceholderPageProps {
  title: string;
  description: string;
  badge?: string;
  moduleName?: string;
  backPath?: string;
  icon?: "construction" | "clock" | "layers" | "shield" | "key" | "sparkles";
  estimatedSprint?: string;
}

export function FeaturePlaceholderPage({
  title,
  description,
  badge = "Đang phát triển",
  moduleName = "Rikkei Jobs Platform",
  backPath,
  icon = "construction",
  estimatedSprint = "Giai đoạn tích hợp tiếp theo",
}: FeaturePlaceholderPageProps) {
  const navigate = useNavigate();

  const renderIcon = () => {
    switch (icon) {
      case "clock":
        return <Clock className="h-10 w-10 text-primary" />;
      case "layers":
        return <Layers className="h-10 w-10 text-primary" />;
      case "shield":
        return <ShieldCheck className="h-10 w-10 text-primary" />;
      case "key":
        return <KeyRound className="h-10 w-10 text-primary" />;
      case "sparkles":
        return <Sparkles className="h-10 w-10 text-primary" />;
      case "construction":
      default:
        return <Construction className="h-10 w-10 text-warning" />;
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 space-y-6">
      {/* Top Back navigation */}
      {backPath && (
        <button
          type="button"
          onClick={() => navigate(backPath)}
          className="inline-flex items-center gap-2 text-xs font-medium text-muted hover:text-foreground transition-colors cursor-pointer"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Quay lại trang trước</span>
        </button>
      )}

      {/* Main card banner */}
      <Card className="overflow-hidden border-border shadow-xs">
        <div className="h-2 bg-gradient-to-r from-primary via-primary-hover to-warning" />
        <CardContent className="p-8 sm:p-12 text-center space-y-6">
          <div className="inline-flex p-4 rounded-2xl bg-slate-50 border border-border-subtle shadow-2xs">
            {renderIcon()}
          </div>

          <div className="max-w-xl mx-auto space-y-2">
            <div className="flex items-center justify-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted">
                {moduleName}
              </span>
              <Badge variant="warning">{badge}</Badge>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
              {title}
            </h1>
            <p className="text-sm text-muted leading-relaxed">
              {description}
            </p>
          </div>

          {/* Details callout box */}
          <div className="max-w-lg mx-auto bg-slate-50/80 border border-border-subtle rounded-xl p-4 text-left text-xs space-y-2.5">
            <div className="flex items-center justify-between">
              <span className="text-muted">Trạng thái kỹ thuật:</span>
              <span className="font-medium text-foreground">Đã định nghĩa hợp đồng API & Giao diện</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted">Kế hoạch hoàn tất:</span>
              <span className="font-semibold text-primary">{estimatedSprint}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-muted">Tích hợp dịch vụ:</span>
              <span className="font-medium text-foreground">Chờ kết nối backend service thực tế</span>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            {backPath ? (
              <Button
                variant="primary"
                onClick={() => navigate(backPath)}
                leftIcon={<ArrowLeft className="h-4 w-4" />}
              >
                Quay về trang chính
              </Button>
            ) : (
              <Button
                variant="primary"
                onClick={() => navigate(-1)}
                leftIcon={<ArrowLeft className="h-4 w-4" />}
              >
                Quay lại
              </Button>
            )}
            <Button
              variant="outline"
              onClick={() => window.location.reload()}
              leftIcon={<ExternalLink className="h-4 w-4" />}
            >
              Làm mới trạng thái
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default FeaturePlaceholderPage;
