import type { AppRouteRecordRaw } from "#src/router/types";
import { useAccessStore } from "#src/store/access";
import { useAuthStore } from "#src/store/auth";
import { useUserStore } from "#src/store/user";

import { resolveHomePathFromMenus } from "./resolve-home-path";

export {
	appScopes,
	getAppHomePath,
	getAppScopeByPath,
	isPlatformRoutePath,
} from "./app-scope-core";
export type { AppScope } from "./app-scope-core";

export { hasPlatformRoleHint, isPlatformRoleCode, resolveDefaultHomePath, resolveHomePathFromActions, resolveHomePathFromMenus } from "./resolve-home-path";
export type { ResolveHomePathOptions } from "./resolve-home-path";

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
	const { platformAccess, businessDomainAccess, roles, actions } = useUserStore.getState();
	const loginRole = useAuthStore.getState().role;
	return { platformAccess, businessDomainAccess: Boolean(businessDomainAccess), roles, loginRole, actions };
}

export function resolveBackHomePath(): string {
	const menus = pickMenusForHome();
	const { platformAccess, businessDomainAccess, roles, loginRole, actions } = getHomePathContext();
	return resolveHomePathFromMenus(menus, platformAccess, { roles, loginRole }, actions, businessDomainAccess);
}
