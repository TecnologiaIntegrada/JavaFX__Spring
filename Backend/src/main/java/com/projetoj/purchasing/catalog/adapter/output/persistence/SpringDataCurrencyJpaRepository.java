package com.projetoj.purchasing.catalog.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCurrencyJpaRepository extends JpaRepository<CurrencyJpaEntity, UUID> {

    List<CurrencyJpaEntity> findAllByActiveTrueOrderByCodeAsc();

    Optional<CurrencyJpaEntity> findByCodeIgnoreCase(String code);
}
