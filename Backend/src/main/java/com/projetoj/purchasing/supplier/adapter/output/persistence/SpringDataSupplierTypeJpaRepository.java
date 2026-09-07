package com.projetoj.purchasing.supplier.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSupplierTypeJpaRepository extends JpaRepository<SupplierTypeJpaEntity, UUID> {

    List<SupplierTypeJpaEntity> findAllByActiveTrueOrderByNameAsc();

    Optional<SupplierTypeJpaEntity> findByCodeIgnoreCase(String code);
}
