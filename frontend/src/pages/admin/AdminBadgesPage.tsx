
import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import {
    Award,
    Search,
    RefreshCw,
    UserRound,
    ShieldCheck,
    Clock,
    Ban,
    CheckCircle2,
    AlertCircle,
} from "lucide-react";

import { toast } from "sonner";

import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { Modal } from "../../components/common/Modal";

import {
    adminBadgeApi,
    type BadgeCandidate,
    type BadgeHistory,
    type BadgeRule,
} from "../../api/endpoints/admin-badge.api";

export function AdminBadgesPage() {
    // =====================================
    // STATE
    // =====================================

    const [candidates, setCandidates] = useState<BadgeCandidate[]>([]);
    const [rules, setRules] = useState<BadgeRule[]>([]);
    const [history, setHistory] = useState<BadgeHistory[]>([]);

    const [keyword, setKeyword] = useState("");

    const [selectedCandidate, setSelectedCandidate] =
        useState<BadgeCandidate | null>(null);

    const [loading, setLoading] = useState(false);
    const [rulesLoading, setRulesLoading] = useState(false);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const [pageError, setPageError] = useState("");

    const [grantModalOpen, setGrantModalOpen] = useState(false);

    const [revokeTarget, setRevokeTarget] =
        useState<BadgeHistory | null>(null);

    const [selectedRuleId, setSelectedRuleId] = useState("");
    const [grantReason, setGrantReason] = useState("");
    const [revokeReason, setRevokeReason] = useState("");

    // Tránh kết quả request cũ ghi đè request mới.
    const searchRequestId = useRef(0);
    const historyRequestId = useRef(0);

    // =====================================
    // COMPUTED DATA
    // =====================================

    const activeBadges = useMemo(
        () => history.filter((item) => item.active),
        [history]
    );

    const inactiveBadges = useMemo(
        () => history.filter((item) => !item.active),
        [history]
    );

    const selectedRule = useMemo(
        () => rules.find(
            (rule) => String(rule.id) === selectedRuleId
        ),
        [rules, selectedRuleId]
    );

    // Không cho trao lại huy hiệu đang có hiệu lực.
    const availableRules = useMemo(
        () =>
            rules.filter(
                (rule) =>
                    !activeBadges.some(
                        (badge) =>
                            String(badge.badgeRuleId) === String(rule.id)
                    )
            ),
        [rules, activeBadges]
    );

    // =====================================
    // LOAD BADGE RULES - MOCK
    // =====================================

    const loadRules = useCallback(async () => {
        setRulesLoading(true);

        try {
            const response = await adminBadgeApi.getRules();

            if (!response.success) {
                throw new Error(
                    response.message || "Không thể tải quy tắc huy hiệu."
                );
            }

            setRules(response.data ?? []);
        } catch (error) {
            console.error("Load badge rules error:", error);

            toast.error(
                error instanceof Error
                    ? error.message
                    : "Không thể tải danh sách quy tắc huy hiệu."
            );
        } finally {
            setRulesLoading(false);
        }
    }, []);

    // =====================================
    // SEARCH CANDIDATES - MOCK
    // =====================================

    const searchCandidates = useCallback(async (value: string) => {
        const requestId = ++searchRequestId.current;

        setLoading(true);
        setPageError("");

        try {
            const response = await adminBadgeApi.searchCandidates(
                value.trim()
            );

            if (requestId !== searchRequestId.current) return;

            if (!response.success) {
                throw new Error(
                    response.message || "Không thể tìm kiếm ứng viên."
                );
            }

            const result = response.data ?? [];

            setCandidates(result);

            setSelectedCandidate((current) => {
                if (!current) return null;

                const stillExists = result.some(
                    (item) => item.userId === current.userId
                );

                return stillExists ? current : null;
            });

            if (result.length === 0) {
                historyRequestId.current++;
                setHistory([]);
            }
        } catch (error) {
            if (requestId !== searchRequestId.current) return;

            console.error("Search candidates error:", error);

            setCandidates([]);
            setSelectedCandidate(null);
            setHistory([]);

            setPageError(
                error instanceof Error
                    ? error.message
                    : "Không thể tải danh sách ứng viên mẫu."
            );

            toast.error("Không thể tìm kiếm ứng viên.");
        } finally {
            if (requestId === searchRequestId.current) {
                setLoading(false);
            }
        }
    }, []);

    // =====================================
    // LOAD CANDIDATE HISTORY - MOCK
    // =====================================

    const loadHistory = useCallback(async (userId: string) => {
        const requestId = ++historyRequestId.current;

        setHistoryLoading(true);
        setHistory([]);

        try {
            const response = await adminBadgeApi.getHistory(userId);

            if (requestId !== historyRequestId.current) return;

            if (!response.success) {
                throw new Error(
                    response.message || "Không thể tải lịch sử huy hiệu."
                );
            }

            setHistory(response.data ?? []);
        } catch (error) {
            if (requestId !== historyRequestId.current) return;

            console.error("Load badge history error:", error);

            setHistory([]);

            toast.error(
                error instanceof Error
                    ? error.message
                    : "Không thể tải lịch sử huy hiệu."
            );
        } finally {
            if (requestId === historyRequestId.current) {
                setHistoryLoading(false);
            }
        }
    }, []);

    // =====================================
    // INITIAL LOAD
    // =====================================

    useEffect(() => {
        void loadRules();
        void searchCandidates("");
    }, [loadRules, searchCandidates]);

    // =====================================
    // LOAD HISTORY WHEN SELECT CANDIDATE
    // =====================================

    useEffect(() => {
        if (!selectedCandidate) {
            historyRequestId.current++;
            setHistory([]);
            setHistoryLoading(false);
            return;
        }

        void loadHistory(selectedCandidate.userId);
    }, [selectedCandidate, loadHistory]);

    // =====================================
    // SELECT CANDIDATE
    // =====================================

    const handleSelectCandidate = (candidate: BadgeCandidate) => {
        if (selectedCandidate?.userId === candidate.userId) {
            return;
        }

        setSelectedCandidate(candidate);
        setHistory([]);
    };

    // =====================================
    // REFRESH
    // =====================================

    const handleRefresh = async () => {
        await Promise.all([
            searchCandidates(keyword),
            loadRules(),
        ]);

        toast.success("Đã làm mới dữ liệu Mock.");
    };

    // =====================================
    // GRANT BADGE
    // =====================================

    const handleGrant = async () => {
        if (!selectedCandidate) {
            toast.error("Vui lòng chọn ứng viên.");
            return;
        }

        if (!selectedRuleId) {
            toast.error("Vui lòng chọn huy hiệu.");
            return;
        }

        if (grantReason.trim().length < 5) {
            toast.error("Lý do trao huy hiệu phải có ít nhất 5 ký tự.");
            return;
        }

        const alreadyHasBadge = activeBadges.some(
            (badge) =>
                String(badge.badgeRuleId) === String(selectedRuleId)
        );

        if (alreadyHasBadge) {
            toast.error("Ứng viên đã sở hữu huy hiệu này.");
            return;
        }

        setSubmitting(true);

        try {
            const response = await adminBadgeApi.grantBadge({
                userId: selectedCandidate.userId,
                badgeRuleId: selectedRuleId,
                reason: grantReason.trim(),
            });

            if (!response.success) {
                throw new Error(
                    response.message || "Không thể trao huy hiệu."
                );
            }

            toast.success("Đã trao huy hiệu thành công.");

            setGrantModalOpen(false);
            setSelectedRuleId("");
            setGrantReason("");

            await loadHistory(selectedCandidate.userId);
        } catch (error) {
            console.error("Grant badge error:", error);

            toast.error(
                error instanceof Error
                    ? error.message
                    : "Không thể trao huy hiệu."
            );
        } finally {
            setSubmitting(false);
        }
    };

    // =====================================
    // REVOKE BADGE
    // =====================================

    const handleRevoke = async () => {
        if (!revokeTarget || !selectedCandidate) {
            return;
        }

        if (revokeReason.trim().length < 5) {
            toast.error("Lý do thu hồi phải có ít nhất 5 ký tự.");
            return;
        }

        setSubmitting(true);

        try {
            const response = await adminBadgeApi.revokeBadge(
                revokeTarget.id,
                revokeReason.trim()
            );

            if (!response.success) {
                throw new Error(
                    response.message || "Không thể thu hồi huy hiệu."
                );
            }

            toast.success("Đã thu hồi huy hiệu thành công.");

            setRevokeTarget(null);
            setRevokeReason("");

            await loadHistory(selectedCandidate.userId);
        } catch (error) {
            console.error("Revoke badge error:", error);

            toast.error(
                error instanceof Error
                    ? error.message
                    : "Không thể thu hồi huy hiệu."
            );
        } finally {
            setSubmitting(false);
        }
    };

    // =====================================
    // HELPERS
    // =====================================

    const formatDate = (
        value: string | null | undefined
    ) => {
        if (!value) return "—";

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return "—";
        }

        return date.toLocaleString("vi-VN");
    };

    const getBadgeTitle = (item: BadgeHistory) => {
        return item.skillName
            ? `${item.badgeCode} – ${item.skillName}`
            : item.badgeCode;
    };

    // =====================================
    // RENDER
    // =====================================

    return (
        <div className="space-y-6">

            {/* HEADER - RED BACKGROUND, WHITE TEXT */}
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-600 p-5 shadow-md">
                <div className="flex items-center gap-3">
                    <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-white/15">
                        <Award className="h-7 w-7 text-white" />
                    </div>

                    <div>
                        <h1 className="text-2xl font-bold text-white">
                            Quản lý huy hiệu ứng viên
                        </h1>

                        <p className="mt-1 text-xs text-white/90">
                            ADMIN • Trao, thu hồi và theo dõi lịch sử huy hiệu Candidate
                        </p>
                    </div>
                </div>

                <Button
                    variant="outline"
                    leftIcon={<RefreshCw className="h-4 w-4" />}
                    onClick={() => void handleRefresh()}
                    disabled={loading || rulesLoading || submitting}
                    className="border-white text-white hover:bg-white hover:text-red-600"
                >
                    Làm mới
                </Button>
            </div>

            {/* SUMMARY */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">

                <div className="rounded-xl border border-border bg-surface p-4">
                    <div className="flex items-center gap-3">
                        <UserRound className="h-5 w-5 text-primary" />

                        <div>
                            <p className="text-xs text-muted">
                                Ứng viên tìm thấy
                            </p>

                            <p className="text-xl font-bold">
                                {loading ? "..." : candidates.length}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="rounded-xl border border-border bg-surface p-4">
                    <div className="flex items-center gap-3">
                        <Award className="h-5 w-5 text-warning" />

                        <div>
                            <p className="text-xs text-muted">
                                Huy hiệu đang sở hữu
                            </p>

                            <p className="text-xl font-bold">
                                {historyLoading ? "..." : activeBadges.length}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="rounded-xl border border-border bg-surface p-4">
                    <div className="flex items-center gap-3">
                        <ShieldCheck className="h-5 w-5 text-success" />

                        <div>
                            <p className="text-xs text-muted">
                                Tổng quy tắc huy hiệu
                            </p>

                            <p className="text-xl font-bold">
                                {rulesLoading ? "..." : rules.length}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* SEARCH CANDIDATE */}
            <section className="rounded-xl border border-border bg-surface p-5">
                <div className="mb-4">
                    <h2 className="font-semibold">
                        Tìm kiếm ứng viên
                    </h2>

                    <p className="mt-1 text-xs text-muted">
                        Tìm theo họ tên, email hoặc mã lớp trong danh sách Candidate Mock.
                    </p>
                </div>

                <form
                    className="flex flex-col gap-3 sm:flex-row"
                    onSubmit={(event) => {
                        event.preventDefault();
                        void searchCandidates(keyword);
                    }}
                >
                    <div className="flex-1">
                        <Input
                            value={keyword}
                            onChange={(event) =>
                                setKeyword(event.target.value)
                            }
                            placeholder="Nhập tên, email hoặc mã lớp..."
                            leftIcon={<Search className="h-4 w-4" />}
                        />
                    </div>

                    <Button
                        type="submit"
                        isLoading={loading}
                    >
                        Tìm kiếm
                    </Button>
                </form>

                {pageError && (
                    <div className="mt-4 flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-danger">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        <span>{pageError}</span>
                    </div>
                )}

                <div className="mt-4 overflow-x-auto">
                    <table className="w-full min-w-[600px] text-left text-sm">
                        <thead>
                        <tr className="border-b border-border text-xs text-muted">
                            <th className="px-3 py-3">Ứng viên</th>
                            <th className="px-3 py-3">Email</th>
                            <th className="px-3 py-3">Lớp</th>
                            <th className="px-3 py-3 text-right">Thao tác</th>
                        </tr>
                        </thead>

                        <tbody>
                        {candidates.map((candidate) => (
                            <tr
                                key={candidate.userId}
                                className={`border-b border-border-subtle ${
                                    selectedCandidate?.userId === candidate.userId
                                        ? "bg-primary-light"
                                        : "hover:bg-slate-50"
                                }`}
                            >
                                <td className="px-3 py-3 font-medium">
                                    {candidate.fullName || "Chưa cập nhật"}
                                </td>

                                <td className="px-3 py-3 text-muted">
                                    {candidate.email || "—"}
                                </td>

                                <td className="px-3 py-3">
                                    {candidate.classCode || "—"}
                                </td>

                                <td className="px-3 py-3 text-right">
                                    <Button
                                        size="sm"
                                        variant={
                                            selectedCandidate?.userId === candidate.userId
                                                ? "secondary"
                                                : "outline"
                                        }
                                        onClick={() =>
                                            handleSelectCandidate(candidate)
                                        }
                                    >
                                        {selectedCandidate?.userId === candidate.userId
                                            ? "Đang chọn"
                                            : "Chọn ứng viên"}
                                    </Button>
                                </td>
                            </tr>
                        ))}

                        {!loading && candidates.length === 0 && (
                            <tr>
                                <td
                                    colSpan={4}
                                    className="px-3 py-8 text-center text-muted"
                                >
                                    {pageError
                                        ? "Không thể tải danh sách ứng viên mẫu."
                                        : "Không tìm thấy ứng viên."}
                                </td>
                            </tr>
                        )}

                        {loading && (
                            <tr>
                                <td
                                    colSpan={4}
                                    className="px-3 py-8 text-center text-muted"
                                >
                                    <RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />
                                    Đang tìm kiếm ứng viên...
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </section>

            {/* SELECTED CANDIDATE */}
            {selectedCandidate && (
                <section className="space-y-5 rounded-xl border border-border bg-surface p-5">

                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h2 className="font-semibold">
                                Quản lý huy hiệu Candidate
                            </h2>

                            <p className="mt-1 text-xs text-muted">
                                {selectedCandidate.fullName}
                                {" • "}
                                {selectedCandidate.email}
                            </p>
                        </div>

                        <Button
                            leftIcon={<Award className="h-4 w-4" />}
                            onClick={() => {
                                setSelectedRuleId("");
                                setGrantReason("");
                                setGrantModalOpen(true);
                            }}
                            disabled={
                                rulesLoading ||
                                rules.length === 0 ||
                                availableRules.length === 0 ||
                                historyLoading ||
                                submitting
                            }
                        >
                            Trao huy hiệu
                        </Button>
                    </div>

                    {historyLoading ? (
                        <div className="py-8 text-center text-sm text-muted">
                            <RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />
                            Đang tải lịch sử huy hiệu...
                        </div>
                    ) : (
                        <>
                            {/* ACTIVE BADGES */}
                            <div>
                                <h3 className="mb-3 text-sm font-semibold">
                                    Huy hiệu đang sở hữu ({activeBadges.length})
                                </h3>

                                {activeBadges.length === 0 ? (
                                    <div className="rounded-lg bg-slate-50 p-4 text-sm text-muted">
                                        Candidate chưa có huy hiệu đang hiệu lực.
                                    </div>
                                ) : (
                                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                                        {activeBadges.map((badge) => (
                                            <div
                                                key={badge.id}
                                                className="rounded-xl border border-border p-4"
                                            >
                                                <div className="flex items-start justify-between gap-2">
                                                    <div className="flex items-center gap-2">
                                                        <Award className="h-6 w-6 shrink-0 text-warning" />

                                                        <div>
                                                            <p className="text-sm font-semibold">
                                                                {getBadgeTitle(badge)}
                                                            </p>

                                                            <p className="text-xs text-muted">
                                                                {badge.grantedType === "MANUAL"
                                                                    ? "ADMIN trao thủ công"
                                                                    : "Hệ thống tự động cấp"}
                                                            </p>
                                                        </div>
                                                    </div>

                                                    <CheckCircle2 className="h-4 w-4 shrink-0 text-success" />
                                                </div>

                                                <p className="mt-3 text-xs text-muted">
                                                    Ngày cấp: {formatDate(badge.grantedAt)}
                                                </p>

                                                <p className="mt-1 text-xs text-muted">
                                                    Lý do: {badge.grantReason || "—"}
                                                </p>

                                                <Button
                                                    size="sm"
                                                    variant="danger"
                                                    className="mt-4 w-full"
                                                    leftIcon={<Ban className="h-4 w-4" />}
                                                    onClick={() => {
                                                        setRevokeTarget(badge);
                                                        setRevokeReason("");
                                                    }}
                                                    disabled={submitting}
                                                >
                                                    Thu hồi huy hiệu
                                                </Button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {/* HISTORY */}
                            <div>
                                <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold">
                                    <Clock className="h-4 w-4" />
                                    Lịch sử trao và thu hồi
                                </h3>

                                <div className="overflow-x-auto">
                                    <table className="w-full min-w-[850px] text-left text-sm">
                                        <thead>
                                        <tr className="border-b border-border text-xs text-muted">
                                            <th className="px-3 py-3">Huy hiệu</th>
                                            <th className="px-3 py-3">Loại cấp</th>
                                            <th className="px-3 py-3">Ngày cấp</th>
                                            <th className="px-3 py-3">Lý do cấp</th>
                                            <th className="px-3 py-3">Ngày thu hồi</th>
                                            <th className="px-3 py-3">Lý do thu hồi</th>
                                            <th className="px-3 py-3">Trạng thái</th>
                                        </tr>
                                        </thead>

                                        <tbody>
                                        {history.map((badge) => (
                                            <tr
                                                key={badge.id}
                                                className="border-b border-border-subtle"
                                            >
                                                <td className="px-3 py-3 font-medium">
                                                    {getBadgeTitle(badge)}
                                                </td>

                                                <td className="px-3 py-3">
                                                    {badge.grantedType === "MANUAL"
                                                        ? "Thủ công"
                                                        : "Tự động"}
                                                </td>

                                                <td className="px-3 py-3 text-xs">
                                                    {formatDate(badge.grantedAt)}
                                                </td>

                                                <td className="max-w-[180px] px-3 py-3 text-xs">
                                                    {badge.grantReason || "—"}
                                                </td>

                                                <td className="px-3 py-3 text-xs">
                                                    {formatDate(badge.revokedAt)}
                                                </td>

                                                <td className="max-w-[180px] px-3 py-3 text-xs">
                                                    {badge.revokeReason || "—"}
                                                </td>

                                                <td className="px-3 py-3">
                                                    {badge.active ? (
                                                        <span className="text-xs font-medium text-success">
                                                                Đang hiệu lực
                                                            </span>
                                                    ) : (
                                                        <span className="text-xs font-medium text-danger">
                                                                Đã thu hồi
                                                            </span>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}

                                        {history.length === 0 && (
                                            <tr>
                                                <td
                                                    colSpan={7}
                                                    className="px-3 py-6 text-center text-muted"
                                                >
                                                    Chưa có lịch sử huy hiệu.
                                                </td>
                                            </tr>
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            {/* REVOKED BADGE SUMMARY */}
                            {inactiveBadges.length > 0 && (
                                <p className="text-xs text-muted">
                                    Tổng số huy hiệu đã thu hồi: {inactiveBadges.length}
                                </p>
                            )}
                        </>
                    )}
                </section>
            )}

            {/* GRANT MODAL */}
            <Modal
                isOpen={grantModalOpen}
                onClose={() => {
                    if (!submitting) {
                        setGrantModalOpen(false);
                    }
                }}
                title="Trao huy hiệu cho Candidate"
                description={
                    selectedCandidate?.fullName || "Candidate được chọn"
                }
                closeOnOverlayClick={!submitting}
                footer={
                    <>
                        <Button
                            variant="outline"
                            onClick={() => setGrantModalOpen(false)}
                            disabled={submitting}
                        >
                            Hủy
                        </Button>

                        <Button
                            onClick={() => void handleGrant()}
                            isLoading={submitting}
                        >
                            Xác nhận trao
                        </Button>
                    </>
                }
            >
                <div className="space-y-4">
                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Chọn huy hiệu
                        </label>

                        <select
                            value={selectedRuleId}
                            onChange={(event) =>
                                setSelectedRuleId(event.target.value)
                            }
                            disabled={submitting || rulesLoading}
                            className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm outline-none focus:border-primary"
                        >
                            <option value="">
                                -- Chọn huy hiệu --
                            </option>

                            {availableRules.map((rule) => (
                                <option
                                    key={rule.id}
                                    value={rule.id}
                                >
                                    {rule.code}
                                    {rule.skillName
                                        ? ` – ${rule.skillName}`
                                        : ""}
                                </option>
                            ))}
                        </select>

                        {availableRules.length === 0 && (
                            <p className="mt-2 text-xs text-muted">
                                Candidate đã sở hữu tất cả huy hiệu hiện có.
                            </p>
                        )}
                    </div>

                    {selectedRule && (
                        <div className="rounded-lg bg-slate-50 p-3 text-xs text-muted">
                            <p>
                                Kỹ năng: {selectedRule.skillName || "Không giới hạn"}
                            </p>

                            <p>
                                Điểm tối thiểu: {selectedRule.minScore}
                            </p>

                            <p>
                                Yêu cầu nhiệm vụ thực tập:{" "}
                                {selectedRule.needInternshipTask ? "Có" : "Không"}
                            </p>
                        </div>
                    )}

                    <div>
                        <label className="mb-2 block text-sm font-medium">
                            Lý do trao huy hiệu *
                        </label>

                        <textarea
                            value={grantReason}
                            onChange={(event) =>
                                setGrantReason(event.target.value)
                            }
                            disabled={submitting}
                            rows={4}
                            maxLength={1000}
                            placeholder="Nhập lý do trao huy hiệu..."
                            className="w-full rounded-lg border border-border bg-surface p-3 text-sm outline-none focus:border-primary"
                        />

                        <p className="mt-1 text-right text-xs text-muted">
                            {grantReason.length}/1000
                        </p>
                    </div>
                </div>
            </Modal>

            {/* REVOKE MODAL */}
            <Modal
                isOpen={!!revokeTarget}
                onClose={() => {
                    if (!submitting) {
                        setRevokeTarget(null);
                    }
                }}
                title="Thu hồi huy hiệu"
                description={
                    revokeTarget
                        ? getBadgeTitle(revokeTarget)
                        : undefined
                }
                closeOnOverlayClick={!submitting}
                footer={
                    <>
                        <Button
                            variant="outline"
                            onClick={() => setRevokeTarget(null)}
                            disabled={submitting}
                        >
                            Hủy
                        </Button>

                        <Button
                            variant="danger"
                            onClick={() => void handleRevoke()}
                            isLoading={submitting}
                        >
                            Xác nhận thu hồi
                        </Button>
                    </>
                }
            >
                <div>
                    <label className="mb-2 block text-sm font-medium">
                        Lý do thu hồi *
                    </label>

                    <textarea
                        value={revokeReason}
                        onChange={(event) =>
                            setRevokeReason(event.target.value)
                        }
                        disabled={submitting}
                        rows={4}
                        maxLength={1000}
                        placeholder="Nhập lý do thu hồi huy hiệu..."
                        className="w-full rounded-lg border border-border bg-surface p-3 text-sm outline-none focus:border-primary"
                    />

                    <p className="mt-1 text-right text-xs text-muted">
                        {revokeReason.length}/1000
                    </p>
                </div>
            </Modal>
        </div>
    );
}

export default AdminBadgesPage;
