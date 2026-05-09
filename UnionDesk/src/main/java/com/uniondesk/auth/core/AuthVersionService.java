package com.uniondesk.auth.core;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限版本管理服务
 * 实现权限变更后 Token 立即失效机制
 * 
 * 机制说明：
 * 1. 每个账号维护一个 auth_version（存储在数据库）
 * 2. Token 签发时包含当前 auth_version
 * 3. 权限变更时递增 auth_version
 * 4. Token 验证时比对版本号，不匹配则拒绝
 * 
 * TODO: 添加 Redis 缓存以提升性能
 */
@Service
public class AuthVersionService {

    private final JdbcClient jdbcClient;

    public AuthVersionService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * 获取账号的当前 auth_version
     */
    public int getCurrentVersion(long accountId, String accountType) {
        return getVersionFromDatabase(accountId, accountType);
    }

    /**
     * 递增账号的 auth_version
     * 用于权限变更、角色变更、账号状态变更等场景
     */
    @Transactional
    public int incrementVersion(long accountId, String accountType) {
        String tableName = getTableName(accountType);
        
        // 数据库递增
        jdbcClient.sql(String.format("""
            UPDATE %s
            SET auth_version = auth_version + 1
            WHERE id = ?
            """, tableName))
            .param(accountId)
            .update();
        
        // 获取新版本号
        int newVersion = getVersionFromDatabase(accountId, accountType);
        
        // 同时使所有会话失效
        invalidateAllSessions(accountId);
        
        return newVersion;
    }

    /**
     * 验证 Token 中的 auth_version 是否有效
     */
    public boolean validateVersion(long accountId, String accountType, int tokenVersion) {
        int currentVersion = getCurrentVersion(accountId, accountType);
        return tokenVersion == currentVersion;
    }

    /**
     * 批量递增多个账号的 auth_version
     * 用于批量权限变更场景
     */
    @Transactional
    public void incrementVersionBatch(List<Long> accountIds, String accountType) {
        for (long accountId : accountIds) {
            incrementVersion(accountId, accountType);
        }
    }

    private int getVersionFromDatabase(long accountId, String accountType) {
        String tableName = getTableName(accountType);
        
        return jdbcClient.sql(String.format("""
            SELECT auth_version FROM %s WHERE id = ?
            """, tableName))
            .param(accountId)
            .query(Integer.class)
            .optional()
            .orElse(1);
    }

    private String getTableName(String accountType) {
        return switch (accountType) {
            case "staff" -> "staff_account";
            case "customer" -> "customer_account";
            default -> "user_account"; // 兼容旧表
        };
    }

    private void invalidateAllSessions(long accountId) {
        // 将该用户的所有会话标记为失效
        jdbcClient.sql("""
            UPDATE auth_login_session
            SET session_status = 'revoked'
            WHERE user_id = ?
              AND session_status = 'active'
            """)
            .param(accountId)
            .update();
    }
}
