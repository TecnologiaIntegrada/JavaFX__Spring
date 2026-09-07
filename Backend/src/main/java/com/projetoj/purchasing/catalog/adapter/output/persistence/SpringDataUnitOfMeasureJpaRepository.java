package com.projetoj.purchasing.catalog.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUnitOfMeasureJpaRepository extends JpaRepository<UnitOfMeasureJpaEntity, UUID> {

    List<UnitOfMeasureJpaEntity> findAllByActiveTrueOrderByCodeAsc();

    Optional<UnitOfMeasureJpaEntity> findByCodeIgnoreCase(String code);
}
