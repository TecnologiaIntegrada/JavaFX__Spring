package com.projetoj.purchasing.requisition.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataPurchaseRequisitionJpaRepository extends JpaRepository<PurchaseRequisitionJpaEntity, UUID> {

    List<PurchaseRequisitionJpaEntity> findAllByStatusOrderByRequisitionNumberDesc(String status);
}
