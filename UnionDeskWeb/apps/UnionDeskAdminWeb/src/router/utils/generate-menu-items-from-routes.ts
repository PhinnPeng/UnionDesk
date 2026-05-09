import type { MenuItemType } from "#src/layout/layout-menu/types";
import type { AppRouteRecordRaw } from "#src/router/types";
import type { AppScope } from "#src/router/extra-info/app-scope";

import { appScopes, isPlatformRoutePath } from "#src/router/extra-info/app-scope";
import { menuIcons } from "#src/icons/menu-icons";
import { isString } from "#src/utils/is";

import { createElement } from "react";
import { Link } from "react-router";

/**
 * 根据路由列表生成菜单项数组。
 *
 * @param routeList 路由列表
 * @param scope 菜单所属域，默认生成业务域菜单
 * @returns 菜单项数组
 */
export function generateMenuItemsFromRoutes(routeList: AppRouteRecordRaw[], scope: AppScope = appScopes.business): MenuItemType[] {
	return routeList.reduce<MenuItemType[]>((acc, item) => {
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
			? generateMenuItemsFromRoutes(item.children.filter(route => !route.index), scope)
			: [];

		if (item.handle?.hideInMenu) {
			return visibleChildren.length > 0 ? [...acc, ...visibleChildren] : acc;
		}

		const label = item.handle?.title;
		const externalLink = item?.handle?.externalLink;
		const iconName = isString(item?.handle?.icon) ? item.handle.icon.trim() : undefined;

		const menuItem: MenuItemType = {
			key: item.path!,
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

		if (iconName) {
			menuItem.icon = iconName;
			if (isString(iconName)) {
				if (menuIcons[iconName]) {
					menuItem.icon = createElement(menuIcons[iconName]);
				}
				else {
					console.warn(
						`menu-icon: icon "${iconName}" not found in src/icons/menu-icons.ts file`,
					);
				}
			}
		}

		if (visibleChildren.length > 0) {
			menuItem.children = visibleChildren;
		}

		return [...acc, menuItem];
	}, []);
}
