package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.mapper.CustomerAccountMapper;
import org.springframework.stereotype.Repository;

@Repository("ticketCustomerAccountRepository")
public class CustomerAccountRepository {

    private final CustomerAccountMapper mapper;

    public CustomerAccountRepository(CustomerAccountMapper mapper) {
        this.mapper = mapper;
    }

    public Long findSubjectIdById(long id) {
        return mapper.findSubjectIdById(id);
    }
}
