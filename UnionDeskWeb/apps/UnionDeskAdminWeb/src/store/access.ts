import type { MenuItemType } from "#src/layout/layout-menu/types";
import type { AppRouteRecordRaw } from "#src/router/types";

import { appScopes } from "#src/router/extra-info/app-scope";
import { rootRoute, router } from "#src/router";
import { ROOT_ROUTE_ID } from "#src/router/constants";
import { baseRoutes } from "#src/router/routes";
import { ascending } from "#src/router/utils/ascending";
import { flattenRoutes } from "#src/router/utils/flatten-routes";
import { generateUserMenus } from "#src/router/utils/generate-user-menus";

import { create } from "zustand";

interface AccessState {
	// 用户后端返回的路由
	userRoutes: AppRouteRecordRaw[]
	// 用户菜单
	userMenus: MenuItemType[]
	// 平台菜单
	platformMenus: MenuItemType[]
	// 有权限的 React Router 路由
	routeList: AppRouteRecordRaw[]
	// 扁平化后的路由，路由 id 作为索引 key
	flatRouteList: Record<string, AppRouteRecordRaw>
	// 是否获取到权限
	isAccessChecked: boolean
}

const initialState: AccessState = {
	userRoutes: [],
	userMenus: [],
	platformMenus: [],
	routeList: [],
	flatRouteList: {},
	isAccessChecked: false,
};

interface AccessAction {
	setAccessStore: (userRoutes: AppRouteRecordRaw[], allRoutes: AppRouteRecordRaw[]) => AccessState
	reset: () => void
};

export const useAccessStore = create<AccessState & AccessAction>(set => ({
	...initialState,

	setAccessStore: (userRoutes, allRoutes) => {
		const newRoutes = ascending([...baseRoutes, ...allRoutes]);
		/* 添加新的路由到根路由 */
		router.patchRoutes(ROOT_ROUTE_ID, allRoutes);
		const flatRouteList = flattenRoutes(newRoutes);
		const userMenus = generateUserMenus(userRoutes, appScopes.business);
		const platformMenus = generateUserMenus(userRoutes, appScopes.platform);
		const newState = {
			userRoutes,
			userMenus,
			platformMenus,
			routeList: newRoutes,
			flatRouteList,
			isAccessChecked: true,
		};
		set(() => newState);
		return newState;
	},

	reset: () => {
		/* 移除动态路由 */
		router._internalSetRoutes(rootRoute);
		set(initialState);
	},
}));
