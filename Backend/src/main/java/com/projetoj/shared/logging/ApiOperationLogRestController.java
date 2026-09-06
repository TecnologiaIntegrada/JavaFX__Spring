package com.projetoj.shared.logging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-logs")
@Tag(name = "API Logs", description = "Log de operacoes da API")
public class ApiOperationLogRestController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final ApiOperationLogService apiOperationLogService;

    public ApiOperationLogRestController(ApiOperationLogService apiOperationLogService) {
        this.apiOperationLogService = apiOperationLogService;
    }

    @GetMapping
    @Operation(summary = "Lista as operacoes registradas, mais recentes primeiro")
    public ResponseEntity<List<ApiOperationLogResponse>> list(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Integer limit
    ) {
        int effectiveLimit = Math.min(limit == null || limit < 1 ? DEFAULT_LIMIT : limit, MAX_LIMIT);
        return ResponseEntity.ok(apiOperationLogService.findRecent(userId, effectiveLimit).stream()
                .map(ApiOperationLogRestController::toResponse)
                .toList());
    }

    private static ApiOperationLogResponse toResponse(ApiOperationLogJpaEntity entity) {
        return new ApiOperationLogResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getHttpMethod(),
                entity.getRequestPath(),
                entity.getQueryString(),
                entity.getRequestBody(),
                entity.getResponseStatus(),
                entity.getClientIp(),
                entity.getOccurredAt()
        );
    }

    public record ApiOperationLogResponse(
            UUID id,
            UUID userId,
            String username,
            String httpMethod,
            String requestPath,
            String queryString,
            String requestBody,
            Integer responseStatus,
            String clientIp,
            Instant occurredAt
    ) {
    }
}
