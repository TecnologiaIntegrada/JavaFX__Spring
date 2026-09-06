package com.projetoj.purchasing.order.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataPurchaseOrderFollowupJpaRepository extends JpaRepository<PurchaseOrderFollowupJpaEntity, UUID> {

    List<PurchaseOrderFollowupJpaEntity> findAllByPurchaseOrderIdOrderByContactAtDesc(UUID purchaseOrderId);
}
