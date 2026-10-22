# Tiến độ VetClinic

Cập nhật: 22/10/2026. Kết quả kiểm thử gần nhất: 22/10/2026 (PostgreSQL 17.11).

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
| Backend | 31/31 đạt trên PostgreSQL 17.11, không bỏ qua bài kiểm thử |
| Frontend API | 8/8 đạt |
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

