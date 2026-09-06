package com.projetoj.purchasing.rfq.adapter.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_quote_item")
public class SupplierQuoteItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_quote_id", nullable = false)
    private UUID supplierQuoteId;

    @Column(name = "rfq_item_id", nullable = false)
    private UUID rfqItemId;

    @Column(name = "offered_qty", nullable = false)
    private BigDecimal offeredQty;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "promised_date")
    private LocalDate promisedDate;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "manufacturer", length = 150)
    private String manufacturer;

    @Column(name = "manufacturer_part_number", length = 100)
    private String manufacturerPartNumber;

    @Column(name = "technical_approved")
    private Boolean technicalApproved;

    @Column(name = "technical_notes", columnDefinition = "text")
    private String technicalNotes;

    @Column(name = "commercial_notes", columnDefinition = "text")
    private String commercialNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public UUID getSupplierQuoteId() {
        return supplierQuoteId;
    }

    public void setSupplierQuoteId(UUID supplierQuoteId) {
        this.supplierQuoteId = supplierQuoteId;
    }

    public UUID getRfqItemId() {
        return rfqItemId;
    }

    public void setRfqItemId(UUID rfqItemId) {
        this.rfqItemId = rfqItemId;
    }

    public BigDecimal getOfferedQty() {
        return offeredQty;
    }

    public void setOfferedQty(BigDecimal offeredQty) {
        this.offeredQty = offeredQty;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public LocalDate getPromisedDate() {
        return promisedDate;
    }

    public void setPromisedDate(LocalDate promisedDate) {
        this.promisedDate = promisedDate;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getManufacturerPartNumber() {
        return manufacturerPartNumber;
    }

    public void setManufacturerPartNumber(String manufacturerPartNumber) {
        this.manufacturerPartNumber = manufacturerPartNumber;
    }

    public Boolean getTechnicalApproved() {
        return technicalApproved;
    }

    public void setTechnicalApproved(Boolean technicalApproved) {
        this.technicalApproved = technicalApproved;
    }

    public String getTechnicalNotes() {
        return technicalNotes;
    }

    public void setTechnicalNotes(String technicalNotes) {
        this.technicalNotes = technicalNotes;
    }

    public String getCommercialNotes() {
        return commercialNotes;
    }

    public void setCommercialNotes(String commercialNotes) {
        this.commercialNotes = commercialNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
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
