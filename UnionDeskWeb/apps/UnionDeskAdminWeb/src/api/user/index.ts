import type { AppRouteRecordRaw } from "#src/router/types";
import type { PermissionSnapshot } from "#src/api/auth";
import type { AppScope } from "#src/router/extra-info/app-scope-core";

import { fetchPermissionSnapshot } from "#src/api/auth";
import { requestBackendJson } from "#src/utils/request";
import { removeDuplicateRoutes } from "#src/router/guard/utils";
import { appScopes, getAppScopeByPath } from "#src/router/extra-info/app-scope-core";
import { businessHomePath, platformHomePath } from "#src/router/extra-info/route-path";
import { accessRoutes } from "#src/router/routes";
import { generateRoutesFromBackend } from "#src/router/utils/generate-routes-from-backend";
import { generateRoutesByFrontend } from "#src/router/utils/generate-routes-from-frontend";
import { useAccessStore } from "#src/store/access";
import { useAuthStore } from "#src/store/auth";
import { usePreferencesStore } from "#src/store/preferences";
import { useUserStore } from "#src/store/user";
import { AUTH_REFRESH_PATH } from "#src/utils/request/constants";

import { buildBackendRoutesFromSnapshot, buildUserInfoFromPermissionSnapshot } from "./utils";
import type { UserInfoType } from "./types";

import { matchRoutes } from "react-router";

export * from "./types";
export { createCaptchaChallenge, fetchLogin, fetchLoginConfig, fetchLogout, fetchPermissionSnapshot, fetchSessionStatus, fetchSetDefaultDomain, fetchSwitchDomain, verifyCaptcha } from "#src/api/auth";

const PERMISSION_SNAPSHOT_CACHE_TTL = 5_000;

let cachedPermissionSnapshot: PermissionSnapshot | null = null;
let cachedPermissionSnapshotAt = 0;
let cachedPermissionSnapshotKey = "";
let pendingPermissionSnapshotPromise: Promise<PermissionSnapshot> | null = null;

function routeTreeHasPath(routes: readonly AppRouteRecordRaw[], targetPath: string): boolean {
	for (const route of routes) {
		if (route.path === targetPath) {
			return true;
		}
		if (route.children?.length && routeTreeHasPath(route.children, targetPath)) {
			return true;
		}
	}
	return false;
}

function buildSnapshotCacheKey(menuScope: AppScope, domainId?: string) {
	const { sid, token } = useAuthStore.getState();
	return `${sid}:${token}:${menuScope}:${domainId ?? 0}`;
}

function resolveMenuScopeForPath(): AppScope {
	const authState = useAuthStore.getState();
	const userState = useUserStore.getState();
	const pathname = typeof window !== "undefined" ? window.location.pathname : "";
	const onPlatform = pathname.startsWith("/platform");
	const platformAccess = userState.platformAccess
		|| authState.role === "super_admin"
		|| authState.role === "platform_admin";
	const businessDomainAccess = (authState.accessibleDomains?.length ?? 0) > 0
		|| Boolean(userState.businessDomainAccess);
	if (onPlatform && platformAccess) {
		return appScopes.platform;
	}
	return businessDomainAccess ? appScopes.business : appScopes.platform;
}

async function fetchSnapshotForScope(menuScope: AppScope, domainId?: string): Promise<{
	userInfo: UserInfoType
	routes: AppRouteRecordRaw[]
	snapshot: PermissionSnapshot
}> {
	const cacheKey = buildSnapshotCacheKey(menuScope, domainId);
	const now = Date.now();
	const authState = useAuthStore.getState();
	const accessibleDomains = authState.accessibleDomains ?? [];
	const businessDomainAccess = accessibleDomains.length > 0
		|| Boolean(useUserStore.getState().businessDomainAccess);
	const platformAccess = useUserStore.getState().platformAccess
		|| authState.role === "super_admin"
		|| authState.role === "platform_admin";

	let snapshot: PermissionSnapshot;
	if (cachedPermissionSnapshot && cachedPermissionSnapshotKey === cacheKey && now - cachedPermissionSnapshotAt < PERMISSION_SNAPSHOT_CACHE_TTL) {
		snapshot = cachedPermissionSnapshot;
	}
	else {
		if (!pendingPermissionSnapshotPromise || cachedPermissionSnapshotKey !== cacheKey) {
			cachedPermissionSnapshotKey = cacheKey;
			pendingPermissionSnapshotPromise = fetchPermissionSnapshot({
				menuScope,
				domainId: menuScope === appScopes.business ? domainId : undefined,
			}).then((result) => {
				cachedPermissionSnapshot = result;
				cachedPermissionSnapshotAt = Date.now();
				return result;
			}).finally(() => {
				pendingPermissionSnapshotPromise = null;
			});
		}
		snapshot = await pendingPermissionSnapshotPromise;
	}

	let userInfo = buildUserInfoFromPermissionSnapshot(snapshot, {
		accessibleDomains,
		businessDomainAccess,
	});
	if (platformAccess || hasPlatformHintFromAuth()) {
		userInfo = { ...userInfo, platformAccess: true };
	}
	return {
		userInfo,
		routes: buildBackendRoutesFromSnapshot(snapshot.menuTree),
		snapshot,
	};
}

function hasPlatformHintFromAuth() {
	const authState = useAuthStore.getState();
	return authState.role === "super_admin" || authState.role === "platform_admin";
}

async function buildDynamicRoutesFromMenus(
	backendMenus: AppRouteRecordRaw[],
	roles: string[],
	scope: AppScope,
): Promise<AppRouteRecordRaw[]> {
	const { enableBackendAccess, enableFrontendAceess } = usePreferencesStore.getState();
	const routes: AppRouteRecordRaw[] = [];
	if (enableBackendAccess && backendMenus.length) {
		routes.push(...await generateRoutesFromBackend(backendMenus));
	}
	if (enableFrontendAceess) {
		routes.push(...generateRoutesByFrontend(accessRoutes, roles));
	}
	else if (scope === appScopes.business && !routeTreeHasPath(routes, "/home")) {
		// 后端菜单模式兜底：business 快照可能缺 /home
		routes.push(...accessRoutes.filter(route => route.path === "/home" || route.path === "/domain"));
	}
	return removeDuplicateRoutes(routes);
}

function mergeIdentityUserInfo(next: UserInfoType): UserInfoType {
	const current = useUserStore.getState();
	if (!current.id) {
		return next;
	}
	return {
		...next,
		id: current.id || next.id,
		username: current.username || next.username,
		email: current.email || next.email,
		phoneNumber: current.phoneNumber || next.phoneNumber,
		avatar: current.avatar || next.avatar,
		description: current.description || next.description,
		platformAccess: current.platformAccess || next.platformAccess,
		businessDomainAccess: current.businessDomainAccess || next.businessDomainAccess,
		roles: next.roles.length ? next.roles : current.roles,
	};
}

/**
 * 确保指定端 bucket 已加载；命中缓存则只 activate。
 * @returns 激活后的 access 投影状态
 */
export async function ensureScopeAccess(
	scope: AppScope,
	options?: { force?: boolean, domainId?: string },
) {
	const access = useAccessStore.getState();
	const authState = useAuthStore.getState();
	const domainId = options?.domainId ?? authState.defaultBusinessDomainId;
	const existing = access.getBucket(scope);

	if (!options?.force && existing) {
		if (scope === appScopes.platform) {
			return access.activateScope(scope);
		}
		if (existing.domainId === domainId || !domainId) {
			return access.activateScope(scope);
		}
	}

	const loaded = await fetchSnapshotForScope(scope, scope === appScopes.business ? domainId : undefined);
	const userInfo = mergeIdentityUserInfo(loaded.userInfo);
	useUserStore.getState().setUserInfo(userInfo);
	useAuthStore.setState(state => ({
		...state,
		user: userInfo,
	}));

	const backendMenus = loaded.routes.length ? loaded.routes : (userInfo.menus ?? []);
	const dynamicRoutes = await buildDynamicRoutesFromMenus(backendMenus, userInfo.roles, scope);

	return useAccessStore.getState().commitScopeBucket(scope, {
		userRoutes: backendMenus,
		dynamicRoutes,
		actions: userInfo.actions,
		domainId: scope === appScopes.business ? domainId : undefined,
	});
}

/**
 * 切端：确保目标 bucket → 激活 → 返回首页 path。
 */
export async function switchAppScope(targetScope: AppScope): Promise<string> {
	await ensureScopeAccess(targetScope);
	return targetScope === appScopes.platform ? platformHomePath : businessHomePath;
}

export function invalidatePermissionSnapshotCache() {
	cachedPermissionSnapshot = null;
	cachedPermissionSnapshotAt = 0;
	cachedPermissionSnapshotKey = "";
	pendingPermissionSnapshotPromise = null;
}

/**
 * 切域后仅刷新 business bucket；platform bucket 保留。
 * 若当前路径在新域不可达则返回 `/home`。
 */
export async function reloadBusinessAccessAfterDomainSwitch(currentPathname: string): Promise<string> {
	invalidatePermissionSnapshotCache();
	useAccessStore.getState().invalidateBusinessBucket();

	const accessState = await ensureScopeAccess(appScopes.business, { force: true });
	if (!accessState) {
		return "/home";
	}

	const matches = matchRoutes(accessState.routeList, currentPathname) ?? [];
	const leaf = matches[matches.length - 1];
	const hasChildren = leaf?.route?.children?.filter(item => !item.index)?.length;
	if (!matches.length || (hasChildren && hasChildren > 0)) {
		return "/home";
	}
	const routeAuth = (leaf?.route as AppRouteRecordRaw | undefined)?.handle?.auth;
	const actions = accessState.businessBucket?.actions ?? useUserStore.getState().actions;
	if (typeof routeAuth === "string" && routeAuth && !actions.includes(routeAuth)) {
		return "/home";
	}
	return currentPathname;
}

async function loadPermissionSnapshotData() {
	const scope = resolveMenuScopeForPath();
	const authState = useAuthStore.getState();
	const loaded = await fetchSnapshotForScope(
		scope,
		scope === appScopes.business ? authState.defaultBusinessDomainId : undefined,
	);
	return {
		userInfo: loaded.userInfo,
		routes: loaded.routes,
	};
}

export async function fetchUserInfo(): Promise<UserInfoType> {
	const { userInfo } = await loadPermissionSnapshotData();
	return userInfo;
}

export async function fetchAsyncRoutes(): Promise<AppRouteRecordRaw[]> {
	const { routes } = await loadPermissionSnapshotData();
	return routes;
}

export async function fetchUserInfoAndRoutes() {
	return loadPermissionSnapshotData();
}

/**
 * AuthGuard 首载 / path scope 不一致时：按 path 确保并激活对应 bucket。
 */
export async function bootstrapAccessForPath(pathname: string) {
	const scope = getAppScopeByPath(pathname);
	return ensureScopeAccess(scope);
}

export interface RefreshTokenResult {
	token: string
	refreshToken: string
}

export async function fetchRefreshToken(data: { readonly refreshToken: string }) {
	return requestBackendJson<RefreshTokenResult>(`v1${AUTH_REFRESH_PATH}`, {
		method: "POST",
		json: data,
	});
}
