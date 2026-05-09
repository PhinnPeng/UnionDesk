package com.uniondesk.config.core;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemConfigService {

    private final JdbcTemplate jdbcTemplate;

    public SystemConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public SystemConfigView load() {
        List<ConfigItemView> items = jdbcTemplate.query("""
                        SELECT config_key, config_value, value_type, description, updated_at
                        FROM system_config
                        ORDER BY config_key ASC
                        """,
                (rs, rowNum) -> new ConfigItemView(
                        rs.getString("config_key"),
                        rs.getString("config_value"),
                        rs.getString("value_type"),
                        rs.getString("description"),
                        rs.getTimestamp("updated_at").toLocalDateTime()));
        return new SystemConfigView(List.copyOf(items));
    }

    @Transactional
    public SystemConfigView update(SystemConfigUpdateCommand command) {
        for (ConfigItemCommand item : command.items()) {
            if (!StringUtils.hasText(item.key())) {
                continue;
            }
            jdbcTemplate.update("""
                            INSERT INTO system_config (
                                config_key, config_value, value_type, description
                            )
                            VALUES (?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                config_value = VALUES(config_value),
                                value_type = VALUES(value_type),
                                description = VALUES(description),
                                updated_at = CURRENT_TIMESTAMP(3)
                            """,
                    item.key().trim(),
                    item.value(),
                    StringUtils.hasText(item.valueType()) ? item.valueType().trim() : "string",
                    item.description());
        }
        return load();
    }

    public record SystemConfigView(List<ConfigItemView> items) {
    }

    public record ConfigItemView(
            String key,
            String value,
            String valueType,
            String description,
            LocalDateTime updatedAt) {
    }

    public record SystemConfigUpdateCommand(List<ConfigItemCommand> items) {

        public SystemConfigUpdateCommand {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record ConfigItemCommand(String key, String value, String valueType, String description) {
    }
}
