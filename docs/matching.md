# Quy chuẩn Công thức Tính Điểm Khớp Ứng viên với Tin Tuyển Dụng (DN-06)

Tài liệu đặc tả chi tiết thuật toán và công thức tính toán điểm độ phù hợp (match score) giữa kỹ năng đã xác thực của ứng viên với các yêu cầu kỹ năng trong tin tuyển dụng (JD).

> [!IMPORTANT]
> **Quy ước nhóm**: Mọi thay đổi đối với trọng số, công thức hoặc quy tắc biên trong tài liệu này **bắt buộc phải được thảo luận và thống nhất giữa toàn bộ các thành viên dự án** trước khi triển khai code.

---

## 1. Công thức Tính Điểm Chi tiết

### 1.1. Khớp kỹ năng (Skill Matching)
- Kỹ năng ứng viên được so khớp với kỹ năng yêu cầu trong JD theo `skillCode` (không phân biệt hoa thường, ví dụ `java_core` khớp với `JAVA_CORE`).
- Nếu kỹ năng trong JD thiếu `skillCode` (hoặc rỗng), thực hiện so khớp theo `skillId` (UUID định danh kỹ năng).

### 1.2. Điểm số kỹ năng của ứng viên (`candidateScore`)
- `candidateScore` **chỉ lấy từ `verifiedScore`** (điểm đã kiểm định qua bài thi hoặc LMS/Lab).
- Nếu ứng viên không có kỹ năng đó hoặc `verifiedScore` là `null`, coi `candidateScore = 0`.
- **TUYỆT ĐỐI KHÔNG sử dụng `selfScore`** (điểm tự khai) để tính điểm khớp chính thức.

### 1.3. Tỷ lệ đáp ứng từng kỹ năng (`skillRatio`)
Với mỗi kỹ năng trong JD:
- Nếu điểm tối thiểu yêu cầu `minScore == 0`: `skillRatio = 1.0` (mặc định đạt tối đa).
- Nếu `minScore > 0`:
  $$\text{skillRatio} = \min\left(\frac{\text{candidateScore}}{\text{minScore}}, 1.0\right)$$
  *(Điểm vượt quá yêu cầu tối đa chỉ nhận tỷ lệ 1.0, không cộng dồn vượt mức).*

### 1.4. Điểm trung bình nhóm bắt buộc và ưu tiên
- **$TB_{\text{req}}$** (Trung bình nhóm bắt buộc): Trung bình cộng của $\text{skillRatio}$ cho tất cả kỹ năng có `required = true`.
- **$TB_{\text{opt}}$** (Trung bình nhóm ưu tiên): Trung bình cộng của $\text{skillRatio}$ cho tất cả kỹ năng có `required = false`.

### 1.5. Điểm khớp tổng thể (`score`)
$$\text{score} = \begin{cases} 
70 \times TB_{\text{req}} + 30 \times TB_{\text{opt}}, & \text{khi JD có cả kỹ năng bắt buộc và ưu tiên} \\
100 \times TB_{\text{req}}, & \text{khi JD chỉ có kỹ năng bắt buộc} \\
100 \times TB_{\text{opt}}, & \text{khi JD chỉ có kỹ năng ưu tiên} \\
0.0, & \text{khi JD không có kỹ năng nào}
\end{cases}$$

- Điểm số được làm tròn chính xác đến **1 chữ số thập phân** theo quy tắc `HALF_UP` (ví dụ `81.916...` $\rightarrow$ `81.9`, `82.45` $\rightarrow$ `82.5`), sử dụng `BigDecimal` để loại bỏ sai số số thực.

### 1.6. Cờ cảnh báo dưới ngưỡng tối thiểu (`belowMinimum`)
- `belowMinimum = true` nếu tồn tại **ít nhất một** kỹ năng bắt buộc (`required = true`) có `minScore > 0` và:
  $$\text{candidateScore} < 0.5 \times \text{minScore}$$
- **Quy tắc nghiêm ngặt**: Điểm ứng viên đạt **đúng bằng 50%** của `minScore` thì **KHÔNG** bị coi là dưới ngưỡng (tức `belowMinimum = false`).

### 1.7. Phân nhóm kỹ năng đạt (`met`) và thiếu (`missing`)
- **`met`**: Tập hợp các kỹ năng (cả bắt buộc lẫn ưu tiên) có $\text{candidateScore} \ge \text{minScore}$. Thuộc tính `gap = 0`.
- **`missing`**: Tập hợp các kỹ năng chưa đạt yêu cầu ($\text{candidateScore} < \text{minScore}$).
  $$\text{gap} = \text{minScore} - \text{candidateScore}$$
- Nếu ứng viên không có kỹ năng nào, danh sách `missing` sẽ **liệt kê đầy đủ mọi kỹ năng** của JD với khoảng cách thiếu bằng đúng `minScore`.
- Kỹ năng ứng viên sở hữu nhưng JD không yêu cầu: **Bỏ qua**, không cộng điểm và không hiển thị trong danh sách so khớp.
- **Quy tắc sắp xếp danh sách `met` và `missing`**: Ưu tiên kỹ năng bắt buộc (`required = true`) lên trước, kỹ năng ưu tiên (`required = false`) ở sau; nếu cùng nhóm thì sắp xếp theo tên kỹ năng (bảng chữ cái A-Z, không phân biệt hoa thường).

---

## 2. Quy ước Biên & Xử lý Ngoại lệ

| Trường hợp biên | Quy ước xử lý | Kết quả |
|---|---|---|
| `minScore = 0` | Không yêu cầu ngưỡng điểm | `skillRatio = 1.0`, luôn nằm trong `met`, không kích hoạt `belowMinimum` |
| `verifiedScore = null` | Kỹ năng chưa được kiểm định | `candidateScore = 0` |
| Chỉ có `selfScore` | Điểm do ứng viên tự đánh giá | Bị loại bỏ hoàn toàn, coi `candidateScore = 0` |
| JD không có kỹ năng nào | JD trống danh mục kỹ năng | `score = 0.0`, `belowMinimum = false`, `met = []`, `missing = []` |
| Ứng viên vượt điểm (`candidateScore > minScore`) | Điểm cao hơn yêu cầu | $\text{skillRatio} = 1.0$ (bị chặn trên tại 1.0, không cộng điểm vượt trần) |
| Làm tròn số thập phân | Chuẩn làm tròn `RoundingMode.HALF_UP` | Giữ đúng 1 chữ số thập phân (`NUMERIC(5,1)` trong database) |

---

## 3. Các Ví dụ Tính toán Chi tiết (Minh họa)

### Ví dụ A: Đạt điểm bắt buộc ở ngưỡng 50% (Không bị `belowMinimum`)
- **JD yêu cầu**:
  1. `JAVA_CORE`: bắt buộc (`required=true`), `minScore = 70`
  2. `SPRING_BOOT`: bắt buộc (`required=true`), `minScore = 60`
  3. `GIT`: ưu tiên (`required=false`), `minScore = 50`
- **Hồ sơ ứng viên (verified)**:
  - `JAVA_CORE`: 70 $\rightarrow$ ratio = $\min(70/70, 1.0) = 1.0$
  - `SPRING_BOOT`: 30 $\rightarrow$ ratio = $\min(30/60, 1.0) = 0.5$
  - `GIT`: 50 $\rightarrow$ ratio = $\min(50/50, 1.0) = 1.0$
- **Tính toán**:
  - $TB_{\text{req}} = (1.0 + 0.5) / 2 = 0.75$
  - $TB_{\text{opt}} = 1.0 / 1 = 1.0$
  - $\text{score} = 70 \times 0.75 + 30 \times 1.0 = 52.5 + 30.0 = \mathbf{82.5}$
  - Xét `SPRING_BOOT`: `candidateScore = 30` đúng bằng $50\% \times 60 \rightarrow$ `belowMinimum = false`.
  - `met`: `[JAVA_CORE (70/70), GIT (50/50)]`
  - `missing`: `[SPRING_BOOT (30/60, gap = 30)]`

### Ví dụ A2: Dưới ngưỡng 50% ở kỹ năng bắt buộc (`belowMinimum = true`)
- **Dữ liệu**: Giống ví dụ A nhưng `SPRING_BOOT` của ứng viên chỉ đạt **29 điểm**.
- **Tính toán**:
  - $\text{ratio}_{\text{SPRING\_BOOT}} = 29 / 60 = 0.483333...$
  - $TB_{\text{req}} = (1.0 + 0.483333...) / 2 = 0.741666...$
  - $\text{score} = 70 \times 0.741666... + 30 \times 1.0 = 51.9166... + 30.0 = 81.9166... \xrightarrow{\text{round}} \mathbf{81.9}$
  - `SPRING_BOOT`: $29 < 30 (50\%) \rightarrow \mathbf{belowMinimum = true}$.

### Ví dụ B: JD chỉ có kỹ năng bắt buộc, thiếu kỹ năng
- **JD yêu cầu**:
  1. `JAVA_CORE`: bắt buộc, `minScore = 80`
  2. `SQL`: bắt buộc, `minScore = 60`
  3. `REACT`: bắt buộc, `minScore = 50`
- **Hồ sơ ứng viên (verified)**:
  - `JAVA_CORE`: 40 $\rightarrow$ ratio = $40/80 = 0.5$
  - `SQL`: 60 $\rightarrow$ ratio = $60/60 = 1.0$
  - `REACT`: không có $\rightarrow$ `candidateScore = 0`, ratio = $0/50 = 0.0$
- **Tính toán**:
  - $TB_{\text{req}} = (0.5 + 1.0 + 0.0) / 3 = 0.5$
  - JD không có kỹ năng ưu tiên $\rightarrow \text{score} = 100 \times 0.5 = \mathbf{50.0}$
  - `REACT`: $0 < 25 (50\%) \rightarrow \mathbf{belowMinimum = true}$.
  - `met`: `[SQL (60/60)]`
  - `missing`: `[JAVA_CORE (40/80, gap = 40), REACT (0/50, gap = 50)]`

### Ví dụ C: Ứng viên không có kỹ năng nào
- **JD yêu cầu**: `JAVA_CORE` (bắt buộc, 80), `GIT` (ưu tiên, 50).
- **Hồ sơ ứng viên**: Rỗng.
- **Tính toán**:
  - `candidateScore` các kỹ năng đều là 0.
  - $\text{score} = \mathbf{0.0}$.
  - $\mathbf{belowMinimum = true}$ (do $0 < 40$).
  - `met = []`.
  - `missing`: Liệt kê đủ mọi kỹ năng của JD: `JAVA_CORE` (gap 80), `GIT` (gap 50).

---

## 4. Các Vị trí Sử dụng Match Engine (Cross-service Reference)

Logic và kết quả tính toán độ khớp của DN-06 là engine dùng chung cho toàn bộ hệ thống:

1. **DN-03 (Tìm kiếm ứng viên theo tiêu chí và kỹ năng)**: Sử dụng điểm khớp kỹ năng để xếp hạng ứng viên trong danh sách kết quả tìm kiếm của Nhà tuyển dụng.
2. **DN-04 (Xem chi tiết hồ sơ ứng viên và điểm phù hợp)**: Hiển thị thanh tiến trình điểm khớp, bảng danh sách kỹ năng `met` / `missing` và cờ cảnh báo `belowMinimum` cho HR khi xem hồ sơ ứng viên nộp vào JD.
3. **AI-01 (Gợi ý việc làm phù hợp cho ứng viên)**: Dựa trên điểm khớp cao nhất để AI gợi ý các JD phù hợp nhất với năng lực của ứng viên.
4. **DT-01 (Đề xuất lộ trình học tập bổ sung kỹ năng thiếu)**: Lấy danh sách kỹ năng trong `missing` kèm `gap` điểm từ API match để gợi ý các khóa đào tạo LMS tương ứng giúp ứng viên lấp đầy khoảng cách kỹ năng.

> [!NOTE]
> **Quyền riêng tư hồ sơ ứng viên**: Việc phân quyền truy cập hồ sơ ứng viên theo chế độ hiển thị (`PUBLIC`, `RECRUITER`, `PRIVATE`) sẽ được đồng bộ và siết chặt bởi phân hệ **HS-05** trong giai đoạn tiếp theo. Tại DN-06, API `/jobs/{id}/match/{userId}` chỉ phục vụ nội bộ HR, STAFF, ADMIN để xem độ khớp kỹ năng phục vụ quy trình tuyển dụng.

