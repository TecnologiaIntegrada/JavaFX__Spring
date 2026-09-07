package com.projetoj.purchasing.requisition.adapter.input.rest;

import com.projetoj.identity.user.adapter.output.persistence.SpringDataUserJpaRepository;
import com.projetoj.identity.user.adapter.output.persistence.UserJpaEntity;
import com.projetoj.purchasing.catalog.adapter.output.persistence.ProductJpaEntity;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataProductJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionApprovalJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionItemJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionApprovalJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionItemJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionJpaRepository;
import com.projetoj.purchasing.shared.DocumentSequenceService;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchase-requisitions")
@Tag(name = "Purchase Requisitions", description = "Requisicoes de compra")
public class PurchaseRequisitionRestController {

    private final SpringDataPurchaseRequisitionJpaRepository requisitionRepository;
    private final SpringDataPurchaseRequisitionItemJpaRepository itemRepository;
    private final SpringDataPurchaseRequisitionApprovalJpaRepository approvalRepository;
    private final SpringDataProductJpaRepository productRepository;
    private final SpringDataUserJpaRepository userRepository;
    private final DocumentSequenceService documentSequenceService;
    private final PurchasingAuditService auditService;

    public PurchaseRequisitionRestController(
            SpringDataPurchaseRequisitionJpaRepository requisitionRepository,
            SpringDataPurchaseRequisitionItemJpaRepository itemRepository,
            SpringDataPurchaseRequisitionApprovalJpaRepository approvalRepository,
            SpringDataProductJpaRepository productRepository,
            SpringDataUserJpaRepository userRepository,
            DocumentSequenceService documentSequenceService,
            PurchasingAuditService auditService
    ) {
        this.requisitionRepository = requisitionRepository;
        this.itemRepository = itemRepository;
        this.approvalRepository = approvalRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.documentSequenceService = documentSequenceService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista requisicoes de compra")
    public ResponseEntity<List<RequisitionResponse>> list(@RequestParam(required = false) String status) {
        List<PurchaseRequisitionJpaEntity> requisitions = requisitionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "requisitionNumber")).stream()
                .filter(requisition -> status == null || status.equalsIgnoreCase(requisition.getStatus()))
                .toList();

        if (requisitions.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<PurchaseRequisitionItemJpaEntity> items = itemRepository.findAllByRequisitionIdInOrderByLineNumberAsc(
                requisitions.stream().map(PurchaseRequisitionJpaEntity::getId).toList());
        Map<UUID, String> requesters = requesterNames(requisitions);
        Map<UUID, ProductJpaEntity> products = productsById(items);

        return ResponseEntity.ok(requisitions.stream()
                .map(requisition -> toResponse(
                        requisition,
                        items.stream().filter(item -> item.getRequisitionId().equals(requisition.getId())).toList(),
                        requesters,
                        products))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca requisicao com itens")
    public ResponseEntity<RequisitionResponse> findById(@PathVariable UUID id) {
        PurchaseRequisitionJpaEntity requisition = findRequisition(id);
        List<PurchaseRequisitionItemJpaEntity> items = itemRepository.findAllByRequisitionIdOrderByLineNumberAsc(id);
        return ResponseEntity.ok(toResponse(
                requisition,
                items,
                requesterNames(List.of(requisition)),
                productsById(items)));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria requisicao de compra com itens")
    public ResponseEntity<RequisitionResponse> create(@Valid @RequestBody RequisitionRequest request) {
        UUID requesterId = AuthenticatedUser.requireUserId();
        Instant now = Instant.now();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(requesterId);

        PurchaseRequisitionJpaEntity requisition = new PurchaseRequisitionJpaEntity();
        requisition.setId(UUID.randomUUID());
        requisition.setRequisitionNumber(documentSequenceService.nextRequisitionNumber());
        requisition.setRequesterUserId(requesterId);
        requisition.setStatus(PurchaseRequisitionJpaEntity.STATUS_DRAFT);
        requisition.setCreatedAt(now);
        apply(requisition, request, now);
        stampCreate(requisition, stamp);
        requisitionRepository.save(requisition);

        List<PurchaseRequisitionItemJpaEntity> items = replaceItems(requisition.getId(), request.items(), now, stamp);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(
                requisition,
                items,
                requesterNames(List.of(requisition)),
                productsById(items)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualiza requisicao em rascunho")
    public ResponseEntity<RequisitionResponse> update(@PathVariable UUID id, @Valid @RequestBody RequisitionRequest request) {
        PurchaseRequisitionJpaEntity requisition = findRequisition(id);
        if (!PurchaseRequisitionJpaEntity.STATUS_DRAFT.equals(requisition.getStatus())) {
            throw new BusinessException("REQUISITION_NOT_EDITABLE", "Somente requisicoes em rascunho podem ser alteradas", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        apply(requisition, request, now);
        stampUpdate(requisition, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        requisitionRepository.save(requisition);

        List<PurchaseRequisitionItemJpaEntity> items = request.items() == null
                ? itemRepository.findAllByRequisitionIdOrderByLineNumberAsc(id)
                : replaceItems(id, request.items(), now, auditService.stampForCreate(AuthenticatedUser.requireUserId()));

        return ResponseEntity.ok(toResponse(
                requisition,
                items,
                requesterNames(List.of(requisition)),
                productsById(items)));
    }

    @PostMapping("/{id}/submit")
    @Transactional
    @Operation(summary = "Envia requisicao para aprovacao")
    public ResponseEntity<RequisitionResponse> submit(@PathVariable UUID id) {
        PurchaseRequisitionJpaEntity requisition = findRequisition(id);
        if (!PurchaseRequisitionJpaEntity.STATUS_DRAFT.equals(requisition.getStatus())) {
            throw new BusinessException("REQUISITION_NOT_SUBMITTABLE", "Somente requisicoes em rascunho podem ser enviadas", HttpStatus.CONFLICT.value());
        }

        List<PurchaseRequisitionItemJpaEntity> items = itemRepository.findAllByRequisitionIdOrderByLineNumberAsc(id);
        if (items.isEmpty()) {
            throw new BusinessException("REQUISITION_WITHOUT_ITEMS", "Requisicao sem itens nao pode ser enviada", HttpStatus.BAD_REQUEST.value());
        }

        Instant now = Instant.now();
        requisition.setStatus(PurchaseRequisitionJpaEntity.STATUS_IN_APPROVAL);
        requisition.setSubmittedAt(now);
        requisition.setUpdatedAt(now);
        stampUpdate(requisition, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        requisitionRepository.save(requisition);

        if (approvalRepository.findAllByRequisitionIdOrderByApprovalLevelAsc(id).isEmpty()) {
            PurchasingAuditService.AuditStamp approvalStamp = auditService.stampForCreate(AuthenticatedUser.requireUserId());
            PurchaseRequisitionApprovalJpaEntity approval = new PurchaseRequisitionApprovalJpaEntity();
            approval.setId(UUID.randomUUID());
            approval.setRequisitionId(id);
            approval.setApprovalLevel(1);
            approval.setApproverUserId(resolveApprover());
            approval.setStatus(PurchaseRequisitionApprovalJpaEntity.STATUS_PENDING);
            approval.setCreatedAt(now);
            stampCreate(approval, approvalStamp);
            approvalRepository.save(approval);
        }

        return ResponseEntity.ok(toResponse(
                requisition,
                items,
                requesterNames(List.of(requisition)),
                productsById(items)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Exclui requisicao em rascunho")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        PurchaseRequisitionJpaEntity requisition = findRequisition(id);
        if (!PurchaseRequisitionJpaEntity.STATUS_DRAFT.equals(requisition.getStatus())) {
            throw new BusinessException("REQUISITION_NOT_DELETABLE", "Somente requisicoes em rascunho podem ser excluidas", HttpStatus.CONFLICT.value());
        }
        itemRepository.deleteAllByRequisitionId(id);
        requisitionRepository.delete(requisition);
        return ResponseEntity.noContent().build();
    }

    /** First active ADMIN user, falling back to any active user so the MVP flow never dead-ends. */
    private UUID resolveApprover() {
        List<UserJpaEntity> admins = userRepository.findActiveByRoleName("ADMIN");
        if (!admins.isEmpty()) {
            return admins.get(0).getId();
        }
        List<UserJpaEntity> anyActive = userRepository.findAllActive();
        if (anyActive.isEmpty()) {
            throw new BusinessException("APPROVER_NOT_FOUND", "Nenhum usuario ativo disponivel para aprovacao", HttpStatus.CONFLICT.value());
        }
        return anyActive.get(0).getId();
    }

    private List<PurchaseRequisitionItemJpaEntity> replaceItems(
            UUID requisitionId,
            List<ItemRequest> requests,
            Instant now,
            PurchasingAuditService.AuditStamp stamp
    ) {
        itemRepository.deleteAllByRequisitionId(requisitionId);
        itemRepository.flush();

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<PurchaseRequisitionItemJpaEntity> entities = new ArrayList<>();
        int lineNumber = 1;
        for (ItemRequest request : requests) {
            if (request.quantity().signum() <= 0) {
                throw new BusinessException("INVALID_ITEM_QUANTITY", "Quantidade deve ser maior que zero", HttpStatus.BAD_REQUEST.value());
            }

            ProductJpaEntity product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Produto nao encontrado: " + request.productId(), HttpStatus.NOT_FOUND.value()));

            PurchaseRequisitionItemJpaEntity item = new PurchaseRequisitionItemJpaEntity();
            item.setId(UUID.randomUUID());
            item.setRequisitionId(requisitionId);
            item.setLineNumber(request.lineNumber() == null ? lineNumber : request.lineNumber());
            item.setProductId(request.productId());
            item.setDescriptionOverride(request.descriptionOverride());
            item.setQuantity(request.quantity());
            item.setUom(product.getUnitOfMeasure());
            item.setRequiredDate(request.requiredDate());
            item.setProjectId(request.projectId());
            item.setCostCenterId(request.costCenterId());
            item.setSuggestedSupplierId(request.suggestedSupplierId());
            item.setTechnicalNotes(request.technicalNotes());
            item.setOrderedQuantity(BigDecimal.ZERO);
            item.setStatus("OPEN");
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            stampCreate(item, stamp);
            entities.add(item);
            lineNumber++;
        }

        return itemRepository.saveAll(entities);
    }

    private PurchaseRequisitionJpaEntity findRequisition(UUID id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("REQUISITION_NOT_FOUND", "Requisicao nao encontrada", HttpStatus.NOT_FOUND.value()));
    }

    private Map<UUID, String> requesterNames(List<PurchaseRequisitionJpaEntity> requisitions) {
        List<UUID> ids = requisitions.stream()
                .map(PurchaseRequisitionJpaEntity::getRequesterUserId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getFullName));
    }

    private Map<UUID, ProductJpaEntity> productsById(List<PurchaseRequisitionItemJpaEntity> items) {
        List<UUID> ids = items.stream()
                .map(PurchaseRequisitionItemJpaEntity::getProductId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));
    }

    private static void apply(PurchaseRequisitionJpaEntity entity, RequisitionRequest request, Instant now) {
        entity.setDepartmentId(request.departmentId());
        entity.setRequestDate(request.requestDate() == null ? LocalDate.now() : request.requestDate());
        entity.setPriority(request.priority().trim().toUpperCase());
        entity.setDemandSource(request.demandSource().trim().toUpperCase());
        entity.setProjectId(request.projectId());
        entity.setCostCenterId(request.costCenterId());
        entity.setJustification(request.justification().trim());
        entity.setUpdatedAt(now);
    }

    private static void stampCreate(PurchaseRequisitionJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(),
                stamp.createdByName(),
                entity::setCreatedBy,
                entity::setCreatedByName,
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static void stampUpdate(PurchaseRequisitionJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(),
                stamp.updatedByName(),
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static void stampCreate(PurchaseRequisitionItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(),
                stamp.createdByName(),
                entity::setCreatedBy,
                entity::setCreatedByName,
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static void stampCreate(PurchaseRequisitionApprovalJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(),
                stamp.createdByName(),
                entity::setCreatedBy,
                entity::setCreatedByName,
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static RequisitionResponse toResponse(
            PurchaseRequisitionJpaEntity entity,
            List<PurchaseRequisitionItemJpaEntity> items,
            Map<UUID, String> requesters,
            Map<UUID, ProductJpaEntity> products
    ) {
        return new RequisitionResponse(
                entity.getId(),
                entity.getRequisitionNumber(),
                entity.getRequesterUserId(),
                requesters.get(entity.getRequesterUserId()),
                entity.getDepartmentId(),
                entity.getRequestDate(),
                entity.getPriority(),
                entity.getDemandSource(),
                entity.getProjectId(),
                entity.getCostCenterId(),
                entity.getJustification(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                entity.getApprovedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getCreatedByName(),
                entity.getUpdatedBy(),
                entity.getUpdatedByName(),
                items.stream().map(item -> toItemResponse(item, products.get(item.getProductId()))).toList()
        );
    }

    private static ItemResponse toItemResponse(PurchaseRequisitionItemJpaEntity item, ProductJpaEntity product) {
        return new ItemResponse(
                item.getId(),
                item.getRequisitionId(),
                item.getLineNumber(),
                item.getProductId(),
                product == null ? null : product.getCode(),
                product == null ? null : product.getDescription(),
                item.getDescriptionOverride(),
                item.getQuantity(),
                item.getUom(),
                item.getRequiredDate(),
                item.getProjectId(),
                item.getCostCenterId(),
                item.getSuggestedSupplierId(),
                item.getTechnicalNotes(),
                item.getOrderedQuantity(),
                item.getStatus(),
                item.getCreatedBy(),
                item.getCreatedByName(),
                item.getUpdatedBy(),
                item.getUpdatedByName()
        );
    }

    public record RequisitionRequest(
            UUID departmentId,
            LocalDate requestDate,
            @NotBlank @Size(max = 20) String priority,
            @NotBlank @Size(max = 30) String demandSource,
            UUID projectId,
            UUID costCenterId,
            @NotBlank String justification,
            @NotEmpty List<@Valid ItemRequest> items
    ) {
    }

    public record ItemRequest(
            Integer lineNumber,
            @NotNull UUID productId,
            @Size(max = 500) String descriptionOverride,
            @NotNull BigDecimal quantity,
            @NotBlank @Size(max = 20) String uom,
            @NotNull LocalDate requiredDate,
            UUID projectId,
            UUID costCenterId,
            UUID suggestedSupplierId,
            String technicalNotes
    ) {
    }

    public record RequisitionResponse(
            UUID id,
            String requisitionNumber,
            UUID requesterUserId,
            String requesterName,
            UUID departmentId,
            LocalDate requestDate,
            String priority,
            String demandSource,
            UUID projectId,
            UUID costCenterId,
            String justification,
            String status,
            Instant submittedAt,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName,
            List<ItemResponse> items
    ) {
    }

    public record ItemResponse(
            UUID id,
            UUID requisitionId,
            Integer lineNumber,
            UUID productId,
            String productCode,
            String productDescription,
            String descriptionOverride,
            BigDecimal quantity,
            String uom,
            LocalDate requiredDate,
            UUID projectId,
            UUID costCenterId,
            UUID suggestedSupplierId,
            String technicalNotes,
            BigDecimal orderedQuantity,
            String status,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
