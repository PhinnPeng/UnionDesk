package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.DomainMemberPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainMemberMapper {

    List<DomainMemberPo> listMembers(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countMembers(
            @Param("domainId") long domainId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);

    DomainMemberPo selectMemberById(
            @Param("id") long id,
            @Param("domainId") long domainId);

    void insert(@Param("staffAccountId") long staffAccountId,
                @Param("businessDomainId") long businessDomainId);

    void insertWithSource(
            @Param("staffAccountId") long staffAccountId,
            @Param("businessDomainId") long businessDomainId,
            @Param("source") String source);

    int updateStatus(
            @Param("status") String status,
            @Param("memberId") long memberId,
            @Param("domainId") long domainId);

    int softDelete(@Param("memberId") long memberId, @Param("domainId") long domainId);

    int countActiveDomainAdmins(
            @Param("domainId") long domainId,
            @Param("excludeMemberId") long excludeMemberId);

    int countActiveDomainSuperAdmins(
            @Param("domainId") long domainId,
            @Param("excludeMemberId") long excludeMemberId);

    int countByDomainAndStaff(
            @Param("domainId") long domainId,
            @Param("staffAccountId") long staffAccountId);

    Long findMemberIdByDomainAndStaff(
            @Param("domainId") long domainId,
            @Param("staffAccountId") long staffAccountId);

    Long findActiveMemberId(
            @Param("domainId") long domainId,
            @Param("staffAccountId") long staffAccountId);
}
