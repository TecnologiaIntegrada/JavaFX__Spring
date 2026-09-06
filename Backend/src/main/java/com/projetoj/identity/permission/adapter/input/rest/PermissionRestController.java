package com.projetoj.identity.permission.adapter.input.rest;

import com.projetoj.identity.permission.adapter.output.persistence.ActionJpaEntity;
import com.projetoj.identity.permission.adapter.output.persistence.ModuleJpaEntity;
import com.projetoj.identity.permission.adapter.output.persistence.PermissionJpaEntity;
import com.projetoj.identity.permission.adapter.output.persistence.SpringDataActionJpaRepository;
import com.projetoj.identity.permission.adapter.output.persistence.SpringDataModuleJpaRepository;
import com.projetoj.identity.permission.adapter.output.persistence.SpringDataPermissionJpaRepository;
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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Permissions", description = "Cadastro de permissoes e acoes")
public class PermissionRestController {

    private final SpringDataPermissionJpaRepository permissionRepository;
    private final SpringDataModuleJpaRepository moduleRepository;
    private final SpringDataActionJpaRepository actionRepository;

    public PermissionRestController(
            SpringDataPermissionJpaRepository permissionRepository,
            SpringDataModuleJpaRepository moduleRepository,
            SpringDataActionJpaRepository actionRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.moduleRepository = moduleRepository;
        this.actionRepository = actionRepository;
    }

    @GetMapping("/permissions")
    @Operation(summary = "Lista permissoes (MODULE:ACTION)")
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        List<PermissionResponse> permissions = permissionRepository.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PermissionResponse::code))
                .toList();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/permissions/{id}")
    @Operation(summary = "Busca permissao por id")
    public ResponseEntity<PermissionResponse> findPermission(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findPermissionEntity(id)));
    }

    @PostMapping("/permissions")
    @Transactional
    @Operation(summary = "Cria permissao (module + action)")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        ModuleJpaEntity module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new BusinessException("MODULE_NOT_FOUND", "Module not found", HttpStatus.NOT_FOUND.value()));
        ActionJpaEntity action = actionRepository.findById(request.actionId())
                .orElseThrow(() -> new BusinessException("ACTION_NOT_FOUND", "Action not found", HttpStatus.NOT_FOUND.value()));

        if (permissionRepository.existsByModule_IdAndAction_Id(module.getId(), action.getId())) {
            throw new BusinessException("DUPLICATE_PERMISSION", "Permission already exists", HttpStatus.CONFLICT.value());
        }

        PermissionJpaEntity entity = new PermissionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setModule(module);
        entity.setAction(action);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(permissionRepository.save(entity)));
    }

    @PutMapping("/permissions/{id}")
    @Transactional
    @Operation(summary = "Atualiza permissao (troca module/action)")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePermissionRequest request
    ) {
        PermissionJpaEntity entity = findPermissionEntity(id);
        ModuleJpaEntity module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new BusinessException("MODULE_NOT_FOUND", "Module not found", HttpStatus.NOT_FOUND.value()));
        ActionJpaEntity action = actionRepository.findById(request.actionId())
                .orElseThrow(() -> new BusinessException("ACTION_NOT_FOUND", "Action not found", HttpStatus.NOT_FOUND.value()));

        permissionRepository.findByModule_IdAndAction_Id(module.getId(), action.getId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("DUPLICATE_PERMISSION", "Permission already exists", HttpStatus.CONFLICT.value());
                });

        entity.setModule(module);
        entity.setAction(action);
        return ResponseEntity.ok(toResponse(permissionRepository.save(entity)));
    }

    @DeleteMapping("/permissions/{id}")
    @Transactional
    @Operation(summary = "Exclui permissao")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        PermissionJpaEntity entity = findPermissionEntity(id);
        permissionRepository.deleteRoleLinks(id);
        permissionRepository.deleteUserLinks(id);
        permissionRepository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actions")
    @Operation(summary = "Lista acoes do catalogo")
    public ResponseEntity<List<ActionResponse>> listActions() {
        List<ActionResponse> actions = actionRepository.findAll(Sort.by("code")).stream()
                .map(a -> new ActionResponse(a.getId(), a.getCode(), a.getDescription()))
                .toList();
        return ResponseEntity.ok(actions);
    }

    private PermissionJpaEntity findPermissionEntity(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PERMISSION_NOT_FOUND", "Permission not found", HttpStatus.NOT_FOUND.value()));
    }

    private PermissionResponse toResponse(PermissionJpaEntity entity) {
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
