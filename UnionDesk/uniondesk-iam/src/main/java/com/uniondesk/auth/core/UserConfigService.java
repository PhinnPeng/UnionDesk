package com.uniondesk.auth.core;

import com.uniondesk.auth.entity.UserConfigPo;
import com.uniondesk.auth.repository.UserConfigRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserConfigService {

    public static final String KEY_DEFAULT_BUSINESS_DOMAIN_ID = "default_business_domain_id";

    private final UserConfigRepository userConfigRepository;

    public UserConfigService(UserConfigRepository userConfigRepository) {
        this.userConfigRepository = userConfigRepository;
    }

    public Optional<String> get(long userId, String configKey) {
        return userConfigRepository.findByUserIdAndKey(userId, configKey)
                .map(UserConfigPo::getConfigValue)
                .filter(StringUtils::hasText);
    }

    public Optional<Long> getPreferredDefaultDomainId(long userId) {
        return get(userId, KEY_DEFAULT_BUSINESS_DOMAIN_ID)
                .map(value -> {
                    try {
                        return Long.parseLong(value.trim());
                    }
                    catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(id -> id != null && id > 0);
    }

    public void upsert(long userId, String configKey, String configValue, String valueType, String description) {
        userConfigRepository.upsert(userId, configKey, configValue, valueType, description);
    }

    public void upsertPreferredDefaultDomainId(long userId, long domainId) {
        upsert(userId, KEY_DEFAULT_BUSINESS_DOMAIN_ID, String.valueOf(domainId), "string", "默认业务域偏好");
    }
}
