export { buildRoutesFromAdminMenuSnapshot } from "./build-routes-from-snapshot";
export type { BuildRoutesFromSnapshotOptions } from "./build-routes-from-snapshot";
export { isAdminMenuCatalog, resolveCatalogRoutePath } from "./catalog-path";
export {
	isDescendantRoutePath,
	nodeHasOwnPage,
	resolvePathlessMenuKey,
	shouldUsePathlessLayout,
} from "./layout-path";
export type { AdminMenuRouteNode, MenuComponentNormalizer, MenuPathNormalizer } from "./types";
