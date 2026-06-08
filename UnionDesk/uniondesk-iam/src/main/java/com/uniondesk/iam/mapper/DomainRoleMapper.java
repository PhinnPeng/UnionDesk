package com.uniondesk.iam.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainRoleMapper {

    Long selectIdByDomainAndCode(@Param("businessDomainId") long businessDomainId, @Param("code") String code);
}
