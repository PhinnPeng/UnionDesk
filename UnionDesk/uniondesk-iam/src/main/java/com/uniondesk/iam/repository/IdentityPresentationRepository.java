package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.DomainMemberPresentationPo;
import com.uniondesk.iam.entity.StaffAccountPresentationPo;
import com.uniondesk.iam.mapper.DomainMemberMapper;
import com.uniondesk.iam.mapper.StaffAccountMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class IdentityPresentationRepository {

    private final StaffAccountMapper staffAccountMapper;
    private final DomainMemberMapper domainMemberMapper;

    public IdentityPresentationRepository(StaffAccountMapper staffAccountMapper, DomainMemberMapper domainMemberMapper) {
        this.staffAccountMapper = staffAccountMapper;
        this.domainMemberMapper = domainMemberMapper;
    }

    public Optional<StaffAccountPresentationPo> findStaffPresentation(long staffAccountId) {
        return Optional.ofNullable(staffAccountMapper.selectPresentationById(staffAccountId));
    }

    public Optional<DomainMemberPresentationPo> findDomainMemberPresentation(long staffAccountId, long domainId) {
        return Optional.ofNullable(domainMemberMapper.selectPresentation(staffAccountId, domainId));
    }
}
