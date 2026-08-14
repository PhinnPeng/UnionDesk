import type { MenuItemType } from "#src/layout/layout-menu/types";
import type { AppScope } from "#src/router/extra-info/app-scope-core";
import type { AppRouteRecordRaw } from "#src/router/types";
import type { RouteObject } from "react-router";

import { appScopes } from "#src/router/extra-info/app-scope-core";
import { createRootRouteConfig, router } from "#src/router";
import { baseRoutes } from "#src/router/routes";
import { ascending } from "#src/router/utils/ascending";
import { flattenRoutes } from "#src/router/utils/flatten-routes";
import { generateUserMenus } from "#src/router/utils/generate-user-menus";
import { useAuthStore } from "#src/store/auth";
import { useUserStore } from "#src/store/user";

import { create } from "zustand";

/** 单端权限包：菜单路由原料 + 可 patch 的动态路由 + actions */
export interface ScopeAccessBucket {
	userRoutes: AppRouteRecordRaw[]
	dynamicRoutes: AppRouteRecordRaw[]
	actions: string[]
	/** 仅 business：生成该包时的业务域 ID */
	domainId?: string | null
	loadedAt: number
}

interface AccessState {
	activeScope: AppScope | null
	platformBucket: ScopeAccessBucket | null
	businessBucket: ScopeAccessBucket | null
	/** 当前端投影：后端菜单原料（兼容旧字段名） */
	userRoutes: AppRouteRecordRaw[]
	userMenus: MenuItemType[]
	platformMenus: MenuItemType[]
	routeList: AppRouteRecordRaw[]
	flatRouteList: Record<string, AppRouteRecordRaw>
	isAccessChecked: boolean
}

const emptyBucketProjection = {
	userRoutes: [] as AppRouteRecordRaw[],
	userMenus: [] as MenuItemType[],
	platformMenus: [] as MenuItemType[],
	routeList: [] as AppRouteRecordRaw[],
	flatRouteList: {} as Record<string, AppRouteRecordRaw>,
};

const initialState: AccessState = {
	activeScope: null,
	platformBucket: null,
	businessBucket: null,
	...emptyBucketProjection,
	isAccessChecked: false,
};

function remountDynamicRoutes(dynamicRoutes: AppRouteRecordRaw[]) {
	/**
	 * React Router 7.15+：`_internalSetRoutes` 只把树写入 HMR 暂存槽（setHmrRoutes），
	 * `router.routes`（stable）不变；须等下一次 `completeNavigation` 才会 `commitHmrRoutes`
	 * 并更新 `state.matches`。若只 set 不 navigate，刷新 `/home` 会一直停在 `*` → 404。
	 *
	 * 因此：干净 base + 动态路由一次 `_internalSetRoutes`，再对当前 URL 强制 REPLACE
	 * navigate（带唯一 state）触发 rematch + commit。
	 *
	 * 用 `as RouteObject` 消除 index/non-index 联合展开的类型冲突；
	 * 不要写 `index: undefined`，否则运行时路由匹配会落到 `*`。
	 */
	const nextRoot = createRootRouteConfig();
	nextRoot[0] = {
		...nextRoot[0]!,
		children: [
			...(nextRoot[0]!.children ?? []),
			...(dynamicRoutes as RouteObject[]),
		],
	} as RouteObject;
	router._internalSetRoutes(nextRoot);

	const { pathname, search, hash, state } = router.state.location;
	const prevState = (typeof state === "object" && state !== null && !Array.isArray(state))
		? state as Record<string, unknown>
		: {};
	void router.navigate(
		{ pathname, search, hash },
		{
			replace: true,
			flushSync: true,
			state: {
				...prevState,
				__accessRemount: Date.now(),
			},
		},
	);
}

function buildProjection(
	activeScope: AppScope | null,
	platformBucket: ScopeAccessBucket | null,
	businessBucket: ScopeAccessBucket | null,
): Pick<AccessState, "userRoutes" | "userMenus" | "platformMenus" | "routeList" | "flatRouteList"> {
	const activeBucket = activeScope === appScopes.platform
		? platformBucket
		: activeScope === appScopes.business
			? businessBucket
			: null;
	const dynamicRoutes = activeBucket?.dynamicRoutes ?? [];
	const routeList = ascending([...baseRoutes, ...dynamicRoutes]);
	return {
		userRoutes: activeBucket?.userRoutes ?? [],
		userMenus: businessBucket
			? generateUserMenus(businessBucket.userRoutes, appScopes.business)
			: [],
		platformMenus: platformBucket
			? generateUserMenus(platformBucket.userRoutes, appScopes.platform)
			: [],
		routeList,
		flatRouteList: flattenRoutes(routeList),
	};
}

function syncActiveActionsToUserStores(actions: string[], menus: AppRouteRecordRaw[]) {
	const userState = useUserStore.getState();
	if (userState.id) {
		useUserStore.setState({
			actions,
			menus,
		});
	}
	const authUser = useAuthStore.getState().user;
	if (authUser) {
		useAuthStore.setState({
			user: {
				...authUser,
				actions,
				menus,
			},
		});
	}
}

interface CommitBucketInput {
	userRoutes: AppRouteRecordRaw[]
	dynamicRoutes: AppRouteRecordRaw[]
	actions: string[]
	domainId?: string | null
}

interface AccessAction {
	/**
	 * 写入某一端 bucket，并激活该端（重置动态路由后 patch）。
	 * 兼容旧调用：未传 scope 时按当前 path / 已有 activeScope 推断。
	 */
		setAccessStore: (
			userRoutes: AppRouteRecordRaw[],
			allRoutes: AppRouteRecordRaw[],
			options?: {
				scope?: AppScope
				actions?: string[]
				domainId?: string | null
			},
		) => AccessState
	commitScopeBucket: (scope: AppScope, input: CommitBucketInput) => AccessState
	activateScope: (scope: AppScope) => AccessState | null
	invalidateBusinessBucket: () => void
	getBucket: (scope: AppScope) => ScopeAccessBucket | null
	reset: () => void
}

function resolveCommitScope(explicit?: AppScope): AppScope {
	if (explicit) {
		return explicit;
	}
	const active = useAccessStore.getState().activeScope;
	if (active) {
		return active;
	}
	const pathname = typeof window !== "undefined" ? window.location.pathname : "";
	return pathname.startsWith("/platform") ? appScopes.platform : appScopes.business;
}

export const useAccessStore = create<AccessState & AccessAction>((set, get) => ({
	...initialState,

	getBucket: (scope) => {
		return scope === appScopes.platform ? get().platformBucket : get().businessBucket;
	},

	commitScopeBucket: (scope, input) => {
		const bucket: ScopeAccessBucket = {
			userRoutes: input.userRoutes,
			dynamicRoutes: input.dynamicRoutes,
			actions: input.actions,
			domainId: scope === appScopes.business ? (input.domainId ?? null) : undefined,
			loadedAt: Date.now(),
		};
		const platformBucket = scope === appScopes.platform ? bucket : get().platformBucket;
		const businessBucket = scope === appScopes.business ? bucket : get().businessBucket;

		remountDynamicRoutes(bucket.dynamicRoutes);
		syncActiveActionsToUserStores(bucket.actions, bucket.userRoutes);

		const projection = buildProjection(scope, platformBucket, businessBucket);
		const newState: AccessState = {
			activeScope: scope,
			platformBucket,
			businessBucket,
			...projection,
			isAccessChecked: true,
		};
		set(() => newState);
		return newState;
	},

	activateScope: (scope) => {
		const bucket = scope === appScopes.platform ? get().platformBucket : get().businessBucket;
		if (!bucket) {
			return null;
		}
		remountDynamicRoutes(bucket.dynamicRoutes);
		syncActiveActionsToUserStores(bucket.actions, bucket.userRoutes);
		const projection = buildProjection(scope, get().platformBucket, get().businessBucket);
		const newState: AccessState = {
			...get(),
			activeScope: scope,
			...projection,
			isAccessChecked: true,
		};
		set(() => newState);
		return newState;
	},

	setAccessStore: (userRoutes, allRoutes, options) => {
		const scope = resolveCommitScope(options?.scope);
		return get().commitScopeBucket(scope, {
			userRoutes,
			dynamicRoutes: allRoutes,
			actions: options?.actions ?? useUserStore.getState().actions ?? [],
			domainId: options?.domainId,
		});
	},

	invalidateBusinessBucket: () => {
		const state = get();
		const platformBucket = state.platformBucket;
		if (state.activeScope === appScopes.business) {
			remountDynamicRoutes([]);
			const projection = buildProjection(null, platformBucket, null);
			set(() => ({
				...state,
				businessBucket: null,
				activeScope: null,
				...projection,
				isAccessChecked: Boolean(platformBucket),
			}));
			return;
		}
		set(() => ({
			...state,
			businessBucket: null,
			userMenus: [],
		}));
	},

	reset: () => {
		router._internalSetRoutes(createRootRouteConfig());
		set(initialState);
	},
}));
