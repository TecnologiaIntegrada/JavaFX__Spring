package com.projetoj.purchasing.catalog.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.ProductJpaEntity;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataProductJpaRepository;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataUnitOfMeasureJpaRepository;
import com.projetoj.purchasing.catalog.adapter.output.persistence.UnitOfMeasureJpaEntity;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierJpaEntity;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Cadastro de produtos")
public class ProductRestController {

    private final SpringDataProductJpaRepository productRepository;
    private final SpringDataSupplierJpaRepository supplierRepository;
    private final SpringDataUnitOfMeasureJpaRepository unitRepository;
    private final PurchasingAuditService auditService;

    public ProductRestController(
            SpringDataProductJpaRepository productRepository,
            SpringDataSupplierJpaRepository supplierRepository,
            SpringDataUnitOfMeasureJpaRepository unitRepository,
            PurchasingAuditService auditService
    ) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista produtos")
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(required = false) Boolean active) {
        List<ProductJpaEntity> products = productRepository.findAll(Sort.by("code")).stream()
                .filter(product -> active == null || product.isActive() == active)
                .toList();
        Map<UUID, SupplierJpaEntity> suppliers = suppliersById(products);
        return ResponseEntity.ok(products.stream()
                .map(product -> toResponse(product, suppliers.get(product.getSupplierId())))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca produto por id")
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        ProductJpaEntity product = findProduct(id);
        SupplierJpaEntity supplier = product.getSupplierId() == null
                ? null
                : supplierRepository.findById(product.getSupplierId()).orElse(null);
        return ResponseEntity.ok(toResponse(product, supplier));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria produto")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        if (productRepository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new BusinessException("DUPLICATE_PRODUCT_CODE", "Codigo de produto ja existe", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        UUID userId = AuthenticatedUser.requireUserId();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(userId);

        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(now);
        apply(entity, request, now);
        stampCreate(entity, stamp);

        ProductJpaEntity saved = productRepository.save(entity);
        SupplierJpaEntity supplier = saved.getSupplierId() == null
                ? null
                : supplierRepository.findById(saved.getSupplierId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved, supplier));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualiza produto")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        ProductJpaEntity entity = findProduct(id);
        if (productRepository.existsByCodeIgnoreCaseAndIdNot(request.code().trim(), id)) {
            throw new BusinessException("DUPLICATE_PRODUCT_CODE", "Codigo de produto ja existe", HttpStatus.CONFLICT.value());
        }

        apply(entity, request, Instant.now());
        stampUpdate(entity, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        ProductJpaEntity saved = productRepository.save(entity);
        SupplierJpaEntity supplier = saved.getSupplierId() == null
                ? null
                : supplierRepository.findById(saved.getSupplierId()).orElse(null);
        return ResponseEntity.ok(toResponse(saved, supplier));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Inativa produto")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ProductJpaEntity entity = findProduct(id);
        entity.setActive(false);
        entity.setUpdatedAt(Instant.now());
        stampUpdate(entity, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        productRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    private ProductJpaEntity findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Produto nao encontrado", HttpStatus.NOT_FOUND.value()));
    }

    private Map<UUID, SupplierJpaEntity> suppliersById(List<ProductJpaEntity> products) {
        List<UUID> ids = products.stream()
                .map(ProductJpaEntity::getSupplierId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return supplierRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));
    }

    private void apply(ProductJpaEntity entity, ProductRequest request, Instant now) {
        entity.setCode(request.code().trim());
        entity.setDescription(request.description().trim());
        entity.setUnitOfMeasure(resolveUnitCode(request.unitOfMeasure()));
        entity.setSupplierId(resolveSupplierId(request.supplierId()));
        entity.setManufacturer(null);
        entity.setManufacturerPartNumber(request.manufacturerPartNumber());
        entity.setActive(request.active() == null || request.active());
        entity.setUpdatedAt(now);
    }

    private String resolveUnitCode(String unitOfMeasure) {
        String code = unitOfMeasure == null ? "" : unitOfMeasure.trim();
        UnitOfMeasureJpaEntity unit = unitRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException(
                        "UNIT_NOT_FOUND",
                        "Unidade de medida nao encontrada: " + code,
                        HttpStatus.BAD_REQUEST.value()
                ));
        if (!unit.isActive()) {
            throw new BusinessException("UNIT_INACTIVE", "Unidade de medida inativa: " + unit.getCode(), HttpStatus.BAD_REQUEST.value());
        }
        return unit.getCode();
    }

    private UUID resolveSupplierId(UUID supplierId) {
        if (supplierId == null) {
            return null;
        }
        if (!supplierRepository.existsById(supplierId)) {
            throw new BusinessException("SUPPLIER_NOT_FOUND", "Fornecedor nao encontrado", HttpStatus.NOT_FOUND.value());
        }
        return supplierId;
    }

    private static void stampCreate(ProductJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(),
                stamp.createdByName(),
                entity::setCreatedBy,
                entity::setCreatedByName,
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static void stampUpdate(ProductJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(),
                stamp.updatedByName(),
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static ProductResponse toResponse(ProductJpaEntity entity, SupplierJpaEntity supplier) {
        String supplierName = null;
        if (supplier != null) {
            supplierName = supplier.getTradeName() != null && !supplier.getTradeName().isBlank()
                    ? supplier.getTradeName()
                    : supplier.getLegalName();
        }
        return new ProductResponse(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getUnitOfMeasure(),
                entity.getSupplierId(),
                supplierName,
                entity.getManufacturerPartNumber(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getCreatedByName(),
                entity.getUpdatedBy(),
                entity.getUpdatedByName()
        );
    }

    public record ProductRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 250) String description,
            @NotBlank @Size(max = 20) String unitOfMeasure,
            UUID supplierId,
            @Size(max = 100) String manufacturerPartNumber,
            Boolean active
    ) {
    }

    public record ProductResponse(
            UUID id,
            String code,
            String description,
            String unitOfMeasure,
            UUID supplierId,
            String supplierName,
            String manufacturerPartNumber,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
