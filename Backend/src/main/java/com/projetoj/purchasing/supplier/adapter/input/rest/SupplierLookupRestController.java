package com.projetoj.purchasing.supplier.adapter.input.rest;

import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierStatusJpaRepository;
import com.projetoj.purchasing.supplier.adapter.output.persistence.SpringDataSupplierTypeJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Supplier lookups", description = "Dominios de tipo e status de fornecedor")
public class SupplierLookupRestController {

    private final SpringDataSupplierTypeJpaRepository typeRepository;
    private final SpringDataSupplierStatusJpaRepository statusRepository;

    public SupplierLookupRestController(
            SpringDataSupplierTypeJpaRepository typeRepository,
            SpringDataSupplierStatusJpaRepository statusRepository
    ) {
        this.typeRepository = typeRepository;
        this.statusRepository = statusRepository;
    }

    @GetMapping("/tipos-fornecedor")
    @Operation(summary = "Lista tipos de fornecedor ativos")
    public ResponseEntity<List<LookupResponse>> types() {
        return ResponseEntity.ok(typeRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(item -> new LookupResponse(item.getId(), item.getCode(), item.getName(), item.isActive()))
                .toList());
    }

    @GetMapping("/status-fornecedor")
    @Operation(summary = "Lista status de fornecedor ativos")
    public ResponseEntity<List<LookupResponse>> statuses() {
        return ResponseEntity.ok(statusRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(item -> new LookupResponse(item.getId(), item.getCode(), item.getName(), item.isActive()))
                .toList());
    }

    public record LookupResponse(
            UUID id,
            String code,
            String name,
            boolean active
    ) {
    }
}
