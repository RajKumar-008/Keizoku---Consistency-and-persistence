package com.dayforge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PingController {

    private final DataSource dataSource;

    public PingController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("app", "KEIZOKU - Consistency and Persistence");
        response.put("timestamp", LocalDateTime.now());

        // Probe database connection health without exposing any credentials
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            response.put("databaseStatus", "CONNECTED");
            response.put("databaseEngine", meta.getDatabaseProductName());
            response.put("databaseVersion", meta.getDatabaseProductVersion());
            response.put("schemaValid", true);
        } catch (Exception e) {
            response.put("databaseStatus", "DISCONNECTED");
            response.put("databaseError", e.getMessage());
            response.put("schemaValid", false);
        }

        return ResponseEntity.ok(response);
    }
}
