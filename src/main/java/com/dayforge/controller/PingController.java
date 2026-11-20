package com.dayforge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "app", "KEIZOKU - Consistency and Persistence",
            "timestamp", LocalDateTime.now()
        ));
    }
}

