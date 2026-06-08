package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.DomainConfigPo;
import com.uniondesk.domain.mapper.DomainConfigMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DomainConfigRepository {

    private final DomainConfigMapper domainConfigMapper;

    public DomainConfigRepository(DomainConfigMapper domainConfigMapper) {
        this.domainConfigMapper = domainConfigMapper;
    }

    public List<DomainConfigPo> findByDomainId(long domainId) {
        return domainConfigMapper.selectByDomainId(domainId);
    }

    public void upsert(long businessDomainId, String configKey, String configValue, String valueType, String description) {
        domainConfigMapper.upsert(businessDomainId, configKey, configValue, valueType, description);
    }
}
