# VetClinic — phòng khám thú y

Theo dõi: [Tiến độ và công việc còn thiếu](docs/PROGRESS.md).


Ứng dụng tiếng Việt phát triển tiếp trên backend Spring Boot/Java 21 và frontend React/TypeScript strict/Vite hiện có. Dự án `../veterinary-clinic-management-system` là tham chiếu phạm vi nghiệp vụ; không sao chép hoặc thay thế database mẫu.

## Khởi động hằng ngày

1. Mở Docker Desktop.
2. PowerShell thứ nhất:

```powershell
cd C:\Users\bachn\vetclinic\vetclinic
docker compose up -d
.\mvnw.cmd spring-boot:run
```

3. PowerShell thứ hai:

```powershell
cd C:\Users\bachn\vetclinic\frontend
npm.cmd ci
npm.cmd run dev
```

`npm ci` chỉ cần khi mới cài hoặc package-lock thay đổi. Mở http://localhost:5173. Giữ cả hai terminal mở. Ctrl+C để dừng. `docker compose stop` giữ nguyên dữ liệu.

## Nâng cấp database hiện có (một lần)

Chưa tự thực thi migration trên PostgreSQL đang dùng. Dừng backend trước. Sao lưu database bằng pgAdmin hoặc `pg_dump`. Hai migration `002_staff_workspace.sql` và `003_report_alignment.sql` trong `vetclinic/docs/migrations/`: thêm vai trò, hỗ trợ/tri thức, danh mục vaccine, thu hồi JWT và trường hồ sơ. Không xóa tài khoản hoặc hồ sơ.

Cách khuyến nghị: tại thư mục backend chạy `.\scripts\upgrade-db.ps1`. Script tạo backup custom-format vào `backups/`, kiểm tra archive bằng pg_restore --list, lưu SHA256, áp dụng migration và đối chiếu số lượng dữ liệu trong 12 bảng gốc. Script đã kiểm tra cú pháp; chưa chạy thực tế vì sandbox không truy cập được Docker named pipe. Chưa kiểm thử restore toàn bộ backup.

Cách thủ công thay thế (sau khi tự backup): mở SQL trong pgAdmin Query Tool vào database vetclinic, hoặc PowerShell tại thư mục backend:

```powershell
Get-Content -Raw .\docs\migrations\002_staff_workspace.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d vetclinic
Get-Content -Raw .\docs\migrations\003_report_alignment.sql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U postgres -d vetclinic
```

Sau đó chạy lại backend và đăng nhập lại (JWT cũ không có credential stamp sẽ hết hiệu lực). Hibernate local vẫn dùng ddl-auto=update, chưa phải hệ thống Flyway tự quản lý phiên bản. Migration trên là SQL có kiểm soát dành cho database đang có; chưa kiểm chứng trên PostgreSQL thật của người dùng.

## Các màn hình

- Tổng quan: lịch hôm nay, chủ nuôi/thú cưng, hóa đơn chưa trả, doanh thu, tải bác sĩ, cảnh báo tiêm/tái khám/tồn kho.
- Chủ nuôi/thú cưng: xem/tạo/sửa/lưu trữ/kích hoạt; lịch sử khám/tiêm/cân nặng trên hồ sơ thú cưng.
- Bác sĩ: thông tin, chuyên môn, giấy phép, lịch làm việc; ADMIN quản lý hoạt động.
- Lịch hẹn: danh sách theo ngày và bộ lọc ngày/bác sĩ/trạng thái; tạo/đổi lịch, tiếp nhận, khám, hoàn thành/hủy.
- Hồ sơ khám: tiếp nhận trực tiếp, triệu chứng, chẩn đoán, điều trị, hẹn tái khám, kê đơn khi đang khám; lịch sử đơn thuốc và lọc bệnh án theo ngày/nội dung. Cảnh báo dị ứng từ hồ sơ và dò chuỗi chất dị ứng trong ghi chú điều trị; không phải cơ sở dữ liệu kiểm tra tương tác thuốc y khoa.
- Thuốc: danh mục, hoạt chất, giá, tồn kho, ngưỡng cảnh báo; kê đơn trừ kho có khóa DB.
- Tiêm chủng: danh mục vaccine dùng chung, ghi chú, thêm/sửa/xóa theo quyền. Ngày tiêm tiếp theo do người có chuyên môn nhập; không mặc định lịch tiêm thực tế từ tên vaccine.
- Hóa đơn: nhiều dòng, VAT, sửa nháp, chốt sau hoàn thành khám, đánh dấu đã nhận tiền, in/lưu PDF bằng trình duyệt.
- Tài khoản: ADMIN cấp tài khoản ADMIN/VET/RECEPTIONIST, khóa/mở, đặt lại mật khẩu. Không đăng ký công khai.
- Tìm kiếm chủ nuôi/thú cưng/lượt khám; hỗ trợ nội bộ; kho tài liệu có nguồn cho giai đoạn AI/RAG.

## Quyền

| Nhóm | ADMIN | VET | RECEPTIONIST |
|---|---|---|---|
| Xem hồ sơ, lịch, danh mục, hóa đơn | Có | Có | Có |
| Tạo/sửa chủ nuôi, thú cưng, lịch hẹn | Có | Có | Có |
| Lưu trữ chủ nuôi | Có | Có | Không |
| Lưu trữ/kích hoạt thú cưng, ghi cân nặng | Có | Có | Có |
| Chuyển trạng thái lịch | Có | Có | Có |
| Ghi hồ sơ, tiêm chủng, kê đơn, tái khám | Có | Có | Không |
| Xóa bản ghi tiêm | Có | Không | Không |
| Thuốc, bác sĩ, tài khoản, tài liệu kiến thức | Quản lý | Xem | Xem (không xem tài khoản) |
| Tạo/sửa/chốt/thanh toán hóa đơn | Có | Xem | Có |
| Hỗ trợ | Tất cả | Yêu cầu của mình | Yêu cầu của mình |

Security backend là nguồn quyết định quyền. Token HS256 có hạn, kiểm tra issuer và tài khoản hiện tại ở mỗi request, đổi mật khẩu/khóa tài khoản làm token mất hiệu lực. Logout thu hồi token trên server rồi xóa phiên ở tab; chưa có refresh token. VET mới được liên kết bác sĩ, ADMIN chỉnh được tên/email/vai trò; tài khoản VET cũ cần ADMIN chọn hồ sơ bác sĩ phù hợp. Login giới hạn 20 request/phút/IP theo bộ nhớ từng tiến trình; triển khai nhiều instance cần limiter tập trung.

## Kiểm tra

```powershell
# backend, chỉ H2 in-memory
.\mvnw.cmd test
# frontend
npm.cmd run build
npm.cmd test
```

Test nghiệp vụ dùng H2 riêng, không kết nối PostgreSQL: quyền từng vai trò, luồng khám/kê đơn/thanh toán, chặn trùng lịch, khóa/đổi mật khẩu vô hiệu token, cân nặng lịch sử, riêng tư hỗ trợ và khôi phục thú cưng. Mockito dùng Java agent qua Surefire để tương thích sandbox Windows.

## Giới hạn cần biết

- Giao diện được viết lại bằng tiếng Việt; không phải bản sao pixel của mẫu. Có lịch tháng, chưa có kéo-thả đổi lịch và lưới tuần theo giờ.
- Chủ nuôi/thú cưng có tìm kiếm/phân trang backend. Danh sách các màn hình mở rộng phân trang phía client; API cũ vẫn trả arrays. Dashboard/search hiện tổng hợp trong bộ nhớ, cần chuyển sang truy vấn aggregate/page khi dữ liệu lớn.
- Doanh thu nhóm theo tháng lập hóa đơn đã thanh toán, chưa lưu giao dịch/thời điểm thu tiền riêng. Đánh dấu PAID không phải cổng thanh toán.
- Chưa có SMTP/SMS, file đính kèm, kiểm tra tương tác thuốc, refresh token, chỉnh/hủy đơn thuốc và hoàn tồn kho.
- Đã có trợ lý bệnh án: trích hồ sơ có nguồn và tích hợp Ollama embeddings/LLM tùy cấu hình. Chưa kiểm chứng chất lượng mô hình thật hoặc dùng pgvector. Xem `docs/AI-RAG.md`.
- Trước production: migration tự động được kiểm chứng, kiểm thử PostgreSQL, backup/restore, HTTPS/reverse proxy, secret riêng và giám sát. Không đưa secret backend vào biến VITE.

Hướng dẫn nghiệm thu và vận hành chi tiết: [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md). Chưa có kết quả nghiệm thu người dùng hoặc triển khai production.

## Bổ sung trợ lý và chạy thử

ADMIN/VET truy cập mục Trợ lý bệnh án. Chế độ trích hồ sơ chạy không cần mô hình. Chế độ AI cần Ollama và cấu hình trong docs/AI-RAG.md.

Bản thử nghiệm riêng: đóng gói backend bằng `./mvnw.cmd package`, cài frontend bằng `npm.cmd ci`, rồi từ thư mục gốc chạy `./start-trial.ps1`. Giao diện cổng 15173, API cổng 18080, chỉ nghe loopback. Dữ liệu H2 file nằm trong vetclinic/demo-data, tách khỏi PostgreSQL hiện có. Tài khoản ngẫu nhiên nằm trong .trial/credentials.json; không commit file này. Biên bản phản hồi tại docs/USER-TRIAL.md đang chờ người dùng thực tế.

