import type { AppRouteRecordRaw } from "#src/router/types";

import { bootstrapAccessForPath, ensureScopeAccess } from "#src/api/user";
import { useCurrentRoute } from "#src/hooks/use-current-route";
import { hideLoading } from "#src/plugins/hide-loading";
import { setupLoading, showLoading } from "#src/plugins/loading";
import { exception403Path, exception404Path, exception500Path, loginPath } from "#src/router/extra-info";
import { getAppScopeByPath, resolveBackHomePath, resolveHomePathFromMenus } from "#src/router/extra-info/app-scope";
import { externalRoutes, whiteRouteNames } from "#src/router/routes";
import { coreRoutes } from "#src/router/routes/core";
import { ascending } from "#src/router/utils/ascending";
import { goLogin } from "#src/utils/request/go-login";
import { useAccessStore } from "#src/store/access";
import { useAuthStore } from "#src/store/auth";
import { useUserStore } from "#src/store/user";

import { isUnauthorizedHttpError } from "#src/utils/http-request-error";
import { buildLoginRedirectSearch, getSafeRedirect } from "#src/utils/safe-redirect";

import { useEffect, useLayoutEffect, useRef } from "react";
import { matchRoutes, Navigate, useLocation, useNavigate } from "react-router";

const noLoginWhiteList = Array.from(whiteRouteNames).filter(item => item !== loginPath);
const EMPTY_MENUS: AppRouteRecordRaw[] = [];

interface AuthGuardProps {
	children?: React.ReactNode
}

/**
 * 动态路由 `_internalSetRoutes` 后，停留在 `/` 再渲染 `<Navigate to="/home" />`
 * 在 React Router 7 下可能不推进 history（空白屏）。必须用 imperative navigate 落到真实首页。
 */
function resolvePostAccessPath(pathname: string, search: string): string {
	if (pathname === "/") {
		return resolveBackHomePath();
	}
	return `${pathname}${search}`;
}

export function AuthGuard({ children }: AuthGuardProps) {
	const navigate = useNavigate();
	const accessLoadInFlightRef = useRef(false);
	const currentRoute = useCurrentRoute();
	const { pathname, search } = useLocation();
	const isLogin = useAuthStore(state => Boolean(state.token));
	const authUserMenus = useAuthStore(state => state.user?.menus ?? EMPTY_MENUS);
	const isAuthorized = useUserStore(state => Boolean(state.id));
	const userRoles = useUserStore(state => state.roles);
	const userActions = useUserStore(state => state.actions);
	const platformAccess = useUserStore(state => state.platformAccess);
	const businessDomainAccess = useUserStore(state => state.businessDomainAccess);
	const loginRole = useAuthStore(state => state.role);
	const setAccessStore = useAccessStore(state => state.setAccessStore);
	const isAccessChecked = useAccessStore(state => state.isAccessChecked);
	const activeScope = useAccessStore(state => state.activeScope);
	const routeList = useAccessStore(state => state.routeList);

	const isPathInNoLoginWhiteList = noLoginWhiteList.includes(pathname);
	const pathScope = getAppScopeByPath(pathname);
	const isScopeReady = isAccessChecked && activeScope === pathScope;
	const awaitingRootHomeRedirect = isLogin && pathname === "/" && isScopeReady && isAuthorized;

	useEffect(() => {
		if (!isLogin || isAuthorized || !isAccessChecked) {
			return;
		}
		goLogin();
	}, [isAccessChecked, isAuthorized, isLogin]);

	/** 等待 access / 跨端对齐 / 根路径跳首页时保证全屏层在 */
	useEffect(() => {
		if (isPathInNoLoginWhiteList || !isLogin || pathname === loginPath) {
			return;
		}
		if (!isScopeReady || pathname === "/") {
			showLoading();
		}
	}, [isLogin, isPathInNoLoginWhiteList, isScopeReady, pathname]);

	/** 就绪且已离开 `/` 再 hide；避免根路径 Navigate 失效时摘掉遮罩露出空白 */
	useEffect(() => {
		if (isPathInNoLoginWhiteList || !isLogin || pathname === loginPath) {
			hideLoading();
			return;
		}
		if (isScopeReady && pathname !== "/") {
			hideLoading();
		}
	}, [isLogin, isPathInNoLoginWhiteList, isScopeReady, pathname]);

	/** 首次加载：按 path 所属端拉取并激活对应 bucket */
	useEffect(() => {
		async function loadAccessData() {
			if (accessLoadInFlightRef.current) {
				return;
			}
			accessLoadInFlightRef.current = true;
			setupLoading();

			try {
				await bootstrapAccessForPath(pathname);
				const nextPath = resolvePostAccessPath(pathname, search);
				navigate(nextPath, {
					replace: true,
					flushSync: true,
				});
				if (nextPath !== "/") {
					hideLoading();
				}
			}
			catch (error) {
				hideLoading();
				if (isUnauthorizedHttpError(error)) {
					goLogin();
					return;
				}
				const fallbackRoutes = ascending([...coreRoutes, ...externalRoutes]);
				setAccessStore([], fallbackRoutes, { scope: pathScope, actions: [] });
				navigate(exception500Path, { replace: true });
			}
			finally {
				accessLoadInFlightRef.current = false;
			}
		}

		if (!whiteRouteNames.includes(pathname) && isLogin && !isAccessChecked) {
			void loadAccessData();
		}
	}, [
		pathname,
		search,
		isLogin,
		isAccessChecked,
		navigate,
		setAccessStore,
		pathScope,
	]);

	/** 已登录且 bucket 已就绪：URL 跨端时切换 / 懒加载对端 bucket（避免业务端软进平台 404） */
	useEffect(() => {
		async function syncScopeWithPath() {
			if (accessLoadInFlightRef.current) {
				return;
			}
			if (!isLogin || !isAccessChecked || whiteRouteNames.includes(pathname) || pathname === loginPath) {
				return;
			}
			if (activeScope === pathScope) {
				return;
			}
			accessLoadInFlightRef.current = true;
			setupLoading();
			try {
				await ensureScopeAccess(pathScope);
				const nextPath = resolvePostAccessPath(pathname, search);
				navigate(nextPath, {
					replace: true,
					flushSync: true,
				});
				if (nextPath !== "/") {
					hideLoading();
				}
			}
			catch (error) {
				hideLoading();
				if (isUnauthorizedHttpError(error)) {
					goLogin();
					return;
				}
				navigate(exception500Path, { replace: true });
			}
			finally {
				accessLoadInFlightRef.current = false;
			}
		}

		void syncScopeWithPath();
	}, [
		activeScope,
		pathScope,
		isLogin,
		isAccessChecked,
		pathname,
		search,
		navigate,
	]);

	/**
	 * 访问 `/` 时用 imperative navigate 跳首页。
	 * 冷启动 `_internalSetRoutes` 后声明式 `<Navigate>` 可能不推进 URL，导致空白屏。
	 */
	useLayoutEffect(() => {
		if (!awaitingRootHomeRedirect) {
			return;
		}
		const homePath = resolveHomePathFromMenus(authUserMenus, platformAccess, {
			roles: userRoles,
			loginRole,
		}, userActions, Boolean(businessDomainAccess));
		if (!homePath || homePath === "/") {
			return;
		}
		navigate(homePath, {
			replace: true,
			flushSync: true,
		});
	}, [
		awaitingRootHomeRedirect,
		authUserMenus,
		platformAccess,
		userRoles,
		loginRole,
		userActions,
		businessDomainAccess,
		navigate,
	]);

	if (isPathInNoLoginWhiteList) {
		return children;
	}

	if (!isLogin) {
		if (pathname !== loginPath) {
			const redirectPath = `${loginPath}${buildLoginRedirectSearch(pathname, search)}`;
			return <Navigate to={redirectPath} replace />;
		}
		return children;
	}

	if (pathname === loginPath) {
		if (isLogin) {
			const redirect = getSafeRedirect(new URLSearchParams(search).get("redirect"));
			return <Navigate to={redirect ?? resolveBackHomePath()} replace />;
		}
		return children;
	}

	if (!isScopeReady) {
		return null;
	}

	if (!isAuthorized) {
		return null;
	}

	/** 根路径由 useLayoutEffect 跳转，避免声明式 Navigate 在动态路由重置后失效 */
	if (pathname === "/") {
		return null;
	}

	/**
	 * 动态路由刚写入时，history 匹配可能仍停在 `*`。
	 * 若 routeList 已能匹配真实页面，先挡住 404 闪屏，等 remount 的强制 navigate 完成。
	 */
	const isCatchAllMatch = currentRoute?.id === "404";
	const expectedMatches = matchRoutes(routeList, pathname) ?? [];
	const expectedLeaf = expectedMatches[expectedMatches.length - 1]?.route;
	if (isCatchAllMatch && expectedLeaf && expectedLeaf.path !== "*") {
		return null;
	}

	const routeRoles = currentRoute?.handle?.roles;
	const ignoreAccess = currentRoute?.handle?.ignoreAccess;

	if (ignoreAccess === true) {
		return children;
	}

	const matches = matchRoutes(routeList, pathname) ?? [];
	const hasChildren = matches[matches.length - 1]?.route?.children?.filter(item => !item.index)?.length;
	if (hasChildren && hasChildren > 0) {
		return <Navigate to={exception404Path} replace />;
	}

	const hasRoutePermission = userRoles.some(role => routeRoles?.includes(role));
	if (routeRoles && routeRoles.length && !hasRoutePermission) {
		return <Navigate to={exception403Path} replace />;
	}

	const routeAuth = currentRoute?.handle?.auth;
	if (routeAuth && !userActions.includes(routeAuth)) {
		return <Navigate to={exception403Path} replace />;
	}

	return children;
}
