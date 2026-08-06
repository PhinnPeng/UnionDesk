package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.AuthCustomerTrustedLoginIpPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthCustomerTrustedLoginIpMapper {

    AuthCustomerTrustedLoginIpPo selectByUserIdAndIp(
            @Param("userId") long userId,
            @Param("clientIp") String clientIp);

    void upsert(
            @Param("userId") long userId,
            @Param("clientIp") String clientIp,
            @Param("lastUsedAt") java.time.LocalDateTime lastUsedAt);

    int countByUserId(@Param("userId") long userId);

    int deleteOldestByUserId(@Param("userId") long userId, @Param("limit") int limit);
}
