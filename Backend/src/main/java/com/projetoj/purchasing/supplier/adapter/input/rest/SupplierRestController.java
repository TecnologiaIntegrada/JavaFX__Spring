package com.projetoj.purchasing.supplier.adapter.input.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierContactJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierStatusJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierTypeJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierContactJpaEntity;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierJpaEntity;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierStatusJpaEntity;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SupplierTypeJpaEntity;
import com.projetoj.purchasing.shared.PurchasingAuditService;
import com.projetoj.purchasing.shared.PurchasingAuditSupport;
import com.projetoj.shared.exception.BusinessException;
import com.projetoj.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Cadastro de fornecedores")
public class SupplierRestController {

    private final SpringDataSupplierJpaRepository supplierRepository;
    private final SpringDataSupplierContactJpaRepository contactRepository;
    private final SpringDataSupplierTypeJpaRepository typeRepository;
    private final SpringDataSupplierStatusJpaRepository statusRepository;
    private final PurchasingAuditService auditService;

    public SupplierRestController(
            SpringDataSupplierJpaRepository supplierRepository,
            SpringDataSupplierContactJpaRepository contactRepository,
            SpringDataSupplierTypeJpaRepository typeRepository,
            SpringDataSupplierStatusJpaRepository statusRepository,
            PurchasingAuditService auditService
    ) {
        this.supplierRepository = supplierRepository;
        this.contactRepository = contactRepository;
        this.typeRepository = typeRepository;
        this.statusRepository = statusRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista fornecedores")
    public ResponseEntity<List<SupplierResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String status
    ) {
        List<SupplierJpaEntity> suppliers = supplierRepository.findAll(Sort.by("legalName")).stream()
                .filter(supplier -> active == null || supplier.isActive() == active)
                .filter(supplier -> status == null || status.equalsIgnoreCase(supplier.getStatus()))
                .toList();

        List<UUID> ids = suppliers.stream().map(SupplierJpaEntity::getId).toList();
        List<SupplierContactJpaEntity> contacts = ids.isEmpty()
                ? List.of()
                : contactRepository.findAllBySupplierIdInOrderByNameAsc(ids);

        return ResponseEntity.ok(suppliers.stream()
                .map(supplier -> toResponse(supplier, contacts.stream()
                        .filter(contact -> contact.getSupplierId().equals(supplier.getId()))
                        .toList()))
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca fornecedor por id")
    public ResponseEntity<SupplierResponse> findById(@PathVariable UUID id) {
        SupplierJpaEntity supplier = findSupplier(id);
        return ResponseEntity.ok(toResponse(supplier, contactRepository.findAllBySupplierIdOrderByNameAsc(id)));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Cria fornecedor com contatos opcionais")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        String taxId = request.taxId().trim();
        boolean active = request.active() == null || request.active();
        if (active && supplierRepository.existsByTaxIdAndActiveIsTrue(taxId)) {
            throw new BusinessException("DUPLICATE_SUPPLIER_TAX_ID", "CNPJ/CPF ja cadastrado para fornecedor ativo", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        UUID userId = AuthenticatedUser.requireUserId();
        PurchasingAuditService.AuditStamp stamp = auditService.stampForCreate(userId);

        SupplierJpaEntity entity = new SupplierJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(now);
        apply(entity, request, now, stamp);
        supplierRepository.save(entity);

        List<SupplierContactJpaEntity> contacts = replaceContacts(entity.getId(), request.contacts(), now);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(entity, contacts));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualiza fornecedor e contatos")
    public ResponseEntity<SupplierResponse> update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        SupplierJpaEntity entity = findSupplier(id);
        String taxId = request.taxId().trim();
        boolean active = request.active() == null || request.active();
        if (active && supplierRepository.existsByTaxIdAndActiveIsTrueAndIdNot(taxId, id)) {
            throw new BusinessException("DUPLICATE_SUPPLIER_TAX_ID", "CNPJ/CPF ja cadastrado para fornecedor ativo", HttpStatus.CONFLICT.value());
        }

        Instant now = Instant.now();
        apply(entity, request, now, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        supplierRepository.save(entity);

        List<SupplierContactJpaEntity> contacts = request.contacts() == null
                ? contactRepository.findAllBySupplierIdOrderByNameAsc(id)
                : replaceContacts(id, request.contacts(), now);

        return ResponseEntity.ok(toResponse(entity, contacts));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Inativa fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        SupplierJpaEntity entity = findSupplier(id);
        entity.setActive(false);
        entity.setStatus("INATIVO");
        entity.setUpdatedAt(Instant.now());
        stampUpdate(entity, auditService.stampForUpdate(AuthenticatedUser.requireUserId()));
        supplierRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contacts")
    @Operation(summary = "Lista contatos do fornecedor")
    public ResponseEntity<List<ContactResponse>> contacts(@PathVariable UUID id) {
        findSupplier(id);
        return ResponseEntity.ok(contactRepository.findAllBySupplierIdOrderByNameAsc(id).stream()
                .map(SupplierRestController::toContactResponse)
                .toList());
    }

    private List<SupplierContactJpaEntity> replaceContacts(UUID supplierId, List<ContactRequest> requests, Instant now) {
        contactRepository.deleteAllBySupplierId(supplierId);
        contactRepository.flush();

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        long primaryCount = requests.stream()
                .filter(contact -> Boolean.TRUE.equals(contact.isPrimary()))
                .filter(contact -> contact.active() == null || contact.active())
                .count();
        if (primaryCount > 1) {
            throw new BusinessException("MULTIPLE_PRIMARY_CONTACTS", "Apenas um contato principal e permitido", HttpStatus.BAD_REQUEST.value());
        }

        java.util.HashSet<String> emails = new java.util.HashSet<>();
        List<SupplierContactJpaEntity> entities = requests.stream()
                .map(request -> {
                    String email = request.email() == null ? "" : request.email().trim();
                    if (email.isBlank()) {
                        throw new BusinessException("CONTACT_EMAIL_REQUIRED", "Informe o e-mail de contato", HttpStatus.BAD_REQUEST.value());
                    }
                    if (!emails.add(email.toLowerCase())) {
                        throw new BusinessException("DUPLICATE_CONTACT_EMAIL", "E-mail de contato duplicado: " + email, HttpStatus.BAD_REQUEST.value());
                    }
                    String name = request.name() == null || request.name().isBlank() ? email : request.name().trim();
                    SupplierContactJpaEntity contact = new SupplierContactJpaEntity();
                    contact.setId(UUID.randomUUID());
                    contact.setSupplierId(supplierId);
                    contact.setName(name);
                    contact.setDepartment(request.department());
                    contact.setJobTitle(request.jobTitle());
                    contact.setPhone(request.phone());
                    contact.setMobile(request.mobile());
                    contact.setEmail(email);
                    contact.setPrimary(Boolean.TRUE.equals(request.isPrimary()));
                    contact.setActive(request.active() == null || request.active());
                    contact.setCreatedAt(now);
                    contact.setUpdatedAt(now);
                    return contact;
                })
                .toList();

        return contactRepository.saveAll(entities);
    }

    private SupplierJpaEntity findSupplier(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUPPLIER_NOT_FOUND", "Fornecedor nao encontrado", HttpStatus.NOT_FOUND.value()));
    }

    private static void stampCreate(SupplierJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampCreate(
                stamp.createdById(),
                stamp.createdByName(),
                entity::setCreatedBy,
                entity::setCreatedByName,
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private static void stampUpdate(SupplierJpaEntity entity, PurchasingAuditService.AuditStamp stamp) {
        PurchasingAuditSupport.stampUpdate(
                stamp.updatedById(),
                stamp.updatedByName(),
                entity::setUpdatedBy,
                entity::setUpdatedByName
        );
    }

    private String resolveTypeCode(String supplierType) {
        String code = supplierType == null ? "" : supplierType.trim();
        SupplierTypeJpaEntity type = typeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException(
                        "SUPPLIER_TYPE_NOT_FOUND",
                        "Tipo de fornecedor nao encontrado: " + code,
                        HttpStatus.BAD_REQUEST.value()
                ));
        if (!type.isActive()) {
            throw new BusinessException("SUPPLIER_TYPE_INACTIVE", "Tipo de fornecedor inativo: " + type.getCode(), HttpStatus.BAD_REQUEST.value());
        }
        return type.getCode();
    }

    private String resolveStatusCode(String status) {
        String code = status == null || status.isBlank() ? "ATIVO" : status.trim();
        SupplierStatusJpaEntity entity = statusRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException(
                        "SUPPLIER_STATUS_NOT_FOUND",
                        "Status de fornecedor nao encontrado: " + code,
                        HttpStatus.BAD_REQUEST.value()
                ));
        if (!entity.isActive()) {
            throw new BusinessException("SUPPLIER_STATUS_INACTIVE", "Status de fornecedor inativo: " + entity.getCode(), HttpStatus.BAD_REQUEST.value());
        }
        return entity.getCode();
    }

    private void apply(SupplierJpaEntity entity, SupplierRequest request, Instant now, PurchasingAuditService.AuditStamp stamp) {
        entity.setLegalName(request.legalName().trim());
        entity.setTradeName(request.tradeName());
        entity.setPersonType(request.personType().trim().toUpperCase());
        entity.setTaxId(request.taxId().trim());
        entity.setStateRegistration(request.stateRegistration());
        entity.setMunicipalRegistration(request.municipalRegistration());
        entity.setSupplierType(resolveTypeCode(request.supplierType()));
        entity.setStatus(resolveStatusCode(request.status()));
        entity.setDefaultCurrency(request.defaultCurrency() == null ? "BRL" : request.defaultCurrency().trim().toUpperCase());
        entity.setDefaultPaymentTerms(request.defaultPaymentTerms());
        entity.setAverageLeadTimeDays(request.averageLeadTimeDays());
        entity.setMinimumOrderValue(request.minimumOrderValue());
        entity.setBuyerUserId(request.buyerUserId());
        entity.setZipCode(request.zipCode());
        entity.setAddressLine(request.addressLine());
        entity.setAddressNumber(request.addressNumber());
        entity.setAddressComplement(request.addressComplement());
        entity.setDistrict(request.district());
        entity.setCity(request.city());
        entity.setStateCode(request.stateCode());
        entity.setCountryCode(request.countryCode() == null ? "BR" : request.countryCode().trim().toUpperCase());
        entity.setNotes(request.notes());
        entity.setActive(request.active() == null || request.active());
        entity.setUpdatedAt(now);
        if (stamp.createdById() != null) {
            stampCreate(entity, stamp);
        } else {
            stampUpdate(entity, stamp);
        }
    }

    private static SupplierResponse toResponse(SupplierJpaEntity entity, List<SupplierContactJpaEntity> contacts) {
        return new SupplierResponse(
                entity.getId(),
                entity.getLegalName(),
                entity.getTradeName(),
                entity.getPersonType(),
                entity.getTaxId(),
                entity.getStateRegistration(),
                entity.getMunicipalRegistration(),
                entity.getSupplierType(),
                entity.getStatus(),
                entity.getDefaultCurrency(),
                entity.getDefaultPaymentTerms(),
                entity.getAverageLeadTimeDays(),
                entity.getMinimumOrderValue(),
                entity.getBuyerUserId(),
                entity.getZipCode(),
                entity.getAddressLine(),
                entity.getAddressNumber(),
                entity.getAddressComplement(),
                entity.getDistrict(),
                entity.getCity(),
                entity.getStateCode(),
                entity.getCountryCode(),
                entity.getNotes(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getCreatedByName(),
                entity.getUpdatedBy(),
                entity.getUpdatedByName(),
                contacts.stream().map(SupplierRestController::toContactResponse).toList()
        );
    }

    private static ContactResponse toContactResponse(SupplierContactJpaEntity entity) {
        return new ContactResponse(
                entity.getId(),
                entity.getSupplierId(),
                entity.getName(),
                entity.getDepartment(),
                entity.getJobTitle(),
                entity.getPhone(),
                entity.getMobile(),
                entity.getEmail(),
                entity.isPrimary(),
                entity.isActive()
        );
    }

    public record SupplierRequest(
            @NotBlank @Size(max = 200) String legalName,
            @Size(max = 200) String tradeName,
            @NotBlank @Pattern(regexp = "[JFjf]") String personType,
            @NotBlank @Size(max = 20) String taxId,
            @Size(max = 30) String stateRegistration,
            @Size(max = 30) String municipalRegistration,
            @NotBlank @Size(max = 30) String supplierType,
            @Size(max = 30) String status,
            @Size(max = 3) String defaultCurrency,
            @Size(max = 100) String defaultPaymentTerms,
            Integer averageLeadTimeDays,
            BigDecimal minimumOrderValue,
            UUID buyerUserId,
            @Size(max = 10) String zipCode,
            @Size(max = 200) String addressLine,
            @Size(max = 20) String addressNumber,
            @Size(max = 100) String addressComplement,
            @Size(max = 100) String district,
            @Size(max = 100) String city,
            @Size(max = 10) String stateCode,
            @Size(max = 2) String countryCode,
            String notes,
            Boolean active,
            List<ContactRequest> contacts
    ) {
    }

    public record ContactRequest(
            @Size(max = 150) String name,
            @Size(max = 100) String department,
            @Size(max = 100) String jobTitle,
            @Size(max = 30) String phone,
            @Size(max = 30) String mobile,
            @NotBlank @Size(max = 200) String email,
            @JsonProperty("isPrimary") Boolean isPrimary,
            Boolean active
    ) {
    }

    public record SupplierResponse(
            UUID id,
            String legalName,
            String tradeName,
            String personType,
            String taxId,
            String stateRegistration,
            String municipalRegistration,
            String supplierType,
            String status,
            String defaultCurrency,
            String defaultPaymentTerms,
            Integer averageLeadTimeDays,
            BigDecimal minimumOrderValue,
            UUID buyerUserId,
            String zipCode,
            String addressLine,
            String addressNumber,
            String addressComplement,
            String district,
            String city,
            String stateCode,
            String countryCode,
            String notes,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            UUID createdById,
            String createdByName,
            UUID updatedById,
            String updatedByName,
            List<ContactResponse> contacts
    ) {
    }

    public record ContactResponse(
            UUID id,
            UUID supplierId,
            String name,
            String department,
            String jobTitle,
            String phone,
            String mobile,
            String email,
            @JsonProperty("isPrimary") boolean isPrimary,
            boolean active
    ) {
    }
}
