package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainConfigService {

    private final JdbcTemplate jdbcTemplate;

    public DomainConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public DomainConfigView load(long domainId) {
        List<ConfigItemView> items = jdbcTemplate.query("""
                        SELECT config_key, config_value, value_type, description, updated_at
                        FROM domain_config
                        WHERE business_domain_id = ?
                        ORDER BY config_key ASC
                        """,
                (rs, rowNum) -> new ConfigItemView(
                        rs.getString("config_key"),
                        rs.getString("config_value"),
                        rs.getString("value_type"),
                        rs.getString("description"),
                        rs.getTimestamp("updated_at").toLocalDateTime()),
                domainId);
        return new DomainConfigView(domainId, List.copyOf(items));
    }

    @Transactional
    public DomainConfigView update(long domainId, DomainConfigUpdateCommand command) {
        for (ConfigItemCommand item : command.items()) {
            if (!StringUtils.hasText(item.key())) {
                continue;
            }
            jdbcTemplate.update("""
                            INSERT INTO domain_config (
                                business_domain_id, config_key, config_value, value_type, description
                            )
                            VALUES (?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                                config_value = VALUES(config_value),
                                value_type = VALUES(value_type),
                                description = VALUES(description),
                                updated_at = CURRENT_TIMESTAMP(3)
                            """,
                    domainId,
                    item.key().trim(),
                    item.value(),
                    StringUtils.hasText(item.valueType()) ? item.valueType().trim() : "string",
                    item.description());
        }
        return load(domainId);
    }

    public record DomainConfigView(long domainId, List<ConfigItemView> items) {
    }

    public record ConfigItemView(
            String key,
            String value,
            String valueType,
            String description,
            LocalDateTime updatedAt) {
    }

    public record DomainConfigUpdateCommand(List<ConfigItemCommand> items) {

        public DomainConfigUpdateCommand {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record ConfigItemCommand(String key, String value, String valueType, String description) {
    }
}
