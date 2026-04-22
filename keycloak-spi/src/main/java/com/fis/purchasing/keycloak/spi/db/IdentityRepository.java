package com.fis.purchasing.keycloak.spi.db;

import com.fis.purchasing.keycloak.spi.model.IdentityUserRecord;
import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IdentityRepository {

    private final DataSource dataSource;

    public IdentityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<IdentityUserRecord> findById(UUID userId) {
        return querySingle("""
                select u.id, u.username, u.email, u.first_name, u.last_name, u.is_active,
                       u.is_locked, u.password_hash, u.created_at
                  from identity.users u
                 where u.id = ?
                   and u.deleted_at is null
                """, statement -> statement.setObject(1, userId));
    }

    public Optional<IdentityUserRecord> findByUsername(String username) {
        return querySingle("""
                select u.id, u.username, u.email, u.first_name, u.last_name, u.is_active,
                       u.is_locked, u.password_hash, u.created_at
                  from identity.users u
                 where u.username = ?
                   and u.deleted_at is null
                """, statement -> statement.setString(1, username));
    }

    public Optional<IdentityUserRecord> findByEmail(String email) {
        return querySingle("""
                select u.id, u.username, u.email, u.first_name, u.last_name, u.is_active,
                       u.is_locked, u.password_hash, u.created_at
                  from identity.users u
                 where u.email = ?
                   and u.deleted_at is null
                """, statement -> statement.setString(1, email));
    }

    public List<IdentityUserRecord> search(String search, boolean exact, int firstResult, int maxResults) {
        String comparator = exact ? "=" : "ilike";
        String pattern = exact ? search : "%" + search + "%";

        String sql = """
                select u.id, u.username, u.email, u.first_name, u.last_name, u.is_active,
                       u.is_locked, u.password_hash, u.created_at
                  from identity.users u
                 where u.deleted_at is null
                   and (
                        u.username %s ? or
                        u.email %s ? or
                        coalesce(u.first_name, '') %s ? or
                        coalesce(u.last_name, '') %s ?
                   )
              order by u.username
                 limit ? offset ?
                """.formatted(comparator, comparator, comparator, comparator);

        return list(sql, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setInt(5, maxResults);
            statement.setInt(6, firstResult);
        });
    }

    public int count(String search, boolean exact) {
        String comparator = exact ? "=" : "ilike";
        String pattern = exact ? search : "%" + search + "%";

        String sql = """
                select count(*)
                  from identity.users u
                 where u.deleted_at is null
                   and (
                        u.username %s ? or
                        u.email %s ? or
                        coalesce(u.first_name, '') %s ? or
                        coalesce(u.last_name, '') %s ?
                   )
                """.formatted(comparator, comparator, comparator, comparator);

        return count(sql, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
        });
    }

    public List<IdentityUserRecord> findByRoleCode(String roleCode, int firstResult, int maxResults) {
        String sql = """
                select u.id, u.username, u.email, u.first_name, u.last_name, u.is_active,
                       u.is_locked, u.password_hash, u.created_at
                  from identity.users u
                  join identity.user_roles ur on ur.user_id = u.id
                  join identity.roles r on r.id = ur.role_id
                 where u.deleted_at is null
                   and r.code = ?
              order by u.username
                 limit ? offset ?
                """;

        return list(sql, statement -> {
            statement.setString(1, roleCode);
            statement.setInt(2, maxResults);
            statement.setInt(3, firstResult);
        });
    }

    public int countByRoleCode(String roleCode) {
        String sql = """
                select count(*)
                  from identity.users u
                  join identity.user_roles ur on ur.user_id = u.id
                  join identity.roles r on r.id = ur.role_id
                 where u.deleted_at is null
                   and r.code = ?
                """;

        return count(sql, statement -> statement.setString(1, roleCode));
    }

    public boolean passwordMatches(UUID userId, String rawPassword) {
        return findById(userId)
                .filter(user -> user.isActive() && !user.isLocked())
                .map(user -> BCrypt.checkpw(rawPassword, user.getPasswordHash()))
                .orElse(false);
    }

    public List<String> findRolesByUserId(UUID userId) {
        String sql = """
                select r.code
                  from identity.user_roles ur
                  join identity.roles r on r.id = ur.role_id
                 where ur.user_id = ?
              order by r.code
                """;

        return listStrings(sql, statement -> statement.setObject(1, userId));
    }

    private Optional<IdentityUserRecord> querySingle(String sql, StatementBinder binder) {
        List<IdentityUserRecord> records = list(sql, binder);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(records.get(0));
    }

    private List<IdentityUserRecord> list(String sql, StatementBinder binder) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<IdentityUserRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    UUID id = resultSet.getObject("id", UUID.class);
                    String username = resultSet.getString("username");
                    String email = resultSet.getString("email");
                    String firstName = resultSet.getString("first_name");
                    String lastName = resultSet.getString("last_name");
                    boolean active = resultSet.getBoolean("is_active");
                    boolean locked = resultSet.getBoolean("is_locked");
                    String passwordHash = resultSet.getString("password_hash");
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    Long createdTimestamp = createdAt == null ? null : createdAt.getTime();
                    List<String> roleCodes = findRolesByUserId(id);

                    records.add(new IdentityUserRecord(id, username, email, firstName, lastName, active, locked, passwordHash, createdTimestamp, roleCodes));
                }
                return records;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query identity.users", exception);
        }
    }

    private List<String> listStrings(String sql, StatementBinder binder) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
                return values;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query string values from identity schema", exception);
        }
    }

    private int count(String sql, StatementBinder binder) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count rows in identity schema", exception);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}