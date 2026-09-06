package com.projetoj.purchasing.order.adapter.input.rest;

import com.projetoj.purchasing.order.adapter.output.persistence.GoodsReceiptItemJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.GoodsReceiptJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.PurchaseOrderItemJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.PurchaseOrderJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataGoodsReceiptItemJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataGoodsReceiptJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataPurchaseOrderItemJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataPurchaseOrderJpaRepository;
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
import jakarta.validation.constraints.NotEmpty;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/goods-receipts")
@Tag(name = "Goods Receipts", description = "Recebimento de materiais")
public class GoodsReceiptRestController {

    private final SpringDataGoodsReceiptJpaRepository receiptRepository;
    private final SpringDataGoodsReceiptItemJpaRepository receiptItemRepository;
    private final SpringDataPurchaseOrderJpaRepository orderRepository;
    private final SpringDataPurchaseOrderItemJpaRepository orderItemRepository;
    private final SpringDataSupplierJpaRepository supplierRepository;
    private final DocumentSequenceService documentSequenceService;
    private final PurchasingAuditService auditService;

    public GoodsReceiptRestController(
            SpringDataGoodsReceiptJpaRepository receiptRepository,
            SpringDataGoodsReceiptItemJpaRepository receiptItemRepository,
            SpringDataPurchaseOrderJpaRepository orderRepository,
            SpringDataPurchaseOrderItemJpaRepository orderItemRepository,
            SpringDataSupplierJpaRepository supplierRepository,
            DocumentSequenceService documentSequenceService,
            PurchasingAuditService auditService
    ) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.supplierRepository = supplierRepository;
        this.documentSequenceService = documentSequenceService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista recebimentos")
    public ResponseEntity<List<GoodsReceiptResponse>> list(
            @RequestParam(required = false) UUID purchaseOrderId,
            @RequestParam(required = false) String status
    ) {
        List<GoodsReceiptJpaEntity> receipts = (purchaseOrderId == null
                ? receiptRepository.findAll(Sort.by(Sort.Direction.DESC, "receivedAt"))
                : receiptRepository.findAllByPurchaseOrderIdOrderByReceivedAtDesc(purchaseOrderId)).stream()
                .filter(receipt -> status == null || status.equalsIgnoreCase(receipt.getStatus()))
                .toList();

        if (receipts.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<GoodsReceiptItemJpaEntity> items = receiptItemRepository.findAllByGoodsReceiptIdIn(
                receipts.stream().map(GoodsReceiptJpaEntity::getId).toList());
        Map<UUID, PurchaseOrderJpaEntity> orders = ordersById(receipts);
        Map<UUID, SupplierJpaEntity> suppliers = suppliersById(receipts);
        Map<UUID, PurchaseOrderItemJpaEntity> orderItems = orderItemsById(items);

        return ResponseEntity.ok(receipts.stream()
                .map(receipt -> toResponse(
                        receipt,
                        items.stream().filter(item -> item.getGoodsReceiptId().equals(receipt.getId())).toList(),
                        orders,
                        suppliers,
                        orderItems))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca recebimento com itens")
    public ResponseEntity<GoodsReceiptResponse> findById(@PathVariable UUID id) {
        GoodsReceiptJpaEntity receipt = findReceipt(id);
        List<GoodsReceiptItemJpaEntity> items = receiptItemRepository.findAllByGoodsReceiptId(id);
        return ResponseEntity.ok(toResponse(
                receipt,
                items,
                ordersById(List.of(receipt)),
                suppliersById(List.of(receipt)),
                orderItemsById(items)));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Registra recebimento de materiais de um pedido")
    public ResponseEntity<GoodsReceiptResponse> create(@Valid @RequestBody GoodsReceiptRequest request) {
        UUID userId = AuthenticatedUser.requireUserId();

        PurchaseOrderJpaEntity order = orderRepository.findById(request.purchaseOrderId())
                .orElseThrow(() -> new BusinessException("PURCHASE_ORDER_NOT_FOUND", "Pedido de compra nao encontrado", HttpStatus.NOT_FOUND.value()));
        if (PurchaseOrderJpaEntity.STATUS_CANCELLED.equals(order.getStatus())) {
            throw new BusinessException("PURCHASE_ORDER_CANCELLED", "Pedido cancelado nao pode receber materiais", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(userId);
        GoodsReceiptJpaEntity receipt = new GoodsReceiptJpaEntity();
        receipt.setId(UUID.randomUUID());
        receipt.setReceiptNumber(documentSequenceService.nextGoodsReceiptNumber());
        receipt.setPurchaseOrderId(order.getId());
        receipt.setSupplierId(order.getSupplierId());
        receipt.setReceivedAt(now);
        receipt.setReceivedBy(userId);
        receipt.setSupplierDocumentNumber(request.supplierDocumentNumber());
        receipt.setStatus(GoodsReceiptJpaEntity.STATUS_OPEN);
        receipt.setNotes(request.notes());
        receipt.setCreatedAt(now);
        stampCreate(receipt, stamp);

        List<GoodsReceiptItemJpaEntity> items = new ArrayList<>();
        for (GoodsReceiptItemRequest line : request.items()) {
            if (line.receivedQty().signum() <= 0) {
                throw new BusinessException("INVALID_RECEIVED_QTY", "Quantidade recebida deve ser maior que zero", HttpStatus.BAD_REQUEST.value());
            }

            PurchaseOrderItemJpaEntity orderItem = orderItemRepository.findWithLockById(line.purchaseOrderItemId())
                    .orElseThrow(() -> new BusinessException("PURCHASE_ORDER_ITEM_NOT_FOUND", "Item do pedido nao encontrado", HttpStatus.NOT_FOUND.value()));
            if (!orderItem.getPurchaseOrderId().equals(order.getId())) {
                throw new BusinessException("PURCHASE_ORDER_ITEM_MISMATCH", "Item nao pertence ao pedido informado", HttpStatus.BAD_REQUEST.value());
            }

            BigDecimal alreadyReceived = nullToZero(orderItem.getReceivedQty());
            BigDecimal newTotal = alreadyReceived.add(line.receivedQty());
            if (newTotal.compareTo(orderItem.getOrderedQty()) > 0) {
                throw new BusinessException(
                        "RECEIVED_QTY_EXCEEDED",
                        "Quantidade recebida excede a quantidade pedida na linha " + orderItem.getLineNumber(),
                        HttpStatus.CONFLICT.value()
                );
            }

            orderItem.setReceivedQty(newTotal);
            orderItem.setStatus(itemStatusFor(newTotal, orderItem.getOrderedQty()));
            orderItem.setUpdatedAt(now);
            stampUpdate(orderItem, auditService.stampForUpdate(userId));
            orderItemRepository.save(orderItem);

            GoodsReceiptItemJpaEntity item = new GoodsReceiptItemJpaEntity();
            item.setId(UUID.randomUUID());
            item.setGoodsReceiptId(receipt.getId());
            item.setPurchaseOrderItemId(orderItem.getId());
            item.setReceivedQty(line.receivedQty());
            item.setLotNumber(line.lotNumber());
            item.setSerialNumber(line.serialNumber());
            item.setItemStatus(GoodsReceiptItemJpaEntity.ITEM_STATUS_RECEIVED);
            item.setNotes(line.notes());
            item.setCreatedAt(now);
            stampCreate(item, stamp);
            items.add(item);
        }

        receiptRepository.save(receipt);
        List<GoodsReceiptItemJpaEntity> savedItems = receiptItemRepository.saveAll(items);
        refreshOrderStatus(order, now, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(
                receipt,
                savedItems,
                Map.of(order.getId(), order),
                suppliersById(List.of(receipt)),
                orderItemsById(savedItems)));
    }

    @PostMapping("/{id}/reverse")
    @Transactional
    @Operation(summary = "Estorna um recebimento e devolve as quantidades ao pedido")
    public ResponseEntity<GoodsReceiptResponse> reverse(@PathVariable UUID id) {
        GoodsReceiptJpaEntity receipt = findReceipt(id);
        if (GoodsReceiptJpaEntity.STATUS_REVERSED.equals(receipt.getStatus())) {
            throw new BusinessException("GOODS_RECEIPT_ALREADY_REVERSED", "Recebimento ja estornado", HttpStatus.CONFLICT.value());
        }

        PurchaseOrderJpaEntity order = orderRepository.findById(receipt.getPurchaseOrderId())
                .orElseThrow(() -> new BusinessException("PURCHASE_ORDER_NOT_FOUND", "Pedido de compra nao encontrado", HttpStatus.NOT_FOUND.value()));

        Instant now = Instant.now();
        UUID userId = AuthenticatedUser.requireUserId();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForUpdate(userId);
        List<GoodsReceiptItemJpaEntity> items = receiptItemRepository.findAllByGoodsReceiptId(receipt.getId());
        for (GoodsReceiptItemJpaEntity item : items) {
            PurchaseOrderItemJpaEntity orderItem = orderItemRepository.findWithLockById(item.getPurchaseOrderItemId())
                    .orElseThrow(() -> new BusinessException("PURCHASE_ORDER_ITEM_NOT_FOUND", "Item do pedido nao encontrado", HttpStatus.NOT_FOUND.value()));

            BigDecimal remaining = nullToZero(orderItem.getReceivedQty()).subtract(item.getReceivedQty());
            if (remaining.signum() < 0) {
                remaining = BigDecimal.ZERO;
            }
            orderItem.setReceivedQty(remaining);
            orderItem.setStatus(itemStatusFor(remaining, orderItem.getOrderedQty()));
            orderItem.setUpdatedAt(now);
            stampUpdate(orderItem, stamp);
            orderItemRepository.save(orderItem);

            item.setItemStatus(GoodsReceiptItemJpaEntity.ITEM_STATUS_REVERSED);
            stampUpdate(item, stamp);
        }
        List<GoodsReceiptItemJpaEntity> savedItems = receiptItemRepository.saveAll(items);

        receipt.setStatus(GoodsReceiptJpaEntity.STATUS_REVERSED);
        stampUpdate(receipt, stamp);
        receiptRepository.save(receipt);
        refreshOrderStatus(order, now, userId);

        return ResponseEntity.ok(toResponse(
                receipt,
                savedItems,
                Map.of(order.getId(), order),
                suppliersById(List.of(receipt)),
                orderItemsById(savedItems)));
    }

    /** Recomputes the order status from the current received quantities of every line. */
    private void refreshOrderStatus(PurchaseOrderJpaEntity order, Instant now, UUID userId) {
        List<PurchaseOrderItemJpaEntity> orderItems = orderItemRepository.findAllByPurchaseOrderIdOrderByLineNumberAsc(order.getId());
        boolean allReceived = !orderItems.isEmpty() && orderItems.stream()
                .allMatch(item -> nullToZero(item.getReceivedQty()).compareTo(item.getOrderedQty()) >= 0);
        boolean anyReceived = orderItems.stream()
                .anyMatch(item -> nullToZero(item.getReceivedQty()).signum() > 0);

        String status;
        if (allReceived) {
            status = PurchaseOrderJpaEntity.STATUS_RECEIVED;
        } else if (anyReceived) {
            status = PurchaseOrderJpaEntity.STATUS_PARTIALLY_RECEIVED;
        } else if (order.getSentAt() != null) {
            status = PurchaseOrderJpaEntity.STATUS_SENT;
        } else {
            status = PurchaseOrderJpaEntity.STATUS_APPROVED;
        }

        order.setStatus(status);
        order.setUpdatedAt(now);
        stampUpdate(order, auditService.stampForUpdate(userId));
        orderRepository.save(order);
    }

    private static String itemStatusFor(BigDecimal receivedQty, BigDecimal orderedQty) {
        if (receivedQty.signum() <= 0) {
            return PurchaseOrderItemJpaEntity.STATUS_OPEN;
        }
        return receivedQty.compareTo(orderedQty) >= 0
                ? PurchaseOrderItemJpaEntity.STATUS_RECEIVED
                : PurchaseOrderItemJpaEntity.STATUS_PARTIALLY_RECEIVED;
    }

    private GoodsReceiptJpaEntity findReceipt(UUID id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GOODS_RECEIPT_NOT_FOUND", "Recebimento nao encontrado", HttpStatus.NOT_FOUND.value()));
    }

    private Map<UUID, PurchaseOrderJpaEntity> ordersById(List<GoodsReceiptJpaEntity> receipts) {
        List<UUID> ids = receipts.stream().map(GoodsReceiptJpaEntity::getPurchaseOrderId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PurchaseOrderJpaEntity::getId, Function.identity()));
    }

    private Map<UUID, SupplierJpaEntity> suppliersById(List<GoodsReceiptJpaEntity> receipts) {
        List<UUID> ids = receipts.stream().map(GoodsReceiptJpaEntity::getSupplierId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return supplierRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));
    }

    private Map<UUID, PurchaseOrderItemJpaEntity> orderItemsById(List<GoodsReceiptItemJpaEntity> items) {
        List<UUID> ids = items.stream().map(GoodsReceiptItemJpaEntity::getPurchaseOrderItemId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderItemRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PurchaseOrderItemJpaEntity::getId, Function.identity()));
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static GoodsReceiptResponse toResponse(
            GoodsReceiptJpaEntity receipt,
            List<GoodsReceiptItemJpaEntity> items,
            Map<UUID, PurchaseOrderJpaEntity> orders,
            Map<UUID, SupplierJpaEntity> suppliers,
            Map<UUID, PurchaseOrderItemJpaEntity> orderItems
    ) {
        PurchaseOrderJpaEntity order = orders.get(receipt.getPurchaseOrderId());
        SupplierJpaEntity supplier = suppliers.get(receipt.getSupplierId());
        return new GoodsReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getPurchaseOrderId(),
                order == null ? null : order.getOrderNumber(),
                order == null ? null : order.getStatus(),
                receipt.getSupplierId(),
                supplier == null ? null : supplier.getLegalName(),
                receipt.getReceivedAt(),
                receipt.getReceivedBy(),
                receipt.getSupplierDocumentNumber(),
                receipt.getStatus(),
                receipt.getNotes(),
                receipt.getCreatedAt(),
                receipt.getCreatedBy(),
                receipt.getCreatedByName(),
                receipt.getUpdatedBy(),
                receipt.getUpdatedByName(),
                items.stream().map(item -> {
                    PurchaseOrderItemJpaEntity orderItem = orderItems.get(item.getPurchaseOrderItemId());
                    return new GoodsReceiptItemResponse(
                            item.getId(),
                            item.getGoodsReceiptId(),
                            item.getPurchaseOrderItemId(),
                            orderItem == null ? null : orderItem.getLineNumber(),
                            orderItem == null ? null : orderItem.getProductId(),
                            orderItem == null ? null : orderItem.getOrderedQty(),
                            orderItem == null ? null : orderItem.getReceivedQty(),
                            orderItem == null ? null : orderItem.getUom(),
                            orderItem == null ? null : orderItem.getStatus(),
                            item.getReceivedQty(),
                            item.getLotNumber(),
                            item.getSerialNumber(),
                            item.getItemStatus(),
                            item.getNotes(),
                            item.getCreatedAt(),
                            item.getCreatedBy(),
                            item.getCreatedByName(),
                            item.getUpdatedBy(),
                            item.getUpdatedByName()
                    );
                }).toList()
        );
    }

    private static void stampCreate(GoodsReceiptJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampCreate(GoodsReceiptItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(GoodsReceiptJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(GoodsReceiptItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
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

    public record GoodsReceiptRequest(
            @NotNull UUID purchaseOrderId,
            @Size(max = 100) String supplierDocumentNumber,
            String notes,
            @NotEmpty @Valid List<GoodsReceiptItemRequest> items
    ) {
    }

    public record GoodsReceiptItemRequest(
            @NotNull UUID purchaseOrderItemId,
            @NotNull BigDecimal receivedQty,
            @Size(max = 100) String lotNumber,
            @Size(max = 150) String serialNumber,
            String notes
    ) {
    }

    public record GoodsReceiptResponse(
            UUID id,
            String receiptNumber,
            UUID purchaseOrderId,
            String purchaseOrderNumber,
            String purchaseOrderStatus,
            UUID supplierId,
            String supplierName,
            Instant receivedAt,
            UUID receivedBy,
            String supplierDocumentNumber,
            String status,
            String notes,
            Instant createdAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName,
            List<GoodsReceiptItemResponse> items
    ) {
    }

    public record GoodsReceiptItemResponse(
            UUID id,
            UUID goodsReceiptId,
            UUID purchaseOrderItemId,
            Integer purchaseOrderLineNumber,
            UUID productId,
            BigDecimal orderedQty,
            BigDecimal orderReceivedQty,
            String uom,
            String purchaseOrderItemStatus,
            BigDecimal receivedQty,
            String lotNumber,
            String serialNumber,
            String itemStatus,
            String notes,
            Instant createdAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
