# [DN-06] Tính toán điểm khớp ứng viên với tin tuyển dụng (JD) theo kỹ năng

## 1. Mô tả Tổng quan
- **Mục tiêu**: Bổ sung tính năng cốt lõi DN-06 vào `job-service`, cho phép tính toán độ phù hợp kỹ năng (match score) giữa năng lực đã kiểm định (`verifiedScore`) của ứng viên và các yêu cầu kỹ năng trong tin tuyển dụng (JD). Phục vụ trực tiếp cho HR đánh giá ứng viên và cho ứng viên tự tra cứu mức độ đáp ứng của bản thân.
- **Service Backend chính**: `backend/job-service` (cổng 8085).
- **Service phụ trợ (bước 0)**: `backend/profile-service` (cổng 8082, bổ sung endpoint nội bộ `GET /api/v1/profiles/internal/{userId}/skill-scores` phục vụ lấy điểm kỹ năng đã kiểm định).
- **OpenAPI Specification**: `docs/api/openapi/job-service-dn06.yaml`.
- **Tài liệu thuật toán chi tiết**: `docs/matching.md`.

---

## 2. Danh sách API Mới (DN-06)

| STT | Phương thức | Đường dẫn | Phân quyền | Mô tả chi tiết |
|:---:|:---:|:---|:---:|:---|
| 1 | `GET` | `/jobs/{id}/match/me` | `ROLE_CANDIDATE` | Ứng viên xem điểm khớp kỹ năng của chính mình với JD ở trạng thái `APPROVED`. Tự động trích xuất `userId` từ JWT token. |
| 2 | `GET` | `/jobs/{id}/match/{userId}` | `ROLE_HR`, `ROLE_STAFF`, `ROLE_ADMIN` | Xem điểm khớp kỹ năng của một ứng viên cụ thể với JD. HR chỉ được xem JD thuộc công ty mình (`404` nếu khác công ty). STAFF/ADMIN xem được mọi JD. |

---

## 3. Công thức Tính Điểm & Quy tắc So khớp

1. **Khớp kỹ năng**: Khớp không phân biệt hoa thường theo `skillCode` (ví dụ: `JAVA_CORE` khớp với `java_core`). Nếu JD không có `skillCode`, so khớp theo `skillId`.
2. **Điểm kỹ năng ứng viên (`candidateScore`)**: Lấy từ `verifiedScore`. Bỏ qua hoàn toàn `selfScore`. Nếu chưa có điểm hoặc chưa kiểm định thì `candidateScore = 0`.
3. **Tỷ lệ đáp ứng từng kỹ năng (`skillRatio`)**:
   - Nếu $\text{minScore} == 0 \rightarrow \text{skillRatio} = 1.0$.
   - Nếu $\text{minScore} > 0 \rightarrow \text{skillRatio} = \min\left(\frac{\text{candidateScore}}{\text{minScore}}, 1.0\right)$.
4. **Điểm tổng (`score`)**:
   - $TB_{\text{req}}$ = Trung bình $\text{skillRatio}$ các kỹ năng bắt buộc (`required=true`).
   - $TB_{\text{opt}}$ = Trung bình $\text{skillRatio}$ các kỹ năng ưu tiên (`required=false`).
   - $\text{score} = 70 \times TB_{\text{req}} + 30 \times TB_{\text{opt}}$.
   - Nếu chỉ có bắt buộc: $\text{score} = 100 \times TB_{\text{req}}$.
   - Nếu chỉ có ưu tiên: $\text{score} = 100 \times TB_{\text{opt}}$.
   - Nếu JD không có kỹ năng nào: $\text{score} = 0.0$.
   - Làm tròn 1 chữ số thập phân (`HALF_UP`) bằng `BigDecimal`.
5. **Cảnh báo dưới ngưỡng (`belowMinimum`)**:
   - `true` nếu tồn tại ít nhất 1 kỹ năng bắt buộc có $\text{minScore} > 0$ và $\text{candidateScore} < 0.5 \times \text{minScore}$.
   - Đúng bằng 50% thì `belowMinimum = false` (nghiêm ngặt).
6. **Danh sách `met` và `missing`**:
   - `met`: Kỹ năng có $\text{candidateScore} \ge \text{minScore}$, $\text{gap} = 0$.
   - `missing`: Kỹ năng có $\text{candidateScore} < \text{minScore}$, $\text{gap} = \text{minScore} - \text{candidateScore}$.
   - Sắp xếp: Kỹ năng bắt buộc đứng trước, ưu tiên đứng sau, cùng nhóm sắp xếp theo tên kỹ năng A-Z.
7. **Cơ chế lưu đệm & Stale Hook**:
   - Điểm khớp được lưu bảng `match_scores` (`score`, `below_minimum`, `detail` JSON, `stale`, `computed_at`).
   - Mỗi khi JD thay đổi kỹ năng hoặc thông tin (`PUT /jobs/{id}/skills`, `PUT /jobs/{id}`), hệ thống gọi hook `matchScoreRepository.markStaleByJobId(jobId)` để đánh dấu `stale = true`.
   - Khi API match được gọi, hệ thống **LUÔN tính toán lại**, cập nhật bảng ghi với `stale = false` và trả kết quả mới nhất.

---

## 4. Bảng Ví dụ Tính tay Đối chiếu với Unit Test

| Kịch bản | Yêu cầu kỹ năng JD | Kỹ năng ứng viên (verified) | Tính toán chi tiết | Điểm (`score`) | `belowMinimum` | `met` / `missing` | Test case đối chiếu |
|---|---|---|---|:---:|:---:|---|---|
| **Ví dụ A** | JAVA_CORE (req, min 70)<br>SPRING_BOOT (req, min 60)<br>GIT (opt, min 50) | JAVA_CORE: 70<br>SPRING_BOOT: 30<br>GIT: 50 | $TB_{\text{req}} = (1.0 + 0.5)/2 = 0.75$<br>$TB_{\text{opt}} = 1.0$<br>Score = $70 \times 0.75 + 30 \times 1.0 = 82.5$<br>SPRING_BOOT 30 đúng bằng 50% của 60 | **82.5** | `false` | **met**: JAVA_CORE, GIT<br>**missing**: SPRING_BOOT (gap 30) | `caseA_mixedRequiredAndOptional_exact50Percent_notBelowMinimum` |
| **Ví dụ A2** | JAVA_CORE (req, min 70)<br>SPRING_BOOT (req, min 60)<br>GIT (opt, min 50) | JAVA_CORE: 70<br>SPRING_BOOT: 29<br>GIT: 50 | $TB_{\text{req}} = (1.0 + 29/60)/2 \approx 0.74167$<br>Score = $70 \times 0.74167 + 30 = 81.916... \rightarrow 81.9$<br>SPRING_BOOT 29 < 30 (50%) | **81.9** | `true` | **met**: JAVA_CORE, GIT<br>**missing**: SPRING_BOOT (gap 31) | `caseA2_candidateScoreBelow50Percent_triggersBelowMinimum` |
| **Ví dụ B** | JAVA_CORE (req, min 80)<br>SQL (req, min 60)<br>REACT (req, min 50) | JAVA_CORE: 40<br>SQL: 60<br>REACT: Không có (0) | $TB_{\text{req}} = (40/80 + 60/60 + 0/50)/3 = 0.5$<br>Score = $100 \times 0.5 = 50.0$<br>REACT 0 < 25 (50% của 50) | **50.0** | `true` | **met**: SQL<br>**missing**: JAVA_CORE (gap 40), REACT (gap 50) | `caseB_onlyRequiredSkills_missingSkill_belowMinimumTrue` |
| **Ví dụ C** | JAVA_CORE (req, min 80)<br>GIT (opt, min 50) | Ứng viên không có kỹ năng nào | Candidate score các kỹ năng = 0<br>Score = 0.0<br>0 < 40 (50% của 80) | **0.0** | `true` | **met**: []<br>**missing**: Đủ cả JAVA_CORE (gap 80) và GIT (gap 50) | `caseC_candidateNoSkills_scoreZero_allInMissing` |

---

## 5. Bảng Đối chiếu Tiêu chí Nghiệm thu với Test

| STT | Tiêu chí nghiệm thu | File kiểm thử | Phương thức test cụ thể | Kết quả |
|:---:|---|---|---|:---:|
| 1 | Tính đúng điểm số, làm tròn 1 chữ số thập phân, phân loại `met`/`missing`, xử lý ngưỡng 50% | `MatchCalculatorTest` | `caseA_...`, `caseA2_...`, `caseB_...` | PASS |
| 2 | Ứng viên không có kỹ năng trả điểm 0, missing đầy đủ, belowMinimum=true | `MatchCalculatorTest` | `caseC_candidateNoSkills_scoreZero_allInMissing` | PASS |
| 3 | Bỏ qua điểm tự khai `selfScore`, chỉ tính điểm đã kiểm định `verifiedScore` | `MatchCalculatorTest` | `caseD_onlySelfScore_verifiedScoreNull_treatedAsZero` | PASS |
| 4 | Điểm ứng viên vượt minScore bị chặn ở tỷ lệ 1.0 | `MatchCalculatorTest` | `caseE_scoreExceedsMinScore_cappedAtOne` | PASS |
| 5 | Xử lý biên: JD chỉ có ưu tiên, JD không có kỹ năng, minScore=0, so khớp hoa thường | `MatchCalculatorTest` | `caseF_edgeCases` | PASS |
| 6 | HR công ty A xem JD công ty A -> 200; JD công ty B -> 404 | `MatchControllerIntegrationTest` | `testAccessControl_matrix` | PASS |
| 7 | CANDIDATE gọi endpoint của HR `/match/{userId}` -> 403; HR gọi `/match/me` -> 403 | `MatchControllerIntegrationTest` | `testAccessControl_matrix` | PASS |
| 8 | CANDIDATE xem `/match/me` trên JD APPROVED -> 200; trên JD DRAFT -> 404 | `MatchControllerIntegrationTest` | `testCandidate_matchMe_approvedVsDraft` | PASS |
| 9 | Đổi min_score của JD qua PUT -> điểm mới khác điểm cũ; bảng ghi stale=true ngay sau khi đổi và stale=false sau khi tính lại | `MatchControllerIntegrationTest` | `testStaleFlagAndRecalculation_onSkillChange` | PASS |
| 10 | Ứng viên không có kỹ năng qua REST API trả về HTTP 200, score 0, missing liệt kê đủ | `MatchControllerIntegrationTest` | `testCandidate_noSkills_returnsZeroScoreAndFullMissing` | PASS |
| 11 | Endpoint nội bộ profile-service: có token -> 200 đúng trường, không có điểm -> [] (HTTP 200), không token -> 401 | `InternalSkillScoreApiTest` (profile-service) | `getSkillScores_success_withToken`, `getSkillScores_emptyList_whenNoScores`, `getSkillScores_unauthorized_whenNoToken` | PASS |

---

## 6. Phần Thêm ở profile-service (Bước 0)

Để `job-service` có thể lấy điểm kỹ năng đã kiểm định của ứng viên mà không bị chặn bởi bộ lọc hiển thị cá nhân (`ProfileVisibilityService`), đã bổ sung endpoint nội bộ:
- **Đường dẫn**: `GET /api/v1/profiles/internal/{userId}/skill-scores`
- **Xác thực**: Yêu cầu JWT token hợp lệ qua header `Authorization: Bearer <token>` (phù hợp với `SecurityConfig` hiện hữu của `profile-service`).
- **Dữ liệu trả về**: Mảng `[{ skillId, skillCode, skillName, verifiedScore, selfScore }]` bọc trong envelope chuẩn `{success, statusCode, message, data, timestamp}`. Nếu `userId` chưa có điểm kỹ năng, trả về mảng rỗng `[]` với mã HTTP `200` (không trả `404`).
- **Quy tắc tuân thủ**: Không sửa bất kỳ file nào sẵn có của `profile-service`. Chỉ thêm 3 file mới:
  1. `com/example/profileservice/skill/api/dto/SkillScoreInternalResponse.java`
  2. `com/example/profileservice/skill/api/InternalSkillScoreController.java`
  3. `com/example/profileservice/skill/api/InternalSkillScoreApiTest.java`
- Đã được kiểm thử và thống nhất với nhóm phụ trách hồ sơ (HS).

---

## 7. Hướng dẫn Chạy & Cấu hình Môi trường

### 7.1. Các biến môi trường hỗ trợ
| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `CANDIDATE_SKILLS_STUB` | `false` (prod), `true` (local/test) | `true`: dùng `StubCandidateSkillClient` in-memory; `false`: kết nối REST tới `profile-service` |
| `PROFILE_SERVICE_URL` | `http://localhost:8082` | Địa chỉ gốc của `profile-service` để gọi API lấy điểm kỹ năng nội bộ |

### 7.2. Thử nghiệm nhanh Local với dữ liệu giả lập (Stub)
Khởi động service với biến môi trường:
```bash
set CANDIDATE_SKILLS_STUB=true
set SKILL_CATALOG_STUB=true
./gradlew bootRun
```
Hoặc chạy với profile `local`:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 8. Ảnh chụp màn hình kết quả kiểm thử (Screenshots)

<!-- Dán ảnh chụp màn hình Swagger UI / Postman / kết quả chạy test tại đây -->
_Chỗ dành để dán ảnh màn hình kiểm thử Swagger UI và Postman._

