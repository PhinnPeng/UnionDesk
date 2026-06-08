import type { AppRouteRecordRaw } from "#src/router/types";

import { platformHomePath } from "./route-path";

const scopePlatform = "platform";
const scopeBusiness = "business";

const businessHomePath = import.meta.env.VITE_BASE_HOME_PATH || "/system/menu";

export interface ResolveHomePathOptions {
	roles?: readonly string[]
	loginRole?: string | null
}

export function isPlatformRoleCode(role?: string | null): boolean {
	if (!role?.trim()) {
		return false;
	}
	const code = role.toLowerCase();
	return code === "super_admin" || code === "platform_admin";
}

/** 登录响应 role 或原始 roles 表明平台管理员（勿用 normalize 后的 admin） */
export function hasPlatformRoleHint(roles: readonly string[], loginRole?: string | null): boolean {
	if (isPlatformRoleCode(loginRole)) {
		return true;
	}
	return roles.some(role => isPlatformRoleCode(role));
}

function getHomePath(scope: typeof scopePlatform | typeof scopeBusiness) {
	return scope === scopePlatform ? platformHomePath : businessHomePath;
}

function hasBusinessMenus(menus: AppRouteRecordRaw[]) {
	return menus.some(menu => menu.handle?.scope !== scopePlatform);
}

function hasPlatformMenus(menus: AppRouteRecordRaw[]): boolean {
	for (const menu of menus) {
		if (menu.handle?.scope === scopePlatform) {
			return true;
		}
		if (menu.children?.length && hasPlatformMenus(menu.children)) {
			return true;
		}
	}
	return false;
}

function shouldPreferPlatformHome(
	menus: AppRouteRecordRaw[],
	platformAccess: boolean,
	options?: ResolveHomePathOptions,
): boolean {
	return platformAccess
		|| hasPlatformMenus(menus)
		|| hasPlatformRoleHint(options?.roles ?? [], options?.loginRole);
}

/**
 * 根据菜单与平台权限解析默认首页（登录成功、访问 `/`、返回首页共用）。
 * 具备 platformAccess、平台 scope 菜单或平台角色时优先 `/platform/home`。
 */
export function resolveHomePathFromMenus(
	menus: AppRouteRecordRaw[],
	platformAccess: boolean,
	options?: ResolveHomePathOptions,
): string {
	if (shouldPreferPlatformHome(menus, platformAccess, options)) {
		return getHomePath(scopePlatform);
	}
	if (menus.length > 0) {
		return hasBusinessMenus(menus)
			? getHomePath(scopeBusiness)
			: getHomePath(scopePlatform);
	}
	return getHomePath(scopeBusiness);
}
