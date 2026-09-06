package com.projetoj.purchasing.requisition.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataPurchaseRequisitionItemJpaRepository extends JpaRepository<PurchaseRequisitionItemJpaEntity, UUID> {

    List<PurchaseRequisitionItemJpaEntity> findAllByRequisitionIdOrderByLineNumberAsc(UUID requisitionId);

    List<PurchaseRequisitionItemJpaEntity> findAllByRequisitionIdInOrderByLineNumberAsc(List<UUID> requisitionIds);

    void deleteAllByRequisitionId(UUID requisitionId);
}
