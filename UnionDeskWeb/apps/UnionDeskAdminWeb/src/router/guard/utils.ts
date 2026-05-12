import type { AppRouteRecordRaw } from "#src/router/types";

export function removeDuplicateRoutes(routes: AppRouteRecordRaw[]) {
	const routesByPath = new Map<string, AppRouteRecordRaw>();
	const result: AppRouteRecordRaw[] = [];

	for (const route of routes) {
		const routePath = route.path!;
		const existingRoute = routesByPath.get(routePath);
		if (existingRoute) {
			if (import.meta.env.DEV) {
				console.warn(`[auth-guard]: Duplicate route path: ${route.path}`);
			}

			if (route.children?.length) {
				existingRoute.children = removeDuplicateRoutes([
					...(existingRoute.children ?? []),
					...route.children,
				]);
			}
			continue;
		}

		routesByPath.set(routePath, route);
		result.push(route);
	}

	return result;
}
