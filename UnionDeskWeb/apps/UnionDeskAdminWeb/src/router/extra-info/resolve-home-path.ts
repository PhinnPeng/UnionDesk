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
 * 按「平台能力 × 域访问名单」二维矩阵解析默认首页（design §0.3）。
 */
export function resolveDefaultHomePath(platformAccess: boolean, businessDomainAccess: boolean): string {
	if (platformAccess && !businessDomainAccess) {
		return getHomePath(scopePlatform);
	}
	if (businessDomainAccess) {
		return getHomePath(scopeBusiness);
	}
	// 双无：无可用控制台，仍落业务首页由守卫展示无权限；避免死循环跳平台
	return getHomePath(scopeBusiness);
}

/**
 * @deprecated 进端已改为名单矩阵；保留供测试过渡，勿用于新逻辑。
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
 * 根据平台能力与域名单解析默认首页。
 * `platformAccess` / `businessDomainAccess` 优先；缺省时回退业务首页。
 */
export function resolveHomePathFromMenus(
	_menus: AppRouteRecordRaw[],
	platformAccess: boolean,
	_options?: ResolveHomePathOptions,
	_actions: readonly string[] = [],
	businessDomainAccess = false,
): string {
	return resolveDefaultHomePath(platformAccess, businessDomainAccess);
}
