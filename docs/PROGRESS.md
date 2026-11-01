# Tiến độ VetClinic

Cập nhật: 02/11/2026. Backend gần nhất: 31/10/2026 (H2 và PostgreSQL); frontend gần nhất: 02/11/2026.

## Giai đoạn hiện tại

Backend và frontend đã chạy bằng PostgreSQL trên máy người dùng; đã giữ nguyên database hiện có. Theo PGNV, các module tuần 6–11 đã có, đang làm phần AI tuần 12 và tích hợp/kiểm thử tuần 13–14. Chưa hoàn tất giai đoạn triển khai thực tế và phản hồi người dùng tuần 15.

## Đã thực hiện và kiểm chứng

- [x] Backend Java/Spring Boot: chủ nuôi, thú cưng, bác sĩ, lịch hẹn và tiếp nhận khám.
- [x] Xác thực JWT và phân quyền ADMIN/VET/RECEPTIONIST; vô hiệu token khi khóa tài khoản, đổi mật khẩu hoặc đăng xuất.
- [x] Hồ sơ khám, triệu chứng, chẩn đoán, điều trị và tái khám.
- [x] Thuốc, kê đơn và trừ tồn kho; danh mục vaccine và lịch sử tiêm.
- [x] Hóa đơn và ghi nhận thanh toán.
- [x] Frontend React/TypeScript kết nối API; build thành công.
- [x] Trợ lý trích hồ sơ theo thú cưng, có mã nguồn và đường dẫn đối chiếu; chặn lễ tân truy cập.
- [x] Tích hợp Ollama embeddings/LLM tùy cấu hình; kiểm thử giao thức với máy chủ giả lập.
- [x] Bản H2 độc lập ngày 21/10; ứng dụng chính đã chuyển sang PostgreSQL ngày 22/10.
- [x] Kiểm tra giao diện: đăng nhập, dashboard, chọn thú cưng, hỏi chẩn đoán và xem nguồn.

## Bằng chứng kiểm thử

| Nhóm | Kết quả gần nhất |
|---|---|
| Backend | 39/39 đạt trên PostgreSQL và H2, không bỏ qua bài kiểm thử |
| Frontend | 14/14 đạt (8 API client, 6 phân quyền giao diện) |
| TypeScript và production build | Đạt |
| HTTP qua frontend build tới backend PostgreSQL kiểm thử | 13/13 đạt |
| Đóng gói Spring Boot JAR | Đạt |

Xem [kết quả PostgreSQL ngày 22/10](verification-2026-10-22-postgres/) và [kết quả H2 ngày 21/10](verification-2026-10-21/). Đã kiểm tra đồng thời đặt lịch/kê đơn, dữ liệu qua restart và khôi phục backup. Phần AI vẫn dùng mô hình giả lập trong kiểm thử.

## Còn thiếu

- [ ] Cài/chạy mô hình AI thật và đánh giá chất lượng trả lời, độ đúng nguồn, câu hỏi thiếu bằng chứng và prompt injection.
- [x] PostgreSQL thực tế: 31 bài kiểm thử, migration bảo toàn số bản ghi, khôi phục đầy đủ backup vào database riêng.
- [x] Chạy frontend build và backend bằng PostgreSQL cục bộ, xác nhận API khớp dữ liệu database; kiểm tra dữ liệu còn sau restart.
- [ ] Triển khai máy chủ Internet/HTTPS nếu nằm trong phạm vi nghiệm thu.
- [ ] Người dùng thực tế thử, ghi nhận phản hồi, sửa lỗi và thử lại. [Biên bản](USER-TRIAL.md) đang chờ thực hiện.
- [ ] Hoàn thiện luận văn với kết quả đo và phản hồi thực tế.

## Lịch sử

### 21/10/2026

Rà mã nguồn nghiệp vụ hiện có; bổ sung trợ lý backend/frontend, phân quyền, chunk nguồn, tích hợp AI tùy chọn, kiểm thử và bản thử H2. Các module nghiệp vụ đã tồn tại trước lượt bổ sung trợ lý; không ghi nhận toàn bộ chúng là công việc viết mới.

### 22/10/2026

Chuẩn bị phiên bản theo dõi trên GitHub: mã nguồn, tài liệu tiến độ và bằng chứng kiểm thử. Loại database, backup, mật khẩu thử nghiệm, file môi trường thực tế và thư mục sinh tự động khỏi bản công bố. Repository khởi đầu từ một bản chụp hiện trạng; không có lịch sử commit trước đó được dựng lại.


### PostgreSQL — 22/10/2026

Docker đã hoạt động. Đã sao lưu, chạy migration 002/003 và giữ nguyên số bản ghi của 12 bảng gốc; khôi phục backup vào database riêng thành công. Ứng dụng chính dùng PostgreSQL database vetclinic, backend 8080 và frontend build 5173. Dữ liệu kiểm thử HTTP/đồng thời được tách sang database riêng. Sau restart, hồ sơ, tồn kho và hóa đơn đã thanh toán vẫn đúng. Có script khởi động/dừng và hướng dẫn tại [POSTGRES-RUN.md](POSTGRES-RUN.md).


## Hoàn thiện nghiệp vụ đơn thuốc và nhật ký — 22/10/2026

Đã sửa/hủy/kê lại đơn và hoàn kho có kiểm soát đồng thời, thêm nhật ký ADMIN, cải thiện xác nhận thanh toán và phân trang/giao diện hẹp. Đã thử luồng phối hợp lễ tân–bác sĩ–lễ tân và quản trị nhật ký bằng trình duyệt trên database riêng. Xem [phạm vi, kết quả và giới hạn](LIFECYCLE-VALIDATION.md). Chưa có phản hồi người dùng độc lập.


## Hoàn thiện thu tiền và chống ghi đè hóa đơn — 31/10/2026

- Lưu `paidAt` khi xác nhận SENT → PAID; hiển thị trong danh sách, chi tiết và bản in hóa đơn. Xác nhận lại bị từ chối, không thay đổi thời điểm thu.
- Dashboard nhóm doanh thu theo tháng thực thu. Hóa đơn PAID cũ không rõ ngày thu vẫn nằm trong tổng, hiển thị riêng và không tự gán tháng.
- Sửa nháp và chốt hóa đơn dùng cùng khóa lượt khám; đọc lại trạng thái sau khóa để từ chối phiên sửa cũ khi người khác đã chốt.
- Bổ sung ba ca hồi quy: ngày thu/tháng doanh thu, dữ liệu PAID cũ và phiên sửa nháp cũ sau chốt từ giao dịch khác. Tổng 39/39 backend đạt trên H2 và PostgreSQL riêng; frontend build và 8/8 API tests đạt.
- Migration 005 đã thử chạy hai lần trên database riêng, giữ nguyên bản ghi/tiền và để ngày thu lịch sử NULL. Database cục bộ đã được backup trước nâng cấp; số bản ghi 12 bảng gốc không đổi.

Bằng chứng: [PostgreSQL](verification-2026-10-31-postgres.txt), [migration thử riêng](verification-2026-10-31-migration.txt), [nâng cấp cục bộ](verification-2026-10-31-upgrade.txt). Chưa có đánh giá AI thật hoặc phản hồi người dùng độc lập trong lượt này.

Bản mới đã đóng gói và chạy tại http://127.0.0.1:5173 (API 8080), health UP. Đã kiểm tra đăng nhập, dashboard có trường doanh thu thiếu ngày thu và hóa đơn có paidAt qua frontend proxy; 2 chủ nuôi, 2 thú cưng hiện có vẫn giữ nguyên. Xem [kết quả chạy cục bộ](verification-2026-10-31-live.json). Đây là kiểm tra HTTP, chưa thay thế nghiệm thu giao diện/người dùng độc lập.

## Tách giao diện quản trị / bác sĩ / thu ngân — 02/11/2026

Đã tách trang chủ, nhóm menu, lối tắt, nội dung và nút thao tác theo vai trò; thêm chặn trang/form khi mở URL trực tiếp và kiểm tra đường dẫn quay lại sau đăng nhập. Bác sĩ tập trung bệnh án/kê đơn/tiêm chủng, thu ngân tập trung hồ sơ đăng ký/tiếp nhận/hóa đơn, quản trị giữ đầy đủ chức năng. Sửa biểu mẫu thú cưng để đợi danh sách chủ nuôi trước khi khởi tạo lựa chọn; thu ngân lưu thông tin đăng ký vẫn giữ ghi chú chuyên môn đã có.

TypeScript/production build và 14/14 frontend tests đạt. Đã kiểm tra trực tiếp ba tài khoản giả lập trên H2 riêng; bác sĩ lưu bệnh án được, thu ngân không có form khám, các URL ngoài phạm vi bị chặn. Không đổi tài khoản/database vận hành hoặc chính sách API backend. Xem [ma trận và bằng chứng](UI-ROLES.md).
