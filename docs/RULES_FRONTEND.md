# RULES_FRONTEND

## Muc tieu

- Tuân thủ tuyệt đối các quy tắc dưới đây khi viết hoặc chỉnh sửa Frontend Angular cho Purchasing Request Portal.
- Ưu tiên tính nhất quán, dễ bảo trì, dễ mở rộng theo feature.

## 1) Kien truc Angular bat buoc

- Bắt buộc dùng Standalone Components cho component, page, dialog, layout mới.
- Hạn chế tối đa NgModules kiểu cũ; chỉ dùng khi có lý do kỹ thuật bắt buộc.
- Bắt buộc tổ chức source theo 3 vùng chính: core, shared, features.
- Bắt buộc cấu trúc thư mục tối thiểu:
- src/app/core: singleton services, guards, interceptors, app-level config.
- src/app/shared: reusable UI components, directives, pipes, shared models/utils.
- src/app/features: module theo nghiệp vụ (auth, purchasing-request, dashboard, ...).
- Bắt buộc tách route theo feature và lazy load khi phù hợp.
- Không đặt business logic nặng trong component template.

## 2) Styling bat buoc (SCSS + TailwindCSS + Bootstrap)

- Bắt buộc dùng SCSS cho component styles và global styles.
- Bắt buộc ưu tiên utility classes của TailwindCSS cho layout/spacing/typography nhanh.
- Chỉ dùng Bootstrap ở các thành phần cần tính chuẩn hóa giao diện nhanh; tránh chồng chéo class không kiểm soát.
- Bắt buộc giữ style nhất quán theo design tokens (mau sac, spacing, radius, shadow).
- Không hard-code inline style trừ trường hợp đặc biệt có giải thích rõ.

## 3) API va State management bat buoc

- Bắt buộc gọi API qua HttpClient.
- Bắt buộc tạo service riêng cho từng domain API; không gọi HttpClient trực tiếp trong component.
- Bắt buộc định nghĩa typed models/interface cho request/response.
- Bắt buộc dùng RxJS đúng chuẩn.
- Ưu tiên async pipe trong template để tự quản lý subscribe/unsubscribe.
- Hạn chế subscribe thủ công; chỉ subscribe khi cần side-effect bắt buộc.
- Nếu subscribe thủ công, bắt buộc cleanup đúng cách (takeUntilDestroyed hoặc tương đương).
- Bắt buộc xử lý loading, error, empty state rõ ràng ở UI.

## 4) Bao mat bat buoc

- Bắt buộc dùng HTTP Interceptor để tự động đính kèm JWT Token vào mọi request cần xác thực.
- Không gắn token thủ công tại từng service hoặc từng request.
- Bắt buộc xử lý response 401/403 tập trung trong interceptor (hoặc auth workflow chuẩn).
- Bắt buộc dùng Route Guards để kiểm tra quyền truy cập trước khi vào route.
- Bắt buộc kiểm tra role/permission cho các route nhạy cảm (User/Admin).
- Bắt buộc điều hướng về trang phù hợp khi không đủ quyền (forbidden hoặc login).

## 5) Quy tac dat ten va code quality bat buoc

- Bắt buộc đặt tên file theo kebab-case.
- Bắt buộc đặt tên class/interface theo PascalCase.
- Bắt buộc đặt tên biến, method theo camelCase.
- Bắt buộc tách component presentational và container khi logic phức tạp.
- Bắt buộc viết code theo hướng typed rõ ràng; tránh any nếu không thật sự cần.
- Bắt buộc pass lint/format trước khi commit.

## 6) Quy tac thuc thi cho AI Agent

- Luôn tạo code Angular mới theo Standalone Components.
- Luôn đặt code đúng thư mục core/shared/features.
- Luôn tạo service API riêng và dùng HttpClient + RxJS chuẩn.
- Luôn thêm interceptor/guard khi có yêu cầu xác thực hoặc phân quyền.
- Luôn ưu tiên SCSS + Tailwind utility; dùng Bootstrap có kiểm soát.
- Từ chối triển khai nếu yêu cầu vi phạm các quy tắc bắt buộc trong tài liệu này.
