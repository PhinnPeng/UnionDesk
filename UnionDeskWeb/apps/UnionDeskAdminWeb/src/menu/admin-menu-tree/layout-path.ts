import { isAdminMenuCatalog, resolveCatalogRoutePath } from "./catalog-path";
import type { AdminMenuRouteNode } from "./types";

/** 子路由 path 是否为父路由 path 的后代（含相等） */
export function isDescendantRoutePath(childPath: string, parentPath: string): boolean {
	if (!parentPath) {
		return false;
	}
	return childPath === parentPath || childPath.startsWith(`${parentPath}/`);
}

/**
 * 是否使用 pathless 布局路由（侧边栏保留层级，子路由保持绝对 path）。
 * 适用于 catalog，以及「有自身 path 但子菜单为同级绝对 path」的 menu 父节点（如权限管理）。
 */
export function shouldUsePathlessLayout(node: AdminMenuRouteNode): boolean {
	if (!node.children.length) {
		return false;
	}
	if (isAdminMenuCatalog(node.menu)) {
		return true;
	}
	return node.children.some(child => !isDescendantRoutePath(child.path, node.path));
}

/** pathless 布局节点在侧边栏使用的 menuKey */
export function resolvePathlessMenuKey(node: AdminMenuRouteNode): string {
	if (isAdminMenuCatalog(node.menu)) {
		return resolveCatalogRoutePath(node.menu);
	}
	return node.path;
}

/** 父节点自身是否对应可访问页面（如 /platform/permission） */
export function nodeHasOwnPage(node: AdminMenuRouteNode): boolean {
	if (isAdminMenuCatalog(node.menu)) {
		return false;
	}
	return Boolean(node.menu.component?.trim()) || Boolean(node.path);
}
