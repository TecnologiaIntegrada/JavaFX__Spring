package com.projetoj.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoj.identity.auth.adapter.output.persistence.AuthSessionJpaEntity;
import com.projetoj.identity.auth.application.AuthSessionService;
import com.projetoj.shared.exception.ApiErrorResponse;
import com.projetoj.shared.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    /**
     * Survives the {@code SecurityContext} cleanup below, so outer filters can still tell who called.
     */
    public static final String AUTH_USER_ID_ATTRIBUTE = "com.projetoj.auth.userId";

    private final AuthSessionService authSessionService;
    private final ApiRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(
            AuthSessionService authSessionService,
            ApiRateLimitService rateLimitService,
            ObjectMapper objectMapper
    ) {
        this.authSessionService = authSessionService;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String apiKey = extractApiKey(request);
            AuthSessionJpaEntity session = authSessionService.requireValidSession(apiKey);
            rateLimitService.checkAuthenticatedUser(session.getUserId());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    session.getUserId(),
                    null,
                    List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(AUTH_USER_ID_ATTRIBUTE, session.getUserId());
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            writeError(response, request, ex);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractApiKey(HttpServletRequest request) {
        String headerKey = request.getHeader(API_KEY_HEADER);
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, BusinessException ex)
            throws IOException {
        response.setStatus(ex.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(),
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        ));
    }
}
