package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.LoginLogPo;
import com.uniondesk.auth.mapper.LoginLogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class LoginAuditRepository {

    private final LoginLogMapper mapper;

    public LoginAuditRepository(LoginLogMapper mapper) {
        this.mapper = mapper;
    }

    public void insert(LoginLogPo po) {
        mapper.insert(po);
    }

    public List<LoginLogPo> findRecentByEventType(String eventType, int limit) {
        return mapper.selectRecentByEventType(eventType, limit);
    }

    public Long findSubjectIdByCustomerAccountId(long customerAccountId) {
        return mapper.selectSubjectIdByCustomerAccountId(customerAccountId);
    }
}
