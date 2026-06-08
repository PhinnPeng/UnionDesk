package com.uniondesk.config.core;

import com.uniondesk.config.entity.SystemConfigPo;
import com.uniondesk.config.repository.SystemConfigRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Transactional(readOnly = true)
    public SystemConfigView load() {
        List<ConfigItemView> items = systemConfigRepository.findAll().stream()
                .map(this::toConfigItemView)
                .toList();
        return new SystemConfigView(List.copyOf(items));
    }

    @Transactional
    public SystemConfigView update(SystemConfigUpdateCommand command) {
        for (ConfigItemCommand item : command.items()) {
            if (!StringUtils.hasText(item.key())) {
                continue;
            }
            systemConfigRepository.upsert(
                    item.key().trim(),
                    item.value(),
                    StringUtils.hasText(item.valueType()) ? item.valueType().trim() : "string",
                    item.description());
        }
        return load();
    }

    private ConfigItemView toConfigItemView(SystemConfigPo po) {
        return new ConfigItemView(
                po.getConfigKey(),
                po.getConfigValue(),
                po.getValueType(),
                po.getDescription(),
                po.getUpdatedAt());
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
