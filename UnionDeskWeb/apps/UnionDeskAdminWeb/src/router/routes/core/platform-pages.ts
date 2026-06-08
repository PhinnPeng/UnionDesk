import type { AppRouteRecordRaw } from "#src/router/types";

import ContainerLayout from "#src/layout/container-layout";

import { lazy } from "react";

const TicketDetail = lazy(() => import("#src/pages/platform/ticket-detail"));
const DomainDetail = lazy(() => import("#src/pages/platform/domains/detail"));

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
];

export default routes;
