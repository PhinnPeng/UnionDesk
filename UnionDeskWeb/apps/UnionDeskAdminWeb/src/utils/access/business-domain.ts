import type { AppRouteRecordRaw } from "#src/router/types";

/**
 * 检查用户是否具有业务域访问权限。
 * 规则：只要用户菜单中存在 scope = "business" 的菜单即可视为有业务域权限。
 */
export function hasBusinessDomainAccess(menus?: AppRouteRecordRaw[]): boolean {
	if (!menus || menus.length === 0) {
		return false;
	}

	return menus.some(menu => menu.handle?.scope === "business");
}
