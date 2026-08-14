package com.uniondesk.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.auth.entity.AuthCustomerTrustedLoginIpPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthCustomerTrustedLoginIpMapper extends BaseMapper<AuthCustomerTrustedLoginIpPo> {

    default AuthCustomerTrustedLoginIpPo selectByUserIdAndIp(long userId, String clientIp) {
        return selectOneByQuery(QueryWrapper.create()
                .from(AuthCustomerTrustedLoginIpPo.class)
                .where(AuthCustomerTrustedLoginIpPo::getUserId).eq(userId)
                .and(AuthCustomerTrustedLoginIpPo::getClientIp).eq(clientIp));
    }

    void upsert(
            @Param("userId") long userId,
            @Param("clientIp") String clientIp,
            @Param("lastUsedAt") LocalDateTime lastUsedAt);

    default int countByUserId(long userId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(AuthCustomerTrustedLoginIpPo.class)
                .where(AuthCustomerTrustedLoginIpPo::getUserId).eq(userId));
    }

    int deleteOldestByUserId(@Param("userId") long userId, @Param("limit") int limit);
}
