package com.projetoj.purchasing.rfq.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSupplierQuoteItemJpaRepository extends JpaRepository<SupplierQuoteItemJpaEntity, UUID> {

    List<SupplierQuoteItemJpaEntity> findAllBySupplierQuoteId(UUID supplierQuoteId);

    List<SupplierQuoteItemJpaEntity> findAllBySupplierQuoteIdIn(List<UUID> supplierQuoteIds);

    void deleteAllBySupplierQuoteId(UUID supplierQuoteId);
}
