package com.projetoj.purchasing.rfq.adapter.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quote_award")
public class QuoteAwardJpaEntity {

    @Id
    private UUID id;

    @Column(name = "rfq_item_id", nullable = false)
    private UUID rfqItemId;

    @Column(name = "supplier_quote_item_id", nullable = false)
    private UUID supplierQuoteItemId;

    @Column(name = "awarded_qty", nullable = false)
    private BigDecimal awardedQty;

    @Column(name = "award_reason", nullable = false, length = 100)
    private String awardReason;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "awarded_by", nullable = false)
    private UUID awardedBy;

    @Column(name = "awarded_at", nullable = false)
    private Instant awardedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_by_name", length = 200)
    private String createdByName;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_by_name", length = 200)
    private String updatedByName;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRfqItemId() {
        return rfqItemId;
    }

    public void setRfqItemId(UUID rfqItemId) {
        this.rfqItemId = rfqItemId;
    }

    public UUID getSupplierQuoteItemId() {
        return supplierQuoteItemId;
    }

    public void setSupplierQuoteItemId(UUID supplierQuoteItemId) {
        this.supplierQuoteItemId = supplierQuoteItemId;
    }

    public BigDecimal getAwardedQty() {
        return awardedQty;
    }

    public void setAwardedQty(BigDecimal awardedQty) {
        this.awardedQty = awardedQty;
    }

    public String getAwardReason() {
        return awardReason;
    }

    public void setAwardReason(String awardReason) {
        this.awardReason = awardReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getAwardedBy() {
        return awardedBy;
    }

    public void setAwardedBy(UUID awardedBy) {
        this.awardedBy = awardedBy;
    }

    public Instant getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(Instant awardedAt) {
        this.awardedAt = awardedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}
