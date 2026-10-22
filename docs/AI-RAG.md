# Trợ lý tra cứu hồ sơ bệnh án

## Đã triển khai

`POST /api/assistant/ask`: `{ "petId": 1, "question": "Chẩn đoán đã ghi nhận?", "generate": false }`.

ADMIN và VET được sử dụng. RECEPTIONIST và người chưa đăng nhập bị chặn ở backend; giao diện cũng chặn route. Hồ sơ thú cưng đã lưu trữ không được truy xuất qua trợ lý.

Nguồn gồm hồ sơ thú cưng, lượt khám, đơn thuốc, tiêm chủng của đúng petId và kho kiến thức dùng chung. Không đưa thông tin liên hệ chủ nuôi vào prompt. Mỗi đoạn có mã nguồn, đường dẫn nội bộ và thời điểm cập nhật. Tài liệu được chia đoạn 900 ký tự, chồng lấn 200 ký tự. Dữ liệu được đọc lại mỗi lần hỏi nên sửa/xóa nguồn có hiệu lực ở lần hỏi tiếp theo.

Chế độ mặc định EXTRACTIVE chuẩn hóa dấu tiếng Việt và tìm từ khóa, trả nguyên đoạn nguồn. Đây là trích xuất, không phải sinh câu trả lời bằng LLM.

Khi `generate=true`, hệ thống lọc tối đa 32 đoạn bằng từ khóa, gọi Ollama embeddings, xếp hạng cosine và lấy tối đa 6 đoạn để đưa vào mô hình hội thoại. Câu trả lời phải có mã nguồn hợp lệ. Thiếu nguồn, dịch vụ lỗi hoặc mô hình chưa bật trả 503, không chuyển ngầm thành một kết quả AI thành công.

## Bật mô hình cục bộ

Chuẩn bị Ollama cùng mô hình hội thoại và embedding. Cấu hình ứng dụng qua biến môi trường Spring Boot:

```powershell
$env:APP_AI_ENABLED='true'
$env:APP_AI_URL='http://127.0.0.1:11434'
$env:APP_AI_MODEL='qwen2.5:7b'
$env:APP_AI_EMBEDDING_MODEL='nomic-embed-text'
```

Tên mô hình là cấu hình mặc định, cần tải mô hình tương ứng và đánh giá trên máy triển khai. Không có mô hình được tự tải trong lượt làm việc này. Chỉ cấu hình endpoint được phòng khám cho phép xử lý dữ liệu bệnh án.

## Giới hạn cần kiểm chứng

- Chưa dùng pgvector hoặc lưu chỉ mục embedding lâu dài. Embeddings được tính theo yêu cầu cho tập ứng viên đã lọc; phù hợp bản thử nghiệm nhỏ, chưa đo tải lớn.
- Lọc từ khóa trước semantic ranking có thể bỏ sót câu hỏi dùng từ đồng nghĩa. Nên hỏi rõ chẩn đoán, điều trị, vaccine hoặc tên thuốc. Chưa đo recall trên bộ câu hỏi thực tế.
- Kiểm tra mã nguồn chỉ xác nhận mã trích dẫn tồn tại; không chứng minh mọi câu sinh ra đều đúng với nguồn. Nội dung cần bác sĩ đối chiếu. Prompt injection được hạn chế qua system prompt và không có công cụ ghi dữ liệu, chưa được coi là đã loại bỏ hoàn toàn.
- Kiểm thử HTTP bằng máy chủ mô hình giả lập kiểm tra giao thức và lỗi; không thay thế kiểm thử chất lượng LLM thật.
- Câu trả lời không tự tạo chẩn đoán, đơn thuốc hay thay đổi hồ sơ. Tài liệu chung phải được phân biệt với thông tin đã ghi nhận của từng bệnh nhân.
