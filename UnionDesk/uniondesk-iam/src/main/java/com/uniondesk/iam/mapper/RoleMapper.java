package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.RolePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper extends BaseMapper<RolePo> {

    default List<RolePo> selectAll() {
        return selectListByQuery(QueryWrapper.create()
                .from(RolePo.class)
                .orderBy(RolePo::getIsSystem, false)
                .orderBy(RolePo::getId, true));
    }

    default RolePo selectById(int id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(RolePo.class)
                .where(RolePo::getId).eq(id));
    }

    default RolePo selectByCode(String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(RolePo.class)
                .where(RolePo::getCode).eq(code));
    }

    int insertRow(RolePo po);

    int updateRow(RolePo po);

    int deleteRowById(@Param("id") int id);

    default int countById(int id) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(RolePo.class)
                .where(RolePo::getId).eq(id));
    }

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

    default List<RolePo> selectByCodes(List<String> codes) {
        return selectListByQuery(QueryWrapper.create()
                .from(RolePo.class)
                .select(RolePo::getId, RolePo::getCode, RolePo::getScope)
                .where(RolePo::getCode).in(codes));
    }

    List<Long> selectUserDomainIds(@Param("userId") long userId);

    int countProtectedRoleHoldersExcluding(@Param("roleCode") String roleCode, @Param("userId") long userId);

    List<BusinessDomainSummary> selectDomainSummariesForSuperAdmin();

    List<BusinessDomainSummary> selectDomainSummariesForUser(@Param("userId") long userId, @Param("roleCodes") List<String> roleCodes);

    void deleteRolePermissions(@Param("roleId") int roleId);

    void deleteRoleBindings(@Param("roleId") int roleId);

    default String selectScopeById(int id) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(RolePo.class)
                .select(RolePo::getScope)
                .where(RolePo::getId).eq(id), String.class);
    }

    record BusinessDomainSummary(Long id, String code, String name) {}
}
