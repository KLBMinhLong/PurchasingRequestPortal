package com.fis.purchasing.keycloak.spi;

import com.fis.purchasing.keycloak.spi.model.IdentityUserRecord;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.UserModelDefaultMethods;
import org.keycloak.storage.StorageId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class UserAdapter extends UserModelDefaultMethods {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ComponentModel storageProviderModel;
    private final IdentityUserRecord userRecord;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel, IdentityUserRecord userRecord) {
        this.session = session;
        this.realm = realm;
        this.storageProviderModel = storageProviderModel;
        this.userRecord = userRecord;
    }

    @Override
    public String getId() {
        return StorageId.keycloakId(storageProviderModel, userRecord.getId().toString());
    }

    @Override
    public String getUsername() {
        return userRecord.getUsername();
    }

    @Override
    public void setUsername(String username) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public boolean isEnabled() {
        return userRecord.isActive() && !userRecord.isLocked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public Long getCreatedTimestamp() {
        return userRecord.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public String getFirstName() {
        return userRecord.getFirstName();
    }

    @Override
    public String getLastName() {
        return userRecord.getLastName();
    }

    @Override
    public String getEmail() {
        return userRecord.getEmail();
    }

    @Override
    public boolean isEmailVerified() {
        return false;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public String getFirstAttribute(String name) {
        return switch (name) {
            case UserModel.USERNAME -> getUsername();
            case UserModel.EMAIL -> getEmail();
            case UserModel.FIRST_NAME -> getFirstName();
            case UserModel.LAST_NAME -> getLastName();
            case UserModel.ENABLED -> Boolean.toString(isEnabled());
            default -> null;
        };
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        if (getUsername() != null) {
            attributes.put(UserModel.USERNAME, List.of(getUsername()));
        }
        if (getEmail() != null) {
            attributes.put(UserModel.EMAIL, List.of(getEmail()));
        }
        if (getFirstName() != null) {
            attributes.put(UserModel.FIRST_NAME, List.of(getFirstName()));
        }
        if (getLastName() != null) {
            attributes.put(UserModel.LAST_NAME, List.of(getLastName()));
        }
        if (!userRecord.getRoleCodes().isEmpty()) {
            attributes.put("roles", userRecord.getRoleCodes());
        }
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return getAttributes().getOrDefault(name, List.of()).stream();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return Stream.empty();
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return userRecord.getRoleCodes().stream()
                .map(realm::getRole)
                .filter(Objects::nonNull)
                .distinct();
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return getRoleMappingsStream();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return Stream.empty();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (role == null) {
            return false;
        }
        return userRecord.getRoleCodes().contains(role.getName()) || userRecord.getRoleCodes().contains(role.getId());
    }

    @Override
    public void grantRole(RoleModel role) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return Stream.empty();
    }

    @Override
    public void joinGroup(GroupModel group) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void leaveGroup(GroupModel group) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return false;
    }

    @Override
    public String getFederationLink() {
        return storageProviderModel.getId();
    }

    @Override
    public void setFederationLink(String link) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public String getServiceAccountClientLink() {
        return null;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new UserCredentialManager(session, realm, this);
    }

    @Override
    public void addRequiredAction(String action) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void removeRequiredAction(String action) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        throw new UnsupportedOperationException("Identity user storage is read-only");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserModel userModel)) {
            return false;
        }
        return Objects.equals(getId(), userModel.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}