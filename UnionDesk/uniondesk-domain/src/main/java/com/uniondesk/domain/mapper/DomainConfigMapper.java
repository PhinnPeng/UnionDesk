package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.DomainConfigPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainConfigMapper {

    List<DomainConfigPo> selectByDomainId(@Param("domainId") long domainId);

    void upsert(
            @Param("businessDomainId") long businessDomainId,
            @Param("configKey") String configKey,
            @Param("configValue") String configValue,
            @Param("valueType") String valueType,
            @Param("description") String description);
}
