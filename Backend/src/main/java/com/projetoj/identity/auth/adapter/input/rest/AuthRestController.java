package com.projetoj.identity.auth.adapter.input.rest;

import com.projetoj.identity.auth.adapter.output.persistence.AuthSessionJpaEntity;
import com.projetoj.identity.auth.application.AuthSessionService;
import com.projetoj.identity.permission.adapter.output.persistence.PermissionJpaEntity;
import com.projetoj.identity.role.adapter.output.persistence.RoleJpaEntity;
import com.projetoj.identity.role.adapter.output.persistence.SpringDataRoleJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.SpringDataUserJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.UserJpaEntity;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.ApiRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticacao")
public class AuthRestController {

    public static final String USERNAME_HEADER = "X-Auth-Username";
    public static final String PASSWORD_HEADER = "X-Auth-Password";

    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataRoleJpaRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final ApiRateLimitService rateLimitService;

    public AuthRestController(
            SpringDataUserJpaRepository userRepository,
            SpringDataRoleJpaRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthSessionService authSessionService,
            ApiRateLimitService rateLimitService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    @Transactional
    @Operation(summary = "Autentica via headers X-Auth-Username / X-Auth-Password e emite chave de API (1h)")
    public ResponseEntity<LoginResponse> login(
            @RequestHeader(value = USERNAME_HEADER, required = false) String usernameHeader,
            @RequestHeader(value = PASSWORD_HEADER, required = false) String passwordHeader,
            HttpServletRequest httpRequest
    ) {
        String username = usernameHeader == null ? "" : usernameHeader.trim();
        String password = passwordHeader == null ? "" : passwordHeader;

        rateLimitService.checkLoginAttempt(username, resolveClientIp(httpRequest));

        if (username.isBlank() || password.isBlank()) {
            throw new BusinessException(
                    "INVALID_CREDENTIALS",
                    "Usuario ou senha invalidos",
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        UserJpaEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS",
                        "Usuario ou senha invalidos",
                        HttpStatus.UNAUTHORIZED.value()
                ));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("USER_INACTIVE", "Usuario nao esta ativo", HttpStatus.FORBIDDEN.value());
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(
                    "INVALID_CREDENTIALS",
                    "Usuario ou senha invalidos",
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        RoleJpaEntity role = roleRepository.findById(user.getRole().getId())
                .orElseThrow(() -> new BusinessException(
                        "ROLE_NOT_FOUND",
                        "Perfil do usuario nao encontrado",
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));

        if (!role.isActive()) {
            throw new BusinessException("ROLE_INACTIVE", "Perfil do usuario esta inativo", HttpStatus.FORBIDDEN.value());
        }

        List<String> permissions = role.getPermissions().stream()
                .map(this::toPermissionCode)
                .sorted(Comparator.naturalOrder())
                .toList();

        AuthSessionJpaEntity session = authSessionService.renewSession(user.getId());

        return ResponseEntity.ok(new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                role.getId(),
                role.getName(),
                permissions,
                session.getApiKey(),
                authSessionService.expiresAt(session)
        ));
    }

    private String toPermissionCode(PermissionJpaEntity permission) {
        return permission.getModule().getCode() + ":" + permission.getAction().getCode();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
