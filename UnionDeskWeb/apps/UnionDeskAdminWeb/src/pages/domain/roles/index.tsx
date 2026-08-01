import type { DomainPermissionItem, DomainRole } from "@uniondesk/shared";
import {
	createDomainRole,
	deleteDomainRole,
	fetchDomainPermissionItems,
	fetchDomainRolePermissions,
	fetchDomainRoles,
	toErrorMessage,
	updateDomainRole,
	updateDomainRolePermissions,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { TableSearchForm } from "#src/components/table-search-form";
import { useAuth } from "#src/hooks/use-auth";
import {
	DOMAIN_ROLE_CREATE,
	DOMAIN_ROLE_DELETE,
	DOMAIN_ROLE_PERMISSION_READ,
	DOMAIN_ROLE_PERMISSION_UPDATE,
	DOMAIN_ROLE_READ,
	DOMAIN_ROLE_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Checkbox,
	Drawer,
	Empty,
	Form,
	Input,
	Modal,
	Space,
	Spin,
	Table,
	Tag,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

const { Text, Title } = Typography;

interface RolesSearchValues {
	keyword?: string;
}

function resolveBusinessDomainId(
	defaultBusinessDomainId: number,
	accessibleDomains: Array<{ id: number }>,
): string {
	if (defaultBusinessDomainId > 0) {
		return String(defaultBusinessDomainId);
	}
	const first = accessibleDomains[0];
	return first ? String(first.id) : "";
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

function translateRoleError(error: unknown): string {
	const message = toErrorMessage(error);
	if (message.includes("preset role cannot be deleted")) {
		return "内置角色不可删除";
	}
	if (message.includes("preset role cannot be updated")) {
		return "内置角色不可修改";
	}
	if (message.includes("role is still bound to members")) {
		return "该角色仍有成员绑定，无法删除";
	}
	if (message.includes("domain role not found")) {
		return "域角色不存在";
	}
	return message;
}

export default function DomainRolesPage() {
	const { message } = App.useApp();
	const { hasPermission } = useAuth();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);
	const [searchForm] = Form.useForm<RolesSearchValues>();
	const [roleForm] = Form.useForm<{ code: string; name: string }>();

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const canUpdate = hasPermission(DOMAIN_ROLE_UPDATE);
	const canDelete = hasPermission(DOMAIN_ROLE_DELETE);
	const canViewPermissions = hasPermission(DOMAIN_ROLE_PERMISSION_READ);
	const canUpdatePermissions = hasPermission(DOMAIN_ROLE_PERMISSION_UPDATE);

	const [loading, setLoading] = useState(false);
	const [roles, setRoles] = useState<DomainRole[]>([]);
	const [keyword, setKeyword] = useState("");
	const [roleModalOpen, setRoleModalOpen] = useState(false);
	const [roleSubmitting, setRoleSubmitting] = useState(false);
	const [editingRole, setEditingRole] = useState<DomainRole | null>(null);

	const [permissionDrawerOpen, setPermissionDrawerOpen] = useState(false);
	const [permissionLoading, setPermissionLoading] = useState(false);
	const [permissionSaving, setPermissionSaving] = useState(false);
	const [selectedRole, setSelectedRole] = useState<DomainRole | null>(null);
	const [allPermissionItems, setAllPermissionItems] = useState<DomainPermissionItem[]>([]);
	const [checkedPermissionIds, setCheckedPermissionIds] = useState<string[]>([]);

	const loadRoles = useCallback(async () => {
		if (!domainId) {
			setRoles([]);
			return;
		}
		setLoading(true);
		try {
			const list = await fetchDomainRoles(domainId);
			setRoles(list);
		}
		catch (error) {
			message.error(translateRoleError(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadRoles();
	}, [loadRoles]);

	const filteredRoles = useMemo(() => {
		const trimmed = keyword.trim().toLowerCase();
		if (!trimmed) {
			return roles;
		}
		return roles.filter((role) => {
			const name = (role.name ?? "").toLowerCase();
			const code = (role.code ?? "").toLowerCase();
			return name.includes(trimmed) || code.includes(trimmed);
		});
	}, [keyword, roles]);

	const handleOpenCreate = () => {
		setEditingRole(null);
		roleForm.resetFields();
		setRoleModalOpen(true);
	};

	const handleOpenEdit = (role: DomainRole) => {
		setEditingRole(role);
		roleForm.setFieldsValue({ code: role.code, name: role.name });
		setRoleModalOpen(true);
	};

	const handleSubmitRole = async () => {
		if (!domainId) {
			return;
		}
		try {
			const values = await roleForm.validateFields();
			setRoleSubmitting(true);
			if (editingRole) {
				await updateDomainRole(domainId, editingRole.id, {
					code: values.code.trim(),
					name: values.name.trim(),
				});
				message.success("角色已更新");
			}
			else {
				await createDomainRole(domainId, {
					code: values.code.trim(),
					name: values.name.trim(),
				});
				message.success("角色已创建");
			}
			setRoleModalOpen(false);
			setEditingRole(null);
			roleForm.resetFields();
			await loadRoles();
		}
		catch (error) {
			if (error && typeof error === "object" && "errorFields" in error) {
				return;
			}
			message.error(translateRoleError(error));
		}
		finally {
			setRoleSubmitting(false);
		}
	};

	const handleDeleteRole = async (role: DomainRole) => {
		if (!domainId) {
			return;
		}
		try {
			await deleteDomainRole(domainId, role.id);
			message.success("角色已删除");
			await loadRoles();
		}
		catch (error) {
			message.error(translateRoleError(error));
		}
	};

	const handleOpenPermissions = useCallback(async (role: DomainRole) => {
		if (!domainId) {
			return;
		}
		setSelectedRole(role);
		setPermissionDrawerOpen(true);
		setPermissionLoading(true);
		setAllPermissionItems([]);
		setCheckedPermissionIds([]);
		try {
			const [assigned, catalog] = await Promise.all([
				fetchDomainRolePermissions(domainId, role.id),
				canUpdatePermissions && !role.preset
					? fetchDomainPermissionItems(domainId)
					: Promise.resolve([] as DomainPermissionItem[]),
			]);
			const assignedIds = assigned.permission_items.map(item => item.id);
			setCheckedPermissionIds(assignedIds);
			if (canUpdatePermissions && !role.preset) {
				setAllPermissionItems(catalog);
			}
			else {
				setAllPermissionItems(assigned.permission_items);
			}
		}
		catch (error) {
			message.error(translateRoleError(error));
			setPermissionDrawerOpen(false);
			setSelectedRole(null);
		}
		finally {
			setPermissionLoading(false);
		}
	}, [canUpdatePermissions, domainId, message]);

	const handleClosePermissionDrawer = () => {
		setPermissionDrawerOpen(false);
		setSelectedRole(null);
		setAllPermissionItems([]);
		setCheckedPermissionIds([]);
	};

	const handleSavePermissions = async () => {
		if (!domainId || !selectedRole) {
			return;
		}
		setPermissionSaving(true);
		try {
			await updateDomainRolePermissions(domainId, selectedRole.id, checkedPermissionIds);
			message.success("权限已更新");
			handleClosePermissionDrawer();
		}
		catch (error) {
			message.error(translateRoleError(error));
		}
		finally {
			setPermissionSaving(false);
		}
	};

	const permissionGroups = useMemo(
		() => groupPermissionItemsByModule(allPermissionItems),
		[allPermissionItems],
	);

	const editablePermissions = Boolean(
		selectedRole && canUpdatePermissions && !selectedRole.preset,
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
				width: 120,
				render: (_, row) => (
					<Tag color={row.preset ? "blue" : "default"}>
						{row.preset ? "预设角色" : "自定义角色"}
					</Tag>
				),
			},
			{
				title: "操作",
				key: "actions",
				width: 260,
				render: (_, row) => (
					<Space size={0} wrap>
						{canViewPermissions
							? (
								<Button type="link" size="small" onClick={() => void handleOpenPermissions(row)}>
									{canUpdatePermissions && !row.preset ? "配置权限" : "查看权限"}
								</Button>
							)
							: null}
						{canUpdate && !row.preset
							? (
								<Button type="link" size="small" onClick={() => handleOpenEdit(row)}>
									编辑
								</Button>
							)
							: null}
						{canDelete && !row.preset
							? (
								<ConfirmPopover
									title="确认删除该角色？"
									onConfirm={() => handleDeleteRole(row)}
								>
									<Button type="link" size="small" danger>
										删除
									</Button>
								</ConfirmPopover>
							)
							: null}
					</Space>
				),
			},
		];
		return base;
	}, [canDelete, canUpdate, canUpdatePermissions, canViewPermissions, handleOpenPermissions]);

	if (!domainId) {
		return (
			<BasicContent>
				<Empty description="暂无可用业务域" />
			</BasicContent>
		);
	}

	return (
		<AuthGuarded auth={DOMAIN_ROLE_READ} fallback={<BasicContent><Empty description="无权限查看域角色" /></BasicContent>}>
			<BasicContent>
				<div className="flex flex-col gap-4">
					<Card
						bordered={false}
						title={(
							<>
								<SearchOutlined />
								{" "}
								筛选条件
							</>
						)}
					>
						<TableSearchForm<RolesSearchValues>
							form={searchForm}
							onFinish={(values) => {
								setKeyword(values.keyword?.trim() ?? "");
							}}
							onReset={() => {
								setKeyword("");
							}}
						>
							<Form.Item name="keyword" label="关键字">
								<Input allowClear placeholder="角色名称 / 编码" />
							</Form.Item>
						</TableSearchForm>
					</Card>
					<Card
						bordered={false}
						title="域角色列表"
						extra={(
							<Space>
								<Button icon={<ReloadOutlined />} onClick={() => void loadRoles()}>
									刷新
								</Button>
								<AuthGuarded auth={DOMAIN_ROLE_CREATE} fallback={null}>
									<Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate}>
										新建角色
									</Button>
								</AuthGuarded>
							</Space>
						)}
					>
						<Table
							rowKey="id"
							loading={loading}
							columns={columns}
							dataSource={filteredRoles}
							pagination={false}
							locale={{ emptyText: "暂无域角色" }}
						/>
					</Card>
				</div>

				<Modal
					title={editingRole ? "编辑角色" : "新建角色"}
					open={roleModalOpen}
					confirmLoading={roleSubmitting}
					destroyOnHidden
					onCancel={() => {
						setRoleModalOpen(false);
						setEditingRole(null);
						roleForm.resetFields();
					}}
					onOk={() => void handleSubmitRole()}
					okText="保存"
					cancelText="取消"
				>
					<Form form={roleForm} layout="vertical">
						<Form.Item
							name="code"
							label="角色编码"
							rules={[
								{ required: true, message: "请输入角色编码" },
								{ pattern: /^[a-z][a-z0-9_]{1,31}$/, message: "编码需小写字母开头，仅含小写字母/数字/下划线" },
							]}
						>
							<Input allowClear placeholder="例如 ops_admin" disabled={Boolean(editingRole)} />
						</Form.Item>
						<Form.Item
							name="name"
							label="角色名称"
							rules={[{ required: true, message: "请输入角色名称" }]}
						>
							<Input allowClear placeholder="例如 运营管理员" />
						</Form.Item>
					</Form>
				</Modal>

				<Drawer
					title={selectedRole
						? `${editablePermissions ? "配置权限" : "已分配权限"} - ${formatDomainRoleLabel(selectedRole)}`
						: "角色权限"}
					open={permissionDrawerOpen}
					width={560}
					destroyOnHidden
					onClose={handleClosePermissionDrawer}
					extra={editablePermissions
						? (
							<Button type="primary" loading={permissionSaving} onClick={() => void handleSavePermissions()}>
								保存
							</Button>
						)
						: null}
				>
					{permissionLoading
						? (
							<div style={{ display: "flex", justifyContent: "center", padding: "64px 0" }}>
								<Spin />
							</div>
						)
						: allPermissionItems.length === 0
							? <Empty description="暂无权限项" />
							: (
								<div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
									{editablePermissions
										? (
											<Checkbox.Group
												value={checkedPermissionIds}
												onChange={values => setCheckedPermissionIds(values.map(String))}
												style={{ width: "100%" }}
											>
												{[...permissionGroups.entries()].map(([moduleKey, items]) => (
													<div key={moduleKey} style={{ marginBottom: 16 }}>
														<Title level={5} style={{ marginBottom: 8, fontSize: 14 }}>
															{moduleKey}
														</Title>
														<div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
															{items.map(item => (
																<Checkbox key={item.id} value={item.id}>
																	{item.name || item.code}
																	{" "}
																	<Text code>{item.code}</Text>
																</Checkbox>
															))}
														</div>
													</div>
												))}
											</Checkbox.Group>
										)
										: [...permissionGroups.entries()].map(([moduleKey, items]) => (
											<div key={moduleKey}>
												<Title level={5} style={{ marginBottom: 8, fontSize: 14 }}>
													{moduleKey}
												</Title>
												<div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
													{items.map(item => (
														<div key={item.id}>
															{item.name || item.code}
															{" "}
															<Text code>{item.code}</Text>
															{item.type ? <Tag style={{ marginLeft: 8 }}>{item.type}</Tag> : null}
														</div>
													))}
												</div>
											</div>
										))}
								</div>
							)}
				</Drawer>
			</BasicContent>
		</AuthGuarded>
	);
}
