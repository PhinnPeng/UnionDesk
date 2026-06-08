package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.AuthLoginConfigPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthLoginConfigMapper {

    List<AuthLoginConfigPo> selectAll();

    void upsert(@Param("configKey") String configKey, @Param("configValue") String configValue);

    LocalDateTime selectMaxUpdatedAt();
}
