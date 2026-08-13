import { businessHomePath, platformHomePath, platformPath } from "./route-path";

/**
 * 端标识常量（无 store 依赖，避免与 access/tabs 形成循环引用）。
 */
export const appScopes = {
	business: "business",
	platform: "platform",
} as const;

export type AppScope = typeof appScopes[keyof typeof appScopes];

export function isPlatformRoutePath(pathname?: string) {
	return typeof pathname === "string" && (pathname === platformPath || pathname.startsWith(`${platformPath}/`));
}

export function getAppScopeByPath(pathname?: string): AppScope {
	return isPlatformRoutePath(pathname) ? appScopes.platform : appScopes.business;
}

export function getAppHomePath(scope: AppScope) {
	return scope === appScopes.platform ? platformHomePath : businessHomePath;
}
