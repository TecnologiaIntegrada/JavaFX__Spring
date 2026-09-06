package com.projetoj.purchasing.order.adapter.output.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrderJpaEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id private UUID id;
    @Column(name="order_number", nullable=false, length=30) private String orderNumber;
    @Column(name="supplier_id", nullable=false) private UUID supplierId;
    @Column(name="buyer_user_id", nullable=false) private UUID buyerUserId;
    @Column(name="order_date", nullable=false) private LocalDate orderDate;
    @Column(name="currency", nullable=false, columnDefinition="bpchar(3)") private String currency;
    @Column(name="payment_terms", length=100) private String paymentTerms;
    @Column(name="delivery_address", columnDefinition="text") private String deliveryAddress;
    @Column(name="freight_terms", length=50) private String freightTerms;
    @Column(name="project_id") private UUID projectId;
    @Column(name="cost_center_id") private UUID costCenterId;
    @Column(name="status", nullable=false, length=30) private String status;
    @Column(name="subtotal", nullable=false) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name="freight_amount", nullable=false) private BigDecimal freightAmount = BigDecimal.ZERO;
    @Column(name="other_costs", nullable=false) private BigDecimal otherCosts = BigDecimal.ZERO;
    @Column(name="total_amount", nullable=false) private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(name="notes", columnDefinition="text") private String notes;
    @Column(name="sent_at") private Instant sentAt;
    @Column(name="confirmed_at") private Instant confirmedAt;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Column(name="created_by") private UUID createdBy;
    @Column(name="created_by_name", length=200) private String createdByName;
    @Column(name="updated_by") private UUID updatedBy;
    @Column(name="updated_by_name", length=200) private String updatedByName;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public String getOrderNumber(){return orderNumber;} public void setOrderNumber(String v){orderNumber=v;}
    public UUID getSupplierId(){return supplierId;} public void setSupplierId(UUID v){supplierId=v;}
    public UUID getBuyerUserId(){return buyerUserId;} public void setBuyerUserId(UUID v){buyerUserId=v;}
    public LocalDate getOrderDate(){return orderDate;} public void setOrderDate(LocalDate v){orderDate=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public String getPaymentTerms(){return paymentTerms;} public void setPaymentTerms(String v){paymentTerms=v;}
    public String getDeliveryAddress(){return deliveryAddress;} public void setDeliveryAddress(String v){deliveryAddress=v;}
    public String getFreightTerms(){return freightTerms;} public void setFreightTerms(String v){freightTerms=v;}
    public UUID getProjectId(){return projectId;} public void setProjectId(UUID v){projectId=v;}
    public UUID getCostCenterId(){return costCenterId;} public void setCostCenterId(UUID v){costCenterId=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;}
    public BigDecimal getFreightAmount(){return freightAmount;} public void setFreightAmount(BigDecimal v){freightAmount=v;}
    public BigDecimal getOtherCosts(){return otherCosts;} public void setOtherCosts(BigDecimal v){otherCosts=v;}
    public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public Instant getSentAt(){return sentAt;} public void setSentAt(Instant v){sentAt=v;}
    public Instant getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(Instant v){confirmedAt=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
    public UUID getCreatedBy(){return createdBy;} public void setCreatedBy(UUID v){createdBy=v;}
    public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;}
    public UUID getUpdatedBy(){return updatedBy;} public void setUpdatedBy(UUID v){updatedBy=v;}
    public String getUpdatedByName(){return updatedByName;} public void setUpdatedByName(String v){updatedByName=v;}
}