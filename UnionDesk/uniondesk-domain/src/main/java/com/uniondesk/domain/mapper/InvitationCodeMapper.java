package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.InvitationCodePo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvitationCodeMapper {

    List<InvitationCodePo> selectByDomainId(
            @Param("domainId") long domainId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomainId(@Param("domainId") long domainId);

    InvitationCodePo selectByIdAndDomain(
            @Param("id") long id,
            @Param("domainId") long domainId);

    InvitationCodePo selectActiveByDomainAndCode(
            @Param("domainId") long domainId,
            @Param("code") String code);

    int insert(InvitationCodePo po);

    int deactivate(
            @Param("id") long id,
            @Param("domainId") long domainId);

    int incrementUsedCount(@Param("id") long id);
}
