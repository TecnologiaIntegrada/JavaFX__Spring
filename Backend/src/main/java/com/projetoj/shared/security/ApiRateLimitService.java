package com.projetoj.shared.security;

import com.projetoj.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita volume consecutivo de chamadas por usuario autenticado e tentativas de login.
 * Janela deslizante em memoria (adequado para instancia unica / ambiente local).
 */
@Service
public class ApiRateLimitService {

    /** Maximo de chamadas autenticadas na janela longa (10s). */
    static final int MAX_REQUESTS_PER_WINDOW = 30;
    static final long WINDOW_MS = 10_000L;

    /** Maximo de chamadas no mesmo segundo (bloqueia rajada consecutiva). */
    static final int MAX_BURST_PER_SECOND = 8;

    /** Maximo de tentativas de login por usuario+IP por minuto. */
    static final int MAX_LOGIN_ATTEMPTS = 5;
    static final long LOGIN_WINDOW_MS = 60_000L;

    private final Map<UUID, Deque<Long>> userHits = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> loginHits = new ConcurrentHashMap<>();

    public void checkAuthenticatedUser(UUID userId) {
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Deque<Long> hits = userHits.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (hits) {
            prune(hits, now, WINDOW_MS);
            long oneSecondAgo = now - 1_000L;
            int burst = 0;
            for (Long ts : hits) {
                if (ts >= oneSecondAgo) {
                    burst++;
                }
            }
            if (burst >= MAX_BURST_PER_SECOND || hits.size() >= MAX_REQUESTS_PER_WINDOW) {
                throw new BusinessException(
                        "RATE_LIMIT_EXCEEDED",
                        "Volume de chamadas excessivo. Aguarde alguns segundos e tente novamente.",
                        HttpStatus.TOO_MANY_REQUESTS.value()
                );
            }
            hits.addLast(now);
        }
    }

    public void checkLoginAttempt(String username, String clientIp) {
        String key = (username == null ? "" : username.trim().toLowerCase()) + "|" + (clientIp == null ? "unknown" : clientIp);
        long now = System.currentTimeMillis();
        Deque<Long> hits = loginHits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            prune(hits, now, LOGIN_WINDOW_MS);
            if (hits.size() >= MAX_LOGIN_ATTEMPTS) {
                throw new BusinessException(
                        "RATE_LIMIT_EXCEEDED",
                        "Muitas tentativas de login. Aguarde um minuto e tente novamente.",
                        HttpStatus.TOO_MANY_REQUESTS.value()
                );
            }
            hits.addLast(now);
        }
    }

    private static void prune(Deque<Long> hits, long now, long windowMs) {
        while (!hits.isEmpty() && now - hits.peekFirst() > windowMs) {
            hits.pollFirst();
        }
    }
}
