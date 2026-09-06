package com.projetoj.purchasing.rfq.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataQuoteAwardJpaRepository extends JpaRepository<QuoteAwardJpaEntity, UUID> {

    List<QuoteAwardJpaEntity> findAllByRfqItemId(UUID rfqItemId);

    List<QuoteAwardJpaEntity> findAllByRfqItemIdIn(List<UUID> rfqItemIds);
}
