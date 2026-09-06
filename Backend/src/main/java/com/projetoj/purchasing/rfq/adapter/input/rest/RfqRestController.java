package com.projetoj.purchasing.rfq.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.ProductJpaEntity;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataProductJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionItemJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionItemJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqItemJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqSupplierJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqItemJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqSupplierJpaRepository;
import com.projetoj.purchasing.shared.DocumentSequenceService;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierJpaEntity;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rfqs")
@Tag(name = "RFQs", description = "Cotacoes (request for quotation)")
public class RfqRestController {

    private final SpringDataRfqJpaRepository rfqRepository;
    private final SpringDataRfqItemJpaRepository rfqItemRepository;
    private final SpringDataRfqSupplierJpaRepository rfqSupplierRepository;
    private final SpringDataPurchaseRequisitionJpaRepository requisitionRepository;
    private final SpringDataPurchaseRequisitionItemJpaRepository requisitionItemRepository;
    private final SpringDataSupplierJpaRepository supplierRepository;
    private final SpringDataProductJpaRepository productRepository;
    private final DocumentSequenceService documentSequenceService;
    private final PurchasingAuditService auditService;

    public RfqRestController(
            SpringDataRfqJpaRepository rfqRepository,
            SpringDataRfqItemJpaRepository rfqItemRepository,
            SpringDataRfqSupplierJpaRepository rfqSupplierRepository,
            SpringDataPurchaseRequisitionJpaRepository requisitionRepository,
            SpringDataPurchaseRequisitionItemJpaRepository requisitionItemRepository,
            SpringDataSupplierJpaRepository supplierRepository,
            SpringDataProductJpaRepository productRepository,
            DocumentSequenceService documentSequenceService,
            PurchasingAuditService auditService
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.rfqSupplierRepository = rfqSupplierRepository;
        this.requisitionRepository = requisitionRepository;
        this.requisitionItemRepository = requisitionItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.documentSequenceService = documentSequenceService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista cotacoes")
    public ResponseEntity<List<RfqResponse>> list(@RequestParam(required = false) String status) {
        List<RfqJpaEntity> rfqs = rfqRepository.findAll(Sort.by(Sort.Direction.DESC, "rfqNumber")).stream()
                .filter(rfq -> status == null || status.equalsIgnoreCase(rfq.getStatus()))
                .toList();

        if (rfqs.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<UUID> ids = rfqs.stream().map(RfqJpaEntity::getId).toList();
        List<RfqItemJpaEntity> items = rfqItemRepository.findAllByRfqIdInOrderByLineNumberAsc(ids);
        List<RfqSupplierJpaEntity> invited = rfqSupplierRepository.findAllByRfqIdIn(ids);
        Map<UUID, ProductJpaEntity> products = productsById(items);
        Map<UUID, SupplierJpaEntity> suppliers = suppliersById(invited);

        return ResponseEntity.ok(rfqs.stream()
                .map(rfq -> toResponse(
                        rfq,
                        items.stream().filter(item -> item.getRfqId().equals(rfq.getId())).toList(),
                        invited.stream().filter(link -> link.getRfqId().equals(rfq.getId())).toList(),
                        products,
                        suppliers))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca cotacao com itens e fornecedores")
    public ResponseEntity<RfqResponse> findById(@PathVariable UUID id) {
        RfqJpaEntity rfq = findRfq(id);
        List<RfqItemJpaEntity> items = rfqItemRepository.findAllByRfqIdOrderByLineNumberAsc(id);
        List<RfqSupplierJpaEntity> invited = rfqSupplierRepository.findAllByRfqId(id);
        return ResponseEntity.ok(toResponse(rfq, items, invited, productsById(items), suppliersById(invited)));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria cotacao a partir de uma requisicao aprovada")
    public ResponseEntity<RfqResponse> create(@Valid @RequestBody RfqRequest request) {
        UUID buyerId = AuthenticatedUser.requireUserId();

        PurchaseRequisitionJpaEntity requisition = requisitionRepository.findById(request.requisitionId())
                .orElseThrow(() -> new BusinessException("REQUISITION_NOT_FOUND", "Requisicao nao encontrada", HttpStatus.NOT_FOUND.value()));
        if (!PurchaseRequisitionJpaEntity.STATUS_APPROVED.equals(requisition.getStatus())) {
            throw new BusinessException("REQUISITION_NOT_APPROVED", "Somente requisicoes aprovadas podem gerar cotacao", HttpStatus.CONFLICT.value());
        }

        List<PurchaseRequisitionItemJpaEntity> requisitionItems =
                requisitionItemRepository.findAllByRequisitionIdOrderByLineNumberAsc(requisition.getId());
        if (requisitionItems.isEmpty()) {
            throw new BusinessException("REQUISITION_WITHOUT_ITEMS", "Requisicao sem itens", HttpStatus.BAD_REQUEST.value());
        }

        List<RfqSupplierInput> supplierInputs = resolveSupplierInputs(request);
        List<UUID> supplierIds = supplierInputs.stream().map(RfqSupplierInput::supplierId).distinct().toList();
        List<SupplierJpaEntity> suppliers = supplierRepository.findAllById(supplierIds);
        if (suppliers.size() != supplierIds.size()) {
            throw new BusinessException("SUPPLIER_NOT_FOUND", "Fornecedor informado nao encontrado", HttpStatus.NOT_FOUND.value());
        }

        Instant now = Instant.now();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(buyerId);
        RfqJpaEntity rfq = new RfqJpaEntity();
        rfq.setId(UUID.randomUUID());
        rfq.setRfqNumber(documentSequenceService.nextRfqNumber());
        rfq.setDescription(request.description());
        rfq.setBuyerUserId(buyerId);
        rfq.setIssueDate(LocalDate.now());
        rfq.setResponseDueDate(request.responseDueDate());
        rfq.setDefaultCurrency(request.defaultCurrency() == null ? "BRL" : request.defaultCurrency().trim().toUpperCase());
        rfq.setGeneralTerms(request.generalTerms());
        rfq.setNotes(request.notes());
        rfq.setStatus(RfqJpaEntity.STATUS_DRAFT);
        rfq.setCreatedAt(now);
        rfq.setUpdatedAt(now);
        stampCreate(rfq, stamp);
        rfqRepository.save(rfq);

        List<RfqItemJpaEntity> items = new ArrayList<>();
        int lineNumber = 1;
        for (PurchaseRequisitionItemJpaEntity source : requisitionItems) {
            RfqItemJpaEntity item = new RfqItemJpaEntity();
            item.setId(UUID.randomUUID());
            item.setRfqId(rfq.getId());
            item.setLineNumber(lineNumber++);
            item.setRequisitionItemId(source.getId());
            item.setProductId(source.getProductId());
            item.setDescription(source.getDescriptionOverride());
            item.setQuantity(source.getQuantity());
            item.setUom(source.getUom());
            item.setRequiredDate(source.getRequiredDate());
            item.setTechnicalSpecification(source.getTechnicalNotes());
            item.setStatus("OPEN");
            stampCreate(item, stamp);
            items.add(item);
        }
        List<RfqItemJpaEntity> savedItems = rfqItemRepository.saveAll(items);

        Map<UUID, String> orientationBySupplier = new LinkedHashMap<>();
        for (RfqSupplierInput input : supplierInputs) {
            orientationBySupplier.putIfAbsent(input.supplierId(), input.orientationComments());
        }

        List<RfqSupplierJpaEntity> invited = supplierIds.stream()
                .map(supplierId -> {
                    RfqSupplierJpaEntity link = new RfqSupplierJpaEntity();
                    link.setId(UUID.randomUUID());
                    link.setRfqId(rfq.getId());
                    link.setSupplierId(supplierId);
                    link.setOrientationComments(orientationBySupplier.get(supplierId));
                    link.setStatus(RfqSupplierJpaEntity.STATUS_INVITED);
                    stampCreate(link, stamp);
                    return link;
                })
                .toList();
        List<RfqSupplierJpaEntity> savedSuppliers = rfqSupplierRepository.saveAll(invited);

        requisition.setStatus(PurchaseRequisitionJpaEntity.STATUS_IN_RFQ);
        requisition.setUpdatedAt(now);
        stampUpdate(requisition, auditService.stampForUpdate(buyerId));
        requisitionRepository.save(requisition);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(
                rfq,
                savedItems,
                savedSuppliers,
                productsById(savedItems),
                suppliersById(savedSuppliers)));
    }

    @PostMapping("/{id}/send")
    @Transactional
    @Operation(summary = "Marca cotacao como enviada aos fornecedores")
    public ResponseEntity<RfqResponse> send(@PathVariable UUID id) {
        RfqJpaEntity rfq = findRfq(id);
        if (!RfqJpaEntity.STATUS_DRAFT.equals(rfq.getStatus())) {
            throw new BusinessException("RFQ_NOT_SENDABLE", "Somente cotacoes em rascunho podem ser enviadas", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForUpdate(AuthenticatedUser.requireUserId());
        rfq.setStatus(RfqJpaEntity.STATUS_SENT);
        rfq.setUpdatedAt(now);
        stampUpdate(rfq, stamp);
        rfqRepository.save(rfq);

        List<RfqSupplierJpaEntity> invited = rfqSupplierRepository.findAllByRfqId(id);
        invited.forEach(link -> {
            link.setStatus(RfqSupplierJpaEntity.STATUS_SENT);
            link.setSentAt(now);
            stampUpdate(link, stamp);
        });
        rfqSupplierRepository.saveAll(invited);

        List<RfqItemJpaEntity> items = rfqItemRepository.findAllByRfqIdOrderByLineNumberAsc(id);
        return ResponseEntity.ok(toResponse(rfq, items, invited, productsById(items), suppliersById(invited)));
    }

    private RfqJpaEntity findRfq(UUID id) {
        return rfqRepository.findById(id)
                .orElseThrow(() -> new BusinessException("RFQ_NOT_FOUND", "Cotacao nao encontrada", HttpStatus.NOT_FOUND.value()));
    }

    private Map<UUID, ProductJpaEntity> productsById(List<RfqItemJpaEntity> items) {
        List<UUID> ids = items.stream().map(RfqItemJpaEntity::getProductId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));
    }

    private Map<UUID, SupplierJpaEntity> suppliersById(List<RfqSupplierJpaEntity> links) {
        List<UUID> ids = links.stream().map(RfqSupplierJpaEntity::getSupplierId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return supplierRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));
    }

    private static RfqResponse toResponse(
            RfqJpaEntity rfq,
            List<RfqItemJpaEntity> items,
            List<RfqSupplierJpaEntity> invited,
            Map<UUID, ProductJpaEntity> products,
            Map<UUID, SupplierJpaEntity> suppliers
    ) {
        return new RfqResponse(
                rfq.getId(),
                rfq.getRfqNumber(),
                rfq.getDescription(),
                rfq.getBuyerUserId(),
                rfq.getIssueDate(),
                rfq.getResponseDueDate(),
                rfq.getDefaultCurrency(),
                rfq.getGeneralTerms(),
                rfq.getNotes(),
                rfq.getStatus(),
                rfq.getCreatedAt(),
                rfq.getUpdatedAt(),
                rfq.getCreatedBy(),
                rfq.getCreatedByName(),
                rfq.getUpdatedBy(),
                rfq.getUpdatedByName(),
                items.stream().map(item -> {
                    ProductJpaEntity product = products.get(item.getProductId());
                    return new RfqItemResponse(
                            item.getId(),
                            item.getRfqId(),
                            item.getLineNumber(),
                            item.getRequisitionItemId(),
                            item.getProductId(),
                            product == null ? null : product.getCode(),
                            product == null ? null : product.getDescription(),
                            item.getDescription(),
                            item.getQuantity(),
                            item.getUom(),
                            item.getRequiredDate(),
                            item.getTechnicalSpecification(),
                            item.getStatus(),
                            item.getCreatedBy(),
                            item.getCreatedByName(),
                            item.getUpdatedBy(),
                            item.getUpdatedByName()
                    );
                }).toList(),
                invited.stream().map(link -> {
                    SupplierJpaEntity supplier = suppliers.get(link.getSupplierId());
                    return new RfqSupplierResponse(
                            link.getId(),
                            link.getRfqId(),
                            link.getSupplierId(),
                            supplier == null ? null : supplier.getLegalName(),
                            supplier == null ? null : supplier.getTradeName(),
                            link.getSupplierContactId(),
                            link.getSentAt(),
                            link.getResponseReceivedAt(),
                            link.getStatus(),
                            link.getOrientationComments(),
                            link.getCreatedBy(),
                            link.getCreatedByName(),
                            link.getUpdatedBy(),
                            link.getUpdatedByName()
                    );
                }).toList()
        );
    }

    private static void stampCreate(RfqJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampCreate(RfqItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampCreate(RfqSupplierJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(RfqJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(RfqSupplierJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(PurchaseRequisitionJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static List<RfqSupplierInput> resolveSupplierInputs(RfqRequest request) {
        if (request.suppliers() != null && !request.suppliers().isEmpty()) {
            return request.suppliers();
        }
        if (request.supplierIds() != null && !request.supplierIds().isEmpty()) {
            return request.supplierIds().stream()
                    .map(id -> new RfqSupplierInput(id, null))
                    .toList();
        }
        throw new BusinessException(
                "SUPPLIERS_REQUIRED",
                "Informe ao menos um fornecedor para a cotacao",
                HttpStatus.BAD_REQUEST.value()
        );
    }

    public record RfqSupplierInput(
            @NotNull UUID supplierId,
            String orientationComments
    ) {
    }

    public record RfqRequest(
            @NotNull UUID requisitionId,
            @NotNull LocalDate responseDueDate,
            List<UUID> supplierIds,
            List<RfqSupplierInput> suppliers,
            String description,
            @Size(max = 3) String defaultCurrency,
            String generalTerms,
            String notes
    ) {
    }

    public record RfqResponse(
            UUID id,
            String rfqNumber,
            String description,
            UUID buyerUserId,
            LocalDate issueDate,
            LocalDate responseDueDate,
            String defaultCurrency,
            String generalTerms,
            String notes,
            String status,
            Instant createdAt,
            Instant updatedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName,
            List<RfqItemResponse> items,
            List<RfqSupplierResponse> suppliers
    ) {
    }

    public record RfqItemResponse(
            UUID id,
            UUID rfqId,
            Integer lineNumber,
            UUID requisitionItemId,
            UUID productId,
            String productCode,
            String productDescription,
            String description,
            BigDecimal quantity,
            String uom,
            LocalDate requiredDate,
            String technicalSpecification,
            String status,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }

    public record RfqSupplierResponse(
            UUID id,
            UUID rfqId,
            UUID supplierId,
            String supplierName,
            String supplierTradeName,
            UUID supplierContactId,
            Instant sentAt,
            Instant responseReceivedAt,
            String status,
            String orientationComments,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
