package com.projetoj.identity.auth.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, UUID> {

    Optional<AuthSessionJpaEntity> findByApiKey(String apiKey);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("DELETE FROM AuthSessionJpaEntity s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("DELETE FROM AuthSessionJpaEntity s WHERE s.apiKey = :apiKey")
    void deleteByApiKey(@Param("apiKey") String apiKey);
}
