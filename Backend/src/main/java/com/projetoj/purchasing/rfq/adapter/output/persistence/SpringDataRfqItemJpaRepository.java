package com.projetoj.purchasing.rfq.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataRfqItemJpaRepository extends JpaRepository<RfqItemJpaEntity, UUID> {

    List<RfqItemJpaEntity> findAllByRfqIdOrderByLineNumberAsc(UUID rfqId);

    List<RfqItemJpaEntity> findAllByRfqIdInOrderByLineNumberAsc(List<UUID> rfqIds);
}
