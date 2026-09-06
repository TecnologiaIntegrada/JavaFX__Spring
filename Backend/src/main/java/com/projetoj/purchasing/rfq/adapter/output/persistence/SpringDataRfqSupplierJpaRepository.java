package com.projetoj.purchasing.rfq.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataRfqSupplierJpaRepository extends JpaRepository<RfqSupplierJpaEntity, UUID> {

    List<RfqSupplierJpaEntity> findAllByRfqId(UUID rfqId);

    List<RfqSupplierJpaEntity> findAllByRfqIdIn(List<UUID> rfqIds);

    boolean existsByRfqIdAndSupplierId(UUID rfqId, UUID supplierId);
}
