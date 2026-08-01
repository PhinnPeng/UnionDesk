import type { AppRouteRecordRaw } from "#src/router/types";

/**
 * 业务域端可达：员工是否在至少一业务域的访问名单中（成员关系）。
 * 不再用 business 菜单推断。
 */
export function hasBusinessDomainAccessFromDomains(domains?: readonly unknown[] | null): boolean {
	return Array.isArray(domains) && domains.length > 0;
}

/**
 * @deprecated 请改用 hasBusinessDomainAccessFromDomains；保留供过渡期回退。
 */
export function hasBusinessDomainAccess(menus?: AppRouteRecordRaw[]): boolean {
	if (!menus || menus.length === 0) {
		return false;
	}
	return menus.some(menu => menu.handle?.scope === "business");
}
