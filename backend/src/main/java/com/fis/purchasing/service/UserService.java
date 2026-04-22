package com.fis.purchasing.service;

import com.fis.purchasing.dto.request.UserCreateRequest;
import com.fis.purchasing.dto.request.UserUpdateRequest;
import com.fis.purchasing.dto.response.UserResponse;
import com.fis.purchasing.entity.RoleEntity;
import com.fis.purchasing.entity.UserEntity;
import com.fis.purchasing.exception.BusinessRuleException;
import com.fis.purchasing.exception.ConflictException;
import com.fis.purchasing.exception.ResourceNotFoundException;
import com.fis.purchasing.mapper.UserMapper;
import com.fis.purchasing.repository.jpa.RoleRepository;
import com.fis.purchasing.repository.jpa.UserRepository;
import com.fis.purchasing.security.AuditChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return userMapper.toResponse(loadUser(userId));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    @AuditChange(action = "USER_CREATE", entity = "identity.users")
    public UserResponse createUser(UserCreateRequest request) {
        validateUniqueFields(request.getUsername(), request.getEmail(), null);

        UserEntity user = new UserEntity();
        applyCreateRequest(user, request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedBy(resolveActor());
        user.setUpdatedBy(resolveActor());
        user.setAttributes(userMapper.toAttributeJson(request.getAttributes()));
        user.setRoles(resolveRoles(request.getRoleCodes()));

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @AuditChange(action = "USER_UPDATE", entity = "identity.users")
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        UserEntity user = loadUser(userId);
        validateUniqueFields(request.getUsername(), request.getEmail(), userId);

        applyUpdateRequest(user, request);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getAttributes() != null) {
            user.setAttributes(userMapper.toAttributeJson(request.getAttributes()));
        }
        if (request.getRoleCodes() != null) {
            user.setRoles(resolveRoles(request.getRoleCodes()));
        }
        user.setUpdatedBy(resolveActor());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    @AuditChange(action = "USER_DELETE", entity = "identity.users")
    public UserResponse deleteUser(UUID userId) {
        UserEntity user = loadUser(userId);
        user.setActive(false);
        user.setLocked(true);
        user.setDeletedAt(OffsetDateTime.now());
        user.setUpdatedBy(resolveActor());
        return userMapper.toResponse(userRepository.save(user));
    }

    private void applyCreateRequest(UserEntity user, UserCreateRequest request) {
        user.setUsername(normalize(request.getUsername()));
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setActive(request.getActive() == null || request.getActive());
        user.setLocked(request.getLocked() != null && request.getLocked());
    }

    private void applyUpdateRequest(UserEntity user, UserUpdateRequest request) {
        if (StringUtils.hasText(request.getUsername())) {
            user.setUsername(normalize(request.getUsername()));
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
            if (request.getActive()) {
                user.setDeletedAt(null);
            }
        }
        if (request.getLocked() != null) {
            user.setLocked(request.getLocked());
        }
    }

    private UserEntity loadUser(UUID userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void validateUniqueFields(String username, String email, UUID userId) {
        if (StringUtils.hasText(username)) {
            boolean usernameExists = userId == null
                    ? userRepository.existsByUsernameIgnoreCase(username.trim())
                    : userRepository.existsByUsernameIgnoreCaseAndIdNot(username.trim(), userId);
            if (usernameExists) {
                throw new ConflictException("Username already exists");
            }
        }
        if (StringUtils.hasText(email)) {
            boolean emailExists = userId == null
                    ? userRepository.existsByEmailIgnoreCase(email.trim())
                    : userRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), userId);
            if (emailExists) {
                throw new ConflictException("Email already exists");
            }
        }
    }

    private Set<RoleEntity> resolveRoles(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> normalizedCodes = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            if (!StringUtils.hasText(roleCode)) {
                throw new BusinessRuleException("Role code cannot be blank");
            }
            normalizedCodes.add(roleCode.trim().toUpperCase(Locale.ROOT));
        }
        List<RoleEntity> roles = roleRepository.findByCodeIn(normalizedCodes);
        if (roles.size() != normalizedCodes.size()) {
            Set<String> foundCodes = roles.stream().map(RoleEntity::getCode).collect(java.util.stream.Collectors.toSet());
            normalizedCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .findFirst()
                    .ifPresent(missingCode -> {
                        throw new ResourceNotFoundException("Role not found: " + missingCode);
                    });
        }
        return new LinkedHashSet<>(roles);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getSubject();
        }
        return null;
    }
}