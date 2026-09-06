package com.projetoj.purchasing.rfq.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataRfqJpaRepository extends JpaRepository<RfqJpaEntity, UUID> {
}
