package com.uniondesk.iam.core;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 平台角色管理服务（遗留 staff_account_platform_role 表）。
 * IAM 读路径以 {@code user_global_role} 为准；本服务待后续 change 收敛。
 */
@Service
public class PlatformRoleService {

    private final JdbcClient jdbcClient;

    public PlatformRoleService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * 为员工授予平台角色
     * 
     * @param staffAccountId 员工账号ID
     * @param platformRoleCodes 平台角色代码列表
     * @throws IllegalStateException 如果违反保活规则
     */
    @Transactional
    public void assignPlatformRoles(long staffAccountId, List<String> platformRoleCodes) {
        // 检查是否要回收 platform_admin
        List<String> currentRoles = getCurrentPlatformRoles(staffAccountId);
        boolean removingPlatformAdmin = currentRoles.contains("platform_admin") 
            && !platformRoleCodes.contains("platform_admin");
        
        if (removingPlatformAdmin) {
            // 检查是否是最后一名激活的 platform_admin
            if (isLastActivePlatformAdmin(staffAccountId)) {
                throw new IllegalStateException("不允许移除最后一个平台管理员，需先指定另一位平台管理员");
            }
        }
        
        // 删除现有角色
        jdbcClient.sql("""
            DELETE FROM staff_account_platform_role
            WHERE staff_account_id = ?
            """)
            .param(staffAccountId)
            .update();
        
        // 添加新角色
        for (String roleCode : platformRoleCodes) {
            Long roleId = getPlatformRoleIdByCode(roleCode);
            if (roleId != null) {
                jdbcClient.sql("""
                    INSERT INTO staff_account_platform_role (staff_account_id, platform_role_id)
                    VALUES (?, ?)
                    """)
                    .param(staffAccountId)
                    .param(roleId)
                    .update();
            }
        }
    }

    /**
     * 获取员工的平台角色列表
     */
    public List<String> getCurrentPlatformRoles(long staffAccountId) {
        return jdbcClient.sql("""
            SELECT pr.code
            FROM staff_account_platform_role sapr
            JOIN platform_role pr ON sapr.platform_role_id = pr.id
            WHERE sapr.staff_account_id = ?
            """)
            .param(staffAccountId)
            .query(String.class)
            .list();
    }

    /**
     * 检查是否是最后一名激活的 platform_admin
     */
    public boolean isLastActivePlatformAdmin(long staffAccountId) {
        Integer count = jdbcClient.sql("""
            SELECT COUNT(DISTINCT sa.id)
            FROM staff_account sa
            JOIN staff_account_platform_role sapr ON sa.id = sapr.staff_account_id
            JOIN platform_role pr ON sapr.platform_role_id = pr.id
            WHERE pr.code = 'platform_admin'
              AND sa.status = 'active'
              AND sa.id != ?
            """)
            .param(staffAccountId)
            .query(Integer.class)
            .single();
        return count == 0;
    }

    /**
     * 检查是否是最后一名业务域的 super_admin
     */
    public boolean isLastDomainSuperAdmin(long staffAccountId, long businessDomainId) {
        Integer count = jdbcClient.sql("""
            SELECT COUNT(DISTINCT dm.id)
            FROM domain_member dm
            JOIN domain_member_role dmr ON dm.id = dmr.domain_member_id
            JOIN domain_role dr ON dmr.domain_role_id = dr.id
            WHERE dr.business_domain_id = ?
              AND dr.code = 'super_admin'
              AND dm.status = 'active'
              AND dm.staff_account_id != ?
            """)
            .param(businessDomainId)
            .param(staffAccountId)
            .query(Integer.class)
            .single();
        return count == 0;
    }

    /**
     * 检查是否是最后一名业务域的 domain_admin
     */
    public boolean isLastDomainAdmin(long staffAccountId, long businessDomainId) {
        Integer count = jdbcClient.sql("""
            SELECT COUNT(DISTINCT dm.id)
            FROM domain_member dm
            JOIN domain_member_role dmr ON dm.id = dmr.domain_member_id
            JOIN domain_role dr ON dmr.domain_role_id = dr.id
            WHERE dr.business_domain_id = ?
              AND dr.code = 'domain_admin'
              AND dm.status = 'active'
              AND dm.staff_account_id != ?
            """)
            .param(businessDomainId)
            .param(staffAccountId)
            .query(Integer.class)
            .single();
        return count == 0;
    }

    /**
     * 验证员工状态变更是否违反保活规则
     * 
     * @param staffAccountId 员工账号ID
     * @param newStatus 新状态
     * @throws IllegalStateException 如果违反保活规则
     */
    public void validateStaffStatusChange(long staffAccountId, String newStatus) {
        if ("disabled".equals(newStatus) || "offboarded".equals(newStatus)) {
            // 检查是否是最后一名 platform_admin
            List<String> platformRoles = getCurrentPlatformRoles(staffAccountId);
            if (platformRoles.contains("platform_admin") && isLastActivePlatformAdmin(staffAccountId)) {
                throw new IllegalStateException("不允许禁用/离职最后一个平台管理员，需先指定另一位平台管理员");
            }
        }
    }

    /**
     * 验证域成员状态变更是否违反保活规则
     */
    public void validateDomainMemberStatusChange(long staffAccountId, long businessDomainId, String newStatus) {
        if ("disabled".equals(newStatus) || "deleted".equals(newStatus)) {
            // 检查是否是最后一名 super_admin
            if (isLastDomainSuperAdmin(staffAccountId, businessDomainId)) {
                throw new IllegalStateException("不允许移除最后一个业务域超级管理员，需先指定另一位业务域超级管理员");
            }
            
            // 检查是否是最后一名 domain_admin
            if (isLastDomainAdmin(staffAccountId, businessDomainId)) {
                throw new IllegalStateException("不允许移除最后一个域管理员，需先指定另一位域管理员");
            }
        }
    }

    /**
     * 验证域成员角色变更是否违反保活规则
     */
    public void validateDomainMemberRoleChange(long staffAccountId, long businessDomainId, List<String> newRoleCodes) {
        // 获取当前角色
        List<String> currentRoles = getCurrentDomainRoles(staffAccountId, businessDomainId);
        
        // 检查是否要移除 super_admin
        if (currentRoles.contains("super_admin") && !newRoleCodes.contains("super_admin")) {
            if (isLastDomainSuperAdmin(staffAccountId, businessDomainId)) {
                throw new IllegalStateException("不允许移除最后一个业务域超级管理员，需先指定另一位业务域超级管理员");
            }
        }
        
        // 检查是否要移除 domain_admin
        if (currentRoles.contains("domain_admin") && !newRoleCodes.contains("domain_admin")) {
            if (isLastDomainAdmin(staffAccountId, businessDomainId)) {
                throw new IllegalStateException("不允许移除最后一个域管理员，需先指定另一位域管理员");
            }
        }
    }

    private List<String> getCurrentDomainRoles(long staffAccountId, long businessDomainId) {
        return jdbcClient.sql("""
            SELECT dr.code
            FROM domain_member dm
            JOIN domain_member_role dmr ON dm.id = dmr.domain_member_id
            JOIN domain_role dr ON dmr.domain_role_id = dr.id
            WHERE dm.staff_account_id = ?
              AND dr.business_domain_id = ?
            """)
            .param(staffAccountId)
            .param(businessDomainId)
            .query(String.class)
            .list();
    }

    private Long getPlatformRoleIdByCode(String code) {
        return jdbcClient.sql("""
            SELECT id FROM platform_role WHERE code = ?
            """)
            .param(code)
            .query(Long.class)
            .optional()
            .orElse(null);
    }

    public record PlatformRoleView(
        long id,
        String code,
        String name,
        boolean preset
    ) {}

    public record StaffPlatformRolesView(
        long staffAccountId,
        List<String> platformRoles
    ) {}
}
