package com.projetoj.shared.logging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataApiOperationLogJpaRepository extends JpaRepository<ApiOperationLogJpaEntity, UUID> {

    List<ApiOperationLogJpaEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);

    List<ApiOperationLogJpaEntity> findAllByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);
}
