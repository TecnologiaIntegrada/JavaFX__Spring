package com.projetoj.purchasing.catalog.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataUnitOfMeasureJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades-medida")
@Tag(name = "Unidades de medida", description = "Cadastro de unidades de medida")
public class UnitOfMeasureRestController {

    private final SpringDataUnitOfMeasureJpaRepository unitRepository;

    public UnitOfMeasureRestController(SpringDataUnitOfMeasureJpaRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @GetMapping
    @Operation(summary = "Lista unidades de medida ativas")
    public ResponseEntity<List<UnitOfMeasureResponse>> list() {
        return ResponseEntity.ok(unitRepository.findAllByActiveTrueOrderByCodeAsc().stream()
                .map(unit -> new UnitOfMeasureResponse(unit.getId(), unit.getCode(), unit.getName(), unit.isActive()))
                .toList());
    }

    public record UnitOfMeasureResponse(
            UUID id,
            String code,
            String name,
            boolean active
    ) {
    }
}
