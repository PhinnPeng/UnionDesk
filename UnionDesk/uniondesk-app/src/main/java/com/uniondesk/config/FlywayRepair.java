package com.uniondesk.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Flyway 迁移修复工具
 * 用于删除失败的迁移记录，让 Flyway 重新执行
 * 
 * 使用方法：
 * 1. 临时添加 @Component 注解（或取消注释下面的 @Component）
 * 2. 启动应用，会自动执行修复
 * 3. 修复完成后，注释掉 @Component 注解，避免重复执行
 */
// @Component  // 取消注释此行以启用修复
@Profile("repair")  // 或者使用 repair profile 启动: --spring.profiles.active=repair
public class FlywayRepair implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public FlywayRepair(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) {
        System.out.println("==========================================");
        System.out.println("Flyway 迁移修复工具");
        System.out.println("==========================================");
        
        try {
            // 检查是否存在失败的迁移
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 0",
                Integer.class,
                "202606230001"
            );
            
            if (count != null && count > 0) {
                System.out.println("发现失败的迁移记录: 202606230001");
                
                // 删除失败的迁移记录
                jdbcTemplate.update(
                    "DELETE FROM flyway_schema_history WHERE version = ?",
                    "202606230001"
                );
                
                System.out.println("✓ 已删除失败的迁移记录");
                System.out.println("请重启应用，Flyway 会自动重新执行该迁移");
            } else {
                System.out.println("未发现失败的迁移记录，无需修复");
            }
        } catch (Exception e) {
            System.err.println("修复失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("==========================================");
    }
}
