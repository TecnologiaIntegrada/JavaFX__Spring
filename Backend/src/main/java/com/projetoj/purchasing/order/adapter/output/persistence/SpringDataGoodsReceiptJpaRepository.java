package com.projetoj.purchasing.order.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataGoodsReceiptJpaRepository extends JpaRepository<GoodsReceiptJpaEntity, UUID> {

    List<GoodsReceiptJpaEntity> findAllByPurchaseOrderIdOrderByReceivedAtDesc(UUID purchaseOrderId);
}
