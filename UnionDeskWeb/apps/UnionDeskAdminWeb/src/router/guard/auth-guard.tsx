import type { AppRouteRecordRaw } from "#src/router/types";

import { fetchUserInfoAndRoutes } from "#src/api/user";
import { useCurrentRoute } from "#src/hooks/use-current-route";
import { hideLoading } from "#src/plugins/hide-loading";
import { setupLoading } from "#src/plugins/loading";
import { exception403Path, exception404Path, exception500Path, loginPath } from "#src/router/extra-info";
import { appScopes, getAppHomePath, resolveBackHomePath } from "#src/router/extra-info/app-scope";
import { accessRoutes, externalRoutes, whiteRouteNames } from "#src/router/routes";
import { coreRoutes } from "#src/router/routes/core";
import { ascending } from "#src/router/utils/ascending";
import { goLogin } from "#src/utils/request/go-login";
import { generateRoutesFromBackend } from "#src/router/utils/generate-routes-from-backend";
import { generateRoutesByFrontend } from "#src/router/utils/generate-routes-from-frontend";
import { useAccessStore } from "#src/store/access";
import { useAuthStore } from "#src/store/auth";
import { usePreferencesStore } from "#src/store/preferences";
import { useUserStore } from "#src/store/user";

import { isUnauthorizedHttpError } from "#src/utils/http-request-error";

import { useEffect, useRef } from "react";
import { matchRoutes, Navigate, useLocation, useNavigate } from "react-router";

import { removeDuplicateRoutes } from "./utils";

const noLoginWhiteList = Array.from(whiteRouteNames).filter(item => item !== loginPath);
const EMPTY_MENUS: AppRouteRecordRaw[] = [];

interface AuthGuardProps {
	children?: React.ReactNode
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
	const userMenus = useUserStore(state => state.menus ?? EMPTY_MENUS);
	const { setAccessStore, isAccessChecked, routeList } = useAccessStore();
	const { enableBackendAccess, enableFrontendAceess } = usePreferencesStore(state => state);

	const isPathInNoLoginWhiteList = noLoginWhiteList.includes(pathname);

	useEffect(() => {
		async function loadAccessData() {
			if (accessLoadInFlightRef.current) {
				return;
			}
			accessLoadInFlightRef.current = true;
			setupLoading();

			const routes: AppRouteRecordRaw[] = [];
			const latestRoles: string[] = isAuthorized ? [...userRoles] : [];
			let backendMenus: AppRouteRecordRaw[] = [];
			let userInfoError: unknown = null;

			try {
				const shouldFetchSnapshot = !isAuthorized || userMenus.length === 0;
				if (shouldFetchSnapshot) {
					const snapshot = await fetchUserInfoAndRoutes();
					latestRoles.splice(0, latestRoles.length, ...snapshot.userInfo.roles);
					backendMenus = snapshot.routes.length ? snapshot.routes : (snapshot.userInfo.menus ?? []);
					useUserStore.getState().setUserInfo(snapshot.userInfo);
				}
				else {
					backendMenus = userMenus;
				}
			}
			catch (error) {
				userInfoError = error;
			}

			if (enableBackendAccess && backendMenus.length) {
				routes.push(...await generateRoutesFromBackend(backendMenus));
			}

			if (enableFrontendAceess) {
				routes.push(...generateRoutesByFrontend(accessRoutes, latestRoles));
			}

			const uniqueRoutes = removeDuplicateRoutes(routes);

			if (userInfoError) {
				hideLoading();
				if (isUnauthorizedHttpError(userInfoError)) {
					goLogin();
					return;
				}
				const fallbackRoutes = ascending([...coreRoutes, ...externalRoutes]);
				setAccessStore([], fallbackRoutes);
				navigate(exception500Path, { replace: true });
				return;
			}

			setAccessStore(backendMenus, uniqueRoutes);
			hideLoading();

			navigate(`${pathname}${search}`, {
				replace: true,
				flushSync: true,
			});
		}

		if (!whiteRouteNames.includes(pathname) && isLogin && !isAccessChecked) {
			void loadAccessData().finally(() => {
				accessLoadInFlightRef.current = false;
			});
		}
	}, [
		pathname,
		search,
		isLogin,
		isAuthorized,
		isAccessChecked,
		enableBackendAccess,
		enableFrontendAceess,
		userRoles,
		navigate,
		setAccessStore,
		userMenus,
	]);

	if (isPathInNoLoginWhiteList) {
		hideLoading();
		return children;
	}

	if (!isLogin) {
		hideLoading();
		if (pathname !== loginPath) {
			const redirectPath = pathname.length > 1 ? `${loginPath}?redirect=${pathname}${search}` : loginPath;
			return <Navigate to={redirectPath} replace />;
		}
		return children;
	}

	if (pathname === loginPath) {
		hideLoading();
		if (isLogin) {
			return <Navigate to={resolveBackHomePath()} replace />;
		}
		return children;
	}

	if (pathname === "/") {
		hideLoading();
		const hasBusinessMenus = authUserMenus.some(menu => menu.handle?.scope !== appScopes.platform);
		const homePath = hasBusinessMenus ? getAppHomePath(appScopes.business) : getAppHomePath(appScopes.platform);
		return <Navigate to={homePath} replace />;
	}

	if (!isAccessChecked) {
		return null;
	}

	if (!isAuthorized) {
		hideLoading();
		goLogin();
		return null;
	}

	hideLoading();

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
