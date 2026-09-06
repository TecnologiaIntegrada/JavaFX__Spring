package com.projetoj.purchasing.rfq.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSupplierQuoteJpaRepository extends JpaRepository<SupplierQuoteJpaEntity, UUID> {

    List<SupplierQuoteJpaEntity> findAllByRfqIdOrderByCreatedAtAsc(UUID rfqId);

    Optional<SupplierQuoteJpaEntity> findByRfqIdAndSupplierId(UUID rfqId, UUID supplierId);
}
