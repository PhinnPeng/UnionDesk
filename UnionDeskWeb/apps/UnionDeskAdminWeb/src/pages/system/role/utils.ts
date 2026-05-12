import type { RoleItemType } from "#src/api/system/role";

import { appScopes } from "#src/router/extra-info/app-scope";

export function getVisibleRoleScope(appScope: string): RoleItemType["scope"] {
	return appScope === appScopes.platform ? "global" : "domain";
}

export function getRoleMenuTreeScope(appScope: string): "platform" | "business" {
	return appScope === appScopes.platform ? "platform" : "business";
}

export function filterRolesByAppScope(roles: readonly RoleItemType[], appScope: string): RoleItemType[] {
	const visibleScope = getVisibleRoleScope(appScope);
	return roles.filter(role => role.scope === visibleScope);
}
