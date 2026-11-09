# TÀI LIỆU ĐẶC TẢ DỰ ÁN: RIKKEI JOBS

## 1. Tổng quan dự án
**Rikkei Jobs** là nền tảng kết nối ứng viên thuộc hệ sinh thái Rikkei Education với các doanh nghiệp có nhu cầu tuyển dụng, hoạt động dựa trên cốt lõi là **năng lực đã được xác thực**. 

Toàn bộ dữ liệu về điểm môn học, kết quả thi, kết quả thực tập (IOC) và đánh giá từ Mentor đều được đồng bộ trực tiếp từ các hệ thống đào tạo đang vận hành. Bên cạnh việc ứng tuyển, hệ thống còn giúp ứng viên nhận diện những kỹ năng còn thiếu so với vị trí công việc mong muốn và tự động gợi ý lộ trình học bổ sung ngay trên hệ thống quản lý học tập (LMS).

**Hành trình tổng thể của học viên/ứng viên:**
`Khảo thí` $\rightarrow$ `Rikkei LMS` $\rightarrow$ `Giám sát học tập` $\rightarrow$ `Thực tập IOC` $\rightarrow$ **`Việc làm (Rikkei Jobs)`**

---

## 2. Bảy Module chức năng cốt lõi

| STT | Tên Module | Chi tiết chức năng và thành phần |
| :--- | :--- | :--- |
| **1** | **Hồ sơ năng lực ứng viên** | - **Skill Profile:** Hồ sơ kỹ năng đi kèm bằng chứng xác thực từ hệ thống.<br>- **CV chuẩn:** Tạo và quản lý CV theo mẫu tiêu chuẩn của Rikkei.<br>- **Portfolio:** Danh mục các dự án thực tế đã thực hiện. |
| **2** | **Doanh nghiệp & JD** | - Quản lý hồ sơ thông tin doanh nghiệp và tài khoản nhân sự (HR).<br>- Đăng tải và quản lý nhu cầu tuyển dụng (JD) theo từng vị trí cùng danh sách kỹ năng, mức độ yêu cầu tương ứng. |
| **3** | **Thực tập (IOC)** | - Doanh nghiệp đặt hàng nhu cầu tuyển thực tập sinh.<br>- Ghép nối ứng viên phù hợp vào các đợt thực tập IOC.<br>- Đồng bộ kết quả nghiệm thu thực tập trực tiếp vào hồ sơ năng lực ứng viên. |
| **4** | **Đánh giá năng lực** | - Hệ thống phiếu đánh giá năng lực dành cho Mentor.<br>- Chức năng tự đánh giá (Self-assessment) dành cho ứng viên.<br>- Tính toán chỉ số năng lực tổng hợp và cấp phát huy hiệu (Badges). |
| **5** | **Đào tạo bổ sung** | - Phân tích và chỉ ra khoảng thiếu hụt năng lực (Skill Gap) của ứng viên so với JD hoặc vị trí mục tiêu.<br>- Đề xuất lộ trình các môn học, bài học bổ sung tương ứng trên LMS. |
| **6** | **Ứng tuyển & Tuyển dụng** | - Quản lý toàn bộ quy trình tuyển dụng khép kín: Ứng tuyển bằng CV $\rightarrow$ Sàng lọc hồ sơ $\rightarrow$ Tổ chức phỏng vấn $\rightarrow$ Gửi thư mời nhận việc (Offer) $\rightarrow$ Xác nhận nhận việc (Onboard). |
| **7** | **AI Career Assistant** | - Trợ lý ảo thông minh hỗ trợ gợi ý vị trí công việc phù hợp với năng lực.<br>- Tư vấn, góp ý và hỗ trợ viết/chỉnh sửa CV.<br>- Hỗ trợ luyện tập trả lời phỏng vấn giả định bám sát theo nội dung từng JD. |

---

## 3. Vai trò người dùng, Xác thực và Phân quyền

### 3.1. Phương thức đăng nhập & Xác thực (Authentication)
*   **Nhóm nội bộ (Ứng viên, Mentor, Cán bộ tuyển dụng, Quản trị):** 
    *   Sử dụng tài khoản LMS sẵn có.
    *   Đăng nhập thông qua cơ chế Single Sign-On (SSO) từ các phân hệ `lms-student`, `lms-admin`, `lms-pm` bằng **vé dùng một lần (One-time ticket)** có thời hạn hiệu lực tối đa **60 giây**.
*   **Nhóm Doanh nghiệp (HR):** 
    *   Sử dụng tài khoản riêng do Cán bộ hệ thống cấp phát.
    *   **Quy tắc bảo mật tài khoản HR:** Bắt buộc đổi mật khẩu trong lần đăng nhập đầu tiên. Nhập sai mật khẩu 5 lần liên tiếp sẽ bị khóa tài khoản trong 15 phút. Tính năng quên mật khẩu gửi đường dẫn khôi phục qua email (đường dẫn chỉ dùng 1 lần và hết hạn sau 30 phút).

### 3.2. Ma trận phân quyền chi tiết (Authorization)
*Lưu ý thiết kế hệ thống: Mọi quyền hạn bắt buộc phải được kiểm tra và chặn lọc trực tiếp ở tầng API (Backend), tuyệt đối không chỉ ẩn nút bấm trên giao diện người dùng (Frontend). Mỗi vai trò cần có kịch bản kiểm thử phân quyền riêng.*

| Nghiệp vụ / Chức năng | Ứng viên | Doanh nghiệp (HR) | Mentor | Cán bộ tuyển dụng | Quản trị (Admin) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Sửa hồ sơ, CV, Portfolio** | Của mình | — | — | — | — |
| **Đồng ý / Rút đồng ý công khai hồ sơ** | Có | — | — | — | — |
| **Duyệt hồ sơ, sửa phần thông tin tự khai** | — | — | — | Có | Có |
| **Đăng mới, chỉnh sửa JD** | — | Của mình | — | Có | Có |
| **Phê duyệt JD** | — | — | — | Có | Có |
| **Tìm kiếm ứng viên đã công khai** | — | Có | — | Có | Có |
| **Ứng tuyển, rút đơn ứng tuyển** | Có | — | — | — | — |
| **Quản lý phỏng vấn, gửi Offer** | — | JD của mình | Ghi kết quả vòng được mời | Xem, hỗ trợ | Xem |
| **Xem Email, SĐT của ứng viên** | Của mình | Khi ứng viên nộp vào JD của mình hoặc được Cán bộ mở khóa | — | Có | Có |
| **Đặt nhu cầu tuyển thực tập (IOC)** | — | Có | — | Có | Có |
| **Ghép ứng viên vào đợt thực tập IOC** | — | — | — | Có | Có |
| **Phiếu đánh giá năng lực** | Tự đánh giá | — | Đánh giá ứng viên được giao | Xem | Xem, sửa (kèm lý do) |
| **Xem khoảng thiếu hụt & lộ trình học bổ sung** | Của mình | — | Của thực tập sinh mình phụ trách | Xem | Xem |
| **Sử dụng AI Career Assistant** | Có | — | — | — | Cấu hình hệ thống |
| **Quản lý danh mục kỹ năng, trọng số, mẫu phiếu** | — | — | — | — | Có |
| **Nhật ký thao tác hệ thống (Audit Log)** | — | — | — | Xem | Có |

---

## 4. Ba luồng nghiệp vụ chính & Chu trình trạng thái

### 4.1. Luồng 1: Tuyển dụng (Recruitment Flow)
1.  Doanh nghiệp tạo và đăng tải JD; Cán bộ tuyển dụng kiểm duyệt.
2.  Hệ thống tự động tính toán **Điểm khớp (Matching Score)**, từ đó gợi ý ứng viên tiềm năng cho doanh nghiệp và gợi ý JD phù hợp cho ứng viên.
3.  Ứng viên chủ động nộp CV ứng tuyển, hoặc doanh nghiệp chủ động gửi lời mời ứng tuyển.
4.  Thực hiện quy trình: Sàng lọc $\rightarrow$ Phỏng vấn (quản lý lịch, tự động tạo phòng họp trực tuyến, ghi nhận kết quả từng vòng) $\rightarrow$ Gửi Offer $\rightarrow$ Nhận việc.

### 4.2. Luồng 2: Thực tập IOC (Internship On Campus Flow)
1.  Doanh nghiệp đăng ký nhu cầu tuyển thực tập sinh thông qua việc tạo JD loại "Thực tập".
2.  Cán bộ thực hiện ghép nối ứng viên vào các đợt thực tập IOC trên hệ thống quản lý dự án (`lms-pm`).
3.  Trong và sau quá trình thực tập, dữ liệu về các công việc (task) đã nghiệm thu, điểm số thực tập và phiếu đánh giá của Mentor sẽ được đẩy về hồ sơ ứng viên.
4.  Doanh nghiệp xem xét kết quả và gửi Offer chính thức cho những thực tập sinh đạt yêu cầu.

### 4.3. Luồng 3: Phát triển năng lực & Đào tạo bổ sung (Upskilling Flow)
1.  Hệ thống tự động dựng **Skill Profile** của ứng viên từ dữ liệu LMS, Khảo thí, Thực tập IOC và đánh giá của Mentor.
2.  Ứng viên lựa chọn một vị trí công việc mục tiêu hoặc một JD cụ thể; hệ thống lập tức phân tích và chỉ ra danh sách các kỹ năng còn thiếu hoặc chưa đạt chuẩn.
3.  Hệ thống gợi ý các môn học, bài học bổ sung tương ứng trên LMS. Khi ứng viên ghi danh và hoàn thành bài học, chỉ số năng lực sẽ tự động được tính toán và cập nhật lại.
4.  Khi chỉ số năng lực đạt đến mức yêu cầu của vị trí/JD, hệ thống thông báo và gợi ý ứng viên nộp đơn ứng tuyển.

---

### 4.4. Chi tiết các chu trình trạng thái (State Machines)

*   **Trạng thái Hồ sơ năng lực:**
    `Riêng tư` (Mặc định) $\rightarrow$ `Chờ duyệt` $\rightarrow$ `Đã duyệt (Công khai)` | `Trả lại (bắt buộc kèm lý do)` | `Rút công khai` (trở về Riêng tư).
*   **Trạng thái JD (Tin tuyển dụng):**
    `Nháp` $\rightarrow$ `Chờ duyệt` $\rightarrow$ `Đang tuyển` $\rightarrow$ `Tạm dừng` | `Đã đóng` (do hết hạn thời gian hoặc đã tuyển đủ số lượng).
*   **Trạng thái Đơn ứng tuyển:**
    *   *Luồng chính:* `Đã gửi` $\rightarrow$ `Sàng lọc` $\rightarrow$ `Phỏng vấn` (hỗ trợ cấu hình nhiều vòng) $\rightarrow$ `Offer` $\rightarrow$ `Nhận việc`.
    *   *Luồng kết thúc rẽ nhánh:* `Không đạt` (bắt buộc ghi rõ rớt ở vòng nào, lý do là gì) | `Ứng viên rút` | `Từ chối Offer`.
    *   *Quy tắc vận hành trong luồng ứng tuyển:*
        *   **Thông báo:** Mỗi lần đơn ứng tuyển chuyển trạng thái, hệ thống tự động gửi thông báo (và email) cho cả ứng viên lẫn doanh nghiệp.
        *   **Lịch phỏng vấn:** Doanh nghiệp đề xuất các khung giờ trống $\rightarrow$ Ứng viên chọn khung giờ phù hợp $\rightarrow$ Hệ thống tự động tạo phòng họp trực tuyến (Rikkei Meet) và gửi thông báo nhắc lịch trước **1 ngày** và trước **1 giờ**.
        *   **Quản lý Offer:** Bao gồm thông tin vị trí, ngày bắt đầu đi làm, thời hạn phản hồi. Nếu quá hạn mà ứng viên không phản hồi, Offer tự động chuyển sang trạng thái đóng.
*   **Trạng thái Thực tập IOC:**
    `Nhu cầu thực tập (JD loại Thực tập)` $\rightarrow$ `Ghép ứng viên vào đợt IOC` $\rightarrow$ `Đang thực tập` $\rightarrow$ `Đã nghiệm thu` $\rightarrow$ `Mentor đánh giá` $\rightarrow$ `Doanh nghiệp Offer` hoặc `Kết thúc`.
*   **Trạng thái Phiếu đánh giá năng lực:**
    `Phiếu được giao` $\rightarrow$ `Nháp` $\rightarrow$ `Đã gửi`. *(Lưu ý: Phiếu một khi đã chuyển sang trạng thái "Đã gửi" thì Mentor không thể sửa; chỉ có Quản trị viên mới có quyền chỉnh sửa và bắt buộc phải nhập lý do)*.
*   **Trạng thái Đào tạo bổ sung:**
    `Chọn vị trí mục tiêu / JD` $\rightarrow$ `Danh sách kỹ năng thiếu` $\rightarrow$ `Ghi danh môn / bài học trên LMS` $\rightarrow$ `Học xong` $\rightarrow$ `Chỉ số tự cập nhật` $\rightarrow$ `Đủ mức yêu cầu (Gợi ý ứng tuyển)`.

---

## 5. Logic Dữ liệu: Bằng chứng, Chỉ số năng lực & Điểm khớp

### 5.1. Nguồn dữ liệu bằng chứng và điều kiện xác thực

| Nguồn dữ liệu | Chi tiết dữ liệu thu thập | Điều kiện để coi là "Đã xác thực" |
| :--- | :--- | :--- |
| **Rikkei LMS** | Kết quả môn học, điểm chuyên cần, bài tập về nhà (BTVN), điểm R-point. | Điểm môn học đã được chốt trên hệ thống. |
| **Khảo thí** | Kết quả điểm của các ca thi theo từng môn học. | Ca thi đã có kết quả điểm chính thức. |
| **Thực tập IOC (`lms-pm`)** | Đợt thực tập, nhóm, đề tài, danh sách task nghiệm thu, điểm nghiệm thu. | Mentor phụ trách đã xác nhận nghiệm thu. |
| **Mentor** | Phiếu đánh giá năng lực chi tiết theo khung kỹ năng chuẩn. | Mentor đã hoàn thành và bấm gửi phiếu. |
| **Cán bộ** | Bổ sung các bằng chứng bị thiếu ở nguồn, hoặc điều chỉnh giá trị bằng tay. | Bắt buộc phải ghi rõ lý do điều chỉnh và lưu vết vào lịch sử. |
| **Ứng viên tự khai** | Kỹ năng tự đánh giá mức độ, các dự án cá nhân bên ngoài, chứng chỉ ngoại khóa. | **Không xác thực** — Luôn gắn nhãn hiển thị rõ là dữ liệu `"Tự khai"`. |

**Cơ chế đồng bộ và xử lý sai sót bằng chứng:**
*   Hệ thống thực hiện đồng bộ dữ liệu tự động định kỳ **mỗi đêm** và đồng bộ tức thời **khi ứng viên mở xem hồ sơ**.
*   **Quy tắc bất biến khi đồng bộ:** Tiến trình đồng bộ tự động tuyệt đối **không được ghi đè** lên các dữ liệu/bằng chứng đã được Cán bộ/Quản trị chỉnh sửa bằng tay trước đó.
*   Ứng viên có nút chức năng **"Báo sai"** đối với từng bằng chứng cụ thể. Khi bấm báo sai, yêu cầu sẽ được đẩy vào hàng chờ xử lý của Quản trị viên.

### 5.2. Công thức tính Chỉ số năng lực & Huy hiệu (Skill Score & Badges)
*   **Thang đo và phân bậc:** Mỗi kỹ năng của ứng viên được lượng hóa bằng một chỉ số từ $0$ đến $100$, đi kèm với phân bậc tương ứng: **Cơ bản**, **Khá**, **Vững**, **Nâng cao**.
*   **Công thức tính toán:** Chỉ số của một kỹ năng là **trung bình cộng có trọng số** từ các nguồn bằng chứng đã xác thực:
    $$\text{Chỉ số kỹ năng} = \frac{\sum (\text{Điểm nguồn}_i \times \text{Trọng số}_i)}{\sum \text{Trọng số}_i}$$
    *(Trọng số của từng nguồn dữ liệu do Quản trị viên cấu hình linh hoạt trên hệ thống).*
*   **Quy tắc đối với dữ liệu Tự khai:** Điểm ứng viên tự đánh giá/tự khai **tuyệt đối không được cộng vào công thức tính chỉ số năng lực chính thức**, mà chỉ được hiển thị thành một vạch/biểu đồ riêng biệt bên cạnh để làm dữ liệu tham chiếu, so sánh.
*   **Cơ chế cấp Huy hiệu (Badges):** Hệ thống tự động cấp huy hiệu kỹ năng dựa trên bộ quy tắc điều kiện kép (Ví dụ: Chỉ số kỹ năng đạt từ bậc *Vững* trở lên **VÀ** có ít nhất $1$ bằng chứng thực tế từ *Thực tập IOC*). Quản trị viên có quyền cấp phát thủ công hoặc thu hồi huy hiệu (bắt buộc kèm lý do).

### 5.3. Thuật toán Điểm khớp với JD (Matching Score) & Khoảng thiếu hụt (Skill Gap)
*   **Cấu trúc yêu cầu của JD:** Khi tạo JD, doanh nghiệp khai báo danh sách các **Kỹ năng bắt buộc** và **Kỹ năng ưu tiên**, mỗi kỹ năng đều gắn với một mức điểm/bậc tối thiểu cần đạt.
*   **Điểm khớp (Matching Score):** Được tính trên thang điểm từ $0$ đến $100$ dựa trên độ tương thích giữa Skill Profile của ứng viên và yêu cầu của JD. 
    *   *Quy tắc hiển thị minh bạch:* Nếu ứng viên bị thiếu bất kỳ một **kỹ năng bắt buộc** nào của JD, hệ thống vẫn hiển thị ứng viên nhưng phải **ghi chú cảnh báo rõ ràng kỹ năng đang thiếu**, không tự ý ẩn ứng viên đi.
*   **Xác định Khoảng thiếu hụt (Skill Gap):** Hệ thống đem so sánh chỉ số hiện tại của từng kỹ năng ứng viên có với mức yêu cầu tối thiểu của JD (hoặc vị trí mục tiêu). Mỗi kỹ năng bị thiếu hụt (chỉ số ứng viên $<$ mức yêu cầu) sẽ được ánh xạ (mapping) trực tiếp sang các môn học hoặc bài học cụ thể trên LMS để ứng viên học bù.

---

## 6. Yêu cầu tích hợp hệ sinh thái (Ecosystem Integrations)

Hệ thống Rikkei Jobs đóng vai trò là trung tâm kết nối, yêu cầu giao tiếp dữ liệu chặt chẽ với các dịch vụ nội bộ và bên ngoài sau:

1.  **Hệ thống Xác thực (`auth-service`):** Xác thực vé SSO dùng một lần (hiệu lực 60 giây) chuyển hướng từ các cổng `lms-student`, `lms-admin`, `lms-pm`.
2.  **Cổng thông tin LMS (`lms-portal-api`):** Kết nối qua API nội bộ (sử dụng khóa dịch vụ / Service Key) để:
    *   Lấy dữ liệu điểm môn học, chuyên cần, BTVN, R-point.
    *   Truy xuất danh mục môn học, bài học gắn với từng kỹ năng; thực hiện ghi danh học bổ sung và theo dõi tiến độ hoàn thành khóa học của ứng viên.
3.  **Hệ thống Khảo thí (`re-khaothi-api`):** Kết nối qua API nội bộ (khóa dịch vụ) để lấy kết quả điểm chính thức của các ca thi.
4.  **Hệ thống Quản lý Thực tập (`lms-pm` / `pm-service`):** Đồng bộ thông tin đợt thực tập, nhóm, đề tài, các task đã nghiệm thu và điểm số; đồng thời cung cấp lối vào để Mentor thực hiện chấm phiếu đánh giá năng lực.
5.  **Hệ thống Phòng họp trực tuyến (`Rikkei Meet`):** Tự động khởi tạo phòng họp trực tuyến và tạo đường dẫn tham gia ngay khi lịch phỏng vấn được ứng viên và doanh nghiệp xác nhận.
6.  **Mô hình Trí tuệ nhân tạo (AI Service):** Tích hợp vào module *AI Career Assistant* để xử lý ngôn ngữ tự nhiên cho việc gợi ý việc làm, đánh giá CV và đóng vai người phỏng vấn.
7.  **Dịch vụ Email thông báo:** Gửi email tự động cho các sự kiện quan trọng: thông báo có đơn ứng tuyển mới, xác nhận và nhắc lịch phỏng vấn, gửi thông báo Offer, khôi phục mật khẩu HR.

**Luồng chảy dữ liệu tổng thể (Data Pipeline):**
*   **Luồng 1 (Hồ sơ $\rightarrow$ Tuyển dụng):** Dữ liệu từ (`LMS` + `Khảo thí` + `lms-pm` + `Mentor`) $\rightarrow$ Hình thành `Bằng chứng` $\rightarrow$ Tính toán `Chỉ số năng lực & Huy hiệu` $\rightarrow$ Tạo dựng `Skill Profile` $\rightarrow$ (Ứng viên đồng ý công khai + Cán bộ duyệt) $\rightarrow$ Hiển thị cho `Doanh nghiệp`, tính `Điểm khớp với JD` $\rightarrow$ `Ứng tuyển` $\rightarrow$ `Offer`.
*   **Luồng 2 (Đào tạo bổ sung):** Kết hợp (`Chỉ số năng lực hiện tại` + `Yêu cầu từ JD`) $\rightarrow$ Tính ra `Khoảng thiếu hụt` $\rightarrow$ Gợi ý & học `Môn bổ sung trên LMS` $\rightarrow$ Sinh ra `Bằng chứng mới` (quay lại cập nhật Chỉ số năng lực).

---

## 7. Quyền riêng tư, Bảo mật và An toàn dữ liệu

Đây là các quy tắc bắt buộc phải tuân thủ nghiêm ngặt trong quá trình thiết kế cơ sở dữ liệu và xây dựng logic hệ thống:

1.  **Cơ chế hiển thị hồ sơ (Opt-in & Approval):** 
    *   Mọi hồ sơ ứng viên mặc định ở trạng thái **Riêng tư**. 
    *   Hồ sơ chỉ xuất hiện trên trang tìm kiếm của Doanh nghiệp khi thỏa mãn đồng thời 2 điều kiện: **Ứng viên chủ động bấm đồng ý công khai** VÀ **Cán bộ tuyển dụng đã phê duyệt**.
2.  **Cơ chế thu hồi quyền công khai (Revoke Consent):**
    *   Ứng viên có quyền rút lại sự đồng ý công khai bất cứ lúc nào. Ngay khi rút, hồ sơ phải **biến mất khỏi kết quả tìm kiếm của doanh nghiệp ngay lập tức**.
    *   *Ngoại lệ:* Đối với các đơn ứng tuyển mà ứng viên đã chủ động nộp trước thời điểm rút công khai, các doanh nghiệp nhận đơn đó vẫn được quyền xem hồ sơ gắn với đơn ứng tuyển đó.
3.  **Bảo vệ thông tin liên hệ cá nhân (PII Masking):**
    *   Doanh nghiệp **tuyệt đối không nhìn thấy Email và Số điện thoại** của ứng viên khi duyệt tìm kiếm hồ sơ công khai.
    *   Thông tin liên hệ chỉ được mở khóa cho một doanh nghiệp cụ thể khi: **(1) Ứng viên chủ động nộp đơn ứng tuyển vào JD của doanh nghiệp đó**, HOẶC **(2) Cán bộ tuyển dụng thực hiện thao tác duyệt mở liên hệ cho doanh nghiệp đó**. 
    *   Mọi hành động mở khóa thông tin liên hệ đều phải được ghi vào nhật ký hệ thống (Audit Log).
4.  **Quyền riêng tư đối với Phiếu đánh giá của Mentor:**
    *   Ứng viên luôn xem được điểm số và lời nhận xét của Mentor dành cho mình.
    *   Doanh nghiệp chỉ được phép xem nội dung phiếu đánh giá này khi hồ sơ của ứng viên đã ở trạng thái Công khai.
5.  **Quyền được xóa dữ liệu (Right to Erasure):**
    *   Khi ứng viên gửi yêu cầu xóa dữ liệu cá nhân: Hệ thống thực hiện xóa Hồ sơ năng lực, CV và Portfolio của ứng viên đó; chỉ giữ lại các bản ghi nhật ký tối thiểu theo đúng quy định pháp lý và vận hành.
6.  **Bảo mật dữ liệu khi giao tiếp với AI (AI Privacy & Rate Limiting):**
    *   Trước khi gửi bất kỳ dữ liệu nào của ứng viên sang mô hình AI bên ngoài (AI Career Assistant), hệ thống **bắt buộc phải che/ẩn (masking/redaction) toàn bộ thông tin định danh cá nhân** bao gồm: Họ tên thật (nếu cần), Email, Số điện thoại, Mã sinh viên/học viên.
    *   AI chỉ được phép sử dụng dữ liệu trong phạm vi hồ sơ của chính ứng viên đang tương tác, tuyệt đối không truy xuất chéo dữ liệu của ứng viên khác.
    *   Áp dụng giới hạn số lượt sử dụng AI mỗi ngày theo từng tài khoản (Rate limiting).
    *   Lưu nhật ký tương tác AI và tự động xóa sau **90 ngày**.
7.  **An toàn tệp tin tải lên (File Upload Security):**
    *   **Định dạng và dung lượng:** Chứng chỉ đính kèm chỉ chấp nhận định dạng `PDF` hoặc ảnh có dung lượng $\le 5\text{MB}$; Ảnh đại diện (Avatar) dung lượng $\le 2\text{MB}$.
    *   **Chặn mã độc:** Cấm tuyệt đối việc tải lên các tệp định dạng `.SVG` và `.HTML` để phòng chống tấn công XSS.
    *   **Truy cập tệp:** Đường dẫn (URL) truy cập vào các tệp tài liệu nhạy cảm phải được bảo vệ bằng mã xác thực có thời hạn (Signed URL / Token).
8.  **Tính toàn vẹn của Nhật ký hệ thống (Immutable Audit Logs):**
    *   Toàn bộ lịch sử chỉnh sửa bằng chứng, lịch sử thay đổi chỉ số năng lực, lịch sử mở khóa liên hệ và lịch sử chuyển đổi trạng thái ứng tuyển là **bất biến (Append-only)** — không một ai (kể cả Quản trị viên cấp cao nhất) có quyền chỉnh sửa hoặc xóa các bản ghi lịch sử này.