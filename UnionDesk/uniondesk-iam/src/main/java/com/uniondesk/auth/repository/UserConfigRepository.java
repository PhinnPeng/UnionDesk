package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.UserConfigPo;
import com.uniondesk.auth.mapper.UserConfigMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserConfigRepository {

    private final UserConfigMapper mapper;

    public UserConfigRepository(UserConfigMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UserConfigPo> findByUserIdAndKey(long userId, String configKey) {
        return Optional.ofNullable(mapper.selectByUserIdAndKey(userId, configKey));
    }

    public void upsert(long userId, String configKey, String configValue, String valueType, String description) {
        mapper.upsert(userId, configKey, configValue, valueType, description);
    }
}
