import type { AppRouteRecordRaw } from "#src/router/types";
import ContainerLayout from "#src/layout/container-layout";
import { domain } from "#src/router/extra-info";

import { lazy } from "react";

const DomainOverview = lazy(() => import("#src/pages/domain/overview"));
const DomainTicketQueue = lazy(() => import("#src/pages/domain/ticket-queue"));
const DomainTicketQueueDetail = lazy(() => import("#src/pages/domain/ticket-queue/detail"));
const DomainBasic = lazy(() => import("#src/pages/domain/basic"));
const DomainMembers = lazy(() => import("#src/pages/domain/members"));
const DomainRoles = lazy(() => import("#src/pages/domain/roles"));
const DomainCustomers = lazy(() => import("#src/pages/domain/customers"));
const DomainOnboarding = lazy(() => import("#src/pages/domain/onboarding"));
const DomainTicketConfig = lazy(() => import("#src/pages/domain/ticket-config"));
const DomainTicketTypeConfig = lazy(() => import("#src/pages/domain/ticket-config/type-config"));
const DomainBlockwords = lazy(() => import("#src/pages/domain/blockwords"));
const DomainSla = lazy(() => import("#src/pages/domain/sla"));
const DomainConsultations = lazy(() => import("#src/pages/domain/consultations"));
const DomainNotifications = lazy(() => import("#src/pages/domain/notifications"));
const DomainConfig = lazy(() => import("#src/pages/domain/config"));
const DomainAuditLogs = lazy(() => import("#src/pages/domain/audit-logs"));
const DomainLoginLogs = lazy(() => import("#src/pages/domain/login-logs"));

function redirectRoute(from: string, to: string): AppRouteRecordRaw {
	return {
		path: from,
		Component: lazy(() =>
			import("#src/pages/domain/legacy-redirect").then(m => ({
				default: m.createLegacyRedirect(to),
			})),
		),
		handle: {
			title: "redirect",
			scope: "business",
			hideInMenu: true,
		},
	};
}

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
					path: "/domain/ticket-queue",
					Component: DomainTicketQueue,
					handle: {
						icon: "ProfileOutlined",
						title: "工单队列",
						scope: "business",
						auth: "ticket.view.domain_all",
					},
				},
				{
					path: "/domain/ticket-queue/:ticketId",
					Component: DomainTicketQueueDetail,
					handle: {
						icon: "ProfileOutlined",
						title: "工单详情",
						scope: "business",
						auth: "ticket.view.domain_all",
						hideInMenu: true,
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
				path: "/domain/customers/list",
				Component: DomainCustomers,
				handle: {
					icon: "SolutionOutlined",
					title: "客户列表",
					scope: "business",
					auth: "domain.customer.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/customers/onboarding",
				Component: DomainOnboarding,
				handle: {
					icon: "UserAddOutlined",
					title: "入域配置",
					scope: "business",
					auth: "domain.invitation_code.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/basic",
				Component: DomainBasic,
				handle: {
					icon: "SettingOutlined",
					title: "通用设置",
					scope: "business",
					auth: "domain.general.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/members",
				Component: DomainMembers,
				handle: {
					icon: "TeamOutlined",
					title: "员工管理",
					scope: "business",
					auth: "domain.member.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/roles",
				Component: DomainRoles,
				handle: {
					icon: "SafetyCertificateOutlined",
					title: "角色管理",
					scope: "business",
					auth: "domain.role.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/onboarding",
				Component: DomainOnboarding,
				handle: {
					icon: "UserAddOutlined",
					title: "入域管理",
					scope: "business",
					auth: "domain.invitation_code.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/config",
				Component: DomainConfig,
				handle: {
					icon: "ControlOutlined",
					title: "参数配置",
					scope: "business",
					auth: "domain.config.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/blockwords",
				Component: DomainBlockwords,
				handle: {
					icon: "StopOutlined",
					title: "屏蔽词库",
					scope: "business",
					auth: "domain.blocked_word.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/sla",
				Component: DomainSla,
				handle: {
					icon: "FieldTimeOutlined",
					title: "SLA 管理",
					scope: "business",
					auth: "domain.sla.read",
				},
			},
			{
				path: "/domain/consultations",
				Component: DomainConsultations,
				handle: {
					icon: "CustomerServiceOutlined",
					title: "在线咨询",
					scope: "business",
					auth: "consultation.view",
				},
			},
			{
				path: "/domain/settings/notifications",
				Component: DomainNotifications,
				handle: {
					icon: "MailOutlined",
					title: "通知配置",
					scope: "business",
					auth: "domain.notification_template.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/audit-logs",
				Component: DomainAuditLogs,
				handle: {
					icon: "FileTextOutlined",
					title: "操作日志",
					scope: "business",
					auth: "domain.audit_log.read",
					hideInMenu: true,
				},
			},
			{
				path: "/domain/settings/login-logs",
				Component: DomainLoginLogs,
				handle: {
					icon: "LoginOutlined",
					title: "登录日志",
					scope: "business",
					auth: "domain.login_log.read",
					hideInMenu: true,
				},
			},
			redirectRoute("/domain/customers", "/domain/customers/list"),
			redirectRoute("/domain/settings", "/domain/settings/basic"),
			redirectRoute("/domain/basic", "/domain/settings/basic"),
			redirectRoute("/domain/members", "/domain/settings/members"),
			redirectRoute("/domain/roles", "/domain/settings/roles"),
			redirectRoute("/domain/onboarding", "/domain/settings/onboarding"),
			redirectRoute("/domain/config", "/domain/settings/config"),
			redirectRoute("/domain/blockwords", "/domain/settings/blockwords"),
			redirectRoute("/domain/notifications", "/domain/settings/notifications"),
			redirectRoute("/domain/audit-logs", "/domain/settings/audit-logs"),
			redirectRoute("/domain/login-logs", "/domain/settings/login-logs"),
		],
	},
];

export default routes;
