package com.projetoj.identity.permission.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPermissionJpaRepository extends JpaRepository<PermissionJpaEntity, UUID> {

    boolean existsByModule_IdAndAction_Id(UUID moduleId, UUID actionId);

    Optional<PermissionJpaEntity> findByModule_IdAndAction_Id(UUID moduleId, UUID actionId);

    long countByModule_Id(UUID moduleId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM role_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    void deleteRoleLinks(@org.springframework.data.repository.query.Param("permissionId") UUID permissionId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM user_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    void deleteUserLinks(@org.springframework.data.repository.query.Param("permissionId") UUID permissionId);
}
