package com.projetoj.purchasing.rfq.adapter.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rfq_supplier")
public class RfqSupplierJpaEntity {

    public static final String STATUS_INVITED = "INVITED";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_RESPONDED = "RESPONDED";
    public static final String STATUS_DECLINED = "DECLINED";

    @Id
    private UUID id;

    @Column(name = "rfq_id", nullable = false)
    private UUID rfqId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "supplier_contact_id")
    private UUID supplierContactId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "response_received_at")
    private Instant responseReceivedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = STATUS_INVITED;

    @Column(name = "orientation_comments", columnDefinition = "text")
    private String orientationComments;

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

    public UUID getRfqId() {
        return rfqId;
    }

    public void setRfqId(UUID rfqId) {
        this.rfqId = rfqId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public UUID getSupplierContactId() {
        return supplierContactId;
    }

    public void setSupplierContactId(UUID supplierContactId) {
        this.supplierContactId = supplierContactId;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getResponseReceivedAt() {
        return responseReceivedAt;
    }

    public void setResponseReceivedAt(Instant responseReceivedAt) {
        this.responseReceivedAt = responseReceivedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrientationComments() {
        return orientationComments;
    }

    public void setOrientationComments(String orientationComments) {
        this.orientationComments = orientationComments;
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
