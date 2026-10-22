# Tiến độ VetClinic

Cập nhật: 22/10/2026. Kết quả kiểm thử gần nhất: 21/10/2026.

## Giai đoạn hiện tại

Đã có bản chạy thử nghiệp vụ backend/frontend. Theo PGNV, các module tuần 6–11 đã có, đang làm phần AI tuần 12 và tích hợp/kiểm thử tuần 13–14. Chưa hoàn tất giai đoạn triển khai thực tế và phản hồi người dùng tuần 15.

## Đã thực hiện và kiểm chứng

- [x] Backend Java/Spring Boot: chủ nuôi, thú cưng, bác sĩ, lịch hẹn và tiếp nhận khám.
- [x] Xác thực JWT và phân quyền ADMIN/VET/RECEPTIONIST; vô hiệu token khi khóa tài khoản, đổi mật khẩu hoặc đăng xuất.
- [x] Hồ sơ khám, triệu chứng, chẩn đoán, điều trị và tái khám.
- [x] Thuốc, kê đơn và trừ tồn kho; danh mục vaccine và lịch sử tiêm.
- [x] Hóa đơn và ghi nhận thanh toán.
- [x] Frontend React/TypeScript kết nối API; build thành công.
- [x] Trợ lý trích hồ sơ theo thú cưng, có mã nguồn và đường dẫn đối chiếu; chặn lễ tân truy cập.
- [x] Tích hợp Ollama embeddings/LLM tùy cấu hình; kiểm thử giao thức với máy chủ giả lập.
- [x] Bản thử cục bộ dùng H2 file riêng, dữ liệu giả lập, tài khoản ngẫu nhiên.
- [x] Kiểm tra giao diện: đăng nhập, dashboard, chọn thú cưng, hỏi chẩn đoán và xem nguồn.

## Bằng chứng kiểm thử

| Nhóm | Kết quả gần nhất |
|---|---|
| Backend | 31/31 đạt, không bỏ qua bài kiểm thử |
| Frontend API | 8/8 đạt |
| TypeScript và production build | Đạt |
| HTTP qua proxy giao diện tới backend thử | 13/13 đạt |
| Đóng gói Spring Boot JAR | Đạt |

Xem [kết quả đã lưu](verification-2026-10-21/). Đây là kết quả trên H2/bản thử cục bộ và mô hình AI giả lập, không chứng minh PostgreSQL hay LLM thật đã được nghiệm thu.

## Còn thiếu

- [ ] Cài/chạy mô hình AI thật và đánh giá chất lượng trả lời, độ đúng nguồn, câu hỏi thiếu bằng chứng và prompt injection.
- [ ] Kiểm thử trên PostgreSQL thực tế; kiểm tra migration và backup/restore. Docker engine đã trả lỗi trong lượt thử trước.
- [ ] Triển khai môi trường thử nghiệm mục tiêu, kiểm tra cấu hình vận hành.
- [ ] Người dùng thực tế thử, ghi nhận phản hồi, sửa lỗi và thử lại. [Biên bản](USER-TRIAL.md) đang chờ thực hiện.
- [ ] Hoàn thiện luận văn với kết quả đo và phản hồi thực tế.

## Lịch sử

### 21/10/2026

Rà mã nguồn nghiệp vụ hiện có; bổ sung trợ lý backend/frontend, phân quyền, chunk nguồn, tích hợp AI tùy chọn, kiểm thử và bản thử H2. Các module nghiệp vụ đã tồn tại trước lượt bổ sung trợ lý; không ghi nhận toàn bộ chúng là công việc viết mới.

### 22/10/2026

Chuẩn bị phiên bản theo dõi trên GitHub: mã nguồn, tài liệu tiến độ và bằng chứng kiểm thử. Loại database, backup, mật khẩu thử nghiệm, file môi trường thực tế và thư mục sinh tự động khỏi bản công bố. Repository khởi đầu từ một bản chụp hiện trạng; không có lịch sử commit trước đó được dựng lại.
