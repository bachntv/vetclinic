# Kiểm chứng nghiệp vụ ngoài AI — 22/10/2026

## Phạm vi hoàn thành

Đã bổ sung sửa, hủy, kê lại đơn thuốc; hoàn lượng cũ và trừ lượng mới trong cùng transaction PostgreSQL. Đơn hủy được giữ lại, có lý do và thời gian. Revision chặn ghi đè từ màn hình cũ; khóa lượt khám và thuốc theo thứ tự ID ngăn trừ/hoàn kho đồng thời sai. Hóa đơn đã chốt/đã thanh toán khóa việc thay đổi đơn. Chỉ xác nhận hoàn kho khi thuốc chưa giao hoặc thực sự đã nhận lại.

Nhật ký ghi tài khoản, thời điểm, đối tượng và dữ liệu trước/sau cho sửa bệnh án, kê/sửa/hủy đơn, chốt hóa đơn và ghi nhận thanh toán. Nhật ký được lưu cùng giao dịch; giao dịch thất bại không sinh nhật ký thành công. Chỉ ADMIN xem và lọc nhật ký; API không có chức năng sửa/xóa nhật ký. Đây chưa phải hệ thống chống can thiệp bởi quản trị database.

Frontend thêm form sửa/hủy/kê lại, lý do bắt buộc, báo lỗi thiếu tồn, xung đột phiên bản và thông báo thành công. Lịch sử có nhãn đơn đã hủy. Xác nhận thanh toán nằm ngay trong trang. Bỏ phân trang trùng ở chủ nuôi/thú cưng, đặt lại trang khi tìm kiếm; bổ sung bố cục form một cột, cuộn bảng và focus bàn phím trên màn hình nhỏ.

## Kết quả tự động

| Kiểm tra | Kết quả |
|---|---|
| Backend trên PostgreSQL thật, database thử riêng | 36/36 đạt; 0 lỗi, 0 bỏ qua |
| Frontend API client | 8/8 đạt |
| TypeScript / production build | Đạt |
| Hai yêu cầu hủy cùng revision | Một 200, một 409; hoàn kho đúng một lần |
| Sửa và hủy đồng thời cùng revision | Một 200, một 409; stock khớp đơn thắng; revision tăng một |
| Nhật ký trong hai kiểm tra đồng thời | Chỉ có tạo đơn và một thay đổi thành công |

Năm bài kiểm thử backend mới bao gồm chu trình sửa/hủy/kê lại, rollback khi thiếu tồn, revision cũ, quyền lễ tân/bác sĩ và khóa đơn sau chốt/thu tiền. Các kết quả 31 bài PostgreSQL và 13 HTTP trong thư mục kiểm chứng trước là bằng chứng của bản trước, không gán lại cho thay đổi mới.

## Kiểm tra trực tiếp bằng trình duyệt

Dữ liệu giả lập, database PostgreSQL riêng; frontend 5174 và backend 18081. Không dùng hồ sơ thật và không phát sinh giao dịch ngân hàng.

| Vai trò / màn hình | Thao tác và kết quả quan sát |
|---|---|
| RECEPTIONIST / lịch hẹn | Đặt lịch thành công; đặt lại trùng bác sĩ/giờ hiện lỗi xung đột 15 phút |
| RECEPTIONIST / lượt khám | Tiếp nhận được; trường bệnh án bị khóa, không có form kê đơn |
| RECEPTIONIST / nhật ký | Truy cập trực tiếp bị 403 |
| VET / lượt khám | Bắt đầu khám, lưu triệu chứng/chẩn đoán/điều trị, hoàn thành được |
| VET / đơn thuốc | Kê 2 từ kho 20 → 18; sửa 99 báo thiếu tồn; sửa 3 → 17; hủy → 20; kê lại 2 → 18 |
| VET / vaccine | Ghi bản tiêm giả lập và xem lại thành công |
| VET / hóa đơn | Xem được; không có nút lập hóa đơn |
| RECEPTIONIST / hóa đơn | Lập 100.000 + VAT 10% = 110.000; chốt SENT; xác nhận thu tiền → PAID, có thông báo thành công |
| ADMIN / nhật ký | Thấy 7 sự kiện của luồng trên, đúng tài khoản bác sĩ/lễ tân; mở trước/sau thấy SENT → PAID, tổng 110.000; lọc tài khoản lễ tân còn 2 sự kiện |
| ADMIN / chủ nuôi | Chuyển trang thứ hai; tìm “QA Chủ” trả một hồ sơ ở trang đầu; chuỗi không tồn tại trả trạng thái trống; sau sửa chỉ còn một bộ phân trang |
| Màn hình hẹp | Quan sát hóa đơn, nhật ký và danh sách tại chiều rộng khoảng 478px; form không bị cắt, bảng cuộn ngang |

Đây là kiểm thử kỹ thuật bởi người phát triển, chưa phải phản hồi của người dùng độc lập. Không khẳng định đã kiểm tra mọi màn hình hoặc từng luồng dưới cả ba tài khoản. Luồng phối hợp được thử theo trách nhiệm lễ tân → bác sĩ → lễ tân; ADMIN kiểm tra quản trị và nhật ký. Quyền ADMIN cho nghiệp vụ được kiểm tra tự động.

## Giới hạn và bước nghiệm thu còn lại

- Chưa có người dùng độc lập tham gia; chưa có nhận xét hoặc điểm hài lòng thực tế. Dùng [USER-TRIAL.md](USER-TRIAL.md) cho buổi thử.
- Hóa đơn nháp không tự lập lại dòng thuốc khi sửa đơn; phải kiểm tra dòng và tổng tiền trước khi chốt. Chưa có nghiệp vụ trả tiền/điều chỉnh hóa đơn đã chốt.
- Nhật ký hiện bao phủ các thao tác trọng yếu nêu trên, chưa bao phủ mọi thay đổi danh mục.
- Bản triển khai hiện là máy cục bộ PostgreSQL; chưa nghiệm thu Internet/HTTPS, tải lớn, hoặc toàn bộ trình duyệt/thiết bị.
- AI thật và đánh giá chất lượng AI nằm ngoài lượt kiểm chứng này.

## Đoạn dùng trong báo cáo đồ án

Hệ thống được kiểm thử trên PostgreSQL bằng 36 ca backend và 8 ca API client frontend, tất cả đạt. Luồng phối hợp lễ tân–bác sĩ được thực hiện trực tiếp từ đặt lịch đến ghi nhận thanh toán trên dữ liệu giả lập. Chức năng sửa/hủy đơn được kiểm tra tính nhất quán tồn kho, rollback khi thiếu thuốc và xung đột đồng thời. Nhật ký lưu tài khoản và trạng thái trước/sau của các thao tác trọng yếu. Phần thử nghiệm với người dùng độc lập chưa thực hiện; kết quả tự kiểm thử không được dùng thay thế đánh giá người dùng. Biên bản, phản hồi và kết quả sửa sau dùng thử sẽ bổ sung sau buổi nghiệm thu.

## Cập nhật ứng dụng cục bộ

Đã sao lưu PostgreSQL dạng custom, kiểm tra archive và checksum trước migration 004. Số bản ghi của toàn bộ 16 bảng có trước migration không đổi; bảng audit_events được thêm mới. Đã khởi động bản JAR/frontend mới tại 8080/5173, health UP; API vẫn khớp PostgreSQL với 2 chủ nuôi và 2 thú cưng hiện có. Các hồ sơ QA nằm ở database riêng, không được chép vào database ứng dụng chính. Bản sao lưu và mật khẩu giữ trên máy, không công bố trong repository.
