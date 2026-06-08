package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.DomainMemberPo;
import com.uniondesk.domain.entity.MemberRolePo;
import com.uniondesk.domain.entity.StaffCandidatePo;
import com.uniondesk.domain.mapper.DomainMemberMapper;
import com.uniondesk.domain.mapper.DomainMemberRoleMapper;
import com.uniondesk.domain.mapper.StaffAccountMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DomainMemberRepository {

    private final DomainMemberMapper domainMemberMapper;
    private final DomainMemberRoleMapper domainMemberRoleMapper;
    private final StaffAccountMapper staffAccountMapper;

    public DomainMemberRepository(
            DomainMemberMapper domainMemberMapper,
            DomainMemberRoleMapper domainMemberRoleMapper,
            StaffAccountMapper staffAccountMapper) {
        this.domainMemberMapper = domainMemberMapper;
        this.domainMemberRoleMapper = domainMemberRoleMapper;
        this.staffAccountMapper = staffAccountMapper;
    }

    public List<DomainMemberPo> findMembers(
            long domainId, String status, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo, int limit, long offset) {
        return domainMemberMapper.listMembers(domainId, status, keyword, createdFrom, createdTo, limit, offset);
    }

    public long countMembers(
            long domainId, String status, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo) {
        return domainMemberMapper.countMembers(domainId, status, keyword, createdFrom, createdTo);
    }

    public DomainMemberPo findMemberById(long id, long domainId) {
        return domainMemberMapper.selectMemberById(id, domainId);
    }

    public void insertMember(long staffAccountId, long businessDomainId) {
        domainMemberMapper.insert(staffAccountId, businessDomainId);
    }

    public void insertMemberWithSource(long staffAccountId, long businessDomainId, String source) {
        domainMemberMapper.insertWithSource(staffAccountId, businessDomainId, source);
    }

    public int updateMemberStatus(String status, long memberId, long domainId) {
        return domainMemberMapper.updateStatus(status, memberId, domainId);
    }

    public int softDeleteMember(long memberId, long domainId) {
        return domainMemberMapper.softDelete(memberId, domainId);
    }

    public int countActiveDomainAdmins(long domainId, long excludeMemberId) {
        return domainMemberMapper.countActiveDomainAdmins(domainId, excludeMemberId);
    }

    public int countActiveDomainSuperAdmins(long domainId, long excludeMemberId) {
        return domainMemberMapper.countActiveDomainSuperAdmins(domainId, excludeMemberId);
    }

    public int countByDomainAndStaff(long domainId, long staffAccountId) {
        return domainMemberMapper.countByDomainAndStaff(domainId, staffAccountId);
    }

    public Long findMemberIdByDomainAndStaff(long domainId, long staffAccountId) {
        return domainMemberMapper.findMemberIdByDomainAndStaff(domainId, staffAccountId);
    }

    public Long findActiveMemberId(long domainId, long staffAccountId) {
        return domainMemberMapper.findActiveMemberId(domainId, staffAccountId);
    }

    public void deleteMemberRoles(long memberId) {
        domainMemberRoleMapper.deleteByMemberId(memberId);
    }

    public void insertMemberRole(long memberId, long roleId) {
        domainMemberRoleMapper.insert(memberId, roleId);
    }

    public List<String> findRoleCodesByMemberId(long memberId) {
        return domainMemberRoleMapper.selectRoleCodesByMemberId(memberId);
    }

    public List<String> findRoleCodesByIds(long domainId, List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return domainMemberRoleMapper.selectRoleCodesByIds(domainId, roleIds);
    }

    public long countRolesByIds(long domainId, List<Long> roleIds) {
        return domainMemberRoleMapper.countRolesByIds(domainId, roleIds);
    }

    public List<MemberRolePo> findRolesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return domainMemberRoleMapper.selectRolesByMemberIds(memberIds);
    }

    public List<StaffCandidatePo> findStaffCandidates(long domainId, String keyword, int limit, long offset) {
        return domainMemberRoleMapper.listStaffCandidates(domainId, keyword, limit, offset);
    }

    public long countStaffCandidates(long domainId, String keyword) {
        return domainMemberRoleMapper.countStaffCandidates(domainId, keyword);
    }

    public int countStaffAccountById(long staffAccountId) {
        return staffAccountMapper.countById(staffAccountId);
    }
}
