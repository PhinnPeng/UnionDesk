/**
 * 平台侧菜单管理 — 前端页面组件手工注册表
 * 用于「前端组件」级联选择；新增页面时在此追加 { label, value } 条目
 */
export type PlatformComRegistryItem = {
	label: string
	value: string
};

export const platformComRegistry: PlatformComRegistryItem[] = [
	{ label: "首页", value: "home" },
	{ label: "关于", value: "about" },

	{ label: "系统管理/用户管理", value: "system/user" },
	{ label: "系统管理/部门管理", value: "system/dept" },
	{ label: "系统管理/角色管理", value: "system/role" },
	{ label: "平台管理/菜单管理", value: "platform/system/menu" },

	{ label: "平台管理/平台首页", value: "platform/home" },
	{ label: "平台管理/业务域管理/业务域列表", value: "platform/domains" },
	{ label: "平台管理/业务域管理/业务域控制台", value: "platform/domains/detail" },
	{ label: "平台管理/业务域管理/域配置", value: "platform/domain-config" },
	{ label: "平台管理/业务域管理/客户入域", value: "platform/domain-onboarding" },
	{ label: "平台管理/平台用户", value: "platform/user" },
	{ label: "平台管理/平台部门", value: "platform/dept" },
	{ label: "平台管理/工单池", value: "platform/ticket-pool" },
	{ label: "平台管理/离职池", value: "platform/offboard-pool" },
	{ label: "平台管理/站内信", value: "platform/inbox" },
	{ label: "平台管理/附件管理", value: "platform/attachments" },
	{ label: "平台管理/导入导出", value: "platform/import-export" },
	{ label: "平台管理/SLA 管理", value: "platform/sla-management" },
	{ label: "平台管理/系统设置", value: "platform/system-settings" },
	{ label: "平台管理/审计日志", value: "platform/audit-logs" },
	{ label: "平台管理/屏蔽词库", value: "platform/blockwords" },
	{ label: "平台管理/日志审计/登录日志", value: "platform/log/login-log" },
	{ label: "平台管理/日志审计/操作日志", value: "platform/log/operation-log" },
	{ label: "平台管理/工单详情", value: "platform/ticket-detail" },
	{ label: "平台管理/组织配置", value: "platform/org-config" },

	{ label: "个人中心/个人资料", value: "personal-center/my-profile" },
	{ label: "个人中心/设置", value: "personal-center/settings" },

	{ label: "隐私政策", value: "privacy-policy" },
	{ label: "服务条款", value: "terms-of-service" },
];

/** 已注册组件的 value 集合，用于快速校验 */
export const registeredPlatformComKeys = new Set(platformComRegistry.map(item => item.value));
