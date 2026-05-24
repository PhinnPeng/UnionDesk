import type { AppRouteRecordRaw } from "#src/router/types";
import { useAccessStore } from "#src/store/access";
import { useAuthStore } from "#src/store/auth";
import { useUserStore } from "#src/store/user";

import { platformHomePath, platformPath } from "./route-path";

export const appScopes = {
	business: "business",
	platform: "platform",
} as const;

export type AppScope = typeof appScopes[keyof typeof appScopes];

const businessHomePath = import.meta.env.VITE_BASE_HOME_PATH || "/system/menu";

export function isPlatformRoutePath(pathname?: string) {
	return typeof pathname === "string" && (pathname === platformPath || pathname.startsWith(`${platformPath}/`));
}

export function getAppScopeByPath(pathname?: string): AppScope {
	return isPlatformRoutePath(pathname) ? appScopes.platform : appScopes.business;
}

export function getAppHomePath(scope: AppScope) {
	return scope === appScopes.platform ? platformHomePath : businessHomePath;
}

function hasBusinessMenus(menus: AppRouteRecordRaw[]) {
	return menus.some(menu => menu.handle?.scope !== appScopes.platform);
}

/**
 * 解析「返回首页」目标路径，与 auth-guard 访问 `/` 时的逻辑一致。
 */
function pickMenusForHome(): AppRouteRecordRaw[] {
	const { userRoutes, isAccessChecked } = useAccessStore.getState();
	if (isAccessChecked && userRoutes.length > 0) {
		return userRoutes;
	}
	const storeMenus = useUserStore.getState().menus ?? [];
	if (storeMenus.length > 0) {
		return storeMenus;
	}
	return useAuthStore.getState().user?.menus ?? [];
}

export function resolveBackHomePath(): string {
	const menus = pickMenusForHome();

	if (menus.length > 0) {
		return hasBusinessMenus(menus)
			? getAppHomePath(appScopes.business)
			: getAppHomePath(appScopes.platform);
	}

	const { platformAccess } = useUserStore.getState();
	return platformAccess ? getAppHomePath(appScopes.platform) : getAppHomePath(appScopes.business);
}
