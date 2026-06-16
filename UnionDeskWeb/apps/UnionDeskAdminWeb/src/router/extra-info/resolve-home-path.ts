import type { AppRouteRecordRaw } from "#src/router/types";

import { businessHomePath, platformHomePath } from "./route-path";

const scopePlatform = "platform";
const scopeBusiness = "business";

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

/**
 * 按快照 actions 权限码组合解析默认首页。
 * 仅 platform.* → 平台控制台；仅非 platform.* 或两者都有 → 统一业务域后台。
 */
export function resolveHomePathFromActions(actions: readonly string[]): string {
	const hasPlatform = actions.some(code => code.startsWith("platform."));
	const hasNonPlatform = actions.some(code => !code.startsWith("platform."));
	if (hasPlatform && !hasNonPlatform) {
		return getHomePath(scopePlatform);
	}
	return getHomePath(scopeBusiness);
}

/**
 * 根据 actions 解析默认首页（登录成功、访问 `/`、返回首页共用）。
 * actions 非空时优先走三元规则；否则回退业务域默认首页。
 */
export function resolveHomePathFromMenus(
	_menus: AppRouteRecordRaw[],
	_platformAccess: boolean,
	_options?: ResolveHomePathOptions,
	actions: readonly string[] = [],
): string {
	if (actions.length > 0) {
		return resolveHomePathFromActions(actions);
	}
	return getHomePath(scopeBusiness);
}
