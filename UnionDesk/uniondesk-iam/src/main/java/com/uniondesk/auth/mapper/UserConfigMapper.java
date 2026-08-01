package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.UserConfigPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserConfigMapper {

    UserConfigPo selectByUserIdAndKey(@Param("userId") long userId, @Param("configKey") String configKey);

    void upsert(@Param("userId") long userId,
                @Param("configKey") String configKey,
                @Param("configValue") String configValue,
                @Param("valueType") String valueType,
                @Param("description") String description);
}
