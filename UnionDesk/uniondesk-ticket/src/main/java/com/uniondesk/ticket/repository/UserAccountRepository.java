package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.UserAccountPo;
import com.uniondesk.ticket.mapper.UserAccountMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {

    private final UserAccountMapper mapper;

    public UserAccountRepository(UserAccountMapper mapper) {
        this.mapper = mapper;
    }

    public UserAccountPo findById(long id) {
        return mapper.findById(id);
    }
}
