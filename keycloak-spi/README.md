# Keycloak Custom User Storage SPI

Module Maven độc lập cho Keycloak 26.1.0, đọc dữ liệu từ PostgreSQL `portal.identity` qua JDBC + HikariCP.

## Build JAR

Từ thư mục `keycloak-spi`:

```bash
mvn clean package
```

JAR sau khi build sẽ nằm ở:

```bash
target/purchasing-request-portal-keycloak-spi-0.0.1-SNAPSHOT.jar
```

## Cách dùng với Docker Compose

Copy JAR vào thư mục provider được mount cho Keycloak:

```bash
cp target/purchasing-request-portal-keycloak-spi-0.0.1-SNAPSHOT.jar ../infrastructure/keycloak/providers/
```

Sau đó khởi động lại Keycloak:

```bash
docker compose -f infrastructure/docker-compose.yml up -d --force-recreate keycloak
```

Keycloak sẽ tự load provider từ `/opt/keycloak/providers` khi container start.

## Biến môi trường SPI

- `SPI_DB_JDBC_URL`
- `SPI_DB_USERNAME`
- `SPI_DB_PASSWORD`
- `SPI_DB_DRIVER`
- `SPI_DB_SCHEMA`
- `SPI_HIKARI_MAX_POOL_SIZE`
- `SPI_HIKARI_MIN_IDLE`
- `SPI_HIKARI_CONNECTION_TIMEOUT_MS`

## Kiến trúc

- `CustomUserStorageProvider`: lookup user, search user, validate password BCrypt.
- `CustomUserStorageProviderFactory`: tạo HikariCP pool và repository.
- `UserAdapter`: map `identity.users` sang `UserModel` tối giản.
- `IdentityRepository`: JDBC thuần để query bảng identity.