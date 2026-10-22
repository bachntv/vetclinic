# Chạy VetClinic với PostgreSQL

## Trạng thái đã xác nhận ngày 22/10/2026

- Backend Spring Boot chạy profile `postgres`, kết nối PostgreSQL 17.11, database `vetclinic`.
- Frontend được build thành tài nguyên tĩnh và phục vụ bằng Vite preview; `/api` chuyển tới backend.
- Giao diện: http://127.0.0.1:5173. Backend: http://127.0.0.1:8080.
- Database hiện có được giữ nguyên. Các tài khoản cũ vẫn giữ mật khẩu cũ; tạo thêm một quản trị cục bộ để truy cập.
- Runtime dùng `ddl-auto=validate`: kiểm tra schema đã có, không tự thay đổi cấu trúc mỗi lần khởi động.
- Backend/frontend chỉ nghe địa chỉ loopback. Đây là bản chạy thực trên máy cá nhân, chưa phải triển khai máy chủ Internet.

## Khởi động hằng ngày trên máy đã thiết lập

1. Mở Docker Desktop.
2. Tại thư mục gốc dự án chạy:

```powershell
.\start-postgres.ps1
```

Script khởi động container PostgreSQL hiện có, lấy cấu hình kết nối từ container, đọc hoặc tạo cấu hình quản trị/JWT trong `.runtime/`, rồi mở backend và frontend. Mật khẩu không được in ra log hoặc ghi vào báo cáo/GitHub. Thông tin truy cập chỉ ở `.runtime/postgres-access.json` trên máy cục bộ.

Dừng ứng dụng:

```powershell
.\stop-postgres-app.ps1
```

Lệnh dừng chỉ dừng đúng frontend/backend do script đã mở, không xóa dữ liệu hoặc dừng PostgreSQL.

## Sau khi thay đổi mã nguồn

```powershell
cd vetclinic
.\mvnw.cmd package
cd ..\frontend
npm.cmd ci
npm.cmd run build
cd ..
.\start-postgres.ps1
```

Dừng bản đang chạy trước khi build và khởi động lại. Nếu Java/Node chưa nằm trên PATH, truyền đường dẫn bằng tham số `-Java` và `-Node`.

Script `start-postgres.ps1` dành cho database đã có schema. Máy mới chưa có container/schema cần thiết lập PostgreSQL và schema trước; script chủ động báo lỗi nếu thiếu, không tự tạo lại hoặc ghi đè database hiện có. Khi nâng cấp database cũ, dừng backend và dùng `vetclinic/scripts/upgrade-db.ps1` để backup và chạy migration 002/003.

## Kiểm thử PostgreSQL tách biệt

Tại thư mục gốc:

```powershell
.\scripts\test-postgres.ps1
```

Script tạo hai database tên riêng theo thời điểm, đặt biến môi trường cho bộ kiểm thử và chạy Maven. Các test dùng `create-drop` trong database mới; không trỏ `TEST_DATABASE_URL` hoặc `WORKFLOW_TEST_DATABASE_URL` vào database đang vận hành. Khi không cấu hình các biến này, `mvnw.cmd test` vẫn dùng H2 để chạy nhanh.

## Bằng chứng kiểm chứng

Ngày 22/10 đã hoàn tất:

- 31/31 kiểm thử backend với datasource PostgreSQL; phần giao thức AI vẫn dùng mô hình giả lập.
- Build và kiểm tra kiểu frontend, 8/8 kiểm thử API frontend.
- 13/13 kiểm tra HTTP qua frontend build trên database PostgreSQL kiểm thử có cùng schema với database hiện có.
- Hai yêu cầu đặt lịch cùng thời điểm: một thành công 201, một bị chặn 409.
- Hai yêu cầu kê đơn cùng trừ kho: một thành công 201, một bị chặn 409; tồn kho 3 trừ 2 còn 1.
- Sau khi khởi động lại backend kiểm thử, hồ sơ, tồn kho và hóa đơn đã thanh toán vẫn đúng.
- Khôi phục đầy đủ backup vào database riêng và đối chiếu số bản ghi trong 12 bảng gốc.
- API ứng dụng chính khớp số hồ sơ của PostgreSQL, không dùng dữ liệu giả trong frontend.

Xem [kết quả đã lưu](verification-2026-10-22-postgres/). Dữ liệu kiểm thử được tách khỏi hồ sơ vận hành. Chưa có nghiệm thu người dùng, đánh giá mô hình AI thật hoặc triển khai Internet.
