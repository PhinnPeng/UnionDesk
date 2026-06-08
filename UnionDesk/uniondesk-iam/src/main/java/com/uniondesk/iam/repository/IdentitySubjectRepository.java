package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.IdentitySubjectPo;
import com.uniondesk.iam.mapper.IdentitySubjectMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class IdentitySubjectRepository {

    private final IdentitySubjectMapper mapper;

    public IdentitySubjectRepository(IdentitySubjectMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<Long> findIdByPhone(String phone) {
        return Optional.ofNullable(mapper.selectIdByPhone(phone));
    }

    public Optional<IdentitySubjectPo> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Optional<Long> findMergedIntoId(long id) {
        return Optional.ofNullable(mapper.selectMergedIntoId(id));
    }

    public void insert(IdentitySubjectPo po) {
        mapper.insert(po);
    }
}
