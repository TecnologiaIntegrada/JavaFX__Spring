package com.projetoj.shared.logging;

import com.projetoj.identity.user.adapter.output.persistence.SpringDataUserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiOperationLogService {

    private static final int USERNAME_CACHE_LIMIT = 500;

    private final SpringDataApiOperationLogJpaRepository logRepository;
    private final SpringDataUserJpaRepository userRepository;
    private final Map<UUID, String> usernameCache = new ConcurrentHashMap<>();

    public ApiOperationLogService(
            SpringDataApiOperationLogJpaRepository logRepository,
            SpringDataUserJpaRepository userRepository
    ) {
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    /**
     * Runs in its own transaction so a rolled back request still leaves an audit trail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ApiOperationLogCommand command) {
        ApiOperationLogJpaEntity entity = new ApiOperationLogJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(command.userId());
        entity.setUsername(resolveUsername(command.userId(), command.fallbackUsername()));
        entity.setHttpMethod(command.httpMethod());
        entity.setRequestPath(command.requestPath());
        entity.setQueryString(command.queryString());
        entity.setRequestBody(command.requestBody());
        entity.setResponseStatus(command.responseStatus());
        entity.setClientIp(command.clientIp());
        entity.setOccurredAt(Instant.now());
        logRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ApiOperationLogJpaEntity> findRecent(UUID userId, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        return userId == null
                ? logRepository.findAllByOrderByOccurredAtDesc(page)
                : logRepository.findAllByUserIdOrderByOccurredAtDesc(userId, page);
    }

    private String resolveUsername(UUID userId, String fallbackUsername) {
        if (userId == null) {
            return fallbackUsername;
        }
        String cached = usernameCache.get(userId);
        if (cached != null) {
            return cached;
        }
        String username = userRepository.findById(userId)
                .map(user -> user.getUsername())
                .orElse(fallbackUsername);
        if (username != null) {
            if (usernameCache.size() >= USERNAME_CACHE_LIMIT) {
                usernameCache.clear();
            }
            usernameCache.put(userId, username);
        }
        return username;
    }

    public record ApiOperationLogCommand(
            UUID userId,
            String fallbackUsername,
            String httpMethod,
            String requestPath,
            String queryString,
            String requestBody,
            Integer responseStatus,
            String clientIp
    ) {
    }
}
