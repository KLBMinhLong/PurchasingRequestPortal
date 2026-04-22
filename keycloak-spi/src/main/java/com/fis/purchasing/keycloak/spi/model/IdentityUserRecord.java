package com.fis.purchasing.keycloak.spi.model;

import java.util.List;
import java.util.UUID;

public final class IdentityUserRecord {

    private final UUID id;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean active;
    private final boolean locked;
    private final String passwordHash;
    private final Long createdTimestamp;
    private final List<String> roleCodes;

    public IdentityUserRecord(UUID id,
                              String username,
                              String email,
                              String firstName,
                              String lastName,
                              boolean active,
                              boolean locked,
                              String passwordHash,
                              Long createdTimestamp,
                              List<String> roleCodes) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
        this.locked = locked;
        this.passwordHash = passwordHash;
        this.createdTimestamp = createdTimestamp;
        this.roleCodes = List.copyOf(roleCodes);
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }
}