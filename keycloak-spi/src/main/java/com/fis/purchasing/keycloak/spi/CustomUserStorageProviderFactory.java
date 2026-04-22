package com.fis.purchasing.keycloak.spi;

import com.fis.purchasing.keycloak.spi.db.IdentityRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {

    public static final String PROVIDER_ID = "portal-identity-spi";

    private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://postgres:5432/portal";
    private static final String DEFAULT_DB_USER = "keycloak_spi";
    private static final String DEFAULT_DB_PASSWORD = "keycloak_spi_dev_password";
    private static final String DEFAULT_DB_DRIVER = "org.postgresql.Driver";
    private static final String DEFAULT_SCHEMA = "identity";

    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, IdentityRepository> repositories = new ConcurrentHashMap<>();

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        IdentityRepository repository = repositories.computeIfAbsent(model.getId(), key -> new IdentityRepository(getOrCreateDataSource(model)));
        return new CustomUserStorageProvider(session, model, repository);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Reads users and roles from portal.identity via JDBC + HikariCP.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        properties.add(stringProperty("jdbcUrl", "JDBC URL", "Portal database JDBC URL", DEFAULT_JDBC_URL));
        properties.add(stringProperty("dbUser", "DB User", "Portal database username", DEFAULT_DB_USER));
        properties.add(stringProperty("dbPassword", "DB Password", "Portal database password", DEFAULT_DB_PASSWORD));
        properties.add(stringProperty("schema", "Schema", "Identity schema", DEFAULT_SCHEMA));
        properties.add(stringProperty("driverClassName", "JDBC Driver", "JDBC driver class", DEFAULT_DB_DRIVER));
        properties.add(stringProperty("maximumPoolSize", "Max Pool Size", "Maximum Hikari pool size", "5"));
        properties.add(stringProperty("minimumIdle", "Min Idle", "Minimum idle connections", "1"));
        properties.add(stringProperty("connectionTimeoutMs", "Connection Timeout (ms)", "Hikari connection timeout in milliseconds", "30000"));
        return Collections.unmodifiableList(properties);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, org.keycloak.models.RealmModel realm, ComponentModel config) throws ComponentValidationException {
        String jdbcUrl = resolve(config, "jdbcUrl", DEFAULT_JDBC_URL);
        String dbUser = resolve(config, "dbUser", DEFAULT_DB_USER);
        String dbPassword = resolve(config, "dbPassword", DEFAULT_DB_PASSWORD);

        if (jdbcUrl.isBlank()) {
            throw new ComponentValidationException("jdbcUrl is required");
        }
        if (dbUser.isBlank()) {
            throw new ComponentValidationException("dbUser is required");
        }
        if (dbPassword.isBlank()) {
            throw new ComponentValidationException("dbPassword is required");
        }
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
        repositories.clear();
    }

    private HikariDataSource getOrCreateDataSource(ComponentModel model) {
        return dataSources.computeIfAbsent(model.getId(), key -> {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName("prp-keycloak-spi-" + key);
            hikariConfig.setJdbcUrl(resolve(model, "jdbcUrl", DEFAULT_JDBC_URL));
            hikariConfig.setUsername(resolve(model, "dbUser", DEFAULT_DB_USER));
            hikariConfig.setPassword(resolve(model, "dbPassword", DEFAULT_DB_PASSWORD));
            hikariConfig.setDriverClassName(resolve(model, "driverClassName", DEFAULT_DB_DRIVER));
            hikariConfig.setSchema(resolve(model, "schema", DEFAULT_SCHEMA));
            hikariConfig.setMaximumPoolSize(parseInteger(resolve(model, "maximumPoolSize", "5"), 5));
            hikariConfig.setMinimumIdle(parseInteger(resolve(model, "minimumIdle", "1"), 1));
            hikariConfig.setConnectionTimeout(parseLong(resolve(model, "connectionTimeoutMs", "30000"), 30000L));
            hikariConfig.setAutoCommit(true);
            return new HikariDataSource(hikariConfig);
        });
    }

    private static ProviderConfigProperty stringProperty(String name, String label, String helpText, String defaultValue) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setHelpText(helpText);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(defaultValue);
        return property;
    }

    private static String resolve(ComponentModel model, String key, String defaultValue) {
        String value = model.getConfig().getFirst(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        String envKey = switch (key) {
            case "jdbcUrl" -> "SPI_DB_JDBC_URL";
            case "dbUser" -> "SPI_DB_USERNAME";
            case "dbPassword" -> "SPI_DB_PASSWORD";
            case "driverClassName" -> "SPI_DB_DRIVER";
            case "schema" -> "SPI_DB_SCHEMA";
            case "maximumPoolSize" -> "SPI_HIKARI_MAX_POOL_SIZE";
            case "minimumIdle" -> "SPI_HIKARI_MIN_IDLE";
            case "connectionTimeoutMs" -> "SPI_HIKARI_CONNECTION_TIMEOUT_MS";
            default -> null;
        };

        if (envKey != null) {
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
        }

        return defaultValue;
    }

    private static int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}