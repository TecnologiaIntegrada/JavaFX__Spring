package com.projetoj.purchasing.catalog.adapter.input.rest;

import com.projetoj.purchasing.catalog.adapter.output.persistence.CurrencyJpaEntity;
import com.projetoj.purchasing.catalog.adapter.output.persistence.SpringDataCurrencyJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moedas")
@Tag(name = "Moedas", description = "Cadastro de moedas")
public class CurrencyRestController {

    private final SpringDataCurrencyJpaRepository currencyRepository;

    public CurrencyRestController(SpringDataCurrencyJpaRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @GetMapping
    @Operation(summary = "Lista moedas ativas")
    public ResponseEntity<List<CurrencyResponse>> list() {
        return ResponseEntity.ok(currencyRepository.findAllByActiveTrueOrderByCodeAsc().stream()
                .map(c -> new CurrencyResponse(c.getId(), c.getCode(), c.getName(), c.getSymbol(), c.isActive()))
                .toList());
    }

    public record CurrencyResponse(
            UUID id,
            String code,
            String name,
            String symbol,
            boolean active
    ) {
    }
}
