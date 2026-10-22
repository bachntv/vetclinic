# Nghiệm thu và vận hành VetClinic (ngoài AI/RAG)

## Phạm vi đã triển khai

| Khoảng trống đối chiếu báo cáo | Kết quả trong mã nguồn |
|---|---|
| Quyền bác sĩ quản lý chủ nuôi/thú cưng/lịch | API và nút thao tác cho VET; lưu trữ chủ nuôi chỉ ADMIN/VET |
| Tiếp nhận trực tiếp | POST /api/visits, trạng thái CHECKED_IN, nút Tiếp nhận trực tiếp |
| Triệu chứng và tìm bệnh án | Trường symptoms, lọc from/to/q ở lịch sử hồ sơ |
| Danh mục vaccine, ghi chú tiêm | vaccine_types, lựa chọn danh mục, notes, giữ tên lịch sử |
| Mô tả thuốc | API và form description |
| Lịch sử đơn thuốc theo thú cưng | GET /api/prescriptions/by-pet/{id}, hiển thị ngày kê và thuốc/liều/dặn dò |
| Sửa nhân viên, liên kết bác sĩ | PUT /api/admin/users/{id}; VET gắn một Vet; khóa bác sĩ khóa tài khoản |
| Đăng xuất server | POST /api/auth/logout; lưu hash token, kiểm tra mỗi request |
| TypeScript | Frontend .tsx/.ts, strict, build bắt buộc typecheck |
| Hợp đồng API | docs/API.md đã cập nhật theo controller hiện tại |

Giữ nguyên hồ sơ và mật khẩu hiện có. Tài khoản VET cũ cần ADMIN mở Tài khoản → Sửa → chọn hồ sơ bác sĩ đúng; hệ thống không suy đoán theo tên. Nếu để trống khi tài khoản chưa có liên kết, hệ thống tạo hồ sơ bác sĩ mới.

## Cập nhật trên máy người dùng

1. Mở Docker Desktop. Dừng backend bằng Ctrl+C; đảm bảo không có người khác đang ghi dữ liệu.
2. Trong PowerShell:

```powershell
cd C:\Users\bachn\vetclinic\vetclinic
docker compose up -d
.\scripts\upgrade-db.ps1
.\mvnw.cmd spring-boot:run
```

Script backup trước khi chạy SQL 002 rồi 003, kiểm tra archive/sha256, giữ số bản ghi của 12 bảng cũ, kiểm tra 4 bảng bổ sung và 5 cột mới. Các migration dùng IF NOT EXISTS, có thể chạy lại sau bản 002. Mỗi migration có transaction riêng; nếu 003 lỗi, không khẳng định cả hai đã rollback. Giữ nguyên backup để điều tra, không xóa volume.

3. PowerShell thứ hai:

```powershell
cd C:\Users\bachn\vetclinic\frontend
npm.cmd ci
npm.cmd run dev
```

4. Mở http://localhost:5173; đăng xuất/đăng nhập lại. Tài khoản cũ vẫn dùng mật khẩu cũ. Ngày 22/10 đã tạo thêm quản trị cục bộ; các tài khoản cũ giữ nguyên. Cấu hình truy cập chỉ lưu tại .runtime, không đưa lên GitHub.

## Kiểm tra chức năng bằng dữ liệu thử riêng

Không ghi tên bệnh nhân thật vào nghiệm thu. Tạo dữ liệu thử chỉ sau khi người phụ trách đồng ý; backend có thể kiểm thử bằng H2 hoặc các database PostgreSQL riêng do scripts/test-postgres.ps1 tạo.

| Bước | Vai trò | Kết quả mong đợi |
|---|---|---|
| Đăng nhập, refresh, đăng xuất | Cả ba | /me đúng người/vai trò; token vừa logout trả 401 |
| Cấp tài khoản VET và gắn bác sĩ | ADMIN | Tạo mới hoặc chọn hồ sơ chưa liên kết; chọn trùng trả 409 |
| Sửa tên/email/vai trò; khóa tài khoản | ADMIN | /me lấy thông tin hiện tại; email/quyền/mật khẩu thay đổi làm token cũ không hợp lệ |
| Ngừng bác sĩ rồi mở lại | ADMIN | Tài khoản bị khóa; mở bác sĩ trước rồi mở tài khoản riêng |
| Tạo/sửa chủ nuôi, thú cưng | Cả ba | Dữ liệu thật từ API; archive không xóa hồ sơ khám |
| Đặt lịch/đổi lịch/hủy, tìm tên pet | Cả ba | Không đặt quá khứ; trùng bác sĩ trong 15 phút trả 409 |
| Tiếp nhận trực tiếp và khám | Cả ba tiếp nhận, ADMIN/VET ghi bệnh án | CHECKED_IN → IN_EXAM; lưu triệu chứng/chẩn đoán/điều trị |
| Lọc bệnh án theo ngày và nội dung | Cả ba | Gồm hai ngày biên; from > to trả 400 |
| Kê thuốc | ADMIN/VET | Chỉ IN_EXAM; trừ kho một lần; không đủ tồn trả 409 |
| Lịch sử đơn theo thú cưng | ADMIN/VET | Có ngày kê, các thuốc/liều/tần suất/số ngày; lễ tân bị 403 |
| Danh mục vaccine và ghi nhận tiêm | ADMIN danh mục; ADMIN/VET tiêm | Ghi chú được lưu; đổi tên catalog không đổi tên bản ghi cũ |
| Hóa đơn, VAT, chốt, thu tiền, in | ADMIN/RECEPTIONIST | Tổng tính từ server; DRAFT → SENT → PAID đúng điều kiện |
| Dashboard/thống kê/cảnh báo | Cả ba | Theo dữ liệu thực, không thay lỗi API bằng dữ liệu giả |
| Hỗ trợ/tri thức | Nhân viên/ADMIN | Chỉ ADMIN xem mọi hỗ trợ; chỉ ADMIN sửa tri thức |
| Dữ liệu trống, backend tắt, lỗi quyền | Cả ba | Có loading, thông báo lỗi/retry, 400/401/403/404/409 rõ ràng |

Ghi ngày, phiên bản, người chạy, kết quả thực tế và ảnh minh chứng. Chưa đánh dấu các hàng này là đã qua nghiệm thu người dùng.

## Kiểm thử tự động và giới hạn

Backend: mvnw.cmd test mặc định dùng H2; scripts/test-postgres.ps1 chạy cùng bộ test trên PostgreSQL tách biệt. Ngày 22/10 đạt 31/31 backend PostgreSQL, 8/8 frontend API và build thành công. 13 kiểm tra HTTP, cạnh tranh đặt lịch/trừ kho và dữ liệu qua restart cũng đã đạt. Chưa có bộ E2E tự động toàn giao diện.

Docker đã hoạt động ngày 22/10. Đã backup, áp dụng SQL 002/003 giữ nguyên số bản ghi của 12 bảng gốc, restore toàn bộ backup vào database riêng và đối chiếu thành công. Xem docs/POSTGRES-RUN.md và bằng chứng verification-2026-10-22-postgres.

## Mẫu ghi nhận phản hồi (chưa có dữ liệu khảo sát)

Ngày / vai trò / tác vụ / kết quả mong đợi / thực tế / ảnh hoặc mã lỗi / mức ảnh hưởng / đề xuất / người xác nhận. Không ghi phản hồi giả hoặc tự điền kết quả hài lòng.

## Trước triển khai công khai

Cần địa chỉ máy chủ/tên miền và quyền triển khai thực tế. Build frontend bằng npm.cmd run build; phục vụ dist với SPA fallback, reverse proxy /api tới backend; Vite proxy chỉ dành phát triển. Dùng HTTPS, JWT_SECRET riêng (không đặt trong biến VITE), cấu hình PostgreSQL riêng, backup có lịch và thử restore. Không dùng secret mặc định của local ngoài Internet. Chưa triển khai production trong đợt này.

Đã tích hợp trợ lý trích hồ sơ có nguồn và Ollama embeddings/LLM tùy cấu hình; mô hình thật chưa được chạy/đánh giá. Các nghiệp vụ thương mại ngoài phạm vi báo cáo như SMS, ngân hàng, PetShop/spa, kéo-thả lịch, hoàn tồn do hủy đơn chưa được bổ sung trong đợt đối chiếu này.
