# RULES_KEYCLOAK_SPI_SECURITY

## Muc tieu

Tai lieu nay la kim chi nam bat buoc cho AI Agent khi code phan IAM/Security cua Purchasing Request Portal voi Keycloak + Spring Security.

- Khong dung DB mac dinh cua Keycloak cho user management.
- Nguon su that cho User/Role/Permission nam trong PostgreSQL `portal.identity`.
- User duoc quan ly qua UI Angular -> Backend Spring Boot -> DB.
- JWT token phai toi gian (minimalist).
- Khong cho phep mot tai khoan dang nhap dong thoi tren 2 thiet bi.

**QUAN TRONG: Keycloak la backend-only service. Frontend KHONG bao gio ket noi truc tiep den Keycloak.**
- Frontend chi noi chuyen voi Backend qua REST API.
- Tat ca tương tác voi Keycloak (OAuth2, OIDC, User Storage SPI) deu duoc xu ly tren Backend.
- Frontend khong su dung keycloak-js library hoac OAuth2 client library.
- Frontend khong co Keycloak client config.

## 1) Kien truc Custom User Storage SPI (Keycloak)

### 1.1 Bat buoc ve SPI interfaces

Java provider cho Keycloak bat buoc implement toi thieu:

- `UserStorageProvider`
- `UserLookupProvider`
- `CredentialInputValidator`

Khuyen nghi implement them khi can:

- `UserQueryProvider` (search/list user cho admin view)
- `CredentialInputUpdater` (neu cho phep doi password qua SPI)
- `OnUserCache` (neu can cache user theo policy)

### 1.2 Rule ket noi CSDL

- SPI phai ket noi JDBC truc tiep vao PostgreSQL DB `portal`, schema `identity`.
- Khong su dung H2/noi bo DB cua Keycloak cho user data.
- Bat buoc dung `HikariCP` cho connection pool trong provider factory.
- Pool phai co timeout va max pool size ro rang (khong de mac dinh tuy tien).
- SQL chi duoc truy cap cac bang identity (`users`, `roles`, `permissions`, `user_roles`, `role_permissions`).
- Password duoc doc tu cot `password_hash` va verify theo BCrypt/Argon2.
- SPI khong duoc luu plaintext password, khong log password/token.

### 1.3 Rule mapping du lieu

- `UserLookupProvider` phai map username/email -> `identity.users`.
- `CredentialInputValidator` phai verify mat khau hash dung thuat toan.
- Roles trong token phai lay tu join `user_roles` + `roles` (va neu can, derive permission tu `role_permissions`).
- Trang thai user (`is_active`, `is_locked`) phai duoc ton trong qua trinh authenticate.

## 2) Rule cho Backend Spring Boot (User Management)

- Backend la he thong duy nhat duoc phep CRUD user trong schema `identity`.
- Moi thao tac Them/Sua/Xoa/Khoa/Mo khoa user bat buoc thong qua API Spring Boot.
- Tuyet doi khong dung Keycloak Admin REST API de tao/sua/xoa user.
- Angular UI quan ly user chi goi API Backend, khong goi truc tiep Keycloak admin endpoint.
- Backend phai hash password (BCrypt/Argon2) truoc khi ghi DB.
- Backend phai enforce business rules IAM:
- username va email unique.
- khong cho xoa hard-delete user dang gan role quan trong (uu tien soft delete/is_active=false).
- cap nhat role qua bang lien ket (`user_roles`), khong hardcode role trong app.
- Tat ca thao tac IAM bat buoc ghi audit log.

## 3) Rule quan ly Session va Logout

### 3.1 Max concurrent sessions = 1

Bat buoc cau hinh tren Keycloak Realm de chan dang nhap dong thoi nhieu thiet bi:

- Realm phai bat policy gioi han concurrent sessions.
- Gia tri `Max concurrent sessions` phai dat = `1` cho user session.
- Khi login moi vuot gioi han, he thong phai tu choi login moi hoac revoke session cu theo chinh sach da thong nhat (uu tien revoke session cu de user tiep tuc tren thiet bi moi, neu business chap nhan).
- AI Agent phai document ro chinh sach duoc chon trong cau hinh realm.

### 3.2 Luong Logout chuan (bat buoc)

Luong logout phai dung thu tu sau:

1. Angular goi endpoint logout cua Backend.
2. Backend goi Keycloak Logout endpoint (OIDC end-session/backchannel) de clear session tap trung.
3. Backend xoa session cuc bo (neu co server-side session/cache).
4. Backend tra ket qua thanh cong cho Angular.
5. Angular xoa local state (`localStorage`/`sessionStorage`/in-memory auth state) va dieu huong ve trang login.

Rule bo sung:

- Khong cho phep Angular chi xoa token local ma khong revoke session tren Keycloak.
- Neu logout voi Keycloak that bai, Backend phai tra ma loi ro rang va log day du (khong lo thong tin nhay cam).

## 4) Rule JWT Token Minimalist

### 4.1 Muc tieu token

- JWT phai nho gon, chi chua thong tin can thiet de authorize.
- Khong nhoi profile payload du thua vao token.

### 4.2 Client Mappers bat buoc

Cau hinh Keycloak Client Mappers de token chi con:

- `sub`
- `preferred_username` (hoac `username` theo chuan da chon)
- `roles` (array role codes lay tu CSDL identity)

Khong dua vao access token cac truong sau neu khong can thiet:

- thong tin ca nhan chi tiet (phone, address, full profile)
- danh sach permission qua chi tiet
- metadata nghiep vu khong phuc vu authorize runtime

### 4.3 Rule verify token phia Backend

- Spring Security Resource Server bat buoc validate:
- issuer (`iss`)
- audience (`aud`) neu co
- signature
- expiration (`exp`)
- mapping claim `roles` -> GrantedAuthority thong nhat prefix (vi du `ROLE_`).
- Backend khong truy tin role tu request body/query; chi tin role tu token da verify.

## 5) Operational Security Rules

- Bat buoc dung TLS cho moi moi truong ngoai local.
- Secrets (DB password, client secret) phai luu trong secret manager/env an toan; khong hardcode.
- Bat buoc rate-limit va audit cho endpoint login/logout/user-management.
- Bat buoc log theo trace-id, khong log token raw/password.
- Bat buoc co ke hoach rotate key/cert va rotate credentials dinh ky.

## 6) Definition of Done cho AI Agent

AI chi duoc xem la hoan tat khi dap ung dong thoi:

- Dung Custom User Storage SPI voi 3 interface toi thieu (`UserStorageProvider`, `UserLookupProvider`, `CredentialInputValidator`).
- Ket noi JDBC + HikariCP den `portal.identity`, khong dung DB user mac dinh cua Keycloak.
- Backend la kenh duy nhat CRUD user; khong dung Keycloak Admin REST API de quan ly user.
- Da cau hinh concurrent sessions = 1 tren realm va ap dung luong logout chuan 5 buoc.
- JWT da toi gian dung mapper, chi giu claim can thiet (`sub`, `username`, `roles`).
