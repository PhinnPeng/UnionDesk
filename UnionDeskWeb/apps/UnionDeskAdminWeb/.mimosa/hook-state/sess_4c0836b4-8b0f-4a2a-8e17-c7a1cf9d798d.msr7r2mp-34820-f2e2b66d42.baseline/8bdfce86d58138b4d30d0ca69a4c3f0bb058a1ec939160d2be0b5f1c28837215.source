import type { RouteObject } from "react-router";

import { addRouteIdByPath } from "#src/router/utils/add-route-id-by-path";

import authRoutes from "./auth";
import exceptionRoutes from "./exception";
import fallbackRoute from "./fallback";
import personalCenterRoutes from "./personal-center";
import platformPagesRoutes from "./platform-pages";

/** 核心路由 */
export const coreRoutes: any = [
	...addRouteIdByPath([...authRoutes, ...exceptionRoutes, ...personalCenterRoutes, ...platformPagesRoutes]),
	...fallbackRoute,
] satisfies RouteObject[];
