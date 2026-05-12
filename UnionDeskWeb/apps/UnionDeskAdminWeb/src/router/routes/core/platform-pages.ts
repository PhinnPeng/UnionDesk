import type { AppRouteRecordRaw } from "#src/router/types";

import { lazy } from "react";

const TicketDetail = lazy(() => import("#src/pages/platform/ticket-detail"));

/**
 * 平台域内置页面路由
 * 这些页面不依赖后端菜单配置，始终可访问，但隐藏于侧栏菜单
 */
const routes: AppRouteRecordRaw[] = [
	{
		path: "/platform/ticket-detail",
		Component: TicketDetail,
		handle: {
			hideInMenu: true,
			scope: "platform",
			title: "工单详情",
		},
	},
];

export default routes;
