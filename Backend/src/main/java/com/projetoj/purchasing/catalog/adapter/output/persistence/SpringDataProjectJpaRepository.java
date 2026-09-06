package com.projetoj.purchasing.catalog.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {
}
