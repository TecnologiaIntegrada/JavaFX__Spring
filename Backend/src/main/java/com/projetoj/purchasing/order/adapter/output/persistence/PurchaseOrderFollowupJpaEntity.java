package com.projetoj.purchasing.order.adapter.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_followup")
public class PurchaseOrderFollowupJpaEntity {

    @Id
    private UUID id;

    @Column(name = "purchase_order_id", nullable = false)
    private UUID purchaseOrderId;

    @Column(name = "purchase_order_item_id")
    private UUID purchaseOrderItemId;

    @Column(name = "contact_at", nullable = false)
    private Instant contactAt;

    @Column(name = "contact_type", nullable = false, length = 30)
    private String contactType;

    @Column(name = "supplier_contact_id")
    private UUID supplierContactId;

    @Column(name = "reported_status", length = 100)
    private String reportedStatus;

    @Column(name = "new_promised_date")
    private LocalDate newPromisedDate;

    @Column(name = "comments", columnDefinition = "text")
    private String comments;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public UUID getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public void setPurchaseOrderItemId(UUID purchaseOrderItemId) {
        this.purchaseOrderItemId = purchaseOrderItemId;
    }

    public Instant getContactAt() {
        return contactAt;
    }

    public void setContactAt(Instant contactAt) {
        this.contactAt = contactAt;
    }

    public String getContactType() {
        return contactType;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public UUID getSupplierContactId() {
        return supplierContactId;
    }

    public void setSupplierContactId(UUID supplierContactId) {
        this.supplierContactId = supplierContactId;
    }

    public String getReportedStatus() {
        return reportedStatus;
    }

    public void setReportedStatus(String reportedStatus) {
        this.reportedStatus = reportedStatus;
    }

    public LocalDate getNewPromisedDate() {
        return newPromisedDate;
    }

    public void setNewPromisedDate(LocalDate newPromisedDate) {
        this.newPromisedDate = newPromisedDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
