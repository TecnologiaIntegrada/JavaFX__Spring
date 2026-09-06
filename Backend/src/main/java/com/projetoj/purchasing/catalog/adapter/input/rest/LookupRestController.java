package com.projetoj.purchasing.catalog.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataCostCenterJpaRepository;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataDepartmentJpaRepository;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataProjectJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Lookups", description = "Cadastros auxiliares de rateio")
public class LookupRestController {

    private static final Sort BY_CODE = Sort.by("code");

    private final SpringDataDepartmentJpaRepository departmentRepository;
    private final SpringDataCostCenterJpaRepository costCenterRepository;
    private final SpringDataProjectJpaRepository projectRepository;

    public LookupRestController(
            SpringDataDepartmentJpaRepository departmentRepository,
            SpringDataCostCenterJpaRepository costCenterRepository,
            SpringDataProjectJpaRepository projectRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.costCenterRepository = costCenterRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/api/v1/departments")
    @Operation(summary = "Lista departamentos")
    public ResponseEntity<List<LookupResponse>> departments() {
        return ResponseEntity.ok(departmentRepository.findAll(BY_CODE).stream()
                .map(entity -> new LookupResponse(entity.getId(), entity.getCode(), entity.getName(), null, entity.isActive()))
                .toList());
    }

    @GetMapping("/api/v1/cost-centers")
    @Operation(summary = "Lista centros de custo")
    public ResponseEntity<List<LookupResponse>> costCenters() {
        return ResponseEntity.ok(costCenterRepository.findAll(BY_CODE).stream()
                .map(entity -> new LookupResponse(entity.getId(), entity.getCode(), entity.getName(), null, entity.isActive()))
                .toList());
    }

    @GetMapping("/api/v1/projects")
    @Operation(summary = "Lista projetos")
    public ResponseEntity<List<LookupResponse>> projects() {
        return ResponseEntity.ok(projectRepository.findAll(BY_CODE).stream()
                .map(entity -> new LookupResponse(entity.getId(), entity.getCode(), entity.getName(), entity.getStatus(), entity.isActive()))
                .toList());
    }

    public record LookupResponse(UUID id, String code, String name, String status, boolean active) {
    }
}
