package com.uniondesk.config.mapper;

import com.uniondesk.config.entity.SystemConfigPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemConfigMapper {

    List<SystemConfigPo> selectAll();

    void upsert(@Param("configKey") String configKey,
                @Param("configValue") String configValue,
                @Param("valueType") String valueType,
                @Param("description") String description);
}
