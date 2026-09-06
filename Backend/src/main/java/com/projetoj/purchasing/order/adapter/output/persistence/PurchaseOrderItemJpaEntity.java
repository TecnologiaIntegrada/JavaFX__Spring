package com.projetoj.purchasing.order.adapter.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_item")
public class PurchaseOrderItemJpaEntity {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String STATUS_RECEIVED = "RECEIVED";

    @Id
    private UUID id;

    @Column(name = "purchase_order_id", nullable = false)
    private UUID purchaseOrderId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "requisition_item_id")
    private UUID requisitionItemId;

    @Column(name = "rfq_item_id")
    private UUID rfqItemId;

    @Column(name = "quote_award_id")
    private UUID quoteAwardId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ordered_qty", nullable = false)
    private BigDecimal orderedQty;

    @Column(name = "received_qty", nullable = false)
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @Column(name = "uom", nullable = false, length = 20)
    private String uom;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "promised_date")
    private LocalDate promisedDate;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "cost_center_id")
    private UUID costCenterId;

    @Column(name = "status", nullable = false, length = 30)
    private String status = STATUS_OPEN;

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

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(UUID purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getRequisitionItemId() {
        return requisitionItemId;
    }

    public void setRequisitionItemId(UUID requisitionItemId) {
        this.requisitionItemId = requisitionItemId;
    }

    public UUID getRfqItemId() {
        return rfqItemId;
    }

    public void setRfqItemId(UUID rfqItemId) {
        this.rfqItemId = rfqItemId;
    }

    public UUID getQuoteAwardId() {
        return quoteAwardId;
    }

    public void setQuoteAwardId(UUID quoteAwardId) {
        this.quoteAwardId = quoteAwardId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getOrderedQty() {
        return orderedQty;
    }

    public void setOrderedQty(BigDecimal orderedQty) {
        this.orderedQty = orderedQty;
    }

    public BigDecimal getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(BigDecimal receivedQty) {
        this.receivedQty = receivedQty;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(UUID costCenterId) {
        this.costCenterId = costCenterId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
