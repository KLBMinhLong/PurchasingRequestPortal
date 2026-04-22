# 02 - Infrastructure Setup (Docker Compose)

Tai lieu nay huong dan khoi dong toan bo ha tang local cho Purchasing Request Portal bang Docker Compose, bao gom:

- PostgreSQL
- Keycloak (su dung PostgreSQL, khong dung H2)
- Apache Kafka (KRaft mode)
- Redis
- Camunda BPMN

## 0. Architecture - Frontend & Backend Communication

**CRITICAL: Frontend chi noi chuyen voi Backend. Tat ca dich vu ben thu ba (Keycloak, PostgreSQL, Kafka, Redis, Camunda) chi tương tac voi Backend.**

```
┌─────────────┐                    ┌─────────────────────────────────────────┐
│   Frontend  │                    │           Backend                       │
│  (Angular)  │  HTTP REST API     │      (Spring Boot)                      │
│             │◄──────────────────►│                                         │
│             │   JWT Token        │                                         │
│ :4200       │                    │ :8082  ┌──────────────────────────────┐ │
└─────────────┘                    │        │ Services (Docker Network)   │ │
                                   │        │ ├─ PostgreSQL :5432         │ │
                                   │        │ ├─ Keycloak :8080           │ │
                                   │        │ ├─ Kafka :9092/:29092       │ │
                                   │        │ ├─ Redis :6379              │ │
                                   │        │ └─ Camunda :8081            │ │
                                   │        └──────────────────────────────┘ │
                                   └─────────────────────────────────────────┘

Frontend KHONG ket noi truc tiep den:
- Keycloak (khong dung keycloak-js)
- PostgreSQL (khong truy cap database)
- Kafka (khong la message producer)
- Redis (khong la cache client)
- Camunda (khong call workflow API)

Tat ca dieu kien tren phai thong qua Backend REST API.
```

## 1. Yeu cau tien quyet

- Docker Desktop hoac Docker Engine + Docker Compose v2
- Da clone source code va dung tai thu muc goc du an

Kiem tra nhanh:

```bash
docker --version
docker compose version
```

## 2. Cau truc file lien quan

- infrastructure/docker-compose.yml
- infrastructure/postgres/init/01-init-databases.sql

## 3. Chay ha tang local

Tu root du an, chay:

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Kiem tra container:

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

Xem log theo service:

```bash
docker compose -f infrastructure/docker-compose.yml logs -f postgres
docker compose -f infrastructure/docker-compose.yml logs -f keycloak
docker compose -f infrastructure/docker-compose.yml logs -f kafka
docker compose -f infrastructure/docker-compose.yml logs -f redis
docker compose -f infrastructure/docker-compose.yml logs -f camunda
```

Dung ha tang:

```bash
docker compose -f infrastructure/docker-compose.yml down
```

Dung va xoa ca volume (can than vi mat du lieu local):

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```

## 4. Port mapping va y nghia

- PostgreSQL: 5432:5432
- Keycloak: 8080:8080
- Kafka internal listener: 9092:9092
- Kafka host listener: 29092:29092
- Redis: 6379:6379
- Camunda BPMN: 8081:8080

Luu y Kafka:

- Neu app chay trong cung Docker network, dung bootstrap server: kafka:9092
- Neu app chay tren may local host (ngoai Docker), dung bootstrap server: localhost:29092

## 5. Bien moi truong mac dinh da duoc pre-config

Cac bien da duoc cau hinh san trong compose voi gia tri fallback. Ban co the override bang bien moi truong he thong hoac file .env tai root neu can.

### PostgreSQL

- POSTGRES_USER (mac dinh: postgres)
- POSTGRES_PASSWORD (mac dinh: postgres_dev_password)
- POSTGRES_DB (mac dinh: portal)

### Keycloak (bat buoc dung PostgreSQL)

- KEYCLOAK_ADMIN (mac dinh: admin)
- KEYCLOAK_ADMIN_PASSWORD (mac dinh: admin_dev_password)
- KC_DB=postgres
- KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak
- KC_DB_USERNAME=keycloak
- KC_DB_PASSWORD=keycloak_dev_password

### Kafka (KRaft mode, khong can Zookeeper)

- KAFKA_ENABLE_KRAFT=yes
- KAFKA_CFG_PROCESS_ROLES=broker,controller
- KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,PLAINTEXT_HOST://:29092,CONTROLLER://:9093
- KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092

### Redis

- REDIS_PASSWORD (mac dinh: redis_dev_password)
- Redis duoc bat appendonly de giam mat du lieu local
- Redis yeu cau mat khau de tranh truy cap vo tinh trong moi truong dev chung

### Camunda BPMN

- DB_DRIVER=org.postgresql.Driver
- DB_URL=jdbc:postgresql://postgres:5432/camunda
- DB_USERNAME=camunda
- DB_PASSWORD=camunda_dev_password
- WAIT_FOR=postgres:5432

## 6. Truy cap nhanh cac dich vu

- Keycloak Admin Console: http://localhost:8080
- Camunda Web Apps: http://localhost:8081
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- Kafka (host client): localhost:29092

## 7. Luu y van hanh dev

- Script infrastructure/postgres/init/01-init-databases.sql chi chay o lan dau tao volume postgres_data.
- Neu ban thay doi script init DB sau khi da tung khoi tao, can xoa volume postgres_data roi up lai.
- Password mac dinh trong tai lieu nay chi danh cho local development, khong dung cho production.
