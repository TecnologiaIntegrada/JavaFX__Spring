package com.projetoj.identity.user.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Optional<UserJpaEntity> findByUsernameIgnoreCase(String username);

    long countByRole_Id(UUID roleId);

    @Query("""
            select u from UserJpaEntity u
            where upper(u.status) = 'ACTIVE' and upper(u.role.name) = :roleName
            order by u.username asc
            """)
    List<UserJpaEntity> findActiveByRoleName(@Param("roleName") String roleName);

    @Query("select u from UserJpaEntity u where upper(u.status) = 'ACTIVE' order by u.username asc")
    List<UserJpaEntity> findAllActive();
}
