package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.AdminMenuPo;
import com.uniondesk.iam.entity.ParentPermissionMappingPo;
import com.uniondesk.iam.entity.RolePermissionRowPo;
import com.uniondesk.iam.entity.RoutePathRowPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMenuMapper {

    List<AdminMenuPo> selectAll(@Param("scope") String scope);

    AdminMenuPo selectById(@Param("id") long id);

    AdminMenuPo selectRequiredButton(@Param("parentMenuId") long parentMenuId);

    void insert(AdminMenuPo po);

    int update(AdminMenuPo po);

    int deleteById(@Param("id") long id);

    int deleteRoleMenuRelations(@Param("menuId") long menuId);

    int countByParentId(@Param("parentId") long parentId);

    List<AdminMenuPo> selectAuthorizedByRoleCodes(@Param("roleCodes") List<String> roleCodes);

    List<RolePermissionRowPo> selectRolePermissionRows(@Param("roleCodes") List<String> roleCodes);

    List<Long> selectRoleMenuIds(@Param("roleId") int roleId, @Param("nodeType") String nodeType);

    void deleteRolePermissions(@Param("roleId") int roleId);

    void deleteRoleMenuRelationsByRoleId(@Param("roleId") int roleId);

    int countRoleById(@Param("roleId") int roleId);

    void insertRoleMenuRelation(@Param("roleId") int roleId, @Param("menuId") long menuId);

    void batchInsertRoleMenuRelations(@Param("roleId") int roleId, @Param("menuIds") List<Long> menuIds);

    List<Long> selectParentIdsByMenuIds(@Param("menuIds") List<Long> menuIds);

    List<Long> selectRequiredButtonIdsByParentIds(@Param("parentIds") List<Long> parentIds);

    String selectRoleScopeById(@Param("roleId") int roleId);

    List<String> selectPermissionCodesByMenuIds(@Param("menuIds") List<Long> menuIds);

    List<Long> selectRequiredMenuIds(@Param("scope") String scope);

    List<Long> selectRequiredButtonIdsByMenuIds(@Param("menuIds") List<Long> menuIds);

    List<String> selectGrantedPermissionCodes(@Param("roleCode") String roleCode);

    List<ParentPermissionMappingPo> selectRequiredPermissionMappings(@Param("scope") String scope);

    void updateCode(@Param("id") long id, @Param("code") String code);

    List<RoutePathRowPo> selectExistingRoutePaths(@Param("selfId") Long selfId);

    int countByRoutePath(@Param("routePath") String routePath, @Param("selfId") Long selfId);

    int countByPermissionCode(@Param("permissionCode") String permissionCode, @Param("selfId") Long selfId);

    int countByIdsAndNodeType(@Param("ids") List<Long> ids, @Param("nodeType") String nodeType);

    int countByIdsAndNodeTypes(@Param("ids") List<Long> ids, @Param("nodeTypes") List<String> nodeTypes);

    List<Long> selectRoleMenuIdsByNodeTypes(@Param("roleId") int roleId, @Param("nodeTypes") List<String> nodeTypes);
}
