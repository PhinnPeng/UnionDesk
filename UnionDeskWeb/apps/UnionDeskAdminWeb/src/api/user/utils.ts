import type { LoginUserView, PermissionSnapshot, PermissionSnapshotMenu } from "@uniondesk/shared";

import type { AppRouteRecordRaw } from "#src/router/types";

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
	["/system/menus", "/platform/menu"],
	["/system/roles", "/platform/role"],
	["/system/users", "/platform/user"],
	["/system/user/offboard-pool", "/platform/offboard-pool"],
	["/system/users/offboard-pool", "/platform/offboard-pool"],
]);

interface BackendRouteNode {
	menu: PermissionSnapshotMenu
	scope: PermissionSnapshotMenu["scope"]
	path: string
	children: BackendRouteNode[]
}

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

export function buildUserInfoFromLoginUser(user: LoginUserView, roles: readonly string[]): UserInfoType {
	return {
		id: user.id,
		avatar: "",
		username: user.username,
		email: user.email ?? "",
		phoneNumber: user.mobile ?? "",
		description: "",
		roles: normalizeAccessRoles(roles),
		actions: [],
		platformAccess: false,
		menus: [],
	};
}

export function buildUserInfoFromPermissionSnapshot(snapshot: PermissionSnapshot): UserInfoType {
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
		menus: buildBackendRoutesFromSnapshot(snapshot.menus),
	};
}

export function hasPlatformAccess(snapshot: PermissionSnapshot): boolean {
	return snapshot.actions.some(action => action.code.startsWith("platform."));
}

export function buildBackendRoutesFromSnapshot(menus: readonly PermissionSnapshotMenu[]): BackendAppRouteRecordRaw[] {
	if (!menus.length) {
		return [];
	}

	const nodes = menus.map((menu) => ({
		menu,
		scope: menu.scope,
		path: normalizeBackendMenuPath(menu.path, menu.scope),
		children: [] as BackendRouteNode[],
	}));

	const nodesById = new Map<number, BackendRouteNode>();
	for (const node of nodes) {
		if (typeof node.menu.id === "number") {
			nodesById.set(node.menu.id, node);
		}
	}

	const roots: BackendRouteNode[] = [];
	for (const node of nodes) {
		const parentId = node.menu.parentId;
		if (typeof parentId === "number" && nodesById.has(parentId)) {
			nodesById.get(parentId)!.children.push(node);
		}
		else {
			roots.push(node);
		}
	}

	normalizeBackendRouteGroups(roots);
	sortBackendRouteNodes(roots);
	return roots.map(node => toAppRouteRecordRaw(node));
}

function getBackendMenuPathMap(scope?: PermissionSnapshotMenu["scope"]) {
	return scope === "platform"
		? PLATFORM_BACKEND_MENU_PATH_MAP
		: BUSINESS_BACKEND_MENU_PATH_MAP;
}

function normalizeBackendMenuPath(path: string, scope?: PermissionSnapshotMenu["scope"]) {
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

function normalizeBackendRouteGroups(nodes: BackendRouteNode[]) {
	for (const node of nodes) {
		if (node.children.length > 0) {
			normalizeBackendRouteGroups(node.children);
			if (!node.children.every(child => isDescendantPath(child.path, node.path))) {
				const commonPrefix = getCommonPathPrefix(node.children.map(child => child.path));
				if (commonPrefix) {
					node.path = commonPrefix;
				}
			}
		}
	}
}

function isDescendantPath(childPath: string, parentPath: string) {
	return childPath === parentPath || childPath.startsWith(`${parentPath}/`);
}

function getCommonPathPrefix(paths: string[]) {
	if (!paths.length) {
		return "";
	}

	const segmentsList = paths.map(path => path.split("/").filter(Boolean));
	const shortestLength = Math.min(...segmentsList.map(segments => segments.length));
	const commonSegments: string[] = [];

	for (let index = 0; index < shortestLength; index += 1) {
		const currentSegment = segmentsList[0][index];
		if (segmentsList.every(segments => segments[index] === currentSegment)) {
			commonSegments.push(currentSegment);
		}
		else {
			break;
		}
	}

	return commonSegments.length ? `/${commonSegments.join("/")}` : "";
}

function sortBackendRouteNodes(nodes: BackendRouteNode[]) {
	nodes.sort((left, right) => {
		const leftOrder = left.menu.orderNo ?? 0;
		const rightOrder = right.menu.orderNo ?? 0;
		if (leftOrder !== rightOrder) {
			return leftOrder - rightOrder;
		}
		return left.path.localeCompare(right.path);
	});
	for (const node of nodes) {
		if (node.children.length) {
			sortBackendRouteNodes(node.children);
		}
	}
}

function toAppRouteRecordRaw(node: BackendRouteNode): BackendAppRouteRecordRaw {
	const route: BackendAppRouteRecordRaw = {
		path: node.path,
		handle: {
			title: node.menu.name,
			icon: node.menu.icon ?? undefined,
			order: node.menu.orderNo ?? 0,
			scope: node.menu.scope ?? undefined,
			hideInMenu: node.menu.hidden ?? false,
			auth: node.menu.permissionCode ?? undefined,
		},
	};

	if (node.children.length) {
		route.children = node.children.map(child => toAppRouteRecordRaw(child));
	}
	if (node.menu.component || !node.children.length) {
		route.component = normalizeBackendComponentPath(node.menu.component, node.path, node.scope);
	}

	return route;
}
