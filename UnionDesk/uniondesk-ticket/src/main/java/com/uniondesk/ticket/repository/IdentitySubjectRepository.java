package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.IdentitySubjectPo;
import com.uniondesk.ticket.mapper.IdentitySubjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository("ticketIdentitySubjectRepository")
public class IdentitySubjectRepository {

    private final IdentitySubjectMapper mapper;

    public IdentitySubjectRepository(IdentitySubjectMapper mapper) {
        this.mapper = mapper;
    }

    public IdentitySubjectPo findById(long id) {
        return mapper.findById(id);
    }

    public Long findIdByPhone(String phone) {
        return mapper.findIdByPhone(phone);
    }

    public void insert(IdentitySubjectPo po) {
        mapper.insert(po);
    }

    public void insertOrIgnore(IdentitySubjectPo po) {
        try {
            mapper.insert(po);
        } catch (DuplicateKeyException ignored) {
            // already exists
        }
    }
}
