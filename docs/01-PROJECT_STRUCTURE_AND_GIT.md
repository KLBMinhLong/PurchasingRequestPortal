# 01 - Project Structure And Git

## 1. Monorepo Structure De Xuat

Cau truc thu muc goc PurchasingRequestPortal duoc de xuat theo mo hinh Monorepo, tach ro Backend, Frontend va ha tang trien khai.

```text
PurchasingRequestPortal/
|-- backend/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |   `-- com/company/purchasing/
|   |   |   |       |-- config/
|   |   |   |       |-- controller/
|   |   |   |       |-- service/
|   |   |   |       |-- repository/
|   |   |   |       |-- domain/
|   |   |   |       |-- dto/
|   |   |   |       |-- mapper/
|   |   |   |       `-- security/
|   |   |   |-- resources/
|   |   |   |   |-- application.yml
|   |   |   |   |-- db/migration/
|   |   |   |   `-- bpmn/
|   |   |   `-- webapp/
|   |   `-- test/
|   |       `-- java/
|   |-- scripts/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- README.md
|
|-- frontend/
|   |-- src/
|   |   |-- app/
|   |   |   |-- core/
|   |   |   |-- shared/
|   |   |   |-- features/
|   |   |   `-- layout/
|   |   |-- assets/
|   |   |-- environments/
|   |   `-- styles/
|   |-- public/
|   |-- scripts/
|   |-- angular.json
|   |-- package.json
|   |-- tsconfig.json
|   `-- README.md
|
|-- infrastructure/
|   |-- docker/
|   |   |-- backend/
|   |   |-- frontend/
|   |   |-- keycloak/
|   |   |-- postgres/
|   |   |-- kafka/
|   |   `-- redis/
|   |-- compose/
|   |   |-- docker-compose.dev.yml
|   |   |-- docker-compose.test.yml
|   |   `-- docker-compose.prod.yml
|   |-- env/
|   |   |-- .env.example
|   |   |-- backend.env.example
|   |   `-- frontend.env.example
|   `-- README.md
|
|-- docs/
|   |-- 01-PROJECT_STRUCTURE_AND_GIT.md
|   |-- 02-ARCHITECTURE.md
|   |-- 03-DEPLOYMENT.md
|   `-- 04-CONTRIBUTING.md
|
|-- .gitignore
|-- README.md
`-- Makefile
```

### Vai tro tung thu muc

- backend/: Chua ma nguon Java Spring Boot, cac module nghiep vu, bao mat (Spring Security), workflow (Camunda), persistence (JPA/MyBatis), migration DB va Dockerfile backend.
- frontend/: Chua ung dung Angular 21, chia module theo feature, assets, environment va script build/test/lint.
- infrastructure/: Chua toan bo cau hinh dong goi/trien khai bang Docker va Docker Compose cho backend, frontend va cac dich vu phu tro nhu PostgreSQL, Keycloak, Kafka, Redis.
- docs/: Chua tai lieu kien truc, quy trinh phat trien, van hanh va tieu chuan lam viec nhom.
- .gitignore: Quy tac bo qua file tam, artifact build, dependency cache, file IDE.
- README.md: Diem vao chinh cua du an, huong dan chay nhanh local.
- Makefile: Tap hop cac lenh tien ich nhat quan (build, test, run, docker up/down).

## 2. Huong dan thiet lap Git ban dau

### Buoc 1: Khoi tao Git repository

```bash
git init
git branch -M main
```

Neu can gan remote:

```bash
git remote add origin <REMOTE_URL>
git remote -v
```

### Buoc 2: Tao file .gitignore tong the o root

Tao file .gitignore tai thu muc goc PurchasingRequestPortal voi noi dung goi y:

```gitignore
# =========================
# OS
# =========================
.DS_Store
Thumbs.db

# =========================
# Node.js / Angular
# =========================
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*
.npm/
.pnpm-store/
.angular/
dist/
coverage/

# =========================
# Java / Spring Boot / Maven / Gradle
# =========================
target/
out/
*.class
*.jar
*.war
*.ear
.mvn/wrapper/maven-wrapper.jar
.gradle/
build/

# =========================
# IDE / Editor
# =========================
.idea/
*.iml
.vscode/*
!.vscode/extensions.json
!.vscode/settings.json
!.vscode/tasks.json

# =========================
# Logs / Temp / Cache
# =========================
logs/
*.log
tmp/
*.tmp
*.swp

# =========================
# Environment files
# =========================
.env
.env.*
!.env.example
!.env.*.example

# =========================
# Docker local overrides
# =========================
**/docker-compose.override.yml
```

### Buoc 3: Tao commit dau tien

```bash
git add .
git commit -m "chore(init): bootstrap monorepo structure and git standards"
```

## 3. Quy uoc Git Branching va Commit Message

### 3.1. Git Branching Rule theo Git Flow

- main: Nhanh phan anh trang thai production on dinh, chi nhan merge tu release hoac hotfix.
- develop: Nhanh tich hop chinh cho chu ky phat trien tiep theo.
- feature/<ticket>-<short-description>: Nhanh phat trien tinh nang moi, tach tu develop.
- bugfix/<ticket>-<short-description>: Nhanh sua loi thong thuong tren develop.
- release/<version>: Nhanh chuan bi phat hanh, vi du release/1.2.0.
- hotfix/<ticket>-<short-description>: Nhanh sua loi khan cap tren main.

Quy tac dat ten:

- Dung lowercase.
- Dung dau gach ngang de tach tu.
- Gan ticket ID neu co (vi du jira, redmine).

Vi du:

- feature/prp-101-create-purchase-request
- bugfix/prp-188-fix-approval-condition
- release/1.0.0
- hotfix/prp-220-fix-login-redirect

### 3.2. Quy chuan Git Commit Message (Conventional Commits)

Dinh dang:

```text
<type>(<scope>): <subject>
```

Loai commit khuyen nghi:

- feat: Them tinh nang moi.
- fix: Sua loi.
- refactor: Tai cau truc code, khong doi hanh vi.
- perf: Toi uu hieu nang.
- test: Bo sung/chinh sua test.
- docs: Cap nhat tai lieu.
- chore: Viec he thong, CI/CD, cau hinh.
- build: Thay doi build system/dependencies.
- ci: Thay doi pipeline CI.

Nguyen tac viet subject:

- Viet hien tai, ngan gon, ro nghia.
- Khong viet hoa chu cai dau (khuyen nghi).
- Khong ket thuc bang dau cham.

Vi du commit:

- feat(frontend): add purchase request form with validation
- feat(backend): implement maker-checker approval workflow
- fix(backend): handle jwt expiration from keycloak
- chore(infra): add docker compose for kafka and redis
- docs(project): add monorepo structure and git rules

## Khuyen nghi ap dung trong team

- Bat buoc pull request vao develop va main.
- Bat buoc review toi thieu 1 reviewer truoc khi merge.
- Bat branch protection cho main va develop.
- Bat CI check (build, unit test, lint) truoc khi merge.
