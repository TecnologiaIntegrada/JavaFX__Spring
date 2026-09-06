package com.projetoj.shared.security;

import com.projetoj.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Reads the authenticated user id, which {@link ApiKeyAuthenticationFilter} publishes both on the
 * {@code SecurityContext} and as a request attribute. The attribute outlives the security context,
 * which is cleared as soon as the authentication filter unwinds.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    /** Current user id, or {@code null} when the call is anonymous. */
    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof UUID userId ? userId : null;
    }

    /** Current user id, failing with 401 when the call is anonymous. */
    public static UUID requireUserId() {
        UUID userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(
                    "UNAUTHENTICATED",
                    "Usuario autenticado nao identificado",
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
        return userId;
    }

    /** Request attribute first, security context as fallback. */
    public static UUID resolveUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(ApiKeyAuthenticationFilter.AUTH_USER_ID_ATTRIBUTE);
        if (attribute instanceof UUID userId) {
            return userId;
        }
        return currentUserId();
    }
}
