import type { LoginUserView, PermissionSnapshot, PermissionSnapshotMenu } from "@uniondesk/shared";

import type { AppRouteRecordRaw } from "#src/router/types";

import { buildRoutesFromAdminMenuSnapshot } from "#src/layout/layout-menu";
import { hasPlatformRoleHint } from "#src/router/extra-info/resolve-home-path";
import { hasBusinessDomainAccess } from "#src/utils/access/business-domain";

import type { UserInfoType } from "./types";

const BUSINESS_BACKEND_MENU_PATH_MAP = new Map<string, string>([
	["/system/menus", "/system/menu"],
	["/system/roles", "/system/role"],
	["/system/users", "/system/user"],
	["/system/users/offboard-pool", "/system/user/offboard-pool"],
]);

const PLATFORM_BACKEND_MENU_PATH_MAP = new Map<string, string>([
	["/platform/menus", "/platform/menu"],
	["/platform/roles", "/platform/role"],
	["/platform/users", "/platform/user"],
	["/platform/user/offboard-pool", "/platform/offboard-pool"],
	["/system/role", "/platform/role"],
	["/system/menus", "/platform/menu"],
	["/system/roles", "/platform/role"],
	["/system/users", "/platform/user"],
	["/system/user/offboard-pool", "/platform/offboard-pool"],
	["/system/users/offboard-pool", "/platform/offboard-pool"],
]);

type BackendAppRouteRecordRaw = AppRouteRecordRaw & {
	component?: string
};

export function normalizeAccessRoles(roles: readonly string[] = []) {
	const normalizedRoles: string[] = [];
	const seen = new Set<string>();

	for (const role of roles) {
		const lowerCaseRole = role.toLowerCase();
		const normalizedRole = lowerCaseRole === "super_admin" || lowerCaseRole === "domain_admin"
			? "admin"
			: lowerCaseRole === "agent"
				? "common"
				: lowerCaseRole;
		if (!seen.has(normalizedRole)) {
			seen.add(normalizedRole);
			normalizedRoles.push(normalizedRole);
		}
	}

	return normalizedRoles;
}

export function buildUserInfoFromLoginUser(
	user: LoginUserView,
	roles: readonly string[],
	loginRole?: string | null,
): UserInfoType {
	return {
		id: user.id,
		avatar: "",
		username: user.username,
		email: user.email ?? "",
		phoneNumber: user.mobile ?? "",
		description: "",
		roles: normalizeAccessRoles(roles),
		actions: [],
		platformAccess: hasPlatformRoleHint(roles, loginRole),
		businessDomainAccess: false,
		menus: [],
	};
}

export function buildUserInfoFromPermissionSnapshot(snapshot: PermissionSnapshot): UserInfoType {
	const menus = buildBackendRoutesFromSnapshot(snapshot.menuTree);
	return {
		id: snapshot.user.id,
		avatar: "",
		username: snapshot.user.username,
		email: snapshot.user.email ?? "",
		phoneNumber: snapshot.user.mobile ?? "",
		description: "",
		roles: normalizeAccessRoles(snapshot.roles),
		actions: snapshot.actions.map(action => action.code),
		platformAccess: hasPlatformAccess(snapshot),
		businessDomainAccess: hasBusinessDomainAccess(menus),
		menus,
	};
}

export function hasPlatformAccess(snapshot: PermissionSnapshot): boolean {
	if (snapshot.roles.some((role) => {
		const code = role.toLowerCase();
		return code === "super_admin" || code === "platform_admin";
	})) {
		return true;
	}
	if (snapshot.menuTree.some(menu => menu.scope === "platform")) {
		return true;
	}
	return snapshot.actions.some(action => action.code.startsWith("platform."));
}

export function buildBackendRoutesFromSnapshot(menus: readonly PermissionSnapshotMenu[]): BackendAppRouteRecordRaw[] {
	return buildRoutesFromAdminMenuSnapshot(menus, {
		normalizeMenuPath: normalizeBackendMenuPath,
		normalizeComponentPath: normalizeBackendComponentPath,
	});
}

function getBackendMenuPathMap(scope?: PermissionSnapshotMenu["scope"]) {
	return scope === "platform"
		? PLATFORM_BACKEND_MENU_PATH_MAP
		: BUSINESS_BACKEND_MENU_PATH_MAP;
}

function normalizeBackendMenuPath(path: string | null | undefined, scope?: PermissionSnapshotMenu["scope"]) {
	if (!path) {
		return "";
	}
	const normalizedPath = path.trim().replace(/\/+$/, "");
	return getBackendMenuPathMap(scope).get(normalizedPath) ?? normalizedPath;
}

function normalizeBackendComponentStem(component: string) {
	const withoutPrefix = component
		.trim()
		.replace(/^\/src\/pages/, "")
		.replace(/\.tsx$/, "")
		.replace(/\/index$/, "")
		.replace(/^\.\//, "/");
	return withoutPrefix.startsWith("/") ? withoutPrefix : `/${withoutPrefix}`;
}

function formatBackendComponentStem(stem: string) {
	if (stem.startsWith("/src/pages/")) {
		return stem;
	}
	if (stem.endsWith(".tsx")) {
		return stem.startsWith("/")
			? `/src/pages${stem}`
			: `/src/pages/${stem}`;
	}
	return stem.startsWith("/")
		? `/src/pages${stem}/index.tsx`
		: `/src/pages/${stem}/index.tsx`;
}

function normalizeBackendComponentPath(component: string | null | undefined, path: string, scope?: PermissionSnapshotMenu["scope"]) {
	if (!component) {
		return `/src/pages${path}/index.tsx`;
	}

	const normalizedComponent = component.trim();
	const normalizedStem = normalizeBackendComponentStem(normalizedComponent);
	const mappedStem = getBackendMenuPathMap(scope).get(normalizedStem) ?? normalizedStem;
	if (normalizedComponent.startsWith("/src/pages/") && mappedStem === normalizedStem) {
		return normalizedComponent;
	}
	return formatBackendComponentStem(mappedStem);
}
