package com.fis.purchasing.repository.jpa;

import com.fis.purchasing.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCode(String code);

    List<RoleEntity> findByCodeIn(Collection<String> codes);

    boolean existsByCode(String code);
}