package com.uniondesk.domain.repository;

import com.uniondesk.domain.mapper.DomainMemberMapper;
import com.uniondesk.domain.mapper.DomainMemberRoleMapper;
import com.uniondesk.domain.mapper.DomainRoleMapper;
import com.uniondesk.domain.mapper.IamRoleBindingMapper;
import com.uniondesk.domain.mapper.StaffAccountMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DomainBootstrapRepository {

    private final DomainRoleMapper domainRoleMapper;
    private final StaffAccountMapper staffAccountMapper;
    private final DomainMemberMapper domainMemberMapper;
    private final DomainMemberRoleMapper domainMemberRoleMapper;
    private final IamRoleBindingMapper iamRoleBindingMapper;

    public DomainBootstrapRepository(
            DomainRoleMapper domainRoleMapper,
            StaffAccountMapper staffAccountMapper,
            DomainMemberMapper domainMemberMapper,
            DomainMemberRoleMapper domainMemberRoleMapper,
            IamRoleBindingMapper iamRoleBindingMapper) {
        this.domainRoleMapper = domainRoleMapper;
        this.staffAccountMapper = staffAccountMapper;
        this.domainMemberMapper = domainMemberMapper;
        this.domainMemberRoleMapper = domainMemberRoleMapper;
        this.iamRoleBindingMapper = iamRoleBindingMapper;
    }

    public void insertRoleIfNotExists(long domainId, String code, String name) {
        domainRoleMapper.insertIfNotExists(domainId, code, name);
    }

    public void seedSuperAdminAllPermissions(long superAdminRoleId) {
        domainRoleMapper.seedSuperAdminAllPermissions(superAdminRoleId);
    }

    public Long findStaffAccountIdById(long userId) {
        return staffAccountMapper.selectIdById(userId);
    }

    public String findUsernameByUserId(long userId) {
        return staffAccountMapper.selectUsernameById(userId);
    }

    public Long findStaffAccountIdByUsername(String username) {
        return staffAccountMapper.selectIdByUsername(username);
    }

    public Long findActiveMemberId(long domainId, long staffAccountId) {
        return domainMemberMapper.findActiveMemberId(domainId, staffAccountId);
    }

    public void insertDomainMember(long staffAccountId, long domainId) {
        domainMemberMapper.insertWithSource(staffAccountId, domainId, "domain_create");
    }

    public Long findDomainRoleId(long domainId, String code) {
        return domainRoleMapper.selectIdByDomainAndCode(domainId, code);
    }

    public void insertMemberRoleIfNotExists(long memberId, long roleId) {
        domainMemberRoleMapper.insertIfNotExists(memberId, roleId);
    }

    public Integer findLegacyRoleId(String code) {
        return iamRoleBindingMapper.selectLegacyRoleId(code);
    }

    public void insertIamRoleBindingIfNotExists(long userId, int roleId, long domainId) {
        iamRoleBindingMapper.insertIfNotExists(userId, roleId, domainId);
    }
}
