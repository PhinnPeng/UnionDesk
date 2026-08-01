import type { AppRouteRecordRaw } from "#src/router/types";

import ContainerLayout from "#src/layout/container-layout";

import { lazy } from "react";

const PlatformHome = lazy(() => import("#src/pages/platform/home"));
const TicketDetail = lazy(() => import("#src/pages/platform/ticket-detail"));
const DomainDetail = lazy(() => import("#src/pages/platform/domains/detail"));
const TicketTypeConfigPage = lazy(() => import("#src/pages/platform/domains/ticket-type-config"));
const TicketTypeFlowConfig = lazy(() => import("#src/pages/platform/domains/ticket-type-config/flow"));
const TicketFormDesign = lazy(() => import("#src/pages/common/form-design"));
const TicketTypeAttributes = lazy(() => import("#src/pages/platform/domains/ticket-type-attributes"));
const PlatformTicketConfig = lazy(() => import("#src/pages/platform/ticket-config"));
const TicketConfigTypes = lazy(() => import("#src/pages/platform/ticket-config/types"));
const PlatformTicketTypeConfig = lazy(() => import("#src/pages/platform/ticket-config/types/config"));
const TicketConfigAttributes = lazy(() => import("#src/pages/platform/ticket-config/attributes"));
const TicketConfigStatuses = lazy(() => import("#src/pages/platform/ticket-config/statuses"));
const TicketConfigTemplates = lazy(() => import("#src/pages/platform/ticket-config/templates"));
const TeamTemplateConfigEntry = lazy(() => import("#src/pages/platform/ticket-config/templates/config"));
const TeamTemplateConfigBasic = lazy(() => import("#src/pages/platform/ticket-config/templates/config/basic"));
const TeamTemplateConfigCollaboration = lazy(() => import("#src/pages/platform/ticket-config/templates/config/collaboration"));

/**
 * 将内置平台页面包裹在 ContainerLayout 内，经 LayoutContent 渲染（侧栏、顶栏、页签）。
 * ContainerLayout 禁止 lazy，避免路由切换闪动。
 */
function withPlatformLayout(route: AppRouteRecordRaw): AppRouteRecordRaw {
	if (!("path" in route) || !route.path || !("Component" in route) || !route.Component) {
		return route;
	}

	return {
		path: route.path,
		id: route.id,
		redirect: route.redirect,
		handle: route.handle,
		Component: ContainerLayout,
		children: [
			{
				index: true,
				Component: route.Component,
				handle: route.handle,
			},
		],
	};
}

/**
 * 平台域内置页面路由
 * 这些页面不依赖后端菜单配置，始终可访问，但隐藏于侧栏菜单
 */
const routes: AppRouteRecordRaw[] = [
	withPlatformLayout({
		path: "/platform/home",
		Component: PlatformHome,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "平台首页",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-detail",
		Component: TicketDetail,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "工单详情",
		},
	}),
	withPlatformLayout({
		path: "/platform/domains/detail/:domainId?",
		Component: DomainDetail,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "业务域控制台",
			currentActiveMenu: "/platform/domains",
		},
	}),
	withPlatformLayout({
		path: "/platform/domains/ticket/form-design/:domainId/:typeId",
		Component: TicketFormDesign,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "表单设计",
			currentActiveMenu: "/platform/domains",
		},
	}),
	withPlatformLayout({
		path: "/platform/domains/ticket-type-config/:domainId/:typeId/flow",
		Component: TicketTypeFlowConfig,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "状态流配置",
			currentActiveMenu: "/platform/domains",
		},
	}),
	withPlatformLayout({
		path: "/platform/domains/ticket-type-config/:domainId/:typeId",
		Component: TicketTypeConfigPage,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "事项类型配置",
			currentActiveMenu: "/platform/domains",
		},
	}),
	withPlatformLayout({
		path: "/platform/domains/ticket-type-attributes/:domainId/:typeId",
		Component: TicketTypeAttributes,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "属性编排",
			currentActiveMenu: "/platform/domains",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config",
		Component: PlatformTicketConfig,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "事项配置",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/attributes",
		Component: TicketConfigAttributes,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "事项属性",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/types/:typeId",
		Component: PlatformTicketTypeConfig,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "事项类型配置",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/types",
		Component: TicketConfigTypes,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "事项类型",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/statuses",
		Component: TicketConfigStatuses,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "事项状态",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/templates/:templateId/basic",
		Component: TeamTemplateConfigBasic,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "团队模板基础信息",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/templates/:templateId/collaboration",
		Component: TeamTemplateConfigCollaboration,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "团队模板协作配置",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/templates/:templateId",
		Component: TeamTemplateConfigEntry,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "团队模板配置",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
	withPlatformLayout({
		path: "/platform/ticket-config/templates",
		Component: TicketConfigTemplates,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "团队模板",
			currentActiveMenu: "/platform/ticket-config",
		},
	}),
];

export default routes;
