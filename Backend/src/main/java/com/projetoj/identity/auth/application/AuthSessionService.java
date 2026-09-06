package com.projetoj.identity.auth.application;

import com.projetoj.identity.auth.adapter.output.persistence.AuthSessionJpaEntity;
import com.projetoj.identity.auth.adapter.output.persistence.SpringDataAuthSessionJpaRepository;
import com.projetoj.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthSessionService {

    public static final Duration SESSION_TTL = Duration.ofHours(1);

    private final SpringDataAuthSessionJpaRepository sessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthSessionService(SpringDataAuthSessionJpaRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public AuthSessionJpaEntity renewSession(UUID userId) {
        sessionRepository.deleteByUserId(userId);

        AuthSessionJpaEntity session = new AuthSessionJpaEntity();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setApiKey(generateApiKey());
        session.setCreatedAt(Instant.now());
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public AuthSessionJpaEntity requireValidSession(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    "AUTH_REQUIRED",
                    "Chave de autenticacao ausente",
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        AuthSessionJpaEntity session = sessionRepository.findByApiKey(apiKey.trim())
                .orElseThrow(() -> new BusinessException(
                        "AUTH_REQUIRED",
                        "Chave de autenticacao invalida",
                        HttpStatus.UNAUTHORIZED.value()
                ));

        Instant expiresAt = session.getCreatedAt().plus(SESSION_TTL);
        if (Instant.now().isAfter(expiresAt)) {
            throw new BusinessException(
                    "AUTH_SESSION_EXPIRED",
                    "Tempo de autenticacao excedido. Faca login novamente.",
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        return session;
    }

    @Transactional
    public void revokeByApiKey(String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            sessionRepository.deleteByApiKey(apiKey.trim());
        }
    }

    public Instant expiresAt(AuthSessionJpaEntity session) {
        return session.getCreatedAt().plus(SESSION_TTL);
    }

    private String generateApiKey() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
