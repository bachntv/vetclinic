# Giao diện theo vai trò

Cập nhật: 02/11/2026. Ba vai trò sử dụng cùng ứng dụng nhưng có trang chủ, nhóm menu, nội dung và thao tác riêng. Vai trò lấy từ tài khoản đã xác thực, không có nút tự chọn hoặc đổi vai trò trên màn hình đăng nhập.

## Phạm vi trên giao diện

| Chức năng | Quản trị (ADMIN) | Bác sĩ (VET) | Thu ngân / tiếp nhận (RECEPTIONIST) |
|---|---|---|---|
| Trang chủ | Tổng quan phòng khám, doanh thu, nhân sự, kho | Lịch khám, bệnh nhân chờ khám, tái khám | Lịch tiếp nhận, hóa đơn chờ thu, lượt cần lập hóa đơn |
| Chủ nuôi | Xem, thêm, sửa, lưu trữ/kích hoạt | Tra cứu | Xem, thêm, sửa, kích hoạt |
| Thú cưng | Quản lý hồ sơ và lịch sử chuyên môn | Tra cứu hồ sơ, ghi cân nặng, xem lịch sử bệnh án/đơn thuốc/tiêm | Thêm/sửa thông tin đăng ký, lưu trữ/kích hoạt, lịch sử tiếp nhận |
| Đặt/đổi lịch, tiếp nhận trực tiếp | Có | Không hiển thị; bác sĩ có đặt tái khám từ bệnh án | Có |
| Trạng thái lượt khám | Toàn bộ chuyển trạng thái hợp lệ | Bắt đầu khám, hoàn thành khám | Tiếp nhận, hủy lịch trước khi khám |
| Bệnh án, đơn thuốc, tái khám | Có | Có | Không hiển thị biểu mẫu và lịch sử chuyên môn |
| Tiêm chủng | Thêm/sửa/xóa | Thêm/sửa | Không có menu/trang |
| Danh mục thuốc, vaccine | Quản lý | Tra cứu, không có thêm/sửa/khóa | Không có menu/trang |
| Bác sĩ, tài khoản, nhật ký | Quản lý | Không có menu/trang | Không có menu/trang |
| Hóa đơn, chốt, thu tiền, in | Có | Không có menu/trang/liên kết hóa đơn | Có |
| Kiến thức, trợ lý bệnh án | Quản lý kiến thức, sử dụng trợ lý | Đọc kiến thức, sử dụng trợ lý | Không có menu/trang |
| Thông báo | Đầy đủ | Nhắc việc chuyên môn | Lịch hẹn sắp tới |
| Hỗ trợ | Xử lý mọi yêu cầu | Yêu cầu của mình | Yêu cầu của mình |

Thu ngân sửa hồ sơ đăng ký không thấy trường dị ứng/bệnh mãn tính; giá trị chuyên môn đã có được giữ lại khi lưu. Trên phiếu tiếp nhận chỉ có lý do khám, trạng thái và liên kết hóa đơn. Bác sĩ thấy bệnh án và đơn thuốc, không thấy các đường dẫn thanh toán. Danh mục chỉ đọc không tạo cột thao tác trống hoặc các nút quản trị bị vô hiệu hóa.

## Điều hướng và quyền

- Menu và các lối tắt dùng cấu hình chung tại `frontend/src/permissions.ts`.
- Mỗi trang kiểm tra quyền trước khi gắn nội dung; nhập URL trực tiếp ngoài phạm vi hiển thị thông báo và nút về bàn làm việc. Form thêm/sửa hồ sơ cũng được chặn riêng.
- Sau đăng nhập, chỉ quay lại URL trước đó khi vai trò mới được phép mở; đổi từ quản trị sang thu ngân không giữ màn hình quản trị.
- Bác sĩ đã liên kết hồ sơ được lọc lịch của mình mặc định. Đây là bộ lọc công việc, không phải giới hạn truy cập từng bản ghi.
- `RECEPTIONIST` được giữ nguyên trong API/database; nhãn người dùng là “Thu ngân / tiếp nhận”, không đổi tài khoản hay dữ liệu cũ.

Ma trận trên mô tả phạm vi **giao diện**. Quyền bảo vệ API hiện có ở backend vẫn được áp dụng độc lập và rộng hơn ở một số thao tác đọc/quản lý hồ sơ. Lượt này không thay đổi chính sách API hay bổ sung bảo mật theo từng bệnh nhân. Việc ẩn trang/nút không được dùng thay thế kiểm tra quyền trên server.

## Kiểm chứng

- TypeScript strict và production build đạt.
- 14/14 kiểm thử frontend đạt: 8 API client, 6 về trang/nút theo quyền, chuyển trạng thái, tính hợp lệ của menu/lối tắt và URL sau đăng nhập.
- Kiểm tra trực tiếp bằng ba tài khoản giả lập trên bản frontend build, API 18082 và H2 trong bộ nhớ; tách khỏi PostgreSQL vận hành. Kết thúc ngày 02/11/2026.
- Thu ngân: không có menu kho/tiêm/trợ lý/tài khoản; URL kho bị chặn; phiếu không có form khám hoặc nút bắt đầu khám; hồ sơ không có lịch sử bệnh án/đơn thuốc. Sửa thông tin đăng ký thành công, giữ nguyên ghi chú dị ứng/bệnh mãn tính.
- Bác sĩ: có trang chủ và lịch khám riêng; không có nút đặt lịch/tiếp nhận; bắt đầu khám và lưu chẩn đoán thành công, kiểm tra lại sau tải trang; có nút kê đơn; không có liên kết thu tiền. URL hóa đơn và sửa hồ sơ đăng ký bị chặn. Danh mục thuốc không có nút thêm/sửa hoặc cột thao tác.
- Quản trị: có đầy đủ menu, tổng quan, nút quản lý thuốc và màn hình tài khoản với nhãn ba vai trò. Đổi sang thu ngân từ màn hình tài khoản về đúng bàn làm việc thu ngân.
- Quan sát giao diện ở chiều rộng mặc định của trình duyệt kiểm tra khoảng 478px: menu chia nhóm, biểu mẫu một cột, bảng cuộn ngang. Chưa khẳng định kiểm tra mọi kích thước hoặc mọi màn hình.

Bằng chứng tại [kết quả kiểm tra](verification-2026-10-31-roles/results.json), [quản trị](verification-2026-10-31-roles/admin.png), [bác sĩ](verification-2026-10-31-roles/doctor.png), [thu ngân](verification-2026-10-31-roles/cashier-full.png). Thư mục mang ngày bắt đầu 31/10; phần kiểm tra hoàn tất ngày 02/11. Đây là kiểm tra kỹ thuật, chưa phải nghiệm thu của người dùng độc lập.

Bản chính tại http://127.0.0.1:5173 đã phục vụ bundle mới và health UP; xem [kiểm tra bản chính](verification-2026-10-31-roles/live.json). Hai tiến trình QA đã dừng sau kiểm tra; không tạo tài khoản thử vào database vận hành.
