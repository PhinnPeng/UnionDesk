package com.uniondesk.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.auth.entity.LoginLogPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogPo> {

    int insertRow(LoginLogPo po);

    default List<LoginLogPo> selectRecentByEventType(String eventType, int limit) {
        return selectListByQuery(QueryWrapper.create()
                .from(LoginLogPo.class)
                .where(LoginLogPo::getEventType).eq(eventType)
                .orderBy(LoginLogPo::getCreatedAt, false)
                .orderBy(LoginLogPo::getId, false)
                .limit(limit));
    }

    Long selectSubjectIdByCustomerAccountId(@Param("customerAccountId") long customerAccountId);

    Long selectSubjectIdByStaffAccountId(@Param("staffAccountId") long staffAccountId);

    int countPasswordFailuresSince(
            @Param("loginName") String loginName,
            @Param("portalType") String portalType,
            @Param("since") LocalDateTime since);
}
