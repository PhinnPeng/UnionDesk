package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.DomainRolePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DomainRoleMapper {

    List<DomainRolePo> selectByDomainId(@Param("domainId") long domainId);

    DomainRolePo selectByIdAndDomain(
            @Param("id") long id,
            @Param("domainId") long domainId);

    Long selectIdByDomainAndCode(
            @Param("domainId") long domainId,
            @Param("code") String code);

    void insert(
            @Param("domainId") long domainId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("preset") int preset);

    void insertIfNotExists(
            @Param("domainId") long domainId,
            @Param("code") String code,
            @Param("name") String name);

    int update(
            @Param("code") String code,
            @Param("name") String name,
            @Param("id") long id,
            @Param("domainId") long domainId);

    void deleteRolePermissions(@Param("roleId") long roleId);

    void insertRolePermission(
            @Param("roleId") long roleId,
            @Param("permissionItemId") long permissionItemId);

    int countRoleMembers(
            @Param("roleId") long roleId,
            @Param("domainId") long domainId);

    void deleteByIdAndDomain(
            @Param("id") long id,
            @Param("domainId") long domainId);

    void seedSuperAdminAllPermissions(@Param("superAdminRoleId") long superAdminRoleId);
}
