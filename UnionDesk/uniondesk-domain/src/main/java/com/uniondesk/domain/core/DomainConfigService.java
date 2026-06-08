package com.uniondesk.domain.core;

import com.uniondesk.domain.entity.DomainConfigPo;
import com.uniondesk.domain.repository.DomainConfigRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainConfigService {

    private final DomainConfigRepository domainConfigRepository;

    public DomainConfigService(DomainConfigRepository domainConfigRepository) {
        this.domainConfigRepository = domainConfigRepository;
    }

    @Transactional(readOnly = true)
    public DomainConfigView load(long domainId) {
        List<ConfigItemView> items = domainConfigRepository.findByDomainId(domainId).stream()
                .map(this::toConfigItemView)
                .toList();
        return new DomainConfigView(domainId, List.copyOf(items));
    }

    @Transactional
    public DomainConfigView update(long domainId, DomainConfigUpdateCommand command) {
        for (ConfigItemCommand item : command.items()) {
            if (!StringUtils.hasText(item.key())) {
                continue;
            }
            domainConfigRepository.upsert(
                    domainId,
                    item.key().trim(),
                    item.value(),
                    StringUtils.hasText(item.valueType()) ? item.valueType().trim() : "string",
                    item.description());
        }
        return load(domainId);
    }

    private ConfigItemView toConfigItemView(DomainConfigPo po) {
        return new ConfigItemView(
                po.getConfigKey(),
                po.getConfigValue(),
                po.getValueType(),
                po.getDescription(),
                po.getUpdatedAt());
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
