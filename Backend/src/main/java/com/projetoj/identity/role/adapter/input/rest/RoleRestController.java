package com.projetoj.identity.role.adapter.input.rest;

import com.projetoj.identity.permission.adapter.input.rest.PermissionResponse;
import com.projetoj.identity.permission.adapter.output.persistence.PermissionJpaEntity;
import com.projetoj.identity.permission.adapter.output.persistence.SpringDataPermissionJpaRepository;
import com.projetoj.identity.role.adapter.output.persistence.RoleJpaEntity;
import com.projetoj.identity.role.adapter.output.persistence.SpringDataRoleJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.SpringDataUserJpaRepository;
import com.projetoj.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Cadastro de perfis e permissoes do perfil")
public class RoleRestController {

    private final SpringDataRoleJpaRepository roleRepository;
    private final SpringDataPermissionJpaRepository permissionRepository;
    private final SpringDataUserJpaRepository userRepository;

    public RoleRestController(
            SpringDataRoleJpaRepository roleRepository,
            SpringDataPermissionJpaRepository permissionRepository,
            SpringDataUserJpaRepository userRepository
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Lista perfis")
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleRepository.findAll(Sort.by("name")).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca perfil por id")
    public ResponseEntity<RoleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findRole(id)));
    }

    @GetMapping("/{id}/permissions")
    @Transactional(readOnly = true)
    @Operation(summary = "Lista permissoes filhas do perfil")
    public ResponseEntity<List<PermissionResponse>> listPermissions(@PathVariable UUID id) {
        RoleJpaEntity role = findRole(id);
        List<PermissionResponse> permissions = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .sorted(Comparator.comparing(PermissionResponse::code))
                .toList();
        return ResponseEntity.ok(permissions);
    }

    @PutMapping("/{id}/permissions")
    @Transactional
    @Operation(summary = "Substitui permissoes do perfil")
    public ResponseEntity<List<PermissionResponse>> updatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        RoleJpaEntity role = findRole(id);
        Set<PermissionJpaEntity> permissions = new HashSet<>();
        for (UUID permissionId : request.permissionIds()) {
            PermissionJpaEntity permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new BusinessException(
                            "PERMISSION_NOT_FOUND",
                            "Permission not found: " + permissionId,
                            HttpStatus.NOT_FOUND.value()
                    ));
            permissions.add(permission);
        }
        role.setPermissions(permissions);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        List<PermissionResponse> response = permissions.stream()
                .map(this::toPermissionResponse)
                .sorted(Comparator.comparing(PermissionResponse::code))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria perfil")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        if (roleRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("DUPLICATE_ROLE", "Role name already exists", HttpStatus.CONFLICT.value());
        }
        LocalDateTime now = LocalDateTime.now();
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.name().trim().toUpperCase());
        entity.setDescription(request.description());
        entity.setActive(Boolean.TRUE.equals(request.active()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(roleRepository.save(entity)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualiza perfil")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        RoleJpaEntity entity = findRole(id);
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new BusinessException("DUPLICATE_ROLE", "Role name already exists", HttpStatus.CONFLICT.value());
        }
        entity.setName(request.name().trim().toUpperCase());
        entity.setDescription(request.description());
        entity.setActive(Boolean.TRUE.equals(request.active()));
        entity.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(roleRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Exclui perfil")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        RoleJpaEntity role = findRole(id);
        if ("ADMIN".equalsIgnoreCase(role.getName())) {
            throw new BusinessException("CANNOT_DELETE_ADMIN_ROLE", "Perfil ADMIN nao pode ser excluido", HttpStatus.CONFLICT.value());
        }
        if (userRepository.countByRole_Id(id) > 0) {
            throw new BusinessException("ROLE_IN_USE", "Role is assigned to users", HttpStatus.CONFLICT.value());
        }
        role.getPermissions().clear();
        roleRepository.save(role);
        roleRepository.delete(role);
        return ResponseEntity.noContent().build();
    }

    private RoleJpaEntity findRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Role not found", HttpStatus.NOT_FOUND.value()));
    }

    private RoleResponse toResponse(RoleJpaEntity entity) {
        return new RoleResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PermissionResponse toPermissionResponse(PermissionJpaEntity entity) {
        String moduleCode = entity.getModule().getCode();
        String actionCode = entity.getAction().getCode();
        return new PermissionResponse(
                entity.getId(),
                moduleCode + ":" + actionCode,
                entity.getModule().getId(),
                moduleCode,
                entity.getModule().getName(),
                entity.getAction().getId(),
                actionCode,
                entity.getAction().getDescription()
        );
    }
}
