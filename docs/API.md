# Hợp đồng API VetClinic

Tiền tố `/api`. Body JSON; request có `Authorization: Bearer <accessToken>`, trừ health/login. POST trả 201 trừ login (200), logout (204), activate (200); GET/PUT/PATCH trả 200; DELETE và reset password trả 204. Lỗi nghiệp vụ: ProblemDetail với status/detail; 400 validation, 401 thiếu/hết hạn token, 403 không có quyền, 404 không thấy, 409 xung đột, 429 quá nhiều đăng nhập.

## Xác thực và nhân viên

| Method | Path | Body / chức năng |
|---|---|---|
| GET | /health | `{status:"UP"}`, công khai |
| POST | /auth/login | `{email,password}` → accessToken, tokenType, expiresInMinutes, email, fullName, role |
| GET | /auth/me | id, email, fullName, role, vetId từ tài khoản hiện tại |
| POST | /auth/logout | 204; thu hồi token gửi trong Authorization đến khi hết hạn |
| GET / POST | /admin/users | ADMIN; tạo `{fullName,email,password,role,vetId?}`; mật khẩu 12–100 ký tự |
| PUT | /admin/users/{id} | ADMIN; `{fullName,email,role,vetId?}`; không tự hạ quyền, giữ ít nhất một ADMIN hoạt động |
| PATCH | /admin/users/{id}/enabled | ADMIN; `{enabled}`; không tự khóa tài khoản đang đăng nhập |
| PUT | /admin/users/{id}/password | ADMIN; `{password}`; token cũ mất hiệu lực |

Role: ADMIN, VET, RECEPTIONIST. Đăng ký chỉ qua ADMIN, không phải endpoint công khai. JWT được kiểm tra issuer, hạn, danh sách thu hồi, enabled/role/credential stamp và trạng thái bác sĩ liên kết. VET mới có thể chọn vetId chưa liên kết hoặc bỏ trống để tạo hồ sơ bác sĩ. ADMIN liên kết thủ công tài khoản cũ; không đoán theo tên. Ngừng hoạt động bác sĩ sẽ khóa tài khoản liên kết; kích hoạt bác sĩ không tự mở tài khoản.

## Chủ nuôi/thú cưng

| Method | Path | Chức năng |
|---|---|---|
| GET / POST | /owners | List array / tạo; GET tùy chọn includeArchived=true |
| GET / PUT / DELETE | /owners/{id} | Xem/sửa/lưu trữ (không xóa thật) |
| POST | /owners/{id}/activate | Kích hoạt lại |
| GET | /owners/{id}/pets | Thú cưng hoạt động |
| GET | /owners/{id}/invoices | Hóa đơn, gồm thú cưng đã lưu trữ |
| GET / POST | /pets | List array / tạo; GET tùy chọn includeArchived=true |
| GET / PUT / DELETE | /pets/{id} | Xem/sửa/lưu trữ |
| POST | /pets/{id}/activate | Kích hoạt lại, chủ nuôi phải hoạt động |
| GET | /pets/{id}/visits | Mọi lượt khám |
| GET | /pets/{id}/medical-records | Lượt đang/đã khám |
| GET | /pets/{id}/invoices | Hóa đơn |
| GET / POST | /pets/{id}/weights | Lịch sử / ghi `{weightKg,recordedAt,note}` |
| GET | /owners/page, /pets/page | q, includeArchived, page (>=0), size (1–100), trả `{content,total,pages,page}` |

Owner body: firstName,lastName bắt buộc; phone,email,address tùy chọn. Pet: ownerId,name,species bắt buộc; breed,speciesNote,birthDate,sex,weightKg,allergies,chronicConditions tùy chọn. Ngày dạng YYYY-MM-DD, ngày giờ dạng YYYY-MM-DDTHH:mm:ss không offset; phòng khám dùng giờ local. Optional bỏ trống gửi null. Giới hạn chính xác theo annotations trong controller.

Cả ba vai trò tạo/sửa. ADMIN/VET lưu trữ chủ nuôi khi không còn thú cưng hoạt động; cả ba lưu trữ/kích hoạt thú cưng. Cả ba ghi cân nặng. Ghi bản đo quá khứ không ghi đè cân nặng mới nhất.

## Bác sĩ, lịch hẹn và hồ sơ

| Method | Path | Body / chức năng |
|---|---|---|
| GET / POST | /vets | Danh sách/tạo; GET includeInactive=true |
| GET / PUT / DELETE | /vets/{id} | Xem/sửa/ngừng hoạt động |
| POST | /vets/{id}/activate | Hoạt động lại |
| GET | /vets/{id}/performance | Số lượt, số hoàn thành, tổng tiền hóa đơn PAID |
| GET / POST | /appointments | Danh sách/tạo |
| GET / PUT / DELETE | /appointments/{id} | Xem/đổi lịch/hủy |
| PUT | /appointments/{id}/status | `{status}` |
| POST | /appointments/{id}/follow-up | `{scheduledAt}`; ADMIN/VET; ngày đúng followUpDate từ lượt COMPLETED |
| GET | /visits, /visits/{id} | Danh sách/chi tiết |
| POST | /visits | Cả ba; `{petId,vetId,scheduledAt?,chiefComplaint?}`; tiếp nhận trực tiếp CHECKED_IN, giờ mặc định hiện tại |
| PUT | /visits/{id} | `{symptoms,diagnosis,treatmentNotes,followUpDate}`; ADMIN/VET |

Vet request: name bắt buộc; specialty,licenseNo,workHours tùy chọn; ADMIN ghi, nhân viên xem. Appointment: petId,vetId,scheduledAt bắt buộc; chiefComplaint tùy chọn. Cả ba tạo/đổi/hủy lịch. Đặt tương lai, bác sĩ/thú cưng hoạt động, chống trùng slot 15 phút với khóa bác sĩ. Status SCHEDULED→CHECKED_IN→IN_EXAM→COMPLETED; SCHEDULED/CHECKED_IN có thể CANCELLED. Cả ba cập nhật status. Hoàn thành cần có chẩn đoán hoặc điều trị. Lịch hẹn và lượt khám là cùng Entity Visit. GET /visits và /pets/{id}/visits, /pets/{id}/medical-records hỗ trợ from/to (YYYY-MM-DD, gồm cả hai ngày) và q (tên thú cưng/lý do/triệu chứng/chẩn đoán/điều trị); khoảng ngày đảo ngược trả 400.

## Tiêm, thuốc, đơn thuốc và hóa đơn

| Method | Path | Body / chức năng |
|---|---|---|
| GET / POST | /vaccine-types | Danh mục dùng chung; nhân viên đọc, ADMIN tạo `{name,manufacturer?,description?}` |
| GET / PUT / DELETE | /vaccine-types/{id} | Đọc / ADMIN sửa / ngừng sử dụng, giữ lịch sử |
| PATCH | /vaccine-types/{id}/active | ADMIN; `{active}` |
| GET / POST | /vaccinations | List/tạo; GET tùy chọn petId |
| GET / PUT / DELETE | /vaccinations/{id} | Xem/sửa/xóa thật bản ghi tiêm |
| GET / POST | /medicines | List/tạo |
| GET / PUT | /medicines/{id} | Xem/sửa |
| PATCH | /medicines/{id}/active | `{active}` |
| GET | /prescriptions/by-visit/{visitId} | Đơn thuốc; 404 nếu chưa có |
| GET | /prescriptions/by-pet/{petId} | ADMIN/VET; lịch sử đơn gồm prescribedAt và các dòng thuốc |
| POST | /prescriptions | `{visitId,notes,items:[{medicineId,dosage,frequency,durationDays,quantity,instructions}]}` |
| GET / POST | /invoices | List/tạo |
| GET | /invoices/{id} | Chi tiết gồm items, subtotal,vatAmount,total |
| PUT | /invoices/{id}/items | `{vatRate,items}` khi DRAFT |
| PATCH | /invoices/{id}/status | `{status}`; DRAFT→SENT→PAID |

Tiêm: `{petId,vaccineId,administeredAt,lotNumber,nextDueDate,administeredBy,notes}` (vaccineType cũ vẫn được chấp nhận để tương thích); ADMIN/VET ghi, ADMIN xóa. Ngày tiếp theo nếu có phải sau ngày tiêm, do người chuyên môn nhập. Lưu tên vaccine tại thời điểm tiêm; đổi tên danh mục không đổi lịch sử. Vaccine ngừng sử dụng không dùng cho bản ghi mới.

Medicine: `{name,activeIngredient,form,unit,stockQuantity,reorderLevel,unitPrice,description}`; ADMIN ghi. Đơn chỉ ADMIN/VET và trạng thái IN_EXAM, mỗi lượt một đơn; khóa lượt/thuốc, trừ tồn trong transaction, lưu đơn giá tại thời điểm kê.

Invoice: `{visitId,vatRate,items:[{description,category,quantity,unitPrice}]}`. vatRate 0–1; category CONSULTATION/VACCINATION/SURGERY/HOSPITAL/MEDICATION/OTHER. Backend tính tiền, làm tròn 2 số thập phân; chặn vượt giới hạn cột total. ADMIN/RECEPTIONIST ghi, VET xem. Chỉ chốt khi khám COMPLETED; chỉ trả tiền khi SENT. Không tích hợp giao dịch ngân hàng.

## Tổng quan, hỗ trợ và tri thức

| Method | Path | Chức năng |
|---|---|---|
| GET | /dashboard/summary | Counts, lịch hôm nay, doanh thu PAID theo tháng lập, tải bác sĩ, cảnh báo tiêm/tái khám/tồn kho |
| GET | /notifications | Lịch 7 ngày, tiêm đến/quá hạn, tái khám 30 ngày, thuốc sắp hết |
| GET | /search?q= | Tối thiểu 2 ký tự; tối đa 50 kết quả mỗi loại owners/pets/visits |
| GET / POST | /support-requests | Gửi `{subject,description}`; nhân viên xem của mình, ADMIN xem hết |
| PATCH | /support-requests/{id}/status | ADMIN; `{status:"OPEN"|"IN_PROGRESS"|"RESOLVED"}` |
| GET / POST | /knowledge | Đọc / ADMIN tạo `{title,content,source}` |
| PUT / DELETE | /knowledge/{id} | ADMIN sửa/xóa tài liệu |

Các API tổng hợp/search hiện đọc danh sách trong bộ nhớ; cần aggregate truy vấn khi tăng dữ liệu. Support chưa gửi mail. Knowledge là dữ liệu văn bản có nguồn, chưa có retrieval/AI.
