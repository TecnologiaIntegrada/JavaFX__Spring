package com.projetoj.purchasing.requisition.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataPurchaseRequisitionApprovalJpaRepository extends JpaRepository<PurchaseRequisitionApprovalJpaEntity, UUID> {

    List<PurchaseRequisitionApprovalJpaEntity> findAllByRequisitionIdOrderByApprovalLevelAsc(UUID requisitionId);

    List<PurchaseRequisitionApprovalJpaEntity> findAllByApproverUserIdAndStatusOrderByCreatedAtAsc(UUID approverUserId, String status);

    List<PurchaseRequisitionApprovalJpaEntity> findAllByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);
}
