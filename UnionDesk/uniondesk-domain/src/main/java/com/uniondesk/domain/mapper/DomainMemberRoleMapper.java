package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.MemberRolePo;
import com.uniondesk.domain.entity.StaffCandidatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainMemberRoleMapper {

    void deleteByMemberId(@Param("memberId") long memberId);

    void insert(@Param("memberId") long memberId, @Param("roleId") long roleId);

    void insertIfNotExists(@Param("memberId") long memberId, @Param("roleId") long roleId);

    List<String> selectRoleCodesByMemberId(@Param("memberId") long memberId);

    List<String> selectRoleCodesByIds(
            @Param("domainId") long domainId,
            @Param("roleIds") List<Long> roleIds);

    long countRolesByIds(
            @Param("domainId") long domainId,
            @Param("roleIds") List<Long> roleIds);

    List<MemberRolePo> selectRolesByMemberIds(@Param("memberIds") List<Long> memberIds);

    List<StaffCandidatePo> listStaffCandidates(
            @Param("domainId") long domainId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countStaffCandidates(
            @Param("domainId") long domainId,
            @Param("keyword") String keyword);
}
