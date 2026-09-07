package com.projetoj.purchasing.supplier.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSupplierStatusJpaRepository extends JpaRepository<SupplierStatusJpaEntity, UUID> {

    List<SupplierStatusJpaEntity> findAllByActiveTrueOrderByNameAsc();

    Optional<SupplierStatusJpaEntity> findByCodeIgnoreCase(String code);
}
