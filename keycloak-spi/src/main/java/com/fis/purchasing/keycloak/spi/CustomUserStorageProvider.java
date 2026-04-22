package com.fis.purchasing.keycloak.spi;

import com.fis.purchasing.keycloak.spi.db.IdentityRepository;
import com.fis.purchasing.keycloak.spi.model.IdentityUserRecord;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, UserQueryProvider, CredentialInputValidator {

    private final KeycloakSession session;
    private final IdentityRepository repository;
    private final org.keycloak.component.ComponentModel componentModel;

    public CustomUserStorageProvider(KeycloakSession session,
                                     org.keycloak.component.ComponentModel componentModel,
                                     IdentityRepository repository) {
        this.session = session;
        this.componentModel = componentModel;
        this.repository = repository;
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (storageId.isLocal() || !CustomUserStorageProviderFactory.PROVIDER_ID.equals(storageId.getProviderId())) {
            return null;
        }

        try {
            UUID userId = UUID.fromString(storageId.getExternalId());
            return repository.findById(userId)
                    .map(user -> new UserAdapter(session, realm, componentModel, user))
                    .orElse(null);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return repository.findByUsername(username)
                .map(user -> new UserAdapter(session, realm, componentModel, user))
                .orElse(null);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return repository.findByEmail(email)
                .map(user -> new UserAdapter(session, realm, componentModel, user))
                .orElse(null);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return searchForUserStream(realm, Map.of(UserModel.SEARCH, search), firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        int offset = firstResult == null || firstResult < 0 ? 0 : firstResult;
        int limit = maxResults == null || maxResults < 0 ? 50 : maxResults;

        String search = params.get(UserModel.SEARCH);
        boolean exact = Boolean.parseBoolean(params.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()));

        if (search != null && !search.isBlank()) {
            return repository.search(search.trim(), exact, offset, limit).stream()
                    .map(user -> new UserAdapter(session, realm, componentModel, user));
        }

        if (params.containsKey(UserModel.USERNAME)) {
            return repository.findByUsername(params.get(UserModel.USERNAME)).stream()
                    .map(user -> new UserAdapter(session, realm, componentModel, user));
        }

        if (params.containsKey(UserModel.EMAIL)) {
            return repository.findByEmail(params.get(UserModel.EMAIL)).stream()
                    .map(user -> new UserAdapter(session, realm, componentModel, user));
        }

        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        if (UserModel.USERNAME.equals(attrName)) {
            return repository.findByUsername(attrValue).stream().map(user -> new UserAdapter(session, realm, componentModel, user));
        }
        if (UserModel.EMAIL.equals(attrName)) {
            return repository.findByEmail(attrValue).stream().map(user -> new UserAdapter(session, realm, componentModel, user));
        }
        if (UserModel.FIRST_NAME.equals(attrName) || UserModel.LAST_NAME.equals(attrName) || UserModel.SEARCH.equals(attrName)) {
            return searchForUserStream(realm, Map.of(UserModel.SEARCH, attrValue), null, null);
        }
        return Stream.empty();
    }

    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        int offset = firstResult == null || firstResult < 0 ? 0 : firstResult;
        int limit = maxResults == null || maxResults < 0 ? 50 : maxResults;
        return repository.findByRoleCode(role.getName(), offset, limit).stream()
                .map(user -> new UserAdapter(session, realm, componentModel, user));
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return repository.count("", false);
    }

    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        String search = params.get(UserModel.SEARCH);
        if (search != null && !search.isBlank()) {
            boolean exact = Boolean.parseBoolean(params.getOrDefault(UserModel.EXACT, Boolean.FALSE.toString()));
            return repository.count(search.trim(), exact);
        }
        if (params.containsKey(UserModel.USERNAME)) {
            return repository.findByUsername(params.get(UserModel.USERNAME)).map(user -> 1).orElse(0);
        }
        if (params.containsKey(UserModel.EMAIL)) {
            return repository.findByEmail(params.get(UserModel.EMAIL)).map(user -> 1).orElse(0);
        }
        return repository.count("", false);
    }

    public int getUsersCount(RealmModel realm, String search) {
        if (search == null || search.isBlank()) {
            return repository.count("", false);
        }
        return repository.count(search.trim(), false);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && user != null && user.isEnabled();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (user == null || input == null || !supportsCredentialType(input.getType())) {
            return false;
        }

        String challengeResponse = input.getChallengeResponse();
        if (challengeResponse == null || challengeResponse.isBlank()) {
            return false;
        }

        String storageUserId = new StorageId(user.getId()).getExternalId();
        try {
            return repository.passwordMatches(UUID.fromString(storageUserId), challengeResponse);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}