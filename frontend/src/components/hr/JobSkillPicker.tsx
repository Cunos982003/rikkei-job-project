import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, Plus, Trash2, AlertCircle, RefreshCw, Check } from "lucide-react";
import { skillsApi, type SkillItem } from "../../api/endpoints/skills.api";
import { toCanonicalSkillCode } from "../../utils/skillUtils";
import { Button } from "../common/Button";
import { Input } from "../common/Input";
import { Card, CardHeader, CardTitle, CardContent } from "../common/Card";
import { Badge } from "../common/Badge";
import { cn } from "../../utils/cn";

export interface SelectedSkillItem {
  skillId: string;
  skillCode?: string;
  skillName: string;
  required: boolean;
  minScore: number;
}

const EMPTY_SKILLS: SkillItem[] = [];

export interface JobSkillPickerProps {
  selectedSkills: SelectedSkillItem[];
  onChange: (skills: SelectedSkillItem[]) => void;
  readOnly?: boolean;
}

export function JobSkillPicker({
  selectedSkills,
  onChange,
  readOnly = false,
}: JobSkillPickerProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedGroup, setSelectedGroup] = useState<string>("ALL");

  const {
    data: skillsResponse,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["skills", "catalog"],
    queryFn: async () => {
      const res = await skillsApi.getSkills();
      return res.data;
    },
    staleTime: 5 * 60 * 1000,
  });

  const allSkills: SkillItem[] = skillsResponse || EMPTY_SKILLS;

  // Danh sách các nhóm (group hoặc category)
  const groups = useMemo(() => {
    const set = new Set<string>();
    allSkills.forEach((s) => {
      const g = s.group || (s as any).category;
      if (g && typeof g === "string" && g.trim()) {
        set.add(g.trim());
      }
    });
    return Array.from(set).sort();
  }, [allSkills]);

  // Set các skillId đã chọn để kiểm tra trùng
  const selectedSkillIds = useMemo(() => {
    return new Set(selectedSkills.map((s) => String(s.skillId).toLowerCase()));
  }, [selectedSkills]);

  // Lọc kỹ năng từ catalog theo search và group
  const filteredCatalogSkills = useMemo(() => {
    return allSkills.filter((skill) => {
      const search = searchTerm.trim().toLowerCase();
      const codeStr = (skill.code || "").toLowerCase();
      const nameStr = (skill.name || "").toLowerCase();
      const matchesSearch =
        search === "" ||
        nameStr.includes(search) ||
        codeStr.includes(search);

      const skillGroup = skill.group || (skill as any).category;
      const matchesGroup =
        selectedGroup === "ALL" ||
        (skillGroup && typeof skillGroup === "string" && skillGroup.trim() === selectedGroup);

      return matchesSearch && matchesGroup;
    });
  }, [allSkills, searchTerm, selectedGroup]);

  // Số lượng kỹ năng bắt buộc
  const requiredCount = useMemo(() => {
    return selectedSkills.filter((s) => s.required).length;
  }, [selectedSkills]);

  // Thêm kỹ năng
  const handleAddSkill = (skill: SkillItem) => {
    if (readOnly) return;
    const skillIdStr = String(skill.id);
    if (selectedSkillIds.has(skillIdStr.toLowerCase())) return;

    const canonicalCode = toCanonicalSkillCode(skill.name, skill.code || skillIdStr);
    const newItem: SelectedSkillItem = {
      skillId: skillIdStr,
      skillCode: canonicalCode,
      skillName: skill.name,
      required: true, // Mặc định bắt buộc
      minScore: 70, // Mặc định 70
    };

    onChange([...selectedSkills, newItem]);
  };

  // Xoá kỹ năng
  const handleRemoveSkill = (index: number) => {
    if (readOnly) return;
    const updated = [...selectedSkills];
    updated.splice(index, 1);
    onChange(updated);
  };

  // Cập nhật thuộc tính của kỹ năng đã chọn
  const handleUpdateItem = (
    index: number,
    field: keyof SelectedSkillItem,
    value: boolean | number
  ) => {
    if (readOnly) return;
    const updated = [...selectedSkills];
    updated[index] = {
      ...updated[index],
      [field]: value,
    };
    onChange(updated);
  };

  return (
    <div className="space-y-6">
      {/* Thống kê kỹ năng */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-xl border border-border bg-slate-50/50">
        <div>
          <h4 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <span>Kỹ năng yêu cầu đã chọn</span>
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-primary-light text-primary">
              {selectedSkills.length}
            </span>
          </h4>
          <p className="text-xs text-muted mt-0.5">
            Tin tuyển dụng cần ít nhất 1 kỹ năng bắt buộc để gửi duyệt tới Cán bộ.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge
            variant={requiredCount > 0 ? "verified" : "warning"}
            size="md"
            showIcon
          >
            Số kỹ năng bắt buộc: {requiredCount}
          </Badge>
        </div>
      </div>

      {/* Danh sách kỹ năng đã chọn */}
      <div className="space-y-3">
        <h4 className="text-xs font-bold uppercase tracking-wider text-muted">
          Kỹ năng đã thiết lập ({selectedSkills.length})
        </h4>

        {selectedSkills.length === 0 ? (
          <div className="p-8 text-center border border-dashed border-border rounded-xl bg-surface">
            <p className="text-sm text-muted">
              Chưa có kỹ năng nào được chọn. Hãy chọn kỹ năng từ danh mục bên dưới.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {selectedSkills.map((item, index) => (
              <div
                key={item.skillId || index}
                className="p-4 rounded-xl border border-border bg-surface shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all"
              >
                {/* Tên kỹ năng */}
                <div className="min-w-[200px]">
                  <p className="text-sm font-bold text-foreground">
                    {item.skillName}
                  </p>
                  {item.skillCode && (
                    <p className="text-xs text-muted font-mono mt-0.5">
                      {item.skillCode}
                    </p>
                  )}
                </div>

                {/* Điều khiển Bắt buộc / Ưu tiên */}
                <div className="flex items-center gap-3">
                  <span className="text-xs text-muted font-medium">Yêu cầu:</span>
                  <div className="inline-flex rounded-lg border border-border p-0.5 bg-slate-100">
                    <button
                      type="button"
                      disabled={readOnly}
                      onClick={() => handleUpdateItem(index, "required", true)}
                      className={cn(
                        "px-2.5 py-1 text-xs font-semibold rounded-md transition-all",
                        item.required
                          ? "bg-primary text-white shadow-xs"
                          : "text-muted hover:text-foreground"
                      )}
                    >
                      Bắt buộc
                    </button>
                    <button
                      type="button"
                      disabled={readOnly}
                      onClick={() => handleUpdateItem(index, "required", false)}
                      className={cn(
                        "px-2.5 py-1 text-xs font-semibold rounded-md transition-all",
                        !item.required
                          ? "bg-slate-700 text-white shadow-xs"
                          : "text-muted hover:text-foreground"
                      )}
                    >
                      Ưu tiên
                    </button>
                  </div>
                </div>

                {/* Điểm tối thiểu: Slider + Number input */}
                <div className="flex items-center gap-3 min-w-[240px]">
                  <span className="text-xs text-muted font-medium shrink-0">
                    Điểm tối thiểu:
                  </span>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    step="5"
                    disabled={readOnly}
                    value={item.minScore}
                    onChange={(e) =>
                      handleUpdateItem(index, "minScore", Number(e.target.value))
                    }
                    className="w-full accent-primary cursor-pointer disabled:cursor-not-allowed"
                  />
                  <input
                    type="number"
                    min="0"
                    max="100"
                    disabled={readOnly}
                    value={item.minScore}
                    onChange={(e) => {
                      const val = Math.min(100, Math.max(0, Number(e.target.value)));
                      handleUpdateItem(index, "minScore", val);
                    }}
                    className="w-14 px-2 py-1 text-xs text-center font-bold border border-border rounded-md bg-surface text-foreground outline-none focus:border-primary disabled:bg-slate-100"
                  />
                </div>

                {/* Nút xoá */}
                {!readOnly && (
                  <button
                    type="button"
                    onClick={() => handleRemoveSkill(index)}
                    className="p-1.5 rounded-lg text-muted hover:text-danger hover:bg-rose-50 transition-colors self-end md:self-center"
                    title="Xoá kỹ năng"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Khối chọn từ danh mục kỹ năng chuẩn (chỉ hiện khi không ở chế độ chỉ đọc) */}
      {!readOnly && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Thêm kỹ năng từ Danh mục chuẩn</CardTitle>
          </CardHeader>

          <CardContent className="space-y-4">
            {/* Lỗi tải danh mục */}
            {isError && (
              <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 flex items-center justify-between gap-3 text-danger text-sm">
                <div className="flex items-center gap-2">
                  <AlertCircle className="h-5 w-5 shrink-0" />
                  <span>
                    Không thể tải danh mục kỹ năng:{" "}
                    {error instanceof Error ? error.message : "Lỗi kết nối máy chủ"}
                  </span>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
                  onClick={() => refetch()}
                >
                  Thử lại
                </Button>
              </div>
            )}

            {/* Ô tìm kiếm + lọc nhóm */}
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="relative flex-1">
                <Input
                  placeholder="Tìm kỹ năng theo tên hoặc mã (Java, React, SQL...)"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  leftIcon={<Search className="h-4 w-4" />}
                />
              </div>

              <select
                value={selectedGroup}
                onChange={(e) => setSelectedGroup(e.target.value)}
                className="h-10 px-3 py-2 text-sm rounded-lg border border-border bg-surface text-foreground outline-none focus:border-primary shrink-0"
              >
                <option value="ALL">Tất cả nhóm</option>
                {groups.map((grp) => (
                  <option key={grp} value={grp}>
                    {grp}
                  </option>
                ))}
              </select>
            </div>

            {/* Danh sách kỹ năng dạng thẻ có thể bấm chọn */}
            {isLoading ? (
              <div className="py-8 text-center text-sm text-muted">
                Đang tải danh mục kỹ năng chuẩn...
              </div>
            ) : filteredCatalogSkills.length === 0 ? (
              <div className="py-6 text-center text-sm text-muted">
                Không tìm thấy kỹ năng nào phù hợp.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2.5 max-h-72 overflow-y-auto p-1 border border-border-subtle rounded-lg">
                {filteredCatalogSkills.map((skill) => {
                  const isAdded = selectedSkillIds.has(String(skill.id).toLowerCase());
                  return (
                    <button
                      key={skill.id}
                      type="button"
                      disabled={isAdded}
                      onClick={() => handleAddSkill(skill)}
                      className={cn(
                        "flex items-center justify-between p-2.5 rounded-lg border text-left transition-all",
                        isAdded
                          ? "bg-slate-100 border-slate-200 text-slate-400 cursor-not-allowed opacity-60"
                          : "bg-surface border-border hover:border-primary/50 hover:bg-slate-50 text-foreground cursor-pointer"
                      )}
                    >
                      <div className="truncate pr-2">
                        <p className="text-xs font-bold truncate">{skill.name}</p>
                        <p className="text-[10px] text-muted truncate">
                          {skill.group || (skill as any).category || skill.code || ""}
                        </p>
                      </div>

                      {isAdded ? (
                        <Check className="h-4 w-4 shrink-0 text-success" />
                      ) : (
                        <Plus className="h-4 w-4 shrink-0 text-muted hover:text-primary" />
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
