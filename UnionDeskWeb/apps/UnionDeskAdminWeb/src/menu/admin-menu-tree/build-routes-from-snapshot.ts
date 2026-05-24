import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import type { AppRouteRecordRaw } from "#src/router/types";

import { isAdminMenuCatalog, resolveCatalogRoutePath } from "./catalog-path";
import {
	nodeHasOwnPage,
	resolvePathlessMenuKey,
	shouldUsePathlessLayout,
} from "./layout-path";
import type { AdminMenuRouteNode, MenuComponentNormalizer, MenuPathNormalizer } from "./types";

type BackendAppRouteRecordRaw = AppRouteRecordRaw & {
	component?: string
};

export interface BuildRoutesFromSnapshotOptions {
	normalizeMenuPath: MenuPathNormalizer
	normalizeComponentPath: MenuComponentNormalizer
}

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
	normalizeMenuPath: MenuPathNormalizer,
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
