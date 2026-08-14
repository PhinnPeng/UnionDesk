package com.uniondesk.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.auth.entity.UserConfigPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserConfigMapper extends BaseMapper<UserConfigPo> {

    default UserConfigPo selectByUserIdAndKey(long userId, String configKey) {
        return selectOneByQuery(QueryWrapper.create()
                .from(UserConfigPo.class)
                .where(UserConfigPo::getUserId).eq(userId)
                .and(UserConfigPo::getConfigKey).eq(configKey));
    }

    void upsert(@Param("userId") long userId,
                @Param("configKey") String configKey,
                @Param("configValue") String configValue,
                @Param("valueType") String valueType,
                @Param("description") String description);
}
