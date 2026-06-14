import type { AppRouteRecordRaw } from "#src/router/types";
import { useAccessStore } from "#src/store/access";
import { useAuthStore } from "#src/store/auth";
import { useUserStore } from "#src/store/user";

import { platformHomePath, platformPath } from "./route-path";
import { resolveHomePathFromMenus } from "./resolve-home-path";

export { hasPlatformRoleHint, isPlatformRoleCode, resolveHomePathFromActions, resolveHomePathFromMenus } from "./resolve-home-path";
export type { ResolveHomePathOptions } from "./resolve-home-path";

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

/**
 * 解析「返回首页」目标路径，与 auth-guard 访问 `/` 时的逻辑一致。
 */
function pickMenusForHome(): AppRouteRecordRaw[] {
	const storeMenus = useUserStore.getState().menus ?? [];
	if (storeMenus.length > 0) {
		return storeMenus;
	}
	const authMenus = useAuthStore.getState().user?.menus ?? [];
	if (authMenus.length > 0) {
		return authMenus;
	}
	const { userRoutes, isAccessChecked } = useAccessStore.getState();
	if (isAccessChecked && userRoutes.length > 0) {
		return userRoutes;
	}
	return [];
}

function getHomePathContext() {
	const { platformAccess, roles, actions } = useUserStore.getState();
	const loginRole = useAuthStore.getState().role;
	return { platformAccess, roles, loginRole, actions };
}

export function resolveBackHomePath(): string {
	const menus = pickMenusForHome();
	const { platformAccess, roles, loginRole, actions } = getHomePathContext();
	return resolveHomePathFromMenus(menus, platformAccess, { roles, loginRole }, actions);
}
