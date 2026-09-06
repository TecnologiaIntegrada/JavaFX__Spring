package com.projetoj.identity.user.adapter.input.rest;

import com.projetoj.identity.role.adapter.output.persistence.RoleJpaEntity;
import com.projetoj.identity.role.adapter.output.persistence.SpringDataRoleJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.SpringDataUserJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.UserJpaEntity;
import com.projetoj.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Cadastro de usuarios")
public class UserRestController {

    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataRoleJpaRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRestController(
            SpringDataUserJpaRepository userRepository,
            SpringDataRoleJpaRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userRepository.findAll(Sort.by("username")).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca usuario por id")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findUser(id)));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria usuario")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException("DUPLICATE_USERNAME", "Username already exists", HttpStatus.CONFLICT.value());
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already exists", HttpStatus.CONFLICT.value());
        }

        RoleJpaEntity role = findRole(request.roleId());
        LocalDateTime now = LocalDateTime.now();
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUsername(request.username().trim());
        entity.setFullName(request.fullName().trim());
        entity.setEmail(request.email().trim().toLowerCase());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setStatus(request.status());
        entity.setRole(role);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(userRepository.save(entity)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualiza usuario")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        UserJpaEntity entity = findUser(id);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already exists", HttpStatus.CONFLICT.value());
        }

        entity.setFullName(request.fullName().trim());
        entity.setEmail(request.email().trim().toLowerCase());
        entity.setStatus(request.status());
        entity.setRole(findRole(request.roleId()));
        if (request.password() != null && !request.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        entity.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(toResponse(userRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Exclui usuario")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UserJpaEntity entity = findUser(id);
        if ("admin".equalsIgnoreCase(entity.getUsername())) {
            throw new BusinessException("CANNOT_DELETE_ADMIN", "Usuario admin nao pode ser excluido", HttpStatus.CONFLICT.value());
        }
        userRepository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    private UserJpaEntity findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND.value()));
    }

    private RoleJpaEntity findRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Role not found", HttpStatus.NOT_FOUND.value()));
    }

    private UserResponse toResponse(UserJpaEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getRole().getId(),
                entity.getRole().getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
