import type { RouteObject } from "react-router";

import { addRouteIdByPath } from "#src/router/utils/add-route-id-by-path";

import authRoutes from "./auth";
import exceptionRoutes from "./exception";
import fallbackRoute from "./fallback";
import personalCenterRoutes from "./personal-center";

/** 核心路由 */
export const coreRoutes: any = [
	...addRouteIdByPath([...authRoutes, ...exceptionRoutes, ...personalCenterRoutes]),
	...fallbackRoute,
] satisfies RouteObject[];
