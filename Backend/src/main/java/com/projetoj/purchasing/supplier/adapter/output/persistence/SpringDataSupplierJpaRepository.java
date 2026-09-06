package com.projetoj.purchasing.supplier.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID> {

    boolean existsByTaxIdAndActiveIsTrue(String taxId);

    boolean existsByTaxIdAndActiveIsTrueAndIdNot(String taxId, UUID id);

    List<SupplierJpaEntity> findAllByIdIn(List<UUID> ids);
}
