package com.projetoj.purchasing.order.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.ProductJpaEntity;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataProductJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.PurchaseOrderFollowupJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.PurchaseOrderItemJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.PurchaseOrderJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataPurchaseOrderFollowupJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataPurchaseOrderItemJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataPurchaseOrderJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.QuoteAwardJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqItemJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataQuoteAwardJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqItemJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataSupplierQuoteItemJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataSupplierQuoteJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SupplierQuoteItemJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SupplierQuoteJpaEntity;
import com.projetoj.purchasing.shared.DocumentSequenceService;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierContactJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierJpaEntity;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.math.RoundingMode;
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
@RequestMapping("/api/v1/purchase-orders")
@Tag(name = "Purchase Orders", description = "Pedidos de compra e follow-up")
public class PurchaseOrderRestController {

    private static final int MONEY_SCALE = 4;

    private final SpringDataPurchaseOrderJpaRepository orderRepository;
    private final SpringDataPurchaseOrderItemJpaRepository orderItemRepository;
    private final SpringDataPurchaseOrderFollowupJpaRepository followupRepository;
    private final SpringDataRfqJpaRepository rfqRepository;
    private final SpringDataRfqItemJpaRepository rfqItemRepository;
    private final SpringDataQuoteAwardJpaRepository awardRepository;
    private final SpringDataSupplierQuoteJpaRepository quoteRepository;
    private final SpringDataSupplierQuoteItemJpaRepository quoteItemRepository;
    private final SpringDataSupplierJpaRepository supplierRepository;
    private final SpringDataSupplierContactJpaRepository supplierContactRepository;
    private final SpringDataProductJpaRepository productRepository;
    private final DocumentSequenceService documentSequenceService;
    private final PurchasingAuditService auditService;

    public PurchaseOrderRestController(
            SpringDataPurchaseOrderJpaRepository orderRepository,
            SpringDataPurchaseOrderItemJpaRepository orderItemRepository,
            SpringDataPurchaseOrderFollowupJpaRepository followupRepository,
            SpringDataRfqJpaRepository rfqRepository,
            SpringDataRfqItemJpaRepository rfqItemRepository,
            SpringDataQuoteAwardJpaRepository awardRepository,
            SpringDataSupplierQuoteJpaRepository quoteRepository,
            SpringDataSupplierQuoteItemJpaRepository quoteItemRepository,
            SpringDataSupplierJpaRepository supplierRepository,
            SpringDataSupplierContactJpaRepository supplierContactRepository,
            SpringDataProductJpaRepository productRepository,
            DocumentSequenceService documentSequenceService,
            PurchasingAuditService auditService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.followupRepository = followupRepository;
        this.rfqRepository = rfqRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.awardRepository = awardRepository;
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.supplierRepository = supplierRepository;
        this.supplierContactRepository = supplierContactRepository;
        this.productRepository = productRepository;
        this.documentSequenceService = documentSequenceService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista pedidos de compra")
    public ResponseEntity<List<PurchaseOrderResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID supplierId
    ) {
        List<PurchaseOrderJpaEntity> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderNumber")).stream()
                .filter(order -> status == null || status.equalsIgnoreCase(order.getStatus()))
                .filter(order -> supplierId == null || supplierId.equals(order.getSupplierId()))
                .toList();

        if (orders.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<UUID> ids = orders.stream().map(PurchaseOrderJpaEntity::getId).toList();
        List<PurchaseOrderItemJpaEntity> items = orderItemRepository.findAllByPurchaseOrderIdInOrderByLineNumberAsc(ids);
        Map<UUID, SupplierJpaEntity> suppliers = suppliersById(orders);
        Map<UUID, ProductJpaEntity> products = productsById(items);

        return ResponseEntity.ok(orders.stream()
                .map(order -> toResponse(
                        order,
                        items.stream().filter(item -> item.getPurchaseOrderId().equals(order.getId())).toList(),
                        suppliers,
                        products))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca pedido de compra com itens")
    public ResponseEntity<PurchaseOrderResponse> findById(@PathVariable UUID id) {
        PurchaseOrderJpaEntity order = findOrder(id);
        List<PurchaseOrderItemJpaEntity> items = orderItemRepository.findAllByPurchaseOrderIdOrderByLineNumberAsc(id);
        return ResponseEntity.ok(toResponse(order, items, suppliersById(List.of(order)), productsById(items)));
    }

    @PostMapping("/from-awards")
    @Transactional
    @Operation(summary = "Gera pedidos de compra a partir das adjudicacoes de uma cotacao, um pedido por fornecedor")
    public ResponseEntity<List<PurchaseOrderResponse>> createFromAwards(@Valid @RequestBody FromAwardsRequest request) {
        UUID buyerId = AuthenticatedUser.requireUserId();

        RfqJpaEntity rfq = rfqRepository.findById(request.rfqId())
                .orElseThrow(() -> new BusinessException("RFQ_NOT_FOUND", "Cotacao nao encontrada", HttpStatus.NOT_FOUND.value()));

        List<RfqItemJpaEntity> rfqItems = rfqItemRepository.findAllByRfqIdOrderByLineNumberAsc(rfq.getId());
        if (rfqItems.isEmpty()) {
            throw new BusinessException("RFQ_WITHOUT_ITEMS", "Cotacao sem itens", HttpStatus.BAD_REQUEST.value());
        }
        Map<UUID, RfqItemJpaEntity> rfqItemsById = rfqItems.stream()
                .collect(Collectors.toMap(RfqItemJpaEntity::getId, Function.identity()));

        List<QuoteAwardJpaEntity> awards = awardRepository.findAllByRfqItemIdIn(rfqItems.stream()
                .map(RfqItemJpaEntity::getId)
                .toList());
        if (awards.isEmpty()) {
            throw new BusinessException("RFQ_WITHOUT_AWARDS", "Cotacao sem adjudicacoes", HttpStatus.CONFLICT.value());
        }

        List<UUID> alreadyOrdered = orderItemRepository
                .findAllByQuoteAwardIdIn(awards.stream().map(QuoteAwardJpaEntity::getId).toList()).stream()
                .map(PurchaseOrderItemJpaEntity::getQuoteAwardId)
                .toList();
        List<QuoteAwardJpaEntity> pendingAwards = awards.stream()
                .filter(award -> !alreadyOrdered.contains(award.getId()))
                .toList();
        if (pendingAwards.isEmpty()) {
            throw new BusinessException("AWARDS_ALREADY_ORDERED", "Adjudicacoes ja convertidas em pedido", HttpStatus.CONFLICT.value());
        }

        Map<UUID, SupplierQuoteItemJpaEntity> quoteItems = quoteItemRepository
                .findAllById(pendingAwards.stream().map(QuoteAwardJpaEntity::getSupplierQuoteItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(SupplierQuoteItemJpaEntity::getId, Function.identity()));
        Map<UUID, SupplierQuoteJpaEntity> quotes = quoteRepository
                .findAllById(quoteItems.values().stream().map(SupplierQuoteItemJpaEntity::getSupplierQuoteId).distinct().toList()).stream()
                .collect(Collectors.toMap(SupplierQuoteJpaEntity::getId, Function.identity()));

        Map<UUID, List<QuoteAwardJpaEntity>> awardsBySupplier = new LinkedHashMap<>();
        for (QuoteAwardJpaEntity award : pendingAwards) {
            SupplierQuoteItemJpaEntity quoteItem = quoteItems.get(award.getSupplierQuoteItemId());
            if (quoteItem == null) {
                throw new BusinessException("SUPPLIER_QUOTE_ITEM_NOT_FOUND", "Item de proposta nao encontrado", HttpStatus.NOT_FOUND.value());
            }
            SupplierQuoteJpaEntity quote = quotes.get(quoteItem.getSupplierQuoteId());
            if (quote == null) {
                throw new BusinessException("SUPPLIER_QUOTE_NOT_FOUND", "Proposta nao encontrada", HttpStatus.NOT_FOUND.value());
            }
            awardsBySupplier.computeIfAbsent(quote.getSupplierId(), key -> new ArrayList<>()).add(award);
        }

        Map<UUID, SupplierJpaEntity> suppliers = supplierRepository.findAllById(awardsBySupplier.keySet()).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));

        Instant now = Instant.now();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(buyerId);
        List<PurchaseOrderResponse> created = new ArrayList<>();

        for (Map.Entry<UUID, List<QuoteAwardJpaEntity>> entry : awardsBySupplier.entrySet()) {
            SupplierJpaEntity supplier = suppliers.get(entry.getKey());
            if (supplier == null) {
                throw new BusinessException("SUPPLIER_NOT_FOUND", "Fornecedor nao encontrado", HttpStatus.NOT_FOUND.value());
            }

            PurchaseOrderJpaEntity order = new PurchaseOrderJpaEntity();
            order.setId(UUID.randomUUID());
            order.setOrderNumber(documentSequenceService.nextPurchaseOrderNumber());
            order.setSupplierId(supplier.getId());
            order.setBuyerUserId(buyerId);
            order.setOrderDate(LocalDate.now());
            order.setStatus(PurchaseOrderJpaEntity.STATUS_APPROVED);
            order.setNotes(request.notes());
            order.setDeliveryAddress(request.deliveryAddress());
            order.setFreightTerms(request.freightTerms());
            order.setCreatedAt(now);
            order.setUpdatedAt(now);
            stampCreate(order, stamp);

            List<PurchaseOrderItemJpaEntity> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal freight = BigDecimal.ZERO;
            BigDecimal otherCosts = BigDecimal.ZERO;
            List<UUID> countedQuotes = new ArrayList<>();
            String currency = null;
            String paymentTerms = null;
            int lineNumber = 1;

            for (QuoteAwardJpaEntity award : entry.getValue()) {
                SupplierQuoteItemJpaEntity quoteItem = quoteItems.get(award.getSupplierQuoteItemId());
                SupplierQuoteJpaEntity quote = quotes.get(quoteItem.getSupplierQuoteId());
                RfqItemJpaEntity rfqItem = rfqItemsById.get(award.getRfqItemId());
                if (rfqItem == null) {
                    throw new BusinessException("RFQ_ITEM_NOT_FOUND", "Item de cotacao nao encontrado", HttpStatus.NOT_FOUND.value());
                }

                if (currency == null) {
                    currency = quote.getCurrency();
                    paymentTerms = quote.getPaymentTerms() == null ? supplier.getDefaultPaymentTerms() : quote.getPaymentTerms();
                }
                if (!countedQuotes.contains(quote.getId())) {
                    countedQuotes.add(quote.getId());
                    freight = freight.add(nullToZero(quote.getFreightAmount()));
                    otherCosts = otherCosts
                            .add(nullToZero(quote.getInsuranceAmount()))
                            .add(nullToZero(quote.getOtherCosts()));
                }

                PurchaseOrderItemJpaEntity item = new PurchaseOrderItemJpaEntity();
                item.setId(UUID.randomUUID());
                item.setPurchaseOrderId(order.getId());
                item.setLineNumber(lineNumber++);
                item.setProductId(rfqItem.getProductId());
                item.setRequisitionItemId(rfqItem.getRequisitionItemId());
                item.setRfqItemId(rfqItem.getId());
                item.setQuoteAwardId(award.getId());
                item.setDescription(rfqItem.getDescription());
                item.setOrderedQty(award.getAwardedQty());
                item.setReceivedQty(BigDecimal.ZERO);
                item.setUom(rfqItem.getUom());
                item.setUnitPrice(quoteItem.getUnitPrice());
                item.setDiscountPercent(nullToZero(quoteItem.getDiscountPercent()));
                item.setPromisedDate(quoteItem.getPromisedDate());
                item.setStatus(PurchaseOrderItemJpaEntity.STATUS_OPEN);
                item.setCreatedAt(now);
                item.setUpdatedAt(now);
                stampCreate(item, stamp);
                items.add(item);

                subtotal = subtotal.add(lineTotal(item));
            }

            order.setCurrency(currency == null ? "BRL" : currency.trim().toUpperCase());
            order.setPaymentTerms(paymentTerms);
            order.setSubtotal(subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            order.setFreightAmount(freight.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            order.setOtherCosts(otherCosts.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            order.setTotalAmount(order.getSubtotal().add(order.getFreightAmount()).add(order.getOtherCosts()));
            orderRepository.save(order);
            List<PurchaseOrderItemJpaEntity> savedItems = orderItemRepository.saveAll(items);

            created.add(toResponse(order, savedItems, Map.of(supplier.getId(), supplier), productsById(savedItems)));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/send")
    @Transactional
    @Operation(summary = "Marca pedido como enviado ao fornecedor")
    public ResponseEntity<PurchaseOrderResponse> send(@PathVariable UUID id) {
        PurchaseOrderJpaEntity order = findOrder(id);
        if (!PurchaseOrderJpaEntity.STATUS_APPROVED.equals(order.getStatus())
                && !PurchaseOrderJpaEntity.STATUS_DRAFT.equals(order.getStatus())) {
            throw new BusinessException("PURCHASE_ORDER_NOT_SENDABLE", "Somente pedidos em rascunho ou aprovados podem ser enviados", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        order.setStatus(PurchaseOrderJpaEntity.STATUS_SENT);
        order.setSentAt(now);
        order.setUpdatedAt(now);
        stampUpdate(order, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        orderRepository.save(order);

        List<PurchaseOrderItemJpaEntity> items = orderItemRepository.findAllByPurchaseOrderIdOrderByLineNumberAsc(id);
        return ResponseEntity.ok(toResponse(order, items, suppliersById(List.of(order)), productsById(items)));
    }

    @PostMapping("/{id}/followups")
    @Transactional
    @Operation(summary = "Registra follow-up do pedido")
    public ResponseEntity<FollowupResponse> createFollowup(
            @PathVariable UUID id,
            @Valid @RequestBody FollowupRequest request
    ) {
        UUID userId = AuthenticatedUser.requireUserId();
        PurchaseOrderJpaEntity order = findOrder(id);

        PurchaseOrderItemJpaEntity item = null;
        if (request.purchaseOrderItemId() != null) {
            item = orderItemRepository.findById(request.purchaseOrderItemId())
                    .orElseThrow(() -> new BusinessException("PURCHASE_ORDER_ITEM_NOT_FOUND", "Item do pedido nao encontrado", HttpStatus.NOT_FOUND.value()));
            if (!item.getPurchaseOrderId().equals(order.getId())) {
                throw new BusinessException("PURCHASE_ORDER_ITEM_MISMATCH", "Item nao pertence ao pedido informado", HttpStatus.BAD_REQUEST.value());
            }
        }
        if (request.supplierContactId() != null && !supplierContactRepository.existsById(request.supplierContactId())) {
            throw new BusinessException("SUPPLIER_CONTACT_NOT_FOUND", "Contato do fornecedor nao encontrado", HttpStatus.NOT_FOUND.value());
        }

        Instant now = Instant.now();
        PurchaseOrderFollowupJpaEntity followup = new PurchaseOrderFollowupJpaEntity();
        followup.setId(UUID.randomUUID());
        followup.setPurchaseOrderId(order.getId());
        followup.setPurchaseOrderItemId(request.purchaseOrderItemId());
        followup.setContactAt(now);
        followup.setContactType(request.contactType().trim().toUpperCase());
        followup.setSupplierContactId(request.supplierContactId());
        followup.setReportedStatus(request.reportedStatus());
        followup.setNewPromisedDate(request.newPromisedDate());
        followup.setComments(request.comments());
        followup.setUserId(userId);
        followup.setCreatedAt(now);
        stampCreate(followup, auditService.stampForCreate(userId));
        followupRepository.save(followup);

        if (item != null && request.newPromisedDate() != null) {
            item.setPromisedDate(request.newPromisedDate());
            item.setUpdatedAt(now);
            stampUpdate(item, auditService.stampForUpdate(userId));
            orderItemRepository.save(item);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toFollowupResponse(followup));
    }

    @GetMapping("/{id}/followups")
    @Operation(summary = "Lista follow-ups do pedido")
    public ResponseEntity<List<FollowupResponse>> listFollowups(@PathVariable UUID id) {
        findOrder(id);
        return ResponseEntity.ok(followupRepository.findAllByPurchaseOrderIdOrderByContactAtDesc(id).stream()
                .map(PurchaseOrderRestController::toFollowupResponse)
                .toList());
    }

    private PurchaseOrderJpaEntity findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PURCHASE_ORDER_NOT_FOUND", "Pedido de compra nao encontrado", HttpStatus.NOT_FOUND.value()));
    }

    private Map<UUID, SupplierJpaEntity> suppliersById(List<PurchaseOrderJpaEntity> orders) {
        List<UUID> ids = orders.stream().map(PurchaseOrderJpaEntity::getSupplierId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return supplierRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));
    }

    private Map<UUID, ProductJpaEntity> productsById(List<PurchaseOrderItemJpaEntity> items) {
        List<UUID> ids = items.stream().map(PurchaseOrderItemJpaEntity::getProductId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal lineTotal(PurchaseOrderItemJpaEntity item) {
        BigDecimal gross = item.getOrderedQty().multiply(item.getUnitPrice());
        BigDecimal discount = gross.multiply(nullToZero(item.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), MONEY_SCALE + 2, RoundingMode.HALF_UP);
        return gross.subtract(discount);
    }

    static PurchaseOrderResponse toResponse(
            PurchaseOrderJpaEntity order,
            List<PurchaseOrderItemJpaEntity> items,
            Map<UUID, SupplierJpaEntity> suppliers,
            Map<UUID, ProductJpaEntity> products
    ) {
        SupplierJpaEntity supplier = suppliers.get(order.getSupplierId());
        return new PurchaseOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSupplierId(),
                supplier == null ? null : supplier.getLegalName(),
                supplier == null ? null : supplier.getTradeName(),
                order.getBuyerUserId(),
                order.getOrderDate(),
                order.getCurrency(),
                order.getPaymentTerms(),
                order.getDeliveryAddress(),
                order.getFreightTerms(),
                order.getProjectId(),
                order.getCostCenterId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getFreightAmount(),
                order.getOtherCosts(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getSentAt(),
                order.getConfirmedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCreatedBy(),
                order.getCreatedByName(),
                order.getUpdatedBy(),
                order.getUpdatedByName(),
                items.stream().map(item -> {
                    ProductJpaEntity product = products.get(item.getProductId());
                    return new PurchaseOrderItemResponse(
                            item.getId(),
                            item.getPurchaseOrderId(),
                            item.getLineNumber(),
                            item.getProductId(),
                            product == null ? null : product.getCode(),
                            product == null ? null : product.getDescription(),
                            item.getRequisitionItemId(),
                            item.getRfqItemId(),
                            item.getQuoteAwardId(),
                            item.getDescription(),
                            item.getOrderedQty(),
                            item.getReceivedQty(),
                            item.getUom(),
                            item.getUnitPrice(),
                            item.getDiscountPercent(),
                            item.getPromisedDate(),
                            item.getStatus(),
                            item.getCreatedBy(),
                            item.getCreatedByName(),
                            item.getUpdatedBy(),
                            item.getUpdatedByName()
                    );
                }).toList()
        );
    }

    private static FollowupResponse toFollowupResponse(PurchaseOrderFollowupJpaEntity followup) {
        return new FollowupResponse(
                followup.getId(),
                followup.getPurchaseOrderId(),
                followup.getPurchaseOrderItemId(),
                followup.getContactAt(),
                followup.getContactType(),
                followup.getSupplierContactId(),
                followup.getReportedStatus(),
                followup.getNewPromisedDate(),
                followup.getComments(),
                followup.getUserId(),
                followup.getCreatedAt(),
                followup.getCreatedBy(),
                followup.getCreatedByName(),
                followup.getUpdatedBy(),
                followup.getUpdatedByName()
        );
    }

    private static void stampCreate(PurchaseOrderJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampCreate(PurchaseOrderItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampCreate(PurchaseOrderFollowupJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(PurchaseOrderJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(PurchaseOrderItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    public record FromAwardsRequest(
            @NotNull UUID rfqId,
            String deliveryAddress,
            @Size(max = 50) String freightTerms,
            String notes
    ) {
    }

    public record FollowupRequest(
            @NotBlank @Size(max = 30) String contactType,
            @Size(max = 100) String reportedStatus,
            String comments,
            UUID purchaseOrderItemId,
            LocalDate newPromisedDate,
            UUID supplierContactId
    ) {
    }

    public record PurchaseOrderResponse(
            UUID id,
            String orderNumber,
            UUID supplierId,
            String supplierName,
            String supplierTradeName,
            UUID buyerUserId,
            LocalDate orderDate,
            String currency,
            String paymentTerms,
            String deliveryAddress,
            String freightTerms,
            UUID projectId,
            UUID costCenterId,
            String status,
            BigDecimal subtotal,
            BigDecimal freightAmount,
            BigDecimal otherCosts,
            BigDecimal totalAmount,
            String notes,
            Instant sentAt,
            Instant confirmedAt,
            Instant createdAt,
            Instant updatedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName,
            List<PurchaseOrderItemResponse> items
    ) {
    }

    public record PurchaseOrderItemResponse(
            UUID id,
            UUID purchaseOrderId,
            Integer lineNumber,
            UUID productId,
            String productCode,
            String productDescription,
            UUID requisitionItemId,
            UUID rfqItemId,
            UUID quoteAwardId,
            String description,
            BigDecimal orderedQty,
            BigDecimal receivedQty,
            String uom,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            LocalDate promisedDate,
            String status,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }

    public record FollowupResponse(
            UUID id,
            UUID purchaseOrderId,
            UUID purchaseOrderItemId,
            Instant contactAt,
            String contactType,
            UUID supplierContactId,
            String reportedStatus,
            LocalDate newPromisedDate,
            String comments,
            UUID userId,
            Instant createdAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
