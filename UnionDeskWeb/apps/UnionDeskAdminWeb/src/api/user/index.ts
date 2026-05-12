import type { AppRouteRecordRaw } from "#src/router/types";
import type { PermissionSnapshot } from "#src/api/auth";

import { fetchPermissionSnapshot } from "#src/api/auth";
import { requestBackendJson } from "#src/api/backend";
import { useAuthStore } from "#src/store/auth";
import { AUTH_REFRESH_PATH } from "#src/utils/request/constants";

import { buildBackendRoutesFromSnapshot, buildUserInfoFromPermissionSnapshot } from "./utils";
import type { UserInfoType } from "./types";

export * from "./types";
export { createCaptchaChallenge, fetchLogin, fetchLoginConfig, fetchLogout, fetchPermissionSnapshot, fetchSessionStatus, verifyCaptcha } from "#src/api/auth";

const PERMISSION_SNAPSHOT_CACHE_TTL = 5_000;

let cachedPermissionSnapshot: PermissionSnapshot | null = null;
let cachedPermissionSnapshotAt = 0;
let cachedPermissionSnapshotKey = "";
let pendingPermissionSnapshotPromise: Promise<PermissionSnapshot> | null = null;

function getPermissionSnapshotCacheKey() {
	const { sid, token } = useAuthStore.getState();
	return `${sid}:${token}`;
}

async function loadPermissionSnapshotData() {
	const cacheKey = getPermissionSnapshotCacheKey();
	const now = Date.now();
	if (cachedPermissionSnapshot && cachedPermissionSnapshotKey === cacheKey && now - cachedPermissionSnapshotAt < PERMISSION_SNAPSHOT_CACHE_TTL) {
		const cachedMenus = cachedPermissionSnapshot.menuTree;
		return {
			userInfo: buildUserInfoFromPermissionSnapshot(cachedPermissionSnapshot),
			routes: buildBackendRoutesFromSnapshot(cachedMenus),
		};
	}

	if (!pendingPermissionSnapshotPromise || cachedPermissionSnapshotKey !== cacheKey) {
		cachedPermissionSnapshotKey = cacheKey;
		pendingPermissionSnapshotPromise = fetchPermissionSnapshot().then((snapshot) => {
			cachedPermissionSnapshot = snapshot;
			cachedPermissionSnapshotAt = Date.now();
			return snapshot;
		}).finally(() => {
			pendingPermissionSnapshotPromise = null;
		});
	}

	const snapshot = await pendingPermissionSnapshotPromise;
	const snapshotMenus = snapshot.menuTree;
	return {
		userInfo: buildUserInfoFromPermissionSnapshot(snapshot),
		routes: buildBackendRoutesFromSnapshot(snapshotMenus),
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
