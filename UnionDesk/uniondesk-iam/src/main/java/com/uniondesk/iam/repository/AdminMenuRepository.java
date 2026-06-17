package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.AdminMenuPo;
import com.uniondesk.iam.entity.ParentPermissionMappingPo;
import com.uniondesk.iam.entity.RolePermissionRowPo;
import com.uniondesk.iam.entity.RolePo;
import com.uniondesk.iam.entity.RoutePathRowPo;
import com.uniondesk.iam.mapper.AdminMenuMapper;
import com.uniondesk.iam.mapper.IamPermissionMapper;
import com.uniondesk.iam.mapper.RoleMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AdminMenuRepository {

    private final AdminMenuMapper adminMenuMapper;
    private final IamPermissionMapper iamPermissionMapper;
    private final RoleMapper roleMapper;

    public AdminMenuRepository(
            AdminMenuMapper adminMenuMapper,
            IamPermissionMapper iamPermissionMapper,
            RoleMapper roleMapper) {
        this.adminMenuMapper = adminMenuMapper;
        this.iamPermissionMapper = iamPermissionMapper;
        this.roleMapper = roleMapper;
    }

    public Optional<RolePo> findRoleById(int roleId) {
        return Optional.ofNullable(roleMapper.selectById(roleId));
    }

    public List<AdminMenuPo> findAll(String scope) {
        return adminMenuMapper.selectAll(scope);
    }

    public Optional<AdminMenuPo> findById(long id) {
        return Optional.ofNullable(adminMenuMapper.selectById(id));
    }

    public Optional<AdminMenuPo> findRequiredButton(long parentMenuId) {
        return Optional.ofNullable(adminMenuMapper.selectRequiredButton(parentMenuId));
    }

    public void insert(AdminMenuPo po) {
        adminMenuMapper.insert(po);
    }

    public int update(AdminMenuPo po) {
        return adminMenuMapper.update(po);
    }

    public void updateCode(long id, String code) {
        adminMenuMapper.updateCode(id, code);
    }

    public int deleteById(long id) {
        return adminMenuMapper.deleteById(id);
    }

    public void deleteRoleMenuRelations(long menuId) {
        adminMenuMapper.deleteRoleMenuRelations(menuId);
    }

    public int countByParentId(long parentId) {
        return adminMenuMapper.countByParentId(parentId);
    }

    public List<AdminMenuPo> findAuthorizedByRoleCodes(List<String> roleCodes) {
        return adminMenuMapper.selectAuthorizedByRoleCodes(roleCodes);
    }

    public List<RolePermissionRowPo> findRolePermissionRows(List<String> roleCodes) {
        return adminMenuMapper.selectRolePermissionRows(roleCodes);
    }

    public List<Long> findRoleMenuIds(int roleId, String nodeType) {
        return adminMenuMapper.selectRoleMenuIds(roleId, nodeType);
    }

    public void deleteRoleMenuRelationsByRoleId(int roleId) {
        adminMenuMapper.deleteRoleMenuRelationsByRoleId(roleId);
    }

    public void insertRoleMenuRelation(int roleId, long menuId) {
        adminMenuMapper.insertRoleMenuRelation(roleId, menuId);
    }

    public void batchInsertRoleMenuRelations(int roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        adminMenuMapper.batchInsertRoleMenuRelations(roleId, menuIds);
    }

    public List<Long> findParentIdsByMenuIds(List<Long> menuIds) {
        return adminMenuMapper.selectParentIdsByMenuIds(menuIds);
    }

    public List<Long> findRequiredButtonIdsByParentIds(List<Long> parentIds) {
        return adminMenuMapper.selectRequiredButtonIdsByParentIds(parentIds);
    }

    public String findRoleScopeById(int roleId) {
        return adminMenuMapper.selectRoleScopeById(roleId);
    }

    public List<RolePo> findRolesByCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectByCodes(roleCodes);
    }

    public List<String> findPermissionCodesByMenuIds(List<Long> menuIds) {
        return adminMenuMapper.selectPermissionCodesByMenuIds(menuIds);
    }

    public List<Long> findRequiredMenuIds(String scope) {
        return adminMenuMapper.selectRequiredMenuIds(scope);
    }

    public List<Long> findRequiredButtonIdsByMenuIds(List<Long> menuIds) {
        return adminMenuMapper.selectRequiredButtonIdsByMenuIds(menuIds);
    }

    public List<String> findGrantedPermissionCodes(String roleCode) {
        return adminMenuMapper.selectGrantedPermissionCodes(roleCode);
    }

    public Map<Long, String> findRequiredPermissionByMenuId(String scope) {
        Map<Long, String> permissions = new LinkedHashMap<>();
        for (ParentPermissionMappingPo row : adminMenuMapper.selectRequiredPermissionMappings(scope)) {
            permissions.put(row.getParentId(), row.getPermissionCode());
        }
        return permissions;
    }

    public List<RoutePathRowPo> findExistingRoutePaths(Long selfId) {
        return adminMenuMapper.selectExistingRoutePaths(selfId);
    }

    public int countByRoutePath(String routePath, Long selfId) {
        return adminMenuMapper.countByRoutePath(routePath, selfId);
    }

    public int countByPermissionCode(String permissionCode, Long selfId) {
        return adminMenuMapper.countByPermissionCode(permissionCode, selfId);
    }

    public int countByIdsAndNodeType(List<Long> ids, String nodeType) {
        return adminMenuMapper.countByIdsAndNodeType(ids, nodeType);
    }

    public int countByIdsAndNodeTypes(List<Long> ids, List<String> nodeTypes) {
        return adminMenuMapper.countByIdsAndNodeTypes(ids, nodeTypes);
    }

    public List<Long> findRoleMenuIdsByNodeTypes(int roleId, List<String> nodeTypes) {
        return adminMenuMapper.selectRoleMenuIdsByNodeTypes(roleId, nodeTypes);
    }

    public int countRoleById(int roleId) {
        return adminMenuMapper.countRoleById(roleId);
    }

    public void deleteRolePermissionsByCatalog(int roleId, List<String> catalogCodes) {
        iamPermissionMapper.deleteRolePermissionsByCatalog(roleId, catalogCodes);
    }

    public void insertRolePermissionsByCodes(int roleId, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }
        iamPermissionMapper.insertRolePermissionsByCodes(roleId, codes);
    }

}
