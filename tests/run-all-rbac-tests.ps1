# ==============================================================================
# NT-08: Script chạy tự động bộ test kiểm quyền (RBAC) & bảo mật xuyên suốt 7 module
# ==============================================================================

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  NT-08: CHẠY BỘ TEST KIỂM QUYỀN (RBAC) & BẢO MẬT 7 MODULE" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$modules = @(
    @{ Name = "Module 1: Auth Service (Quyền & Metrics)"; Path = "backend/auth-service"; Test = "*AuthMetricsIntegrationTest*" },
    @{ Name = "Module 2: Profile Service (Bảo mật liên hệ PII & File)"; Path = "backend/profile-service"; Test = "*CandidateContactPrivacyTest*" },
    @{ Name = "Module 3: Job Service (Quản lý tin tuyển dụng HR/Staff)"; Path = "backend/job-service"; Test = "*JobControllerIntegrationTest*" },
    @{ Name = "Module 4: Recruitment Service (RBAC, SQLi, XSS, DB Constraints)"; Path = "backend/recruitment-service"; Test = "*RbacSecurityIntegrationTest*" },
    @{ Name = "Module 5: Learning Service (Phân quyền LMS / Khóa học)"; Path = "backend/learning-service"; Test = "*StaffUpskillingControllerTest*" },
    @{ Name = "Module 6: Assessment & Evidence Service (Minh chứng & Đánh giá)"; Path = "backend/assessment-service"; Test = "*TraceContextFilterTest*" },
    @{ Name = "Module 7: AI Career Service (Bảo mật AI & Chat)"; Path = "ai_career_service"; Test = "*MockProviderChatTest*" }
)

$results = @()
$allPassed = $true

foreach ($mod in $modules) {
    Write-Host "`n>>> Đang chạy kiểm thử: $($mod.Name)..." -ForegroundColor Yellow
    Push-Location $mod.Path
    try {
        $cmd = "./gradlew test --tests `"$($mod.Test)`" --quiet"
        Invoke-Expression $cmd
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  -> [PASSED] $($mod.Name)" -ForegroundColor Green
            $results += [PSCustomObject]@{ Module = $mod.Name; Status = "PASSED" }
        } else {
            Write-Host "  -> [FAILED] $($mod.Name) (Exit Code: $LASTEXITCODE)" -ForegroundColor Red
            $results += [PSCustomObject]@{ Module = $mod.Name; Status = "FAILED" }
            $allPassed = $false
        }
    } catch {
        Write-Host "  -> [ERROR] Lỗi thực thi: $_" -ForegroundColor Red
        $results += [PSCustomObject]@{ Module = $mod.Name; Status = "ERROR" }
        $allPassed = $false
    } finally {
        Pop-Location
    }
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "                    TỔNG KẾT KẾT QUẢ KIỂM THỬ                   " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
$results | Format-Table -AutoSize

if ($allPassed) {
    Write-Host ">>> TẤT CẢ TEST KIỂM QUYỀN VÀ BẢO MẬT 7 MODULE ĐỀU ĐẠT! <<<" -ForegroundColor Green
    exit 0
} else {
    Write-Host ">>> CÓ MODULE KHÔNG ĐẠT YÊU CẦU KIỂM THỬ! <<<" -ForegroundColor Red
    exit 1
}
