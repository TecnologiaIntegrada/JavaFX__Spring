package com.projetoj.purchasing.rfq.adapter.input.rest;

import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqItemJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqSupplierJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqItemJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqSupplierJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataSupplierQuoteItemJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataSupplierQuoteJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SupplierQuoteItemJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SupplierQuoteJpaEntity;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierJpaEntity;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/supplier-quotes")
@Tag(name = "Supplier Quotes", description = "Propostas recebidas dos fornecedores")
public class SupplierQuoteRestController {

    private final SpringDataSupplierQuoteJpaRepository quoteRepository;
    private final SpringDataSupplierQuoteItemJpaRepository quoteItemRepository;
    private final SpringDataRfqJpaRepository rfqRepository;
    private final SpringDataRfqItemJpaRepository rfqItemRepository;
    private final SpringDataRfqSupplierJpaRepository rfqSupplierRepository;
    private final SpringDataSupplierJpaRepository supplierRepository;
    private final PurchasingAuditService auditService;

    public SupplierQuoteRestController(
            SpringDataSupplierQuoteJpaRepository quoteRepository,
            SpringDataSupplierQuoteItemJpaRepository quoteItemRepository,
            SpringDataRfqJpaRepository rfqRepository,
            SpringDataRfqItemJpaRepository rfqItemRepository,
            SpringDataRfqSupplierJpaRepository rfqSupplierRepository,
            SpringDataSupplierJpaRepository supplierRepository,
            PurchasingAuditService auditService
    ) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.rfqRepository = rfqRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.rfqSupplierRepository = rfqSupplierRepository;
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista propostas, opcionalmente por cotacao")
    public ResponseEntity<List<QuoteResponse>> list(@RequestParam(required = false) UUID rfqId) {
        List<SupplierQuoteJpaEntity> quotes = rfqId == null
                ? quoteRepository.findAll()
                : quoteRepository.findAllByRfqIdOrderByCreatedAtAsc(rfqId);
        return ResponseEntity.ok(decorate(quotes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca proposta com itens")
    public ResponseEntity<QuoteResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(decorate(List.of(findQuote(id))).get(0));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Registra proposta de fornecedor com itens")
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody QuoteRequest request) {
        RfqJpaEntity rfq = rfqRepository.findById(request.rfqId())
                .orElseThrow(() -> new BusinessException("RFQ_NOT_FOUND", "Cotacao nao encontrada", HttpStatus.NOT_FOUND.value()));
        if (!supplierRepository.existsById(request.supplierId())) {
            throw new BusinessException("SUPPLIER_NOT_FOUND", "Fornecedor nao encontrado", HttpStatus.NOT_FOUND.value());
        }
        if (quoteRepository.findByRfqIdAndSupplierId(request.rfqId(), request.supplierId()).isPresent()) {
            throw new BusinessException("DUPLICATE_SUPPLIER_QUOTE", "Fornecedor ja possui proposta nesta cotacao", HttpStatus.CONFLICT.value());
        }

        Map<UUID, RfqItemJpaEntity> rfqItems = rfqItemRepository.findAllByRfqIdOrderByLineNumberAsc(rfq.getId()).stream()
                .collect(Collectors.toMap(RfqItemJpaEntity::getId, Function.identity()));

        Instant now = Instant.now();
        UUID userId = AuthenticatedUser.requireUserId();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(userId);
        SupplierQuoteJpaEntity quote = new SupplierQuoteJpaEntity();
        quote.setId(UUID.randomUUID());
        quote.setRfqId(rfq.getId());
        quote.setSupplierId(request.supplierId());
        quote.setSupplierQuoteNumber(request.supplierQuoteNumber());
        quote.setQuoteDate(request.quoteDate() == null ? LocalDate.now() : request.quoteDate());
        quote.setValidUntil(request.validUntil());
        quote.setCurrency(request.currency() == null ? rfq.getDefaultCurrency() : request.currency().trim().toUpperCase());
        quote.setPaymentTerms(request.paymentTerms());
        quote.setFreightAmount(request.freightAmount() == null ? BigDecimal.ZERO : request.freightAmount());
        quote.setInsuranceAmount(request.insuranceAmount() == null ? BigDecimal.ZERO : request.insuranceAmount());
        quote.setOtherCosts(request.otherCosts() == null ? BigDecimal.ZERO : request.otherCosts());
        quote.setStatus(SupplierQuoteJpaEntity.STATUS_RECEIVED);
        quote.setNotes(request.notes());
        quote.setCreatedAt(now);
        quote.setUpdatedAt(now);
        stampCreate(quote, stamp);
        quoteRepository.save(quote);

        List<SupplierQuoteItemJpaEntity> items = new ArrayList<>();
        for (QuoteItemRequest itemRequest : request.items()) {
            if (!rfqItems.containsKey(itemRequest.rfqItemId())) {
                throw new BusinessException("RFQ_ITEM_NOT_FOUND", "Item de cotacao nao pertence a esta RFQ", HttpStatus.BAD_REQUEST.value());
            }
            SupplierQuoteItemJpaEntity item = new SupplierQuoteItemJpaEntity();
            item.setId(UUID.randomUUID());
            item.setSupplierQuoteId(quote.getId());
            item.setRfqItemId(itemRequest.rfqItemId());
            item.setOfferedQty(itemRequest.offeredQty());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setDiscountPercent(itemRequest.discountPercent() == null ? BigDecimal.ZERO : itemRequest.discountPercent());
            item.setPromisedDate(itemRequest.promisedDate());
            item.setLeadTimeDays(itemRequest.leadTimeDays());
            item.setManufacturer(itemRequest.manufacturer());
            item.setManufacturerPartNumber(itemRequest.manufacturerPartNumber());
            item.setTechnicalApproved(itemRequest.technicalApproved());
            item.setTechnicalNotes(itemRequest.technicalNotes());
            item.setCommercialNotes(itemRequest.commercialNotes());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            stampCreate(item, stamp);
            items.add(item);
        }
        quoteItemRepository.saveAll(items);

        rfqSupplierRepository.findAllByRfqId(rfq.getId()).stream()
                .filter(link -> link.getSupplierId().equals(request.supplierId()))
                .findFirst()
                .ifPresent(link -> {
                    link.setStatus(RfqSupplierJpaEntity.STATUS_RESPONDED);
                    link.setResponseReceivedAt(now);
                    stampUpdate(link, auditService.stampForUpdate(userId));
                    rfqSupplierRepository.save(link);
                });

        if (RfqJpaEntity.STATUS_SENT.equals(rfq.getStatus()) || RfqJpaEntity.STATUS_DRAFT.equals(rfq.getStatus())) {
            rfq.setStatus(RfqJpaEntity.STATUS_QUOTED);
            rfq.setUpdatedAt(now);
            stampUpdate(rfq, auditService.stampForUpdate(userId));
            rfqRepository.save(rfq);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(decorate(List.of(quote)).get(0));
    }

    private SupplierQuoteJpaEntity findQuote(UUID id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUPPLIER_QUOTE_NOT_FOUND", "Proposta nao encontrada", HttpStatus.NOT_FOUND.value()));
    }

    private List<QuoteResponse> decorate(List<SupplierQuoteJpaEntity> quotes) {
        if (quotes.isEmpty()) {
            return List.of();
        }

        List<SupplierQuoteItemJpaEntity> items = quoteItemRepository.findAllBySupplierQuoteIdIn(
                quotes.stream().map(SupplierQuoteJpaEntity::getId).toList());
        Map<UUID, SupplierJpaEntity> suppliers = supplierRepository
                .findAllById(quotes.stream().map(SupplierQuoteJpaEntity::getSupplierId).distinct().toList()).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));
        Map<UUID, RfqItemJpaEntity> rfqItems = items.isEmpty()
                ? Map.of()
                : rfqItemRepository.findAllById(items.stream().map(SupplierQuoteItemJpaEntity::getRfqItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(RfqItemJpaEntity::getId, Function.identity()));

        return quotes.stream()
                .map(quote -> {
                    SupplierJpaEntity supplier = suppliers.get(quote.getSupplierId());
                    List<QuoteItemResponse> quoteItems = items.stream()
                            .filter(item -> item.getSupplierQuoteId().equals(quote.getId()))
                            .map(item -> {
                                RfqItemJpaEntity rfqItem = rfqItems.get(item.getRfqItemId());
                                BigDecimal gross = item.getUnitPrice().multiply(item.getOfferedQty());
                                BigDecimal discount = gross.multiply(item.getDiscountPercent()).movePointLeft(2);
                                return new QuoteItemResponse(
                                        item.getId(),
                                        item.getSupplierQuoteId(),
                                        item.getRfqItemId(),
                                        rfqItem == null ? null : rfqItem.getLineNumber(),
                                        rfqItem == null ? null : rfqItem.getProductId(),
                                        item.getOfferedQty(),
                                        item.getUnitPrice(),
                                        item.getDiscountPercent(),
                                        gross.subtract(discount),
                                        item.getPromisedDate(),
                                        item.getLeadTimeDays(),
                                        item.getManufacturer(),
                                        item.getManufacturerPartNumber(),
                                        item.getTechnicalApproved(),
                                        item.getTechnicalNotes(),
                                        item.getCommercialNotes(),
                                        item.getCreatedBy(),
                                        item.getCreatedByName(),
                                        item.getUpdatedBy(),
                                        item.getUpdatedByName()
                                );
                            })
                            .toList();

                    BigDecimal itemsTotal = quoteItems.stream()
                            .map(QuoteItemResponse::totalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new QuoteResponse(
                            quote.getId(),
                            quote.getRfqId(),
                            quote.getSupplierId(),
                            supplier == null ? null : supplier.getLegalName(),
                            quote.getSupplierQuoteNumber(),
                            quote.getQuoteDate(),
                            quote.getValidUntil(),
                            quote.getCurrency(),
                            quote.getPaymentTerms(),
                            quote.getFreightAmount(),
                            quote.getInsuranceAmount(),
                            quote.getOtherCosts(),
                            itemsTotal,
                            itemsTotal
                                    .add(quote.getFreightAmount())
                                    .add(quote.getInsuranceAmount())
                                    .add(quote.getOtherCosts()),
                            quote.getStatus(),
                            quote.getNotes(),
                            quote.getCreatedAt(),
                            quote.getUpdatedAt(),
                            quote.getCreatedBy(),
                            quote.getCreatedByName(),
                            quote.getUpdatedBy(),
                            quote.getUpdatedByName(),
                            quoteItems
                    );
                })
                .toList();
    }

    private static void stampCreate(SupplierQuoteJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampCreate(SupplierQuoteItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
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

    public record QuoteRequest(
            @NotNull UUID rfqId,
            @NotNull UUID supplierId,
            @Size(max = 100) String supplierQuoteNumber,
            LocalDate quoteDate,
            LocalDate validUntil,
            @Size(max = 3) String currency,
            @Size(max = 100) String paymentTerms,
            BigDecimal freightAmount,
            BigDecimal insuranceAmount,
            BigDecimal otherCosts,
            String notes,
            @NotEmpty List<@Valid QuoteItemRequest> items
    ) {
    }

    public record QuoteItemRequest(
            @NotNull UUID rfqItemId,
            @NotNull BigDecimal offeredQty,
            @NotNull BigDecimal unitPrice,
            BigDecimal discountPercent,
            LocalDate promisedDate,
            Integer leadTimeDays,
            @Size(max = 150) String manufacturer,
            @Size(max = 100) String manufacturerPartNumber,
            Boolean technicalApproved,
            String technicalNotes,
            String commercialNotes
    ) {
    }

    public record QuoteResponse(
            UUID id,
            UUID rfqId,
            UUID supplierId,
            String supplierName,
            String supplierQuoteNumber,
            LocalDate quoteDate,
            LocalDate validUntil,
            String currency,
            String paymentTerms,
            BigDecimal freightAmount,
            BigDecimal insuranceAmount,
            BigDecimal otherCosts,
            BigDecimal itemsTotal,
            BigDecimal grandTotal,
            String status,
            String notes,
            Instant createdAt,
            Instant updatedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName,
            List<QuoteItemResponse> items
    ) {
    }

    public record QuoteItemResponse(
            UUID id,
            UUID supplierQuoteId,
            UUID rfqItemId,
            Integer rfqLineNumber,
            UUID productId,
            BigDecimal offeredQty,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal totalPrice,
            LocalDate promisedDate,
            Integer leadTimeDays,
            String manufacturer,
            String manufacturerPartNumber,
            Boolean technicalApproved,
            String technicalNotes,
            String commercialNotes,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
