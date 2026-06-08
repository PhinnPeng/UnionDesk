package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.AuthLoginConfigPo;
import com.uniondesk.auth.mapper.AuthLoginConfigMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class LoginConfigRepository {

    private final AuthLoginConfigMapper mapper;

    public LoginConfigRepository(AuthLoginConfigMapper mapper) {
        this.mapper = mapper;
    }

    public List<AuthLoginConfigPo> findAll() {
        return mapper.selectAll();
    }

    public void upsert(String configKey, String configValue) {
        mapper.upsert(configKey, configValue);
    }

    public LocalDateTime findMaxUpdatedAt() {
        return mapper.selectMaxUpdatedAt();
    }
}
