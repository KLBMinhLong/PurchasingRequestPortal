# RULES_BACKEND

## Muc tieu

- Tuân thủ tuyệt đối các quy tắc dưới đây khi viết hoặc chỉnh sửa Backend Java Spring Boot cho Purchasing Request Portal.
- Ưu tiên code rõ ràng, dễ bảo trì, nhất quán toàn dự án.

## 1) Kien truc bat buoc

### 1.1 Layered / Clean Architecture

- Bắt buộc áp dụng Layered Architecture hoặc Clean Architecture.
- Bắt buộc đi đúng luồng: Controller -> Service/UseCase -> Repository.
- Không cho phép Controller gọi trực tiếp Repository.
- Không cho phép code nghiệp vụ nằm trong Controller.

### 1.2 Backend la gateway cho tat ca dich vu ben thu ba

**QUAN TRONG: Backend la DIEM DUNG DIEN DUNG de tương tác voi tat ca dich vu ben thu ba:**
- Frontend chi noi chuyen voi Backend.
- Backend phu cap dang nhap, xac thuc, cap phat token (thong qua Keycloak + Custom User Storage SPI).
- Backend ket noi voi PostgreSQL, Redis, Kafka, Camunda.
- Frontend KHONG bao gio truy cap truc tiep Keycloak, PostgreSQL, hoac bat ky dich vu khac.
- Tat ca API endpoint cua Backend phai co authentication/authorization check; khong cho phep Frontend bypass.

### 1.3 Package structure bat buoc

- Bắt buộc tách package theo vai trò kỹ thuật và nghiệp vụ.
- Bắt buộc chuẩn package gốc: com.fis.purchasing.
- Bắt buộc tổ chức package tối thiểu như sau:
- com.fis.purchasing.controller: REST API endpoints.
- com.fis.purchasing.service: business service.
- com.fis.purchasing.usecase: use-case orchestration (neu dung Clean Architecture).
- com.fis.purchasing.dto.request: request DTO.
- com.fis.purchasing.dto.response: response DTO.
- com.fis.purchasing.entity: JPA entities.
- com.fis.purchasing.repository.jpa: JPA repositories.
- com.fis.purchasing.repository.mybatis: MyBatis mapper/repository.
- com.fis.purchasing.config: Spring, security, database config.
- com.fis.purchasing.exception: custom exception va global handler.
- com.fis.purchasing.mapper: mapping giua entity va DTO.

## 2) Coding convention bat buoc

- Bắt buộc đặt tên class bằng PascalCase, danh từ rõ nghĩa.
- Bắt buộc đặt tên method và biến bằng camelCase, động từ rõ nghĩa cho method.
- Bắt buộc đặt tên hằng số bằng UPPER_SNAKE_CASE.
- Bắt buộc đặt tên boolean theo dạng is/has/can.
- Không dùng tên mơ hồ như data, temp, obj, value1.
- Bắt buộc mỗi class chỉ giữ một trách nhiệm chính.
- Bắt buộc dùng Lombok để giảm boilerplate code.
- Ưu tiên dùng Lombok phù hợp: Getter, Setter, Builder, RequiredArgsConstructor, Value.
- Không lạm dụng Data cho mọi class; chọn annotation theo nhu cầu thực tế.

## 3) API va DTO bat buoc

- Mọi Controller bắt buộc trả về chuẩn response chung ApiResponse<T>.
- Bắt buộc thống nhất các trường response metadata: code, message, data, timestamp, path.
- Bắt buộc dùng DTO cho cả input và output API.
- Tuyệt đối không trả trực tiếp Entity trong response.
- Bắt buộc validate request bằng Jakarta Validation (Valid, NotNull, NotBlank, Size, ...).
- Bắt buộc xử lý mapping Entity <-> DTO qua mapper layer, không map thủ công rải rác.

## 4) Exception handling bat buoc

- Bắt buộc dùng RestControllerAdvice để bắt lỗi tập trung toàn hệ thống.
- Bắt buộc chuẩn hóa error response theo một format thống nhất.
- Bắt buộc map rõ các nhóm lỗi: validation, business, unauthorized, forbidden, not found, internal error.
- Bắt buộc tạo custom exception cho lỗi nghiệp vụ.
- Không trả stack trace hoặc thông tin nhạy cảm ra API response.
- Bắt buộc log đầy đủ tại server, nhưng response chỉ trả thông tin an toàn cho client.

## 5) Security va Data access bat buoc

- Bắt buộc dùng JPA/Hibernate cho CRUD và truy vấn đơn giản.
- Bắt buộc dùng MyBatis cho báo cáo/truy vấn phức tạp, tối ưu hiệu năng.
- Không trộn lẫn JPA và MyBatis thiếu kiểm soát trong cùng một use-case.
- Bắt buộc bảo mật API bằng Spring Security + JWT.
- Bắt buộc validate token do Keycloak cấp (issuer, signature, exp, roles/scopes).
- Bắt buộc phân quyền theo role rõ ràng tại endpoint và tầng nghiệp vụ.
- Bắt buộc vô hiệu hóa mọi endpoint không cần public access.

## 6) Quy tac thuc thi cho AI Agent

- Luôn tạo code theo đúng package chuẩn đã định nghĩa.
- Luôn tạo DTO trước khi tạo endpoint mới.
- Luôn thêm hoặc cập nhật Global Exception Handler khi phát sinh loại lỗi mới.
- Luôn tuân thủ chuẩn ApiResponse<T> cho mọi endpoint.
- Luôn kiểm tra yêu cầu bảo mật JWT/role trước khi hoàn tất code.
- Từ chối triển khai nếu yêu cầu vi phạm các quy tắc bắt buộc trong tài liệu này.
