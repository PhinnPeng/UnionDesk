import type { AppRouteRecordRaw } from "#src/router/types";
import ContainerLayout from "#src/layout/container-layout";
import { domain } from "#src/router/extra-info";

import { lazy } from "react";

const DomainOverview = lazy(() => import("#src/pages/domain/overview"));
const DomainBasic = lazy(() => import("#src/pages/domain/basic"));
const DomainMembers = lazy(() => import("#src/pages/domain/members"));
const DomainRoles = lazy(() => import("#src/pages/domain/roles"));
const DomainCustomers = lazy(() => import("#src/pages/domain/customers"));
const DomainOnboarding = lazy(() => import("#src/pages/domain/onboarding"));
const DomainTicketConfig = lazy(() => import("#src/pages/domain/ticket-config"));
const DomainTicketTypeConfig = lazy(() => import("#src/pages/domain/ticket-config/type-config"));
const DomainBlockwords = lazy(() => import("#src/pages/domain/blockwords"));
const DomainNotifications = lazy(() => import("#src/pages/domain/notifications"));
const DomainConfig = lazy(() => import("#src/pages/domain/config"));
const DomainAuditLogs = lazy(() => import("#src/pages/domain/audit-logs"));
const DomainLoginLogs = lazy(() => import("#src/pages/domain/login-logs"));

const routes: AppRouteRecordRaw[] = [
	{
		path: "/domain",
		Component: ContainerLayout,
		handle: {
			icon: "SettingOutlined",
			title: "域模块",
			order: domain,
			scope: "business",
			hideInMenu: true,
		},
		children: [
			{
				path: "/domain/overview",
				Component: DomainOverview,
				handle: {
					icon: "DashboardOutlined",
					title: "运营概览",
					scope: "business",
					auth: "domain.overview.read",
				},
			},
			{
				path: "/domain/basic",
				Component: DomainBasic,
				handle: {
					icon: "SettingOutlined",
					title: "通用设置",
					scope: "business",
					auth: "domain.general.read",
				},
			},
			{
				path: "/domain/members",
				Component: DomainMembers,
				handle: {
					icon: "TeamOutlined",
					title: "人员管理",
					scope: "business",
					auth: "domain.member.read",
				},
			},
			{
				path: "/domain/roles",
				Component: DomainRoles,
				handle: {
					icon: "SafetyCertificateOutlined",
					title: "角色管理",
					scope: "business",
					auth: "domain.role.read",
				},
			},
			{
				path: "/domain/customers",
				Component: DomainCustomers,
				handle: {
					icon: "SolutionOutlined",
					title: "客户管理",
					scope: "business",
					auth: "domain.customer.read",
				},
			},
			{
				path: "/domain/onboarding",
				Component: DomainOnboarding,
				handle: {
					icon: "UserAddOutlined",
					title: "入域管理",
					scope: "business",
					auth: "domain.invitation_code.read",
				},
			},
			{
				path: "/domain/ticket-config",
				Component: DomainTicketConfig,
				handle: {
					icon: "AppstoreOutlined",
					title: "事项配置",
					scope: "business",
					auth: "domain.ticket_type.read",
				},
			},
			{
				path: "/domain/ticket-config/types/:typeId",
				Component: DomainTicketTypeConfig,
				handle: {
					icon: "AppstoreOutlined",
					title: "事项类型配置",
					scope: "business",
					auth: "domain.ticket_type.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/blockwords",
				Component: DomainBlockwords,
				handle: {
					icon: "StopOutlined",
					title: "屏蔽词库",
					scope: "business",
					auth: "domain.blocked_word.read",
				},
			},
			{
				path: "/domain/notifications",
				Component: DomainNotifications,
				handle: {
					icon: "MailOutlined",
					title: "通知配置",
					scope: "business",
					auth: "domain.notification_template.read",
				},
			},
			{
				path: "/domain/config",
				Component: DomainConfig,
				handle: {
					icon: "ControlOutlined",
					title: "参数配置",
					scope: "business",
					auth: "domain.config.read",
				},
			},
			{
				path: "/domain/audit-logs",
				Component: DomainAuditLogs,
				handle: {
					icon: "FileTextOutlined",
					title: "操作日志",
					scope: "business",
					auth: "domain.audit_log.read",
				},
			},
			{
				path: "/domain/login-logs",
				Component: DomainLoginLogs,
				handle: {
					icon: "LoginOutlined",
					title: "登录日志",
					scope: "business",
					auth: "domain.login_log.read",
				},
			},
		],
	},
];

export default routes;
