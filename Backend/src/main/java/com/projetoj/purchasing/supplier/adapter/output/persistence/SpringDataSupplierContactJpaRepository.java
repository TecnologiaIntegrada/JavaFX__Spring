package com.projetoj.purchasing.supplier.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSupplierContactJpaRepository extends JpaRepository<SupplierContactJpaEntity, UUID> {

    List<SupplierContactJpaEntity> findAllBySupplierIdOrderByNameAsc(UUID supplierId);

    List<SupplierContactJpaEntity> findAllBySupplierIdInOrderByNameAsc(List<UUID> supplierIds);

    void deleteAllBySupplierId(UUID supplierId);
}
