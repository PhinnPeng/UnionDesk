package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.mapper.StaffAccountMapper;
import org.springframework.stereotype.Repository;

@Repository("ticketStaffAccountRepository")
public class StaffAccountRepository {

    private final StaffAccountMapper mapper;

    public StaffAccountRepository(StaffAccountMapper mapper) {
        this.mapper = mapper;
    }

    public Long findSubjectIdById(long id) {
        return mapper.findSubjectIdById(id);
    }
}
