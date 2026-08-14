package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.DomainConfigPo;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainConfigMapper extends BaseMapper<DomainConfigPo> {

    default List<DomainConfigPo> selectByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(DomainConfigPo.class)
                .where(DomainConfigPo::getBusinessDomainId).eq(domainId)
                .orderBy(DomainConfigPo::getConfigKey, true));
    }

    @Insert("INSERT INTO domain_config ("
            + " business_domain_id, config_key, config_value, value_type, description"
            + ")"
            + " VALUES (#{businessDomainId}, #{configKey}, #{configValue}, #{valueType}, #{description})"
            + " ON DUPLICATE KEY UPDATE"
            + " config_value = VALUES(config_value),"
            + " value_type = VALUES(value_type),"
            + " description = VALUES(description),"
            + " updated_at = CURRENT_TIMESTAMP(3)")
    void upsert(
            @Param("businessDomainId") long businessDomainId,
            @Param("configKey") String configKey,
            @Param("configValue") String configValue,
            @Param("valueType") String valueType,
            @Param("description") String description);
}
