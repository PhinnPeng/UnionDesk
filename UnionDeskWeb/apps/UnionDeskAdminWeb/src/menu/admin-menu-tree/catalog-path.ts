import type { PermissionSnapshotMenu } from "@uniondesk/shared";

/** 无 route_path、无 component 的节点视为目录（catalog） */
export function isAdminMenuCatalog(menu: PermissionSnapshotMenu): boolean {
	const hasRoutePath = Boolean(menu.path?.trim());
	const hasComponent = Boolean(menu.component?.trim());
	return !hasRoutePath && !hasComponent;
}

/** 为目录节点生成稳定且唯一的路由 path，避免多个 catalog 被合并到同一空 path */
export function resolveCatalogRoutePath(menu: PermissionSnapshotMenu): string {
	const scopePrefix = menu.scope === "platform" ? "/platform" : "/business";
	const stableKey = typeof menu.id === "number"
		? String(menu.id)
		: menu.code?.trim() || "unknown";
	return `${scopePrefix}/catalog/${stableKey}`;
}
