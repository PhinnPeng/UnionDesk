package com.uniondesk.common.web;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api/v1/readiness")
    public Map<String, Object> readiness() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("db", checkDatabase());
        return result;
    }

    private Map<String, String> checkDatabase() {
        try (var conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            return Map.of("status", "UP");
        } catch (Exception ex) {
            return Map.of("status", "DOWN", "error", ex.getMessage());
        }
    }
}
