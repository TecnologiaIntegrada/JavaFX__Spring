package com.projetoj.purchasing.dashboard.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataProductJpaRepository;
import com.projetoj.purchasing.order.adapter.output.persistence.PurchaseOrderJpaEntity;
import com.projetoj.purchasing.order.adapter.output.persistence.SpringDataPurchaseOrderJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionApprovalJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionApprovalJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionJpaRepository;
import com.projetoj.purchasing.rfq.adapter.output.persistence.RfqJpaEntity;
import com.projetoj.purchasing.rfq.adapter.output.persistence.SpringDataRfqJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/purchasing/dashboard")
@Tag(name = "Purchasing Dashboard", description = "Indicadores do modulo de compras")
public class PurchasingDashboardRestController {

    private static final Set<String> CLOSED_REQUISITION_STATUSES = Set.of(
            PurchaseRequisitionJpaEntity.STATUS_REJECTED,
            PurchaseRequisitionJpaEntity.STATUS_IN_ORDER
    );

    private static final Set<String> CLOSED_RFQ_STATUSES = Set.of(
            RfqJpaEntity.STATUS_AWARDED,
            RfqJpaEntity.STATUS_CANCELLED
    );

    private static final Set<String> CLOSED_PURCHASE_ORDER_STATUSES = Set.of(
            PurchaseOrderJpaEntity.STATUS_RECEIVED,
            PurchaseOrderJpaEntity.STATUS_CANCELLED
    );

    private final SpringDataSupplierJpaRepository supplierRepository;
    private final SpringDataProductJpaRepository productRepository;
    private final SpringDataPurchaseRequisitionJpaRepository requisitionRepository;
    private final SpringDataRfqJpaRepository rfqRepository;
    private final SpringDataPurchaseOrderJpaRepository purchaseOrderRepository;
    private final SpringDataPurchaseRequisitionApprovalJpaRepository approvalRepository;

    public PurchasingDashboardRestController(
            SpringDataSupplierJpaRepository supplierRepository,
            SpringDataProductJpaRepository productRepository,
            SpringDataPurchaseRequisitionJpaRepository requisitionRepository,
            SpringDataRfqJpaRepository rfqRepository,
            SpringDataPurchaseOrderJpaRepository purchaseOrderRepository,
            SpringDataPurchaseRequisitionApprovalJpaRepository approvalRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.requisitionRepository = requisitionRepository;
        this.rfqRepository = rfqRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.approvalRepository = approvalRepository;
    }

    @GetMapping
    @Operation(summary = "Retorna indicadores do modulo de compras")
    public ResponseEntity<DashboardResponse> getDashboard() {
        List<PurchaseRequisitionJpaEntity> requisitions = requisitionRepository.findAll();
        List<RfqJpaEntity> rfqs = rfqRepository.findAll();
        List<PurchaseOrderJpaEntity> purchaseOrders = purchaseOrderRepository.findAll();

        Totals totals = new Totals(
                supplierRepository.count(),
                productRepository.count(),
                requisitions.stream().filter(this::isOpenRequisition).count(),
                rfqs.stream().filter(this::isOpenRfq).count(),
                purchaseOrders.stream().filter(this::isOpenPurchaseOrder).count(),
                approvalRepository.countByStatus(PurchaseRequisitionApprovalJpaEntity.STATUS_PENDING)
        );

        return ResponseEntity.ok(new DashboardResponse(
                totals,
                countByStatus(purchaseOrders.stream().map(PurchaseOrderJpaEntity::getStatus).toList()),
                countByStatus(requisitions.stream().map(PurchaseRequisitionJpaEntity::getStatus).toList()),
                monthlyPurchaseOrders(purchaseOrders)
        ));
    }

    private boolean isOpenRequisition(PurchaseRequisitionJpaEntity requisition) {
        return !CLOSED_REQUISITION_STATUSES.contains(requisition.getStatus());
    }

    private boolean isOpenRfq(RfqJpaEntity rfq) {
        return !CLOSED_RFQ_STATUSES.contains(rfq.getStatus());
    }

    private boolean isOpenPurchaseOrder(PurchaseOrderJpaEntity order) {
        return !CLOSED_PURCHASE_ORDER_STATUSES.contains(order.getStatus());
    }

    private static List<StatusCount> countByStatus(List<String> statuses) {
        Map<String, Long> counts = new LinkedHashMap<>();
        statuses.forEach(status -> counts.merge(status, 1L, Long::sum));
        return counts.entrySet().stream()
                .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<MonthlyPurchaseOrder> monthlyPurchaseOrders(List<PurchaseOrderJpaEntity> orders) {
        Map<YearMonth, MonthlyAccumulator> byMonth = new LinkedHashMap<>();
        for (PurchaseOrderJpaEntity order : orders) {
            if (order.getCreatedAt() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(order.getCreatedAt().atZone(ZoneId.systemDefault()));
            byMonth.computeIfAbsent(month, ignored -> new MonthlyAccumulator())
                    .add(order.getTotalAmount());
        }

        List<YearMonth> months = new ArrayList<>(byMonth.keySet());
        months.sort(Comparator.naturalOrder());

        return months.stream()
                .map(month -> {
                    MonthlyAccumulator accumulator = byMonth.get(month);
                    return new MonthlyPurchaseOrder(
                            month.toString(),
                            accumulator.count,
                            accumulator.amount
                    );
                })
                .toList();
    }

    private static final class MonthlyAccumulator {
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        private void add(BigDecimal value) {
            count++;
            amount = amount.add(value == null ? BigDecimal.ZERO : value);
        }
    }

    public record DashboardResponse(
            Totals totals,
            List<StatusCount> purchaseOrdersByStatus,
            List<StatusCount> requisitionsByStatus,
            List<MonthlyPurchaseOrder> monthlyPurchaseOrders
    ) {
    }

    public record Totals(
            long suppliers,
            long products,
            long openRequisitions,
            long openRfqs,
            long openPurchaseOrders,
            long pendingApprovals
    ) {
    }

    public record StatusCount(
            String status,
            long count
    ) {
    }

    public record MonthlyPurchaseOrder(
            String month,
            long count,
            BigDecimal amount
    ) {
    }
}
