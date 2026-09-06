package com.projetoj.shared.logging;

import com.projetoj.identity.auth.adapter.input.rest.AuthRestController;
import com.projetoj.shared.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Persists one {@code api_operation_log} row per {@code /api/v1/**} call, login included.
 * Registered as the outermost filter of the security chain so it also captures rejected requests.
 */
@Component
public class ApiOperationLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiOperationLoggingFilter.class);

    private static final String LOGGED_PATH_PREFIX = "/api/v1/";
    private static final int MAX_BODY_LENGTH = 50 * 1024;
    private static final int MAX_PATH_LENGTH = 500;
    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int MAX_USERNAME_LENGTH = 100;
    private static final int MAX_CLIENT_IP_LENGTH = 64;
    private static final String REDACTED = "***REDACTED***";

    /** Matches any JSON string property whose name contains "password" or "senha". */
    private static final Pattern SECRET_JSON_PROPERTY = Pattern.compile(
            "(\"[A-Za-z0-9_]*(?:[Pp]assword|[Ss]enha|[Ss]ecret|[Tt]oken)[A-Za-z0-9_]*\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");

    private final ApiOperationLogService apiOperationLogService;

    public ApiOperationLoggingFilter(ApiOperationLogService apiOperationLogService) {
        this.apiOperationLogService = apiOperationLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !path.startsWith(LOGGED_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(cachingRequest, response);
        } finally {
            try {
                apiOperationLogService.record(buildCommand(cachingRequest, response));
            } catch (Exception ex) {
                log.warn("Falha ao registrar log de operacao para {} {}", request.getMethod(), request.getRequestURI(), ex);
            }
        }
    }

    private ApiOperationLogService.ApiOperationLogCommand buildCommand(
            ContentCachingRequestWrapper request,
            HttpServletResponse response
    ) {
        return new ApiOperationLogService.ApiOperationLogCommand(
                AuthenticatedUser.resolveUserId(request),
                truncate(request.getHeader(AuthRestController.USERNAME_HEADER), MAX_USERNAME_LENGTH),
                request.getMethod(),
                truncate(request.getRequestURI(), MAX_PATH_LENGTH),
                truncate(request.getQueryString(), MAX_QUERY_LENGTH),
                extractBody(request),
                response.getStatus(),
                truncate(resolveClientIp(request), MAX_CLIENT_IP_LENGTH)
        );
    }

    private String extractBody(ContentCachingRequestWrapper request) {
        byte[] cached = request.getContentAsByteArray();
        if (cached.length == 0) {
            return null;
        }
        String body = new String(cached, resolveCharset(request));
        return truncate(redact(body), MAX_BODY_LENGTH);
    }

    private static Charset resolveCharset(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private static String redact(String body) {
        return SECRET_JSON_PROPERTY.matcher(body).replaceAll("$1\"" + REDACTED + "\"");
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
