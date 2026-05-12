-- 修复平台菜单 hidden 标志：审计日志、域配置、SLA管理、系统设置应在侧边栏可见
-- 工单详情保持 hidden=1（详情页，从工单池进入，不需要侧边栏入口）

UPDATE iam_admin_menu SET hidden = 0
WHERE route_path IN (
    '/platform/audit-logs',
    '/platform/domain-config',
    '/platform/sla-management',
    '/platform/system-settings'
)
AND hidden = 1;
