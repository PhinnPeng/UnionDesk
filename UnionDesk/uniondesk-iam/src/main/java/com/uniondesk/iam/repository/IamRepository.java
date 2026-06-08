package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.ApiGrantPo;
import com.uniondesk.iam.entity.EffectivePermissionGrantPo;
import com.uniondesk.iam.entity.IamResourcePo;
import com.uniondesk.iam.entity.RolePo;
import com.uniondesk.iam.entity.RoutePermissionPo;
import com.uniondesk.iam.entity.UserAccountPo;
import com.uniondesk.iam.entity.UserSummaryPo;
import com.uniondesk.iam.mapper.BusinessDomainMapper;
import com.uniondesk.iam.mapper.IamPermissionMapper;
import com.uniondesk.iam.mapper.IamResourceMapper;
import com.uniondesk.iam.mapper.RoleMapper;
import com.uniondesk.iam.mapper.RoleMapper.BusinessDomainSummary;
import com.uniondesk.iam.mapper.UserAccountMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class IamRepository {

    private final IamResourceMapper iamResourceMapper;
    private final RoleMapper roleMapper;
    private final UserAccountMapper userAccountMapper;
    private final IamPermissionMapper iamPermissionMapper;
    private final BusinessDomainMapper businessDomainMapper;

    public IamRepository(
            IamResourceMapper iamResourceMapper,
            RoleMapper roleMapper,
            UserAccountMapper userAccountMapper,
            IamPermissionMapper iamPermissionMapper,
            BusinessDomainMapper businessDomainMapper) {
        this.iamResourceMapper = iamResourceMapper;
        this.roleMapper = roleMapper;
        this.userAccountMapper = userAccountMapper;
        this.iamPermissionMapper = iamPermissionMapper;
        this.businessDomainMapper = businessDomainMapper;
    }

    public List<IamResourcePo> findMenuByRoleAndClient(String roleCode, String clientCode) {
        return iamResourceMapper.selectMenuByRoleAndClient(roleCode, clientCode);
    }

    public List<IamResourcePo> findActionByRoleAndClient(String roleCode, String clientCode) {
        return iamResourceMapper.selectActionByRoleAndClient(roleCode, clientCode);
    }

    public List<String> findUserRoleCodesByClientAdmin(long userId) {
        return roleMapper.selectUserRoleCodesByClientAdmin(userId);
    }

    public List<String> findUserRoleCodesByClientOther(long userId, String clientCode) {
        return roleMapper.selectUserRoleCodesByClientOther(userId, clientCode);
    }

    public List<IamResourcePo> findResources(String resourceType, String clientScope) {
        return iamResourceMapper.selectByFilters(resourceType, clientScope);
    }

    public void insertResource(IamResourcePo po) {
        iamResourceMapper.insert(po);
    }

    public int updateResource(IamResourcePo po) {
        return iamResourceMapper.update(po);
    }

    public List<IamResourcePo> findResourcesByRoleId(int roleId) {
        return iamResourceMapper.selectByRoleId(roleId);
    }

    public void deleteRoleResources(int roleId) {
        iamResourceMapper.deleteRoleResources(roleId);
    }

    public void insertRoleResource(int roleId, long resourceId) {
        iamResourceMapper.insertRoleResource(roleId, resourceId);
    }

    public List<IamResourcePo> findMenuTree(String clientScope) {
        return iamResourceMapper.selectMenuTree(clientScope);
    }

    public int countResourceChildren(long parentId) {
        return iamResourceMapper.countByParentId(parentId);
    }

    public int countResourceBindings(long resourceId) {
        return iamResourceMapper.countBindingsByResourceId(resourceId);
    }

    public int deleteResource(long id) {
        return iamResourceMapper.deleteById(id);
    }

    public List<RolePo> findAllRoles() {
        return roleMapper.selectAll();
    }

    public void insertRole(RolePo po) {
        roleMapper.insert(po);
    }

    public int updateRole(RolePo po) {
        return roleMapper.update(po);
    }

    public int countUserGlobalRoleBindings(int roleId) {
        return roleMapper.countUserGlobalRoleBindings(roleId);
    }

    public int countUserDomainRoleBindings(int roleId) {
        return roleMapper.countUserDomainRoleBindings(roleId);
    }

    public void deleteRolePermissions(int roleId) {
        roleMapper.deleteRolePermissions(roleId);
    }

    public void deleteRoleBindings(int roleId) {
        roleMapper.deleteRoleBindings(roleId);
    }

    public int deleteRole(int roleId) {
        return roleMapper.deleteById(roleId);
    }

    public List<Long> findMenuResourceIdsByRole(int roleId) {
        return iamResourceMapper.selectMenuResourceIdsByRole(roleId);
    }

    public List<Long> findActionResourceIdsByRole(int roleId) {
        return iamResourceMapper.selectActionResourceIdsByRole(roleId);
    }

    public List<UserAccountPo> findUsersByEmploymentStatus(boolean offboardedOnly) {
        return userAccountMapper.selectByEmploymentStatus(offboardedOnly);
    }

    public Optional<UserAccountPo> findUserById(long id) {
        return Optional.ofNullable(userAccountMapper.selectById(id));
    }

    public int updateUserSelective(long id, String username, String nickname, String mobile, String email,
                                   String remark, String passwordHash, String accountType, Integer status) {
        return userAccountMapper.updateSelective(id, username, nickname, mobile, email, remark, passwordHash, accountType, status);
    }

    public int offboardUser(long id, LocalDateTime offboardedAt, Long offboardedBy, String offboardReason) {
        return userAccountMapper.offboard(id, offboardedAt, offboardedBy, offboardReason);
    }

    public int restoreUser(long id) {
        return userAccountMapper.restore(id);
    }

    public int countTicketReferences(long userId) {
        return userAccountMapper.countTicketReferences(userId);
    }

    public int countConsultationReferences(long userId) {
        return userAccountMapper.countConsultationReferences(userId);
    }

    public void clearOffboardedBy(long userId) {
        userAccountMapper.clearOffboardedBy(userId);
    }

    public void deleteUserDomainRoles(long userId) {
        roleMapper.deleteUserDomainRoles(userId);
    }

    public void deleteUserGlobalRoles(long userId) {
        roleMapper.deleteUserGlobalRoles(userId);
    }

    public void deleteUserOrganizations(long userId) {
        userAccountMapper.deleteUserOrganizations(userId);
    }

    public void deleteLoginLogsByUsername(long userId) {
        userAccountMapper.deleteLoginLogsByUsername(userId);
    }

    public void deleteSessions(long userId) {
        userAccountMapper.deleteSessions(userId);
    }

    public void deleteUser(long userId) {
        userAccountMapper.deleteById(userId);
    }

    public int revokeSessionsOnOffboard(long userId, LocalDateTime revokedAt) {
        return userAccountMapper.revokeSessionsOnOffboard(userId, revokedAt);
    }

    public List<ApiGrantPo> findActionGrants(String roleCode, String clientCode) {
        return iamResourceMapper.selectActionGrants(roleCode, clientCode);
    }

    public List<IamResourcePo> findResourcesForRoles(List<String> roleCodes, List<String> resourceTypes, String clientCode) {
        return iamResourceMapper.selectForRoles(roleCodes, resourceTypes, clientCode);
    }

    public List<BusinessDomainSummary> findDomainSummariesForSuperAdmin() {
        return roleMapper.selectDomainSummariesForSuperAdmin();
    }

    public List<BusinessDomainSummary> findDomainSummariesForUser(long userId, List<String> roleCodes) {
        return roleMapper.selectDomainSummariesForUser(userId, roleCodes);
    }

    public Optional<UserSummaryPo> findUserSummaryById(long userId) {
        return Optional.ofNullable(userAccountMapper.selectSummaryById(userId));
    }

    public List<String> findUserRoleCodes(long userId) {
        return roleMapper.selectUserRoleCodes(userId);
    }

    public int countProtectedRoleHoldersExcluding(String roleCode, long userId) {
        return roleMapper.countProtectedRoleHoldersExcluding(roleCode, userId);
    }

    public List<Long> findUserDomainIds(long userId) {
        return roleMapper.selectUserDomainIds(userId);
    }

    public void deleteUserRoleBindings(long userId) {
        roleMapper.deleteUserRoleBindings(userId);
    }

    public void insertUserGlobalRole(long userId, int roleId) {
        roleMapper.insertUserGlobalRole(userId, roleId);
    }

    public void insertRoleBindingGlobal(long userId, int roleId) {
        roleMapper.insertRoleBindingGlobal(userId, roleId);
    }

    public void insertUserDomainRole(long userId, int roleId, long businessDomainId) {
        roleMapper.insertUserDomainRole(userId, roleId, businessDomainId);
    }

    public void insertRoleBindingDomain(long userId, int roleId, long businessDomainId) {
        roleMapper.insertRoleBindingDomain(userId, roleId, businessDomainId);
    }

    public List<RolePo> findRolesByCodes(List<String> codes) {
        return roleMapper.selectByCodes(codes);
    }

    public int countDomainsByIds(List<Long> ids) {
        return businessDomainMapper.countByIds(ids);
    }

    public int countResourcesByIds(List<Long> ids) {
        return iamResourceMapper.countByIds(ids);
    }

    public int countResourcesByIdsAndType(List<Long> ids, String resourceType) {
        return iamResourceMapper.countByIdsAndType(ids, resourceType);
    }

    public List<RoutePermissionPo> findRoutePermissions(String httpMethod) {
        return iamPermissionMapper.selectRoutePermissions(httpMethod);
    }

    public List<EffectivePermissionGrantPo> findEffectiveGrants(long userId, List<String> codes) {
        return iamPermissionMapper.selectEffectiveGrants(userId, codes);
    }

    public Optional<IamResourcePo> findResourceById(long id) {
        return Optional.ofNullable(iamResourceMapper.selectById(id));
    }

    public Optional<IamResourcePo> findResourceByCode(String resourceCode) {
        return Optional.ofNullable(iamResourceMapper.selectByCode(resourceCode));
    }

    public Optional<RolePo> findRoleById(int id) {
        return Optional.ofNullable(roleMapper.selectById(id));
    }

    public Optional<RolePo> findRoleByCode(String code) {
        return Optional.ofNullable(roleMapper.selectByCode(code));
    }

    public int countRoleById(int roleId) {
        return roleMapper.countById(roleId);
    }
}
