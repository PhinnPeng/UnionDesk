import type { PermissionSnapshotMenu } from "@uniondesk/shared";

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
