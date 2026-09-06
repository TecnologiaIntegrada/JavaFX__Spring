package com.projetoj.purchasing.shared;

import com.projetoj.identity.user.adapter.output.persistence.SpringDataUserJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.UserJpaEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PurchasingAuditService {

    private final SpringDataUserJpaRepository userRepository;
    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    public PurchasingAuditService(SpringDataUserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuditStamp stampForCreate(UUID userId) {
        String name = resolveName(userId);
        return new AuditStamp(userId, name, userId, name);
    }

    public AuditStamp stampForUpdate(UUID userId) {
        return new AuditStamp(null, null, userId, resolveName(userId));
    }

    public String resolveName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return nameCache.computeIfAbsent(userId, id -> userRepository.findById(id)
                .map(user -> {
                    if (user.getFullName() != null && !user.getFullName().isBlank()) {
                        return user.getFullName().trim();
                    }
                    if (user.getUsername() != null && !user.getUsername().isBlank()) {
                        return user.getUsername().trim();
                    }
                    return id.toString();
                })
                .orElse(id.toString()));
    }

    public record AuditStamp(
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
