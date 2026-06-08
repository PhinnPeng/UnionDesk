import type { PermissionSnapshotMenu } from "@uniondesk/shared";

/**
 * 菜单项目类型
 */
export interface MenuItemType {
	/**
	 * 菜单路径,item 的唯一标志
	 */
	key: string
	/**
	 * 菜单项标题
	 */
	label: React.ReactNode
	/**
	 * 子菜单的菜单项
	 */
	children?: MenuItemType[]
	/**
	 * 菜单图标
	 */
	icon?: React.ReactNode
	/**
	 * 是否禁用菜单
	 * @default false
	 */
	disabled?: boolean
}

export interface AdminMenuRouteNode {
	menu: PermissionSnapshotMenu
	scope: PermissionSnapshotMenu["scope"]
	path: string
	children: AdminMenuRouteNode[]
}

export type MenuPathNormalizer = (
	path: string | null | undefined,
	scope?: PermissionSnapshotMenu["scope"],
) => string;

export type MenuComponentNormalizer = (
	component: string | null | undefined,
	path: string,
	scope?: PermissionSnapshotMenu["scope"],
) => string;

export interface BuildRoutesFromSnapshotOptions {
	normalizeMenuPath: MenuPathNormalizer
	normalizeComponentPath: MenuComponentNormalizer
}
