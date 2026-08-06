package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.AuthCustomerTrustedLoginIpPo;
import com.uniondesk.auth.mapper.AuthCustomerTrustedLoginIpMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TrustedLoginIpRepository {

    private final AuthCustomerTrustedLoginIpMapper mapper;

    public TrustedLoginIpRepository(AuthCustomerTrustedLoginIpMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<AuthCustomerTrustedLoginIpPo> findByUserIdAndIp(long userId, String clientIp) {
        return Optional.ofNullable(mapper.selectByUserIdAndIp(userId, clientIp));
    }

    public void upsert(long userId, String clientIp, LocalDateTime lastUsedAt) {
        mapper.upsert(userId, clientIp, lastUsedAt);
    }

    public int countByUserId(long userId) {
        return mapper.countByUserId(userId);
    }

    public int deleteOldestByUserId(long userId, int limit) {
        if (limit <= 0) {
            return 0;
        }
        return mapper.deleteOldestByUserId(userId, limit);
    }
}
