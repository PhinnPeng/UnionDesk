package com.uniondesk.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.auth.entity.AuthLoginConfigPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthLoginConfigMapper extends BaseMapper<AuthLoginConfigPo> {

    default List<AuthLoginConfigPo> selectAll() {
        return selectListByQuery(QueryWrapper.create().from(AuthLoginConfigPo.class));
    }

    void upsert(@Param("configKey") String configKey, @Param("configValue") String configValue);

    LocalDateTime selectMaxUpdatedAt();
}
