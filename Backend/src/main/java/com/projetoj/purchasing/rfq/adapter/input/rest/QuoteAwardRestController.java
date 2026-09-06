package com.projetoj.purchasing.rfq.adapter.input.rest;

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
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierJpaEntity;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/quote-awards")
@Tag(name = "Quote Awards", description = "Adjudicacao de propostas")
public class QuoteAwardRestController {

    private final SpringDataQuoteAwardJpaRepository awardRepository;
    private final SpringDataRfqJpaRepository rfqRepository;
    private final SpringDataRfqItemJpaRepository rfqItemRepository;
    private final SpringDataSupplierQuoteJpaRepository quoteRepository;
    private final SpringDataSupplierQuoteItemJpaRepository quoteItemRepository;
    private final SpringDataSupplierJpaRepository supplierRepository;
    private final PurchasingAuditService auditService;

    public QuoteAwardRestController(
            SpringDataQuoteAwardJpaRepository awardRepository,
            SpringDataRfqJpaRepository rfqRepository,
            SpringDataRfqItemJpaRepository rfqItemRepository,
            SpringDataSupplierQuoteJpaRepository quoteRepository,
            SpringDataSupplierQuoteItemJpaRepository quoteItemRepository,
            SpringDataSupplierJpaRepository supplierRepository,
            PurchasingAuditService auditService
    ) {
        this.awardRepository = awardRepository;
        this.rfqRepository = rfqRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista adjudicacoes, opcionalmente por cotacao")
    public ResponseEntity<List<AwardResponse>> list(@RequestParam(required = false) UUID rfqId) {
        List<QuoteAwardJpaEntity> awards;
        if (rfqId == null) {
            awards = awardRepository.findAll();
        } else {
            List<UUID> rfqItemIds = rfqItemRepository.findAllByRfqIdOrderByLineNumberAsc(rfqId).stream()
                    .map(RfqItemJpaEntity::getId)
                    .toList();
            awards = rfqItemIds.isEmpty() ? List.of() : awardRepository.findAllByRfqItemIdIn(rfqItemIds);
        }
        return ResponseEntity.ok(decorate(awards));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Adjudica um item de proposta")
    public ResponseEntity<AwardResponse> create(@Valid @RequestBody AwardRequest request) {
        UUID userId = AuthenticatedUser.requireUserId();

        RfqItemJpaEntity rfqItem = rfqItemRepository.findById(request.rfqItemId())
                .orElseThrow(() -> new BusinessException("RFQ_ITEM_NOT_FOUND", "Item de cotacao nao encontrado", HttpStatus.NOT_FOUND.value()));
        SupplierQuoteItemJpaEntity quoteItem = quoteItemRepository.findById(request.supplierQuoteItemId())
                .orElseThrow(() -> new BusinessException("SUPPLIER_QUOTE_ITEM_NOT_FOUND", "Item de proposta nao encontrado", HttpStatus.NOT_FOUND.value()));

        if (!quoteItem.getRfqItemId().equals(rfqItem.getId())) {
            throw new BusinessException("AWARD_ITEM_MISMATCH", "Item de proposta nao corresponde ao item da cotacao", HttpStatus.BAD_REQUEST.value());
        }
        if (Boolean.FALSE.equals(quoteItem.getTechnicalApproved())) {
            throw new BusinessException("QUOTE_ITEM_NOT_TECHNICALLY_APPROVED", "Item reprovado tecnicamente nao pode ser adjudicado", HttpStatus.CONFLICT.value());
        }
        if (request.awardedQty().signum() <= 0) {
            throw new BusinessException("INVALID_AWARDED_QTY", "Quantidade adjudicada deve ser maior que zero", HttpStatus.BAD_REQUEST.value());
        }

        BigDecimal alreadyAwarded = awardRepository.findAllByRfqItemId(rfqItem.getId()).stream()
                .map(QuoteAwardJpaEntity::getAwardedQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alreadyAwarded.add(request.awardedQty()).compareTo(rfqItem.getQuantity()) > 0) {
            throw new BusinessException("AWARDED_QTY_EXCEEDED", "Quantidade adjudicada excede a quantidade cotada", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(userId);
        QuoteAwardJpaEntity award = new QuoteAwardJpaEntity();
        award.setId(UUID.randomUUID());
        award.setRfqItemId(rfqItem.getId());
        award.setSupplierQuoteItemId(quoteItem.getId());
        award.setAwardedQty(request.awardedQty());
        award.setAwardReason(request.awardReason().trim());
        award.setNotes(request.notes());
        award.setAwardedBy(userId);
        award.setAwardedAt(now);
        stampCreate(award, stamp);
        awardRepository.save(award);

        rfqItem.setStatus("AWARDED");
        stampUpdate(rfqItem, auditService.stampForUpdate(userId));
        rfqItemRepository.save(rfqItem);

        quoteRepository.findById(quoteItem.getSupplierQuoteId()).ifPresent(quote -> {
            quote.setStatus(SupplierQuoteJpaEntity.STATUS_AWARDED);
            quote.setUpdatedAt(now);
            stampUpdate(quote, auditService.stampForUpdate(userId));
            quoteRepository.save(quote);

            rfqRepository.findById(quote.getRfqId()).ifPresent(rfq -> {
                rfq.setStatus(RfqJpaEntity.STATUS_AWARDED);
                rfq.setUpdatedAt(now);
                stampUpdate(rfq, auditService.stampForUpdate(userId));
                rfqRepository.save(rfq);
            });
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(decorate(List.of(award)).get(0));
    }

    private List<AwardResponse> decorate(List<QuoteAwardJpaEntity> awards) {
        if (awards.isEmpty()) {
            return List.of();
        }

        Map<UUID, RfqItemJpaEntity> rfqItems = rfqItemRepository
                .findAllById(awards.stream().map(QuoteAwardJpaEntity::getRfqItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(RfqItemJpaEntity::getId, Function.identity()));
        Map<UUID, SupplierQuoteItemJpaEntity> quoteItems = quoteItemRepository
                .findAllById(awards.stream().map(QuoteAwardJpaEntity::getSupplierQuoteItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(SupplierQuoteItemJpaEntity::getId, Function.identity()));
        Map<UUID, SupplierQuoteJpaEntity> quotes = quoteItems.isEmpty()
                ? Map.of()
                : quoteRepository.findAllById(quoteItems.values().stream()
                        .map(SupplierQuoteItemJpaEntity::getSupplierQuoteId).distinct().toList()).stream()
                .collect(Collectors.toMap(SupplierQuoteJpaEntity::getId, Function.identity()));
        Map<UUID, SupplierJpaEntity> suppliers = quotes.isEmpty()
                ? Map.of()
                : supplierRepository.findAllById(quotes.values().stream()
                        .map(SupplierQuoteJpaEntity::getSupplierId).distinct().toList()).stream()
                .collect(Collectors.toMap(SupplierJpaEntity::getId, Function.identity()));

        return awards.stream()
                .map(award -> {
                    RfqItemJpaEntity rfqItem = rfqItems.get(award.getRfqItemId());
                    SupplierQuoteItemJpaEntity quoteItem = quoteItems.get(award.getSupplierQuoteItemId());
                    SupplierQuoteJpaEntity quote = quoteItem == null ? null : quotes.get(quoteItem.getSupplierQuoteId());
                    SupplierJpaEntity supplier = quote == null ? null : suppliers.get(quote.getSupplierId());
                    return new AwardResponse(
                            award.getId(),
                            award.getRfqItemId(),
                            rfqItem == null ? null : rfqItem.getRfqId(),
                            rfqItem == null ? null : rfqItem.getLineNumber(),
                            rfqItem == null ? null : rfqItem.getProductId(),
                            award.getSupplierQuoteItemId(),
                            quote == null ? null : quote.getId(),
                            quote == null ? null : quote.getSupplierId(),
                            supplier == null ? null : supplier.getLegalName(),
                            quoteItem == null ? null : quoteItem.getUnitPrice(),
                            quoteItem == null ? null : quoteItem.getDiscountPercent(),
                            award.getAwardedQty(),
                            award.getAwardReason(),
                            award.getNotes(),
                            award.getAwardedBy(),
                            award.getAwardedAt(),
                            award.getCreatedBy(),
                            award.getCreatedByName(),
                            award.getUpdatedBy(),
                            award.getUpdatedByName()
                    );
                })
                .toList();
    }

    private static void stampCreate(QuoteAwardJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(), stamp.createdByName(),
                entity::setCreatedBy, entity::setCreatedByName,
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(SupplierQuoteJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(RfqJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    private static void stampUpdate(RfqItemJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(), stamp.updatedByName(),
                entity::setUpdatedBy, entity::setUpdatedByName
        );
    }

    public record AwardRequest(
            @NotNull UUID rfqItemId,
            @NotNull UUID supplierQuoteItemId,
            @NotNull BigDecimal awardedQty,
            @NotBlank @Size(max = 100) String awardReason,
            String notes
    ) {
    }

    public record AwardResponse(
            UUID id,
            UUID rfqItemId,
            UUID rfqId,
            Integer rfqLineNumber,
            UUID productId,
            UUID supplierQuoteItemId,
            UUID supplierQuoteId,
            UUID supplierId,
            String supplierName,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal awardedQty,
            String awardReason,
            String notes,
            UUID awardedBy,
            Instant awardedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
