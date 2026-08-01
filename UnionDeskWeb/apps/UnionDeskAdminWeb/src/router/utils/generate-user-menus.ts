import type { MenuItemType } from "#src/layout/layout-menu/types";
import type { AppRouteRecordRaw } from "#src/router/types";
import type { AppScope } from "#src/router/extra-info/app-scope-core";

import { appScopes, isPlatformRoutePath } from "#src/router/extra-info/app-scope-core";
import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";
import { isString } from "#src/utils/is";

import { createElement } from "react";
import { Link } from "react-router";

/**
 * 根据用户后端返回的路由生成菜单。
 *
 * 与 generateMenuItemsFromRoutes 的区别：
 * - 不合并 baseRoutes
 * - 直接按用户后端返回的路由结构生成
 */
export function generateUserMenus(userRoutes: AppRouteRecordRaw[], scope: AppScope = appScopes.business): MenuItemType[] {
	const result = userRoutes.reduce<MenuItemType[]>((acc, item) => {
		const itemScope = item.handle?.scope === "platform"
			? appScopes.platform
			: item.handle?.scope === "business"
				? appScopes.business
				: isPlatformRoutePath(item.path)
					? appScopes.platform
					: appScopes.business;
		if (itemScope !== scope) {
			return acc;
		}

		const visibleChildren: MenuItemType[] = Array.isArray(item.children) && item.children.length > 0
			? generateUserMenus(item.children.filter(route => !route.index), scope)
			: [];

		if (item.handle?.hideInMenu) {
			return visibleChildren.length > 0 ? [...acc, ...visibleChildren] : acc;
		}

		const label = item.handle?.title;
		const externalLink = item?.handle?.externalLink;
		const rawIcon = item?.handle?.icon;
		const iconName = isString(rawIcon) ? rawIcon.trim() : undefined;

		const routeMenuKey = item.handle?.menuKey ?? item.path;
		if (!routeMenuKey) {
			return visibleChildren.length > 0 ? [...acc, ...visibleChildren] : acc;
		}
		const menuKey = getUniqueMenuKey(acc, routeMenuKey, label, visibleChildren.length > 0);
		const menuItem: MenuItemType = {
			key: menuKey,
			label: externalLink
				? createElement(
					Link,
					{
						// 阻止事件冒泡，避免触发菜单的点击事件
						onClick: (e) => {
							e.stopPropagation();
						},
						to: externalLink,
						target: "_blank",
						rel: "noopener noreferrer",
					},
					label,
				)
				: (
					label
				),
		};

		// 后端菜单多为图标名字符串；静态路由可能已是 React 节点，勿覆盖为占位符
		menuItem.icon = iconName ? resolveMenuIcon(iconName) : (rawIcon ?? resolveMenuIcon(undefined));

		if (visibleChildren.length > 0) {
			menuItem.children = visibleChildren;
		}

		return [...acc, menuItem];
	}, []);
	
	return result;
}

function getUniqueMenuKey(menus: MenuItemType[], baseKey: string, label: React.ReactNode, hasChildren: boolean) {
	if (!menus.some(item => item?.key === baseKey)) {
		return baseKey;
	}

	if (!hasChildren) {
		return baseKey;
	}

	const labelKey = typeof label === "string" ? label : "catalog";
	let nextKey = `${baseKey}::${labelKey}`;
	let index = 1;
	while (menus.some(item => item?.key === nextKey)) {
		nextKey = `${baseKey}::${labelKey}-${index}`;
		index += 1;
	}
	return nextKey;
}
