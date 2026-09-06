package com.projetoj.purchasing.order.adapter.output.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPurchaseOrderItemJpaRepository extends JpaRepository<PurchaseOrderItemJpaEntity, UUID> {

    List<PurchaseOrderItemJpaEntity> findAllByPurchaseOrderIdOrderByLineNumberAsc(UUID purchaseOrderId);

    List<PurchaseOrderItemJpaEntity> findAllByPurchaseOrderIdInOrderByLineNumberAsc(List<UUID> purchaseOrderIds);

    List<PurchaseOrderItemJpaEntity> findAllByQuoteAwardIdIn(List<UUID> quoteAwardIds);

    /** Serializes concurrent receipts against the same line so the received quantity can never overshoot. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseOrderItemJpaEntity> findWithLockById(UUID id);
}
