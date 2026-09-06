package com.projetoj.identity.permission.adapter.input.rest;

import com.projetoj.identity.permission.adapter.output.persistence.ModuleJpaEntity;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/modules")
@Tag(name = "Modules", description = "Cadastro de modulos")
public class ModuleRestController {

    private final SpringDataModuleJpaRepository moduleRepository;
    private final SpringDataPermissionJpaRepository permissionRepository;

    public ModuleRestController(
            SpringDataModuleJpaRepository moduleRepository,
            SpringDataPermissionJpaRepository permissionRepository
    ) {
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @Operation(summary = "Lista modulos")
    public ResponseEntity<List<ModuleResponse>> list() {
        return ResponseEntity.ok(moduleRepository.findAll(Sort.by("code")).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca modulo por id")
    public ResponseEntity<ModuleResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findModule(id)));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria modulo")
    public ResponseEntity<ModuleResponse> create(@Valid @RequestBody CreateModuleRequest request) {
        if (moduleRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException("DUPLICATE_MODULE", "Module code already exists", HttpStatus.CONFLICT.value());
        }
        LocalDateTime now = LocalDateTime.now();
        ModuleJpaEntity entity = new ModuleJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode(request.code().trim().toUpperCase());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setActive(Boolean.TRUE.equals(request.active()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(moduleRepository.save(entity)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualiza modulo")
    public ResponseEntity<ModuleResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateModuleRequest request) {
        ModuleJpaEntity entity = findModule(id);
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setActive(Boolean.TRUE.equals(request.active()));
        entity.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(moduleRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Exclui modulo")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ModuleJpaEntity entity = findModule(id);
        if (permissionRepository.countByModule_Id(id) > 0) {
            throw new BusinessException("MODULE_IN_USE", "Module has linked permissions", HttpStatus.CONFLICT.value());
        }
        moduleRepository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    private ModuleJpaEntity findModule(UUID id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("MODULE_NOT_FOUND", "Module not found", HttpStatus.NOT_FOUND.value()));
    }

    private ModuleResponse toResponse(ModuleJpaEntity entity) {
        return new ModuleResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
