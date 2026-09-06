package com.projetoj.identity.permission.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataActionJpaRepository extends JpaRepository<ActionJpaEntity, UUID> {

    Optional<ActionJpaEntity> findByCodeIgnoreCase(String code);
}
