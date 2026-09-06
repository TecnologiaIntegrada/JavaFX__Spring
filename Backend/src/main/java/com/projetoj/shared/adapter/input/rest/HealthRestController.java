package com.projetoj.shared.adapter.input.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Verificacao de disponibilidade da API e do banco")
public class HealthRestController {

    private final DataSource dataSource;

    public HealthRestController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    @Operation(summary = "Verifica se a API e o PostgreSQL estao operacionais")
    public ResponseEntity<Map<String, String>> health() {
        String databaseStatus = checkDatabase();
        String overallStatus = "UP".equals(databaseStatus) ? "UP" : "DEGRADED";

        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", overallStatus);
        body.put("api", "UP");
        body.put("database", databaseStatus);
        return ResponseEntity.ok(body);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
