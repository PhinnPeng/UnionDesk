package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.RolePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper {

    List<RolePo> selectAll();

    RolePo selectById(@Param("id") int id);

    RolePo selectByCode(@Param("code") String code);

    void insert(RolePo po);

    int update(RolePo po);

    int deleteById(@Param("id") int id);

    int countById(@Param("id") int id);

    int countUserGlobalRoleBindings(@Param("roleId") int roleId);

    int countUserDomainRoleBindings(@Param("roleId") int roleId);

    void deleteUserGlobalRoles(@Param("userId") long userId);

    void deleteUserDomainRoles(@Param("userId") long userId);

    void deleteUserRoleBindings(@Param("userId") long userId);

    void insertUserGlobalRole(@Param("userId") long userId, @Param("roleId") int roleId);

    void insertUserDomainRole(@Param("userId") long userId, @Param("roleId") int roleId, @Param("businessDomainId") long businessDomainId);

    void insertRoleBindingGlobal(@Param("userId") long userId, @Param("roleId") int roleId);

    void insertRoleBindingDomain(@Param("userId") long userId, @Param("roleId") int roleId, @Param("businessDomainId") long businessDomainId);

    List<String> selectUserRoleCodes(@Param("userId") long userId);

    List<String> selectUserRoleCodesByClientAdmin(@Param("userId") long userId);

    List<String> selectUserRoleCodesByClientOther(@Param("userId") long userId, @Param("clientCode") String clientCode);

    List<RolePo> selectByCodes(@Param("codes") List<String> codes);

    List<Long> selectUserDomainIds(@Param("userId") long userId);

    int countProtectedRoleHoldersExcluding(@Param("roleCode") String roleCode, @Param("userId") long userId);

    List<BusinessDomainSummary> selectDomainSummariesForSuperAdmin();

    List<BusinessDomainSummary> selectDomainSummariesForUser(@Param("userId") long userId, @Param("roleCodes") List<String> roleCodes);

    void deleteRolePermissions(@Param("roleId") int roleId);

    void deleteRoleBindings(@Param("roleId") int roleId);

    String selectScopeById(@Param("id") int id);

    public record BusinessDomainSummary(Long id, String code, String name) {}
}
