# Purchasing Request Portal

Monorepo for Purchasing Request Portal.

## Current structure

- backend
- frontend
- infrastructure
- docs

## Run project nhanh (don gian)

Huong dan nay giup ban chay local theo thu tu dung:

1. Infrastructure (PostgreSQL, Keycloak, Redis, Kafka, Camunda)
2. Keycloak Custom User Storage SPI
3. Backend Spring Boot
4. Frontend (hien dang la scaffold)

## 1) Yeu cau tien quyet

- Docker Desktop + Docker Compose v2
- Java 21 (LTS)
- Maven 3.9+

Kiem tra nhanh:

```bash
docker --version
docker compose version
mvn -version
java -version
```

## 2) Cau hinh env

Copy file env mau o root:

```bash
cp .env.example .env
```

Gia tri mac dinh da du de chay local.

## 3) Start infrastructure

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Kiem tra trang thai:

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

## 4) Build va nap Keycloak SPI provider

Build JAR SPI:

```bash
cd keycloak-spi
mvn clean package -DskipTests
```

Copy JAR vao thu muc provider duoc mount vao Keycloak:

```bash
cp target/purchasing-request-portal-keycloak-spi-0.0.1-SNAPSHOT.jar ../infrastructure/keycloak/providers/
```

Khoi dong lai Keycloak de load provider + import realm:

```bash
cd ..
docker compose -f infrastructure/docker-compose.yml up -d --force-recreate keycloak
```

## 5) Keycloak setup (realm/client)

Du an da import san:

- realm: purchasing-portal
- client: prp-backend
- role: ADMIN, USER, APPROVER, REQUESTOR

Dang nhap Admin Console:

- URL: http://localhost:8080
- User: KEYCLOAK_ADMIN trong file .env
- Password: KEYCLOAK_ADMIN_PASSWORD trong file .env

Sau khi dang nhap, can gan role cho service-account cua client prp-backend trong realm-management:

- manage-users
- query-users
- view-users
- view-realm

Tai lieu chi tiet: [infrastructure/keycloak/README.md](infrastructure/keycloak/README.md)

## 6) Chay Backend

Tu thu muc backend:

```bash
cd backend
mvn -DskipTests compile
mvn spring-boot:run
```

Backend mac dinh chay tai:

- http://localhost:8082

## 7) Frontend hien tai

From `frontend` folder:

```bash
npm install
npm start
```

Default URL:

- `http://localhost:4200`

## 8) Lenh thuong dung

Dung stack:

```bash
docker compose -f infrastructure/docker-compose.yml down
```

Reset toan bo du lieu local (can than):

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```
