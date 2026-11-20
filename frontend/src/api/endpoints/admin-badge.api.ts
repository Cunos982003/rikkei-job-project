
export interface BadgeCandidate {
    userId: string;
    fullName: string;
    email: string;
    classCode: string;
}

export interface BadgeRule {
    id: string;
    code: string;
    skillName: string;
    minScore: number;
    needInternshipTask: boolean;
}

export interface BadgeHistory {
    id: string;
    userId: string;
    badgeRuleId: string;
    badgeCode: string;
    skillName: string;
    grantedType: string;
    grantedAt: string;
    revokedAt: string | null;
    grantReason: string;
    revokeReason: string | null;
    active: boolean;
}

interface MockResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

// ======================================
// MOCK CANDIDATES
// ======================================

const mockCandidates: BadgeCandidate[] = [
    {
        userId: "CAND001",
        fullName: "Nguyễn Văn An",
        email: "an.nguyen@example.com",
        classCode: "JAVA-K18",
    },
    {
        userId: "CAND002",
        fullName: "Trần Thị Bình",
        email: "binh.tran@example.com",
        classCode: "JAVA-K18",
    },
    {
        userId: "CAND003",
        fullName: "Lê Hoàng Nam",
        email: "nam.le@example.com",
        classCode: "REACT-K19",
    },
    {
        userId: "CAND004",
        fullName: "Phạm Minh Đức",
        email: "duc.pham@example.com",
        classCode: "JAVA-K19",
    },
    {
        userId: "CAND005",
        fullName: "Đỗ Thu Hà",
        email: "ha.do@example.com",
        classCode: "REACT-K19",
    },
];

// ======================================
// MOCK BADGE RULES
// ======================================

const mockRules: BadgeRule[] = [
    {
        id: "BADGE001",
        code: "JAVA_EXPERT",
        skillName: "Java Expert",
        minScore: 90,
        needInternshipTask: true,
    },
    {
        id: "BADGE002",
        code: "SPRING_BOOT",
        skillName: "Spring Boot",
        minScore: 85,
        needInternshipTask: true,
    },
    {
        id: "BADGE003",
        code: "REACT_MASTER",
        skillName: "React Master",
        minScore: 90,
        needInternshipTask: true,
    },
    {
        id: "BADGE004",
        code: "SQL_PRO",
        skillName: "SQL Professional",
        minScore: 80,
        needInternshipTask: false,
    },
    {
        id: "BADGE005",
        code: "DOCKER_READY",
        skillName: "Docker Ready",
        minScore: 85,
        needInternshipTask: false,
    },
];

// ======================================
// MOCK BADGE HISTORY
// ======================================

let mockHistory: BadgeHistory[] = [
    {
        id: "HISTORY001",
        userId: "CAND001",
        badgeRuleId: "BADGE001",
        badgeCode: "JAVA_EXPERT",
        skillName: "Java Expert",
        grantedType: "MANUAL",
        grantedAt: "2026-11-15T09:00:00",
        revokedAt: null,
        grantReason: "Hoàn thành xuất sắc bài đánh giá Java.",
        revokeReason: null,
        active: true,
    },
    {
        id: "HISTORY002",
        userId: "CAND002",
        badgeRuleId: "BADGE004",
        badgeCode: "SQL_PRO",
        skillName: "SQL Professional",
        grantedType: "MANUAL",
        grantedAt: "2026-11-16T10:30:00",
        revokedAt: null,
        grantReason: "Đạt kết quả tốt trong bài kiểm tra SQL.",
        revokeReason: null,
        active: true,
    },
    {
        id: "HISTORY003",
        userId: "CAND003",
        badgeRuleId: "BADGE003",
        badgeCode: "REACT_MASTER",
        skillName: "React Master",
        grantedType: "MANUAL",
        grantedAt: "2026-11-17T14:00:00",
        revokedAt: null,
        grantReason: "Hoàn thành dự án React đạt yêu cầu.",
        revokeReason: null,
        active: true,
    },
];

// Mô phỏng thời gian phản hồi, không sử dụng mạng.
const mockDelay = (ms = 200) =>
    new Promise<void>((resolve) => setTimeout(resolve, ms));

// ======================================
// MOCK ADMIN BADGE API
// Không import Axios, không gọi Backend.
// ======================================

export const adminBadgeApi = {
    async getRules(): Promise<MockResponse<BadgeRule[]>> {
        await mockDelay();

        return {
            success: true,
            data: [...mockRules],
        };
    },

    async searchCandidates(
        keyword: string
    ): Promise<MockResponse<BadgeCandidate[]>> {
        await mockDelay();

        const normalized = keyword.trim().toLowerCase();

        const results = mockCandidates.filter((candidate) => {
            return (
                candidate.fullName.toLowerCase().includes(normalized) ||
                candidate.email.toLowerCase().includes(normalized) ||
                candidate.classCode.toLowerCase().includes(normalized)
            );
        });

        return {
            success: true,
            data: results,
        };
    },

    async getHistory(
        userId: string
    ): Promise<MockResponse<BadgeHistory[]>> {
        await mockDelay();

        return {
            success: true,
            data: mockHistory
                .filter((badge) => badge.userId === userId)
                .map((badge) => ({ ...badge })),
        };
    },

    async grantBadge(data: {
        userId: string;
        badgeRuleId: string;
        reason: string;
    }): Promise<MockResponse<BadgeHistory>> {
        await mockDelay();

        const candidate = mockCandidates.find(
            (item) => item.userId === data.userId
        );

        if (!candidate) {
            return {
                success: false,
                message: "Không tìm thấy Candidate.",
                data: null as unknown as BadgeHistory,
            };
        }

        const rule = mockRules.find(
            (item) => item.id === data.badgeRuleId
        );

        if (!rule) {
            return {
                success: false,
                message: "Không tìm thấy quy tắc huy hiệu.",
                data: null as unknown as BadgeHistory,
            };
        }

        const alreadyActive = mockHistory.some(
            (item) =>
                item.userId === data.userId &&
                item.badgeRuleId === data.badgeRuleId &&
                item.active
        );

        if (alreadyActive) {
            return {
                success: false,
                message: "Candidate đã sở hữu huy hiệu này.",
                data: null as unknown as BadgeHistory,
            };
        }

        const newBadge: BadgeHistory = {
            id: `HISTORY${Date.now()}${Math.floor(Math.random() * 1000)}`,
            userId: data.userId,
            badgeRuleId: rule.id,
            badgeCode: rule.code,
            skillName: rule.skillName,
            grantedType: "MANUAL",
            grantedAt: new Date().toISOString(),
            revokedAt: null,
            grantReason: data.reason,
            revokeReason: null,
            active: true,
        };

        mockHistory = [newBadge, ...mockHistory];

        return {
            success: true,
            data: { ...newBadge },
        };
    },

    async revokeBadge(
        id: string,
        reason: string
    ): Promise<MockResponse<BadgeHistory>> {
        await mockDelay();

        const badge = mockHistory.find((item) => item.id === id);

        if (!badge || !badge.active) {
            return {
                success: false,
                message: "Không tìm thấy huy hiệu đang hoạt động.",
                data: null as unknown as BadgeHistory,
            };
        }

        badge.active = false;
        badge.revokedAt = new Date().toISOString();
        badge.revokeReason = reason;

        return {
            success: true,
            data: { ...badge },
        };
    },
};
