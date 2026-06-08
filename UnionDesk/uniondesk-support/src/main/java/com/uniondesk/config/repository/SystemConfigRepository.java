package com.uniondesk.config.repository;

import com.uniondesk.config.entity.SystemConfigPo;
import com.uniondesk.config.mapper.SystemConfigMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SystemConfigRepository {

    private final SystemConfigMapper mapper;

    public SystemConfigRepository(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    public List<SystemConfigPo> findAll() {
        return mapper.selectAll();
    }

    public void upsert(String configKey, String configValue, String valueType, String description) {
        mapper.upsert(configKey, configValue, valueType, description);
    }
}
