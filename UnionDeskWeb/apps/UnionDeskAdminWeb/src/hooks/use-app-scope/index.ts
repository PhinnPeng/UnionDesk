import { useMemo } from "react";
import { useLocation } from "react-router";

import { getAppScopeByPath } from "#src/router/extra-info/app-scope-core";

/**
 * 根据当前路由判断应用域。
 */
export function useAppScope() {
	const { pathname } = useLocation();

	return useMemo(() => getAppScopeByPath(pathname), [pathname]);
}
