package com.projetoj.purchasing.requisition.adapter.input.rest;

import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionApprovalJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.PurchaseRequisitionJpaEntity;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionApprovalJpaRepository;
import com.projetoj.purchasing.requisition.adapter.output.persistence.SpringDataPurchaseRequisitionJpaRepository;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/requisition-approvals")
@Tag(name = "Requisition Approvals", description = "Aprovacoes de requisicao de compra")
public class RequisitionApprovalRestController {

    private final SpringDataPurchaseRequisitionApprovalJpaRepository approvalRepository;
    private final SpringDataPurchaseRequisitionJpaRepository requisitionRepository;
    private final PurchasingAuditService auditService;

    public RequisitionApprovalRestController(
            SpringDataPurchaseRequisitionApprovalJpaRepository approvalRepository,
            SpringDataPurchaseRequisitionJpaRepository requisitionRepository,
            PurchasingAuditService auditService
    ) {
        this.approvalRepository = approvalRepository;
        this.requisitionRepository = requisitionRepository;
        this.auditService = auditService;
    }

    @GetMapping("/pending")
    @Operation(summary = "Lista aprovacoes pendentes do usuario autenticado")
    public ResponseEntity<List<ApprovalResponse>> pending() {
        UUID userId = AuthenticatedUser.requireUserId();
        List<PurchaseRequisitionApprovalJpaEntity> approvals = approvalRepository
                .findAllByApproverUserIdAndStatusOrderByCreatedAtAsc(userId, PurchaseRequisitionApprovalJpaEntity.STATUS_PENDING);
        return ResponseEntity.ok(decorate(approvals));
    }

    @GetMapping
    @Operation(summary = "Lista aprovacoes por requisicao")
    public ResponseEntity<List<ApprovalResponse>> list(@RequestParam(required = false) UUID requisitionId) {
        List<PurchaseRequisitionApprovalJpaEntity> approvals = requisitionId == null
                ? approvalRepository.findAll()
                : approvalRepository.findAllByRequisitionIdOrderByApprovalLevelAsc(requisitionId);
        return ResponseEntity.ok(decorate(approvals));
    }

    @PutMapping("/{id}/comments")
    @Transactional
    @Operation(summary = "Atualiza comentarios de aprovacao pendente")
    public ResponseEntity<ApprovalResponse> updateComments(
            @PathVariable UUID id,
            @Valid @RequestBody CommentsRequest request
    ) {
        PurchaseRequisitionApprovalJpaEntity approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("APPROVAL_NOT_FOUND", "Aprovacao nao encontrada", HttpStatus.NOT_FOUND.value()));

        if (!PurchaseRequisitionApprovalJpaEntity.STATUS_PENDING.equals(approval.getStatus())) {
            throw new BusinessException(
                    "APPROVAL_NOT_PENDING",
                    "Comentarios so podem ser editados enquanto o status for PENDING",
                    HttpStatus.CONFLICT.value()
            );
        }

        UUID userId = AuthenticatedUser.requireUserId();
        if (!approval.getApproverUserId().equals(userId)) {
            throw new BusinessException("APPROVAL_NOT_ASSIGNED", "Aprovacao atribuida a outro usuario", HttpStatus.FORBIDDEN.value());
        }

        approval.setComments(request.comments());
        stampUpdate(approval, auditService.stampForUpdate(userId));
        approvalRepository.save(approval);
        return ResponseEntity.ok(decorate(List.of(approval)).get(0));
    }

    @PostMapping("/{id}/decide")
    @Transactional
    @Operation(summary = "Registra decisao de aprovacao")
    public ResponseEntity<ApprovalResponse> decide(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request) {
        PurchaseRequisitionApprovalJpaEntity approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("APPROVAL_NOT_FOUND", "Aprovacao nao encontrada", HttpStatus.NOT_FOUND.value()));

        if (!PurchaseRequisitionApprovalJpaEntity.STATUS_PENDING.equals(approval.getStatus())) {
            throw new BusinessException("APPROVAL_ALREADY_DECIDED", "Aprovacao ja possui decisao registrada", HttpStatus.CONFLICT.value());
        }

        UUID userId = AuthenticatedUser.requireUserId();
        if (!approval.getApproverUserId().equals(userId)) {
            throw new BusinessException("APPROVAL_NOT_ASSIGNED", "Aprovacao atribuida a outro usuario", HttpStatus.FORBIDDEN.value());
        }

        String decision = request.decision().trim().toUpperCase();
        PurchaseRequisitionJpaEntity requisition = requisitionRepository.findById(approval.getRequisitionId())
                .orElseThrow(() -> new BusinessException("REQUISITION_NOT_FOUND", "Requisicao nao encontrada", HttpStatus.NOT_FOUND.value()));

        Instant now = Instant.now();
        switch (decision) {
            case PurchaseRequisitionApprovalJpaEntity.STATUS_APPROVED -> {
                requisition.setStatus(PurchaseRequisitionJpaEntity.STATUS_APPROVED);
                requisition.setApprovedAt(now);
            }
            case PurchaseRequisitionApprovalJpaEntity.STATUS_REJECTED ->
                    requisition.setStatus(PurchaseRequisitionJpaEntity.STATUS_REJECTED);
            case PurchaseRequisitionApprovalJpaEntity.STATUS_RETURNED -> {
                requisition.setStatus(PurchaseRequisitionJpaEntity.STATUS_DRAFT);
                requisition.setSubmittedAt(null);
            }
            default -> throw new BusinessException(
                    "INVALID_DECISION",
                    "Decisao invalida. Use APPROVED, REJECTED ou RETURNED",
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        approval.setStatus(decision);
        approval.setDecisionAt(now);
        approval.setComments(request.comments());
        stampUpdate(approval, auditService.stampForUpdate(userId));
        approvalRepository.save(approval);

        requisition.setUpdatedAt(now);
        stampUpdate(requisition, auditService.stampForUpdate(userId));
        requisitionRepository.save(requisition);

        return ResponseEntity.ok(decorate(List.of(approval)).get(0));
    }

    private List<ApprovalResponse> decorate(List<PurchaseRequisitionApprovalJpaEntity> approvals) {
        if (approvals.isEmpty()) {
            return List.of();
        }
        Map<UUID, PurchaseRequisitionJpaEntity> requisitions = requisitionRepository
                .findAllById(approvals.stream()
                        .map(PurchaseRequisitionApprovalJpaEntity::getRequisitionId)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(PurchaseRequisitionJpaEntity::getId, Function.identity()));

        return approvals.stream()
                .map(approval -> {
                    PurchaseRequisitionJpaEntity requisition = requisitions.get(approval.getRequisitionId());
                    return new ApprovalResponse(
                            approval.getId(),
                            approval.getRequisitionId(),
                            requisition == null ? null : requisition.getRequisitionNumber(),
                            requisition == null ? null : requisition.getStatus(),
                            requisition == null ? null : requisition.getPriority(),
                            requisition == null ? null : requisition.getJustification(),
                            approval.getApprovalLevel(),
                            approval.getApproverUserId(),
                            approval.getStatus(),
                            approval.getDecisionAt(),
                            approval.getComments(),
                            approval.getCreatedAt(),
                            approval.getCreatedBy(),
                            approval.getCreatedByName(),
                            approval.getUpdatedBy(),
                            approval.getUpdatedByName()
                    );
                })
                .toList();
    }

    private static void stampUpdate(PurchaseRequisitionApprovalJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(),
                stamp.updatedByName(),
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static void stampUpdate(PurchaseRequisitionJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(),
                stamp.updatedByName(),
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    public record CommentsRequest(
            String comments
    ) {
    }

    public record DecisionRequest(
            @NotBlank String decision,
            String comments
    ) {
    }

    public record ApprovalResponse(
            UUID id,
            UUID requisitionId,
            String requisitionNumber,
            String requisitionStatus,
            String requisitionPriority,
            String requisitionJustification,
            Integer approvalLevel,
            UUID approverUserId,
            String status,
            Instant decisionAt,
            String comments,
            Instant createdAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName
    ) {
    }
}
