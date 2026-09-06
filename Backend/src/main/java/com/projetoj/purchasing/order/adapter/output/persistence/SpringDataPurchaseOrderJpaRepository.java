package com.projetoj.purchasing.order.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataPurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderJpaEntity, UUID> {

    List<PurchaseOrderJpaEntity> findAllBySupplierIdOrderByOrderNumberDesc(UUID supplierId);
}
