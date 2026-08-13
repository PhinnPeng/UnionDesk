package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.LoginLogPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginLogMapper {

    void insert(LoginLogPo po);

    List<LoginLogPo> selectRecentByEventType(@Param("eventType") String eventType, @Param("limit") int limit);

    Long selectSubjectIdByCustomerAccountId(@Param("customerAccountId") long customerAccountId);

    Long selectSubjectIdByStaffAccountId(@Param("staffAccountId") long staffAccountId);

    int countPasswordFailuresSince(
            @Param("loginName") String loginName,
            @Param("portalType") String portalType,
            @Param("since") LocalDateTime since);
}
