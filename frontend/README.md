# VetClinic frontend

React 19 + TypeScript strict + Vite 7, giao diện tiếng Việt, API thật. Hướng dẫn toàn dự án và migration: ../README.md. API: ../docs/API.md. AI/RAG: ../docs/AI-RAG.md.

## Chạy

Trong C:\Users\bachn\vetclinic\frontend:

    npm.cmd ci
    npm.cmd run dev

Mở http://localhost:5173. Backend cần chạy ở http://localhost:8080. Vite proxy giữ nguyên /api. Muốn đổi đích dev: .env.local với API_PROXY_TARGET=http://localhost:8080 rồi restart Vite. Không đặt secret backend vào biến VITE.

## Kiểm tra

    npm.cmd test
    npm.cmd run typecheck
    npm.cmd run build
    npm.cmd run preview

Preview chỉ xem build; production cần reverse proxy /api và SPA fallback. resolve.preserveSymlinks hỗ trợ sandbox Windows.

## Cấu trúc

- api.ts: Bearer, JSON, ProblemDetail, timeout 15 giây, lỗi mạng/204/401/session expiration.
- auth.tsx: login rồi /auth/me, sessionStorage theo tab, guard ADMIN/VET/RECEPTIONIST; backend quyết định quyền.
- ui.tsx: hook dữ liệu có hủy request, loading/error/retry, form/bảng/badge/phân trang.
- App.tsx: layout/menu, chủ nuôi/thú cưng, khôi phục lưu trữ, lịch sử hóa đơn. Danh sách chủ nuôi/thú cưng dùng tìm kiếm/phân trang backend.
- Clinical.tsx: lịch tháng/danh sách, tiếp nhận/khám, kê đơn, tái khám, cân nặng/lịch sử.
- Resources.tsx: bác sĩ, tiêm chủng, thuốc/tồn kho, tài khoản, thống kê bác sĩ.
- Invoices.tsx: tạo/sửa nháp, chốt/thanh toán, xem/in mọi dòng.
- Workspace.tsx: dashboard, tìm kiếm, thông báo, hỗ trợ, kho kiến thức.
- types.ts: kiểu dữ liệu DTO; build chạy tsc --noEmit trước Vite.
- styles.css/workspace.css: responsive và chế độ in.

API mới cần backend mới và migration một lần. Nếu dashboard hoặc /owners/page trả 404 khi đã có menu mới: backend cũ chưa restart. Không thay API lỗi bằng dữ liệu giả.

api.test.js thay fetch trong bộ nhớ để kiểm tra hợp đồng client, không gọi/ghi PostgreSQL. Test CRUD backend trong ClinicWorkflowTests dùng H2 riêng. Chưa có bộ E2E tự động toàn giao diện.
