package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.DomainMemberPo;
import com.uniondesk.iam.entity.DomainMemberPresentationPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainMemberMapper {

    DomainMemberPresentationPo selectPresentation(@Param("staffAccountId") long staffAccountId, @Param("domainId") long domainId);

    List<Long> selectDistinctDomainIds(@Param("staffAccountId") long staffAccountId);

    List<String> selectRoleCodes(@Param("staffAccountId") long staffAccountId);

    Long selectId(@Param("businessDomainId") long businessDomainId, @Param("staffAccountId") long staffAccountId);

    void insert(@Param("staffAccountId") long staffAccountId, @Param("businessDomainId") long businessDomainId);

    void deleteRolesByMemberId(@Param("domainMemberId") long domainMemberId);

    void insertRole(@Param("domainMemberId") long domainMemberId, @Param("domainRoleId") long domainRoleId);
}
