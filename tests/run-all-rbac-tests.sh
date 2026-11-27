#!/usr/bin/env bash
# ==============================================================================
# NT-08: Script chạy tự động bộ test kiểm quyền (RBAC) & bảo mật xuyên suốt 7 module
# ==============================================================================

set -e

echo "================================================================="
echo "  NT-08: CHẠY BỘ TEST KIỂM QUYỀN (RBAC) & BẢO MẬT 7 MODULE"
echo "================================================================="

modules=(
    "backend/auth-service|*AuthMetricsIntegrationTest*|Module 1: Auth Service"
    "backend/profile-service|*CandidateContactPrivacyTest*|Module 2: Profile Service"
    "backend/job-service|*JobControllerIntegrationTest*|Module 3: Job Service"
    "backend/recruitment-service|*RbacSecurityIntegrationTest*|Module 4: Recruitment Service"
    "backend/learning-service|*StaffUpskillingControllerTest*|Module 5: Learning Service"
    "backend/assessment-service|*TraceContextFilterTest*|Module 6: Assessment & Evidence Service"
    "ai_career_service|*MockProviderChatTest*|Module 7: AI Career Service"
)

for entry in "${modules[@]}"; do
    IFS="|" read -r path test name <<< "$entry"
    echo -e "\n>>> Đang chạy kiểm thử: $name..."
    (cd "$path" && ./gradlew test --tests "$test" --quiet)
    echo -e "  -> [PASSED] $name"
done

echo -e "\n================================================================="
echo ">>> TẤT CẢ TEST KIỂM QUYỀN VÀ BẢO MẬT 7 MODULE ĐỀU ĐẠT! <<<"
echo "================================================================="
