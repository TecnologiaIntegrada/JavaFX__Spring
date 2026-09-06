package com.projetoj.purchasing.shared;

import com.projetoj.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands out gap-free document numbers. The row is locked with {@code FOR UPDATE} and released when the
 * caller's transaction commits, so two concurrent callers can never read the same number.
 */
@Service
public class DocumentSequenceService {

    public static final String PURCHASE_REQUISITION = "PURCHASE_REQUISITION";
    public static final String RFQ = "RFQ";
    public static final String PURCHASE_ORDER = "PURCHASE_ORDER";
    public static final String GOODS_RECEIPT = "GOODS_RECEIPT";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String nextRequisitionNumber() {
        return next(PURCHASE_REQUISITION, "RC");
    }

    @Transactional
    public String nextRfqNumber() {
        return next(RFQ, "RFQ");
    }

    @Transactional
    public String nextPurchaseOrderNumber() {
        return next(PURCHASE_ORDER, "PC");
    }

    @Transactional
    public String nextGoodsReceiptNumber() {
        return next(GOODS_RECEIPT, "REC");
    }

    @Transactional
    public String next(String documentType, String prefix) {
        Object current;
        try {
            current = entityManager
                    .createNativeQuery("SELECT next_number FROM document_sequence WHERE document_type = :documentType FOR UPDATE")
                    .setParameter("documentType", documentType)
                    .getSingleResult();
        } catch (NoResultException ex) {
            throw new BusinessException(
                    "DOCUMENT_SEQUENCE_NOT_FOUND",
                    "Sequencia de documento nao configurada: " + documentType,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        entityManager
                .createNativeQuery("UPDATE document_sequence SET next_number = next_number + 1 WHERE document_type = :documentType")
                .setParameter("documentType", documentType)
                .executeUpdate();

        return "%s-%06d".formatted(prefix, ((Number) current).longValue());
    }
}
