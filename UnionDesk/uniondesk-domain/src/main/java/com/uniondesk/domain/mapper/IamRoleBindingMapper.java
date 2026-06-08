package com.uniondesk.domain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IamRoleBindingMapper {

    int selectIdByBinding(
            @Param("userId") long userId,
            @Param("roleId") int roleId,
            @Param("domainId") long domainId);

    void insertIfNotExists(
            @Param("userId") long userId,
            @Param("roleId") int roleId,
            @Param("domainId") long domainId);

    Integer selectLegacyRoleId(@Param("code") String code);
}
