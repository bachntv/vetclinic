# Trợ lý tra cứu hồ sơ bệnh án

## Đã triển khai

`POST /api/assistant/ask`: `{ "petId": 1, "question": "Chẩn đoán đã ghi nhận?", "generate": false }`.

ADMIN và VET được sử dụng. RECEPTIONIST và người chưa đăng nhập bị chặn ở backend; giao diện cũng chặn route. Hồ sơ thú cưng đã lưu trữ không được truy xuất qua trợ lý.

Nguồn gồm hồ sơ thú cưng, lượt khám, đơn thuốc, tiêm chủng của đúng petId và kho kiến thức dùng chung. Không đưa thông tin liên hệ chủ nuôi vào prompt. Mỗi đoạn có mã nguồn, đường dẫn nội bộ và thời điểm cập nhật. Tài liệu được chia đoạn 900 ký tự, chồng lấn 200 ký tự. Dữ liệu được đọc lại mỗi lần hỏi nên sửa/xóa nguồn có hiệu lực ở lần hỏi tiếp theo.

Chế độ mặc định EXTRACTIVE chuẩn hóa dấu tiếng Việt và tìm từ khóa, trả nguyên đoạn nguồn. Đây là trích xuất, không phải sinh câu trả lời bằng LLM.

Khi `generate=true`, hệ thống lọc tối đa 32 đoạn bằng từ khóa, gọi Ollama embeddings, xếp hạng cosine và lấy tối đa 6 đoạn để đưa vào mô hình hội thoại. Câu trả lời phải có mã nguồn hợp lệ. Thiếu nguồn, dịch vụ lỗi hoặc mô hình chưa bật trả 503, không chuyển ngầm thành một kết quả AI thành công.

## Bật mô hình cục bộ

Mở Docker Desktop. Từ thư mục gốc VetClinic, chạy lệnh sau trong PowerShell:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\start-ai.ps1
```

Lần đầu, script khởi động Ollama và tải Qwen2.5 1.5B cùng mô hình embedding nomic-embed-text. Các mô hình được lưu trong Docker volume để tái sử dụng. Script kiểm tra cả hai mô hình bằng dữ liệu giả lập trước khi khởi động lại VetClinic với AI bật. Ollama chỉ được mở trên địa chỉ 127.0.0.1:11435 của máy chạy VetClinic.

Vào **Trợ lý bệnh án**, chọn một hồ sơ có dữ liệu, bật **Tổng hợp bằng AI cục bộ** rồi đặt câu hỏi. Nếu AI gặp lỗi, có thể bấm **Trích hồ sơ cho câu hỏi này** để xem trực tiếp các đoạn khớp.

Có thể dùng tham số `-Model` và `-EmbeddingModel` để chọn mô hình khác. Nếu đã có một Ollama riêng, có thể cấu hình `APP_AI_ENABLED`, `APP_AI_URL`, `APP_AI_MODEL` và `APP_AI_EMBEDDING_MODEL` trong môi trường của tiến trình VetClinic.
## Giới hạn cần kiểm chứng

- Chưa dùng pgvector hoặc lưu chỉ mục embedding lâu dài. Embeddings được tính theo yêu cầu cho tập ứng viên đã lọc; phù hợp bản thử nghiệm nhỏ, chưa đo tải lớn.
- Lọc từ khóa trước semantic ranking có thể bỏ sót câu hỏi dùng từ đồng nghĩa. Nên hỏi rõ chẩn đoán, điều trị, vaccine hoặc tên thuốc. Chưa đo recall trên bộ câu hỏi thực tế.
- Kiểm tra mã nguồn chỉ xác nhận mã trích dẫn tồn tại; không chứng minh mọi câu sinh ra đều đúng với nguồn. Nội dung cần bác sĩ đối chiếu. Prompt injection được hạn chế qua system prompt và không có công cụ ghi dữ liệu, chưa được coi là đã loại bỏ hoàn toàn.
- Kiểm thử HTTP bằng máy chủ mô hình giả lập kiểm tra giao thức và lỗi; không thay thế kiểm thử chất lượng LLM thật.
- Câu trả lời không tự tạo chẩn đoán, đơn thuốc hay thay đổi hồ sơ. Tài liệu chung phải được phân biệt với thông tin đã ghi nhận của từng bệnh nhân.
