package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.StaffAccountPo;
import com.uniondesk.iam.mapper.BusinessDomainMapper;
import com.uniondesk.iam.mapper.DomainMemberMapper;
import com.uniondesk.iam.mapper.DomainRoleMapper;
import com.uniondesk.iam.mapper.StaffAccountMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class StaffAccountRepository {

    private final StaffAccountMapper staffAccountMapper;
    private final DomainMemberMapper domainMemberMapper;
    private final DomainRoleMapper domainRoleMapper;
    private final BusinessDomainMapper businessDomainMapper;

    public StaffAccountRepository(
            StaffAccountMapper staffAccountMapper,
            DomainMemberMapper domainMemberMapper,
            DomainRoleMapper domainRoleMapper,
            BusinessDomainMapper businessDomainMapper) {
        this.staffAccountMapper = staffAccountMapper;
        this.domainMemberMapper = domainMemberMapper;
        this.domainRoleMapper = domainRoleMapper;
        this.businessDomainMapper = businessDomainMapper;
    }

    public List<StaffAccountPo> findAll() {
        return staffAccountMapper.selectAll();
    }

    public Optional<StaffAccountPo> findById(long id) {
        return Optional.ofNullable(staffAccountMapper.selectById(id));
    }

    public void insert(StaffAccountPo po) {
        staffAccountMapper.insert(po);
    }

    public int updateSelective(long id, String username, String realName, String nickname,
                               String phone, String email, String passwordHash, String status) {
        return staffAccountMapper.updateSelective(id, username, realName, nickname, phone, email, passwordHash, status);
    }

    public int updateStatus(long id, String status) {
        return staffAccountMapper.updateStatus(id, status);
    }

    public int revokeActiveSessions(long userId, String revokedReason) {
        return staffAccountMapper.revokeActiveSessions(userId, revokedReason);
    }

    public List<Long> findBusinessDomainIds(long staffAccountId) {
        return domainMemberMapper.selectDistinctDomainIds(staffAccountId);
    }

    public List<String> findDomainRoleCodes(long staffAccountId) {
        return domainMemberMapper.selectRoleCodes(staffAccountId);
    }

    public Optional<Long> findDomainMemberId(long domainId, long staffAccountId) {
        return Optional.ofNullable(domainMemberMapper.selectId(domainId, staffAccountId));
    }

    public void insertDomainMember(long staffAccountId, long domainId) {
        domainMemberMapper.insert(staffAccountId, domainId);
    }

    public void deleteDomainMemberRoles(long domainMemberId) {
        domainMemberMapper.deleteRolesByMemberId(domainMemberId);
    }

    public void insertDomainMemberRole(long domainMemberId, long domainRoleId) {
        domainMemberMapper.insertRole(domainMemberId, domainRoleId);
    }

    public Optional<Long> findDomainRoleId(long domainId, String code) {
        return Optional.ofNullable(domainRoleMapper.selectIdByDomainAndCode(domainId, code));
    }

    public int countActiveDomain(long domainId) {
        return businessDomainMapper.countActiveById(domainId);
    }
}
