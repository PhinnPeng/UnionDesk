import type { ReactElement } from "react";

import type { PermissionSnapshotMenu } from "@uniondesk/shared";
import type { AppRouteRecordRaw } from "#src/router/types";
import type { AdminMenuRouteNode, BuildRoutesFromSnapshotOptions } from "./types";
import type { MenuItemType } from "./types";
import { isString } from "#src/utils/is";
import { removeTrailingSlash } from "#src/router/utils/remove-trailing-slash";
import { cloneElement, isValidElement } from "react";

/**
 * 将菜单树中的所有 label 转换为国际化文本
 * @param menus 原始菜单数组
 * @param t Translation 函数
 * @returns 转换后的菜单数组
 */
export function translateMenus(menus: MenuItemType[], t: (key: string) => string): MenuItemType[] {
	return menus.map((menu) => {
		let translatedLabel: React.ReactNode = menu.label;
		if (isValidElement(menu.label)) {
			const translatedChildren = t((menu.label as ReactElement<{ children: string }>).props.children);
			translatedLabel = cloneElement(menu.label, {}, translatedChildren ?? "");
		}
		if (isString(menu.label)) {
			translatedLabel = t(menu.label);
		}
		const translatedMenu = {
			...menu,
			label: translatedLabel,
		};

		if (menu.children && menu.children.length > 0) {
			translatedMenu.children = translateMenus(menu.children, t);
		}

		return translatedMenu;
	});
}

/**
 * 通过路径查找根菜单
 *
 * @param menus 菜单列表
 * @param path 菜单路径，可选
 * @returns 包含查找到的菜单、根菜单和根菜单路径的对象
 */
export function findRootMenuByPath(menus: MenuItemType[], path?: string): {
	findMenu: MenuItemType | null
	rootMenu: MenuItemType | null
	rootMenuPath: string | null
} {
	// 初始化返回值
	let findMenu: MenuItemType | null = null;
	let rootMenu: MenuItemType | null = null;
	let rootMenuPath: string | null = null;

	// 如果没有提供路径，返回默认值
	if (!path) {
		return {
			findMenu: null,
			rootMenu: null,
			rootMenuPath: null,
		};
	}

	// 递归查找函数
	const find = (
		list: MenuItemType[],
		targetPath: string,
		parents: MenuItemType[] = [],
	): boolean => {
		for (const menu of list) {
			// 如果找到目标菜单
			if (menu.key === targetPath) {
				findMenu = menu;
				// 如果没有父级菜单，说明当前菜单就是根菜单
				if (parents.length === 0) {
					rootMenu = menu;
					rootMenuPath = menu.key;
				}
				else {
					// 获取最顶层的父级菜单
					rootMenu = parents[0];
					rootMenuPath = parents[0].key;
				}
				return true;
			}

			// 如果有子菜单，继续递归查找
			if (menu.children && menu.children.length > 0) {
				// 将当前菜单加入父级菜单数组
				const found = find(menu.children, targetPath, [...parents, menu]);
				if (found) {
					return true;
				}
			}
		}
		return false;
	};

	// 开始查找
	find(menus, path);

	return {
		findMenu,
		rootMenu,
		rootMenuPath,
	};
}

interface MenuMatchLike {
	id?: string
	pathname?: string
	handle?: {
		currentActiveMenu?: string
		hideInMenu?: boolean
	}
}

export function getSelectedMenuKeys(matches: MenuMatchLike[], menuParentKeys: Record<string, string[]>) {
	// First, try to find a route that specifies currentActiveMenu (highest priority)
	const currentActiveMatch = matches.findLast(routeItem =>
		routeItem.handle?.currentActiveMenu,
	);

	// If found, return the currentActiveMenu path with its parent keys
	if (currentActiveMatch?.handle?.currentActiveMenu) {
		const activeMenuPath = removeTrailingSlash(currentActiveMatch.handle.currentActiveMenu);
		const parentKeys = menuParentKeys[activeMenuPath] || [];
		return [...parentKeys, activeMenuPath];
	}

	// Fallback: Find the last visible route (not hidden in menu)
	const latestVisibleMatch = matches.findLast(routeItem =>
		routeItem.handle?.hideInMenu !== true,
	);

	// Prefer pathname over id because backend routes now use backend:* ids
	const routePath = removeTrailingSlash(latestVisibleMatch?.pathname ?? latestVisibleMatch?.id ?? "");
	if (routePath.length > 0) {
		const parentKeys = menuParentKeys[routePath] || [];
		return [...parentKeys, routePath];
	}

	// Default return empty array if no matches found
	return [];
}

/**
 * 递归查找第一个子菜单路径下的最深层级的第一个菜单项
 *
 * @param splitSideNavItems 菜单列表
 * @returns 找到的最深层级的第一个菜单项
 */
export function findDeepestFirstItem(splitSideNavItems: MenuItemType[]): MenuItemType | null {
	// 如果列表为空，返回 null
	if (!splitSideNavItems || splitSideNavItems.length === 0) {
		return null;
	}

	// 获取第一个菜单项
	const firstItem = splitSideNavItems[0];

	// 如果当前项有子菜单，继续递归查找
	if (firstItem.children && firstItem.children.length > 0) {
		return findDeepestFirstItem(firstItem.children);
	}

	// 如果没有子菜单了，说明到达最底层，返回当前项
	return firstItem;
}

/**
 * 获取菜单项的父级键
 *
 * @param menuItems 菜单项数组
 * @returns 返回记录每个菜单项键对应的父级键数组的对象
 */
export function getParentKeys(menuItems: MenuItemType[]): Record<string, string[]> {
	const parentKeyMap: Record<string, string[]> = {};

	function traverse(items: MenuItemType[], parentKeys: string[] = []) {
		for (const item of items) {
			// 记录当前 key 的父级 key 数组
			parentKeyMap[item.key] = [...parentKeys];

			// 如果有子节点，递归遍历
			if (Array.isArray(item.children) && item.children.length) {
				traverse(item.children, [...parentKeys, item.key]);
			}
		}
	}

	traverse(menuItems);
	return parentKeyMap;
}

// ==================== catalog-path.ts ====================

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

// ==================== layout-path.ts ====================

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

/** 父节点自身是否对应可访问页面（catalog 目录无独立页） */
export function nodeHasOwnPage(node: AdminMenuRouteNode): boolean {
	if (isAdminMenuCatalog(node.menu)) {
		return false;
	}
	return Boolean(node.menu.component?.trim()) || Boolean(node.path);
}

// ==================== build-routes-from-snapshot.ts ====================

type BackendAppRouteRecordRaw = AppRouteRecordRaw & {
	component?: string
};

export function buildRoutesFromAdminMenuSnapshot(
	menus: readonly PermissionSnapshotMenu[],
	options: BuildRoutesFromSnapshotOptions,
): BackendAppRouteRecordRaw[] {
	const flatMenus = flattenSnapshotMenus(menus);
	if (!flatMenus.length) {
		return [];
	}

	const nodes = flatMenus.map(menu => ({
		menu,
		scope: menu.scope,
		path: resolveNodePath(menu, options.normalizeMenuPath),
		children: [] as AdminMenuRouteNode[],
	}));

	const nodesById = new Map<number, AdminMenuRouteNode>();
	for (const node of nodes) {
		if (typeof node.menu.id === "number") {
			nodesById.set(node.menu.id, node);
		}
	}

	const roots: AdminMenuRouteNode[] = [];
	for (const node of nodes) {
		const parentId = node.menu.parentId;
		if (typeof parentId === "number" && nodesById.has(parentId)) {
			nodesById.get(parentId)!.children.push(node);
		}
		else {
			roots.push(node);
		}
	}

	const disambiguatedRoots = disambiguateDuplicateLeafPaths(roots);
	sortAdminMenuRouteNodes(disambiguatedRoots);
	return disambiguatedRoots.map(node => toAppRouteRecordRaw(node, options));
}

function flattenSnapshotMenus(menus: readonly PermissionSnapshotMenu[]): PermissionSnapshotMenu[] {
	const flatMenus: PermissionSnapshotMenu[] = [];
	const visit = (nodes: readonly PermissionSnapshotMenu[]) => {
		for (const node of nodes) {
			const { children, ...menu } = node;
			flatMenus.push(menu);
			if (children?.length) {
				visit(children);
			}
		}
	};
	visit(menus);
	return flatMenus;
}

function resolveNodePath(
	menu: PermissionSnapshotMenu,
	normalizeMenuPath: (path: string | null | undefined, scope?: PermissionSnapshotMenu["scope"]) => string,
): string {
	if (isAdminMenuCatalog(menu)) {
		return resolveCatalogRoutePath(menu);
	}
	return normalizeMenuPath(menu.path, menu.scope);
}

/** 仅对同级重复 path 的叶子菜单做 key 区分，不合并 catalog */
function disambiguateDuplicateLeafPaths(nodes: AdminMenuRouteNode[]): AdminMenuRouteNode[] {
	return nodes.map((node) => {
		const children = node.children.length
			? disambiguateDuplicateLeafPaths(node.children)
			: [];
		return {
			...node,
			path: resolveDisambiguatedPath(node, nodes),
			children,
		};
	});
}

function resolveDisambiguatedPath(node: AdminMenuRouteNode, siblings: AdminMenuRouteNode[]): string {
	if (node.children.length > 0) {
		return node.path;
	}

	const duplicateLeafCount = siblings.filter(
		sibling => sibling.path === node.path && sibling.children.length === 0,
	).length;
	if (duplicateLeafCount <= 1) {
		return node.path;
	}

	const stableKey = node.menu.id ?? node.menu.code;
	return stableKey != null ? `${node.path}::${stableKey}` : node.path;
}

function sortAdminMenuRouteNodes(nodes: AdminMenuRouteNode[]) {
	nodes.sort((left, right) => {
		const leftOrder = left.menu.orderNo ?? 0;
		const rightOrder = right.menu.orderNo ?? 0;
		if (leftOrder !== rightOrder) {
			return leftOrder - rightOrder;
		}
		return left.path.localeCompare(right.path);
	});
	for (const node of nodes) {
		if (node.children.length) {
			sortAdminMenuRouteNodes(node.children);
		}
	}
}

function toAppRouteRecordRaw(
	node: AdminMenuRouteNode,
	options: BuildRoutesFromSnapshotOptions,
): BackendAppRouteRecordRaw {
	const usePathlessLayout = shouldUsePathlessLayout(node);
	const route: BackendAppRouteRecordRaw = {
		id: getBackendRouteId(node),
		handle: {
			title: node.menu.name,
			icon: node.menu.icon ?? undefined,
			order: node.menu.orderNo ?? 0,
			scope: node.menu.scope ?? undefined,
			hideInMenu: node.menu.hidden ?? false,
			auth: node.menu.permissionCode ?? undefined,
			menuKey: usePathlessLayout ? resolvePathlessMenuKey(node) : undefined,
		},
	};

	if (usePathlessLayout) {
		const layoutChildren: BackendAppRouteRecordRaw[] = [];
		if (nodeHasOwnPage(node)) {
			layoutChildren.push(toLeafAppRouteRecordRaw(node, options));
		}
		layoutChildren.push(...node.children.map(child => toAppRouteRecordRaw(child, options)));
		route.children = layoutChildren;
		return route;
	}

	route.path = node.path;
	if (node.children.length) {
		route.children = node.children.map(child => toAppRouteRecordRaw(child, options));
	}
	if (node.menu.component || !node.children.length) {
		route.component = options.normalizeComponentPath(
			node.menu.component,
			node.path,
			node.scope,
		);
	}

	return route;
}

function toLeafAppRouteRecordRaw(
	node: AdminMenuRouteNode,
	options: BuildRoutesFromSnapshotOptions,
): BackendAppRouteRecordRaw {
	return {
		id: getBackendRouteId(node),
		path: node.path,
		handle: {
			title: node.menu.name,
			icon: node.menu.icon ?? undefined,
			order: node.menu.orderNo ?? 0,
			scope: node.menu.scope ?? undefined,
			hideInMenu: node.menu.hidden ?? false,
			auth: node.menu.permissionCode ?? undefined,
		},
		component: options.normalizeComponentPath(
			node.menu.component,
			node.path,
			node.scope,
		),
	};
}

function getBackendRouteId(node: AdminMenuRouteNode) {
	const stableKey = typeof node.menu.id === "number"
		? String(node.menu.id)
		: node.menu.code || node.path;
	return `backend:${node.scope ?? "unknown"}:${stableKey}`;
}
