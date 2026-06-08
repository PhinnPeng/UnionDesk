import type { DomainPermissionItem, DomainRole } from "@uniondesk/shared";
import { fetchPlatformDomainRolePermissions, fetchPlatformDomainRoles, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { useAuth } from "#src/hooks/use-auth";

import { App, Button, Descriptions, Drawer, Empty, Spin, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import { DOMAIN_ROLE_PERMISSION_READ_PERMISSION, DOMAIN_ROLES_READ_PERMISSION } from "./detail-shared";

import styles from "./detail-roles.module.less";

const { Title, Text } = Typography;

export interface DetailRolesProps {
	domainId: string;
}

function formatDomainRoleLabel(role: { code?: string | null; name?: string | null }): string {
	if (role.code === "super_admin") {
		return "所有人";
	}
	return role.name ?? role.code ?? "—";
}

function groupPermissionItemsByModule(items: DomainPermissionItem[]): Map<string, DomainPermissionItem[]> {
	const grouped = new Map<string, DomainPermissionItem[]>();
	for (const item of items) {
		const moduleKey = item.module?.trim() || "其他";
		const list = grouped.get(moduleKey) ?? [];
		list.push(item);
		grouped.set(moduleKey, list);
	}
	return grouped;
}

export function DetailRoles({ domainId }: DetailRolesProps) {
	const { message } = App.useApp();
	const { hasPermission } = useAuth();

	const [loading, setLoading] = useState(false);
	const [roles, setRoles] = useState<DomainRole[]>([]);
	const [permissionDrawerOpen, setPermissionDrawerOpen] = useState(false);
	const [permissionLoading, setPermissionLoading] = useState(false);
	const [selectedRole, setSelectedRole] = useState<DomainRole | null>(null);
	const [permissionItems, setPermissionItems] = useState<DomainPermissionItem[]>([]);

	const loadRoles = useCallback(async () => {
		setLoading(true);
		try {
			const list = await fetchPlatformDomainRoles(domainId);
			setRoles(list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadRoles();
	}, [loadRoles]);

	const handleViewPermissions = useCallback(async (role: DomainRole) => {
		setSelectedRole(role);
		setPermissionDrawerOpen(true);
		setPermissionLoading(true);
		setPermissionItems([]);
		try {
			const data = await fetchPlatformDomainRolePermissions(domainId, role.id);
			setPermissionItems(data.permission_items);
		}
		catch (error) {
			message.error(toErrorMessage(error));
			setPermissionDrawerOpen(false);
			setSelectedRole(null);
		}
		finally {
			setPermissionLoading(false);
		}
	}, [domainId, message]);

	const handleClosePermissionDrawer = () => {
		setPermissionDrawerOpen(false);
		setSelectedRole(null);
		setPermissionItems([]);
	};

	const canViewPermissions = hasPermission(DOMAIN_ROLE_PERMISSION_READ_PERMISSION);

	const permissionGroups = useMemo(
		() => groupPermissionItemsByModule(permissionItems),
		[permissionItems],
	);

	const columns: TableColumnsType<DomainRole> = useMemo(() => {
		const base: TableColumnsType<DomainRole> = [
			{
				title: "角色名称",
				key: "name",
				render: (_, row) => formatDomainRoleLabel(row),
			},
			{
				title: "角色编码",
				dataIndex: "code",
				render: (_, row) => <Text code>{row.code}</Text>,
			},
			{
				title: "类型",
				key: "preset",
				render: (_, row) => (
					<Tag color={row.preset ? "blue" : "default"}>
						{row.preset ? "预设角色" : "自定义角色"}
					</Tag>
				),
			},
		];
		if (canViewPermissions) {
			base.push({
				title: "操作",
				key: "actions",
				render: (_, row) => (
					<Button type="link" size="small" onClick={() => void handleViewPermissions(row)}>
						查看权限
					</Button>
				),
			});
		}
		return base;
	}, [canViewPermissions, handleViewPermissions]);

	return (
		<AuthGuarded auth={DOMAIN_ROLES_READ_PERMISSION} fallback={<Empty description="无权限查看角色管理" />}>
			<div>
				<Title level={5} className={styles.pageTitle}>
					角色管理
				</Title>
				<Table
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={roles}
					pagination={false}
					locale={{ emptyText: "暂无域角色" }}
				/>
			</div>
			<Drawer
				title={selectedRole ? `已分配权限 - ${formatDomainRoleLabel(selectedRole)}` : "已分配权限"}
				open={permissionDrawerOpen}
				width={520}
				destroyOnClose
				onClose={handleClosePermissionDrawer}
			>
				{permissionLoading ? (
					<div className={styles.drawerLoading}>
						<Spin />
					</div>
				) : permissionItems.length === 0 ? (
					<Empty description="该角色尚未分配权限项" />
				) : (
					<div className={styles.permissionGroups}>
						{[...permissionGroups.entries()].map(([moduleKey, items]) => (
							<div key={moduleKey}>
								<Title level={5} className={styles.permissionModuleTitle}>
									{moduleKey}
								</Title>
								<Descriptions column={1} size="small" bordered>
									{items.map(item => (
										<Descriptions.Item key={item.id} label={item.name || item.code}>
											<Text code>{item.code}</Text>
											{item.type ? (
												<Tag className={styles.permissionTypeTag}>{item.type}</Tag>
											) : null}
										</Descriptions.Item>
									))}
								</Descriptions>
							</div>
						))}
					</div>
				)}
			</Drawer>
		</AuthGuarded>
	);
}
