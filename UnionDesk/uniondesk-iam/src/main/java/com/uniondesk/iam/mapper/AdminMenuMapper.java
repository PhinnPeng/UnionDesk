package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.AdminMenuPo;
import com.uniondesk.iam.entity.ParentPermissionMappingPo;
import com.uniondesk.iam.entity.RolePermissionRowPo;
import com.uniondesk.iam.entity.RoutePathRowPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMenuMapper extends BaseMapper<AdminMenuPo> {

    default List<AdminMenuPo> selectAll(String scope) {
        return selectListByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getScope).eq(scope, If::hasText)
                .orderBy(AdminMenuPo::getOrderNo, true)
                .orderBy(AdminMenuPo::getId, true));
    }

    default AdminMenuPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getId).eq(id));
    }

    default AdminMenuPo selectRequiredButton(long parentMenuId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getParentId).eq(parentMenuId)
                .and(AdminMenuPo::getNodeType).eq("button")
                .and(AdminMenuPo::getRequired).eq(1));
    }

    int insertRow(AdminMenuPo po);

    int updateRow(AdminMenuPo po);

    int deleteRowById(@Param("id") long id);

    int deleteRoleMenuRelations(@Param("menuId") long menuId);

    default int countByParentId(long parentId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getParentId).eq(parentId));
    }

    List<AdminMenuPo> selectAuthorizedByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    List<RolePermissionRowPo> selectRolePermissionRows(@Param("roleCodes") List<String> roleCodes);

    List<Long> selectRoleMenuIds(@Param("roleId") int roleId, @Param("nodeType") String nodeType);

    void deleteRolePermissions(@Param("roleId") int roleId);

    void deleteRoleMenuRelationsByRoleId(@Param("roleId") int roleId);

    int countRoleById(@Param("roleId") int roleId);

    void insertRoleMenuRelation(@Param("roleId") int roleId, @Param("menuId") long menuId);

    void batchInsertRoleMenuRelations(@Param("roleId") int roleId, @Param("menuIds") List<Long> menuIds);

    List<Long> selectParentIdsByMenuIds(@Param("menuIds") List<Long> menuIds);

    default List<Long> selectRequiredButtonIdsByParentIds(List<Long> parentIds) {
        return selectObjectListByQueryAs(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .select(AdminMenuPo::getId)
                .where(AdminMenuPo::getParentId).in(parentIds)
                .and(AdminMenuPo::getNodeType).eq("button")
                .and(AdminMenuPo::getRequired).eq(1), Long.class);
    }

    String selectRoleScopeById(@Param("roleId") int roleId);

    default List<String> selectPermissionCodesByMenuIds(List<Long> menuIds) {
        return selectObjectListByQueryAs(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .select(AdminMenuPo::getPermissionCode)
                .where(AdminMenuPo::getId).in(menuIds), String.class);
    }

    default List<Long> selectRequiredMenuIds(String scope) {
        return selectObjectListByQueryAs(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .select(AdminMenuPo::getId)
                .where(AdminMenuPo::getNodeType).eq("menu")
                .and(AdminMenuPo::getRequired).eq(1)
                .and(AdminMenuPo::getScope).eq(scope)
                .and(AdminMenuPo::getStatus).eq(1), Long.class);
    }

    default List<Long> selectRequiredButtonIdsByMenuIds(List<Long> menuIds) {
        return selectObjectListByQueryAs(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .select(AdminMenuPo::getId)
                .where(AdminMenuPo::getParentId).in(menuIds)
                .and(AdminMenuPo::getNodeType).eq("button")
                .and(AdminMenuPo::getRequired).eq(1)
                .and(AdminMenuPo::getStatus).eq(1), Long.class);
    }

    List<String> selectGrantedPermissionCodes(@Param("roleCode") String roleCode);

    default List<ParentPermissionMappingPo> selectRequiredPermissionMappings(String scope) {
        return selectListByQueryAs(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .select(AdminMenuPo::getParentId, AdminMenuPo::getPermissionCode)
                .where(AdminMenuPo::getNodeType).eq("button")
                .and(AdminMenuPo::getRequired).eq(1)
                .and(AdminMenuPo::getParentId).isNotNull()
                .and(AdminMenuPo::getScope).eq(scope, If::notNull), ParentPermissionMappingPo.class);
    }

    default int updateCode(long id, String code) {
        AdminMenuPo update = new AdminMenuPo();
        update.setCode(code);
        return updateByQuery(update, true, QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getId).eq(id));
    }

    default List<RoutePathRowPo> selectExistingRoutePaths(Long selfId) {
        return selectListByQueryAs(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .select(AdminMenuPo::getRoutePath, AdminMenuPo::getScope)
                .where(AdminMenuPo::getRoutePath).isNotNull()
                .and(AdminMenuPo::getId).ne(selfId, If::notNull), RoutePathRowPo.class);
    }

    default int countByRoutePath(String routePath, Long selfId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getRoutePath).eq(routePath)
                .and(AdminMenuPo::getId).ne(selfId, If::notNull));
    }

    default int countByPermissionCode(String permissionCode, Long selfId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getPermissionCode).eq(permissionCode)
                .and(AdminMenuPo::getId).ne(selfId, If::notNull));
    }

    default int countByIdsAndNodeType(List<Long> ids, String nodeType) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getId).in(ids)
                .and(AdminMenuPo::getNodeType).eq(nodeType));
    }

    default int countByIdsAndNodeTypes(List<Long> ids, List<String> nodeTypes) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(AdminMenuPo.class)
                .where(AdminMenuPo::getId).in(ids)
                .and(AdminMenuPo::getNodeType).in(nodeTypes));
    }

    List<Long> selectRoleMenuIdsByNodeTypes(@Param("roleId") int roleId, @Param("nodeTypes") List<String> nodeTypes);
}
