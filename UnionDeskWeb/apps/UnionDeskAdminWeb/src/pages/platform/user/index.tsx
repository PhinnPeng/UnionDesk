import type { IamUser, PlatformOrganizationView } from "@uniondesk/shared";
import { toErrorMessage } from "@uniondesk/shared";
import type { TableProps } from "antd";
import type { Key } from "react";

import type { RoleItemType } from "#src/api/system/role";
import { fetchOffboardPlatformUser, fetchPlatformOffboardPoolUsers, fetchPlatformUsers } from "#src/api/platform/iam";
import { fetchPlatformOrganizations } from "#src/api/platform/organization";
import { fetchRoleList } from "#src/api/system/role";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import {
	PLATFORM_USER_ROW_ACTIONS,
	PLATFORM_USER_TOOLBAR_ACTIONS,
	buildDepartmentNameMap,
	buildRoleNameMap,
	filterPlatformUsers,
	toPlatformUserRow,
	type PlatformUserRow,
	type PlatformUserSearchValues,
} from "#src/pages/platform/user/utils";
import { filterAssignablePlatformRoles } from "#src/pages/system/role/utils";

import {
	ApartmentOutlined,
	DeleteOutlined,
	EditOutlined,
	EllipsisOutlined,
	PlusCircleOutlined,
	SwapOutlined,
	UserDeleteOutlined,
} from "@ant-design/icons";
import { App, Button, Card, Col, Dropdown, Modal, Row, Space, Table, Tag, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

import { DeptTreeBlock } from "./components/dept-tree-block";
import { Detail } from "./components/detail";
import { ResetPasswordModal } from "./components/reset-password-modal";
import { SearchPanel } from "./components/search-panel";

const statusText: Record<PlatformUserRow["status"], string> = {
	active: "在职",
	disabled: "停用",
	offboard: "离职",
};

const statusColor: Record<PlatformUserRow["status"], string> = {
	active: "success",
	disabled: "default",
	offboard: "warning",
};

function centeredTitle(title: string) {
	return <div className="w-full text-center">{title}</div>;
}

function replaceUsers(currentUsers: IamUser[], nextUsers: IamUser[]): IamUser[] {
	const nextById = new Map(nextUsers.map(user => [user.id, user]));
	const merged = currentUsers.map(user => nextById.get(user.id) ?? user);
	for (const nextUser of nextUsers) {
		if (!currentUsers.some(user => user.id === nextUser.id)) {
			merged.unshift(nextUser);
		}
	}
	return merged;
}

function collectOrganizationDescendantIds(organizations: PlatformOrganizationView[], organizationId: number | null): Set<number> | null {
	if (organizationId == null) {
		return null;
	}

	const childrenByParent = new Map<number, number[]>();
	for (const organization of organizations) {
		if (organization.parentId == null) {
			continue;
		}
		const children = childrenByParent.get(organization.parentId) ?? [];
		children.push(organization.id);
		childrenByParent.set(organization.parentId, children);
	}

	const descendantIds = new Set<number>();
	const visit = (currentId: number) => {
		if (descendantIds.has(currentId)) {
			return;
		}
		descendantIds.add(currentId);
		for (const childId of childrenByParent.get(currentId) ?? []) {
			visit(childId);
		}
	};

	visit(organizationId);
	return descendantIds;
}

export default function PlatformUser() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { hasPermission } = useAuth();
	const canReadOffboardPool = hasPermission("platform.user.offboard_pool.read");

	const [users, setUsers] = useState<IamUser[]>([]);
	const [offboardUsers, setOffboardUsers] = useState<IamUser[]>([]);
	const [organizations, setOrganizations] = useState<PlatformOrganizationView[]>([]);
	const [roles, setRoles] = useState<RoleItemType[]>([]);
	const [searchValues, setSearchValues] = useState<PlatformUserSearchValues>({});
	const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
	const [selectedOrganizationId, setSelectedOrganizationId] = useState<number | null>(null);
	const [loading, setLoading] = useState(false);
	const [detailOpen, setDetailOpen] = useState(false);
	const [detailMode, setDetailMode] = useState<"create" | "edit">("create");
	const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
	const [resetPasswordUserId, setResetPasswordUserId] = useState<number | null>(null);

	const reloadUsers = useCallback(async () => {
		setLoading(true);
		try {
			const userList = await fetchPlatformUsers();
			setUsers(userList);
		}
		catch (error) {
			setUsers([]);
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [message]);

	const reloadOffboardUsers = useCallback(async () => {
		if (!canReadOffboardPool) {
			setOffboardUsers([]);
			return;
		}

		try {
			const offboardPoolUsers = await fetchPlatformOffboardPoolUsers();
			setOffboardUsers(offboardPoolUsers);
		}
		catch (error) {
			setOffboardUsers([]);
			message.error(toErrorMessage(error));
		}
	}, [canReadOffboardPool, message]);

	const reloadLookups = useCallback(async () => {
		const [organizationResult, roleResult] = await Promise.allSettled([
			fetchPlatformOrganizations(),
			fetchRoleList(),
		]);

		if (organizationResult.status === "fulfilled") {
			setOrganizations(organizationResult.value);
		}
		else {
			message.error(`加载部门失败：${toErrorMessage(organizationResult.reason)}`);
		}

		if (roleResult.status === "fulfilled") {
			setRoles(roleResult.value);
		}
		else {
			message.error(`加载角色失败：${toErrorMessage(roleResult.reason)}`);
		}
	}, [message]);

	useEffect(() => {
		void reloadUsers();
		void reloadOffboardUsers();
		void reloadLookups();
	}, [reloadLookups, reloadOffboardUsers, reloadUsers]);

	useEffect(() => {
		setSelectedRowKeys([]);
	}, [selectedOrganizationId]);

	const departmentNameMap = useMemo(() => buildDepartmentNameMap(organizations), [organizations]);
	const roleNameMap = useMemo(() => buildRoleNameMap(roles), [roles]);
	const platformRoles = useMemo(() => filterAssignablePlatformRoles(roles), [roles]);
	const platformRoleCodeSet = useMemo(() => new Set(platformRoles.map(role => role.code)), [platformRoles]);
	const visibleUsers = useMemo(
		() => (canReadOffboardPool ? replaceUsers(users, offboardUsers) : users),
		[canReadOffboardPool, offboardUsers, users],
	);
	const userByIdMap = useMemo(() => new Map(visibleUsers.map(user => [user.id, user])), [visibleUsers]);
	const selectedOrganizationIds = useMemo(
		() => collectOrganizationDescendantIds(organizations, selectedOrganizationId),
		[organizations, selectedOrganizationId],
	);
	const filteredUsers = useMemo(() => {
		const usersInSelectedOrganization = selectedOrganizationIds == null
			? visibleUsers
			: visibleUsers.filter((user) => {
				const organizationIds = user.organizationIds ?? [];
				return organizationIds.some(organizationId => selectedOrganizationIds.has(organizationId));
			});
		return filterPlatformUsers(usersInSelectedOrganization, searchValues);
	}, [searchValues, selectedOrganizationIds, visibleUsers]);

	const tableRows = useMemo(
		() => filteredUsers.map(user => toPlatformUserRow(user, departmentNameMap, roleNameMap)),
		[departmentNameMap, filteredUsers, roleNameMap],
	);

	const activeCount = filteredUsers.filter(user => user.employmentStatus !== "offboarded" && user.status === 1).length;
	const disabledCount = filteredUsers.filter(user => user.employmentStatus !== "offboarded" && user.status !== 1).length;
	const roleCount = new Set(filteredUsers.flatMap(user => user.roleCodes)).size;

	const syncUsers = useCallback((nextUsers: IamUser[]) => {
		setUsers(currentUsers => replaceUsers(currentUsers, nextUsers));
	}, []);

	const handleOpenCreate = () => {
		setSelectedUserId(null);
		setDetailMode("create");
		setDetailOpen(true);
	};

	const handleOpenEdit = (userId: number) => {
		setSelectedUserId(userId);
		setDetailMode("edit");
		setDetailOpen(true);
	};

	const handleCloseDetail = () => {
		setDetailOpen(false);
		setSelectedUserId(null);
	};

	const handleOpenResetPassword = (userId: number) => {
		setResetPasswordUserId(userId);
	};

	const handleCloseResetPassword = () => {
		setResetPasswordUserId(null);
	};

	const handleImportExport = () => {
		navigate("/platform/import-export");
	};

	const handleOffboardUsers = (targetIds: number[]) => {
		const targets = targetIds
			.map(id => userByIdMap.get(id))
			.filter((user): user is IamUser => Boolean(user))
			.filter(user => user.employmentStatus !== "offboarded");
		if (targets.length === 0) {
			return;
		}

		const firstTarget = targets[0];
		Modal.confirm({
			title: targets.length > 1 ? "批量离职" : "用户离职",
			content: targets.length > 1
				? `确认将选中的 ${targets.length} 个用户标记为离职吗？`
				: `确认将用户 ${firstTarget.username} 标记为离职吗？`,
			okText: "确认",
			cancelText: "取消",
			onOk: async () => {
				try {
					await Promise.all(targets.map(user => fetchOffboardPlatformUser(user.id)));
					await Promise.all([
						reloadUsers(),
						reloadOffboardUsers(),
					]);
					setSelectedRowKeys([]);
					message.success(targets.length > 1 ? "批量离职成功" : "用户离职成功");
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	};

	const selectedUser = selectedUserId == null ? null : userByIdMap.get(selectedUserId) ?? null;
	const resetPasswordUser = resetPasswordUserId == null ? null : userByIdMap.get(resetPasswordUserId) ?? null;
	const selectedUserForDetail = useMemo(() => {
		if (!selectedUser) {
			return null;
		}
		return {
			...selectedUser,
			roleCodes: selectedUser.roleCodes.filter(code => platformRoleCodeSet.has(code)),
		};
	}, [platformRoleCodeSet, selectedUser]);

	const columns: TableProps<PlatformUserRow>["columns"] = [
		{ title: centeredTitle("账号"), dataIndex: "username", width: 160, align: "center" },
		{ title: centeredTitle("手机号"), dataIndex: "mobile", width: 140, align: "center" },
		{ title: centeredTitle("邮箱"), dataIndex: "email", width: 200, align: "center" },
		{
			title: centeredTitle("部门"),
			dataIndex: "departmentLabels",
			width: 220,
			align: "center",
			render: (_, record) => (
				<Space size={4} wrap>
					{record.departmentLabels.map(label => <Tag key={label}>{label}</Tag>)}
				</Space>
			),
		},
		{
			title: centeredTitle("角色"),
			dataIndex: "roleLabels",
			align: "center",
			render: (_, record) => (
				<Space size={4} wrap>
					{record.roleLabels.map(label => <Tag key={label}>{label}</Tag>)}
				</Space>
			),
		},
		{
			title: centeredTitle("状态"),
			dataIndex: "status",
			width: 100,
			align: "center",
			render: (_, record) => <Tag color={statusColor[record.status]}>{statusText[record.status]}</Tag>,
		},
		{ title: centeredTitle("最近登录"), dataIndex: "lastLoginAt", width: 180, align: "center" },
		{
			title: centeredTitle("操作"),
			key: "action",
			width: 240,
			align: "center",
			render: (_, record) => (
				<Space size={8}>
					<AuthGuarded auth={PLATFORM_USER_ROW_ACTIONS[0].auth}>
						<Button
							type="link"
							size="small"
							icon={<EditOutlined />}
							onClick={() => handleOpenEdit(record.id)}
						>
							{PLATFORM_USER_ROW_ACTIONS[0].label}
						</Button>
					</AuthGuarded>
					{record.status === "offboard" ? null : (
						<AuthGuarded auth={PLATFORM_USER_ROW_ACTIONS[1].auth}>
							<Button
								type="link"
								size="small"
								danger
								icon={<UserDeleteOutlined />}
								onClick={() => handleOffboardUsers([record.id])}
							>
								{PLATFORM_USER_ROW_ACTIONS[1].label}
							</Button>
						</AuthGuarded>
					)}
					<AuthGuarded auth={PLATFORM_USER_ROW_ACTIONS[2].auth}>
						<Dropdown
							menu={{
								items: [{ key: PLATFORM_USER_ROW_ACTIONS[2].key, label: PLATFORM_USER_ROW_ACTIONS[2].label }],
								onClick: ({ key }) => {
									if (key === PLATFORM_USER_ROW_ACTIONS[2].key) {
										handleOpenResetPassword(record.id);
									}
								},
							}}
						>
							<Button type="link" size="small" icon={<EllipsisOutlined />}>
								更多
							</Button>
						</Dropdown>
					</AuthGuarded>
				</Space>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4">
				<Row gutter={[16, 16]}>
					<Col xs={24} md={8}>
						<Card>
							<Typography.Text type="secondary">在职用户</Typography.Text>
							<Typography.Title level={3} className="!mb-0 !mt-2">{activeCount}</Typography.Title>
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card>
							<Typography.Text type="secondary">停用账号</Typography.Text>
							<Typography.Title level={3} className="!mb-0 !mt-2">{disabledCount}</Typography.Title>
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card>
							<Typography.Text type="secondary">平台角色</Typography.Text>
							<Typography.Title level={3} className="!mb-0 !mt-2">{roleCount}</Typography.Title>
						</Card>
					</Col>
				</Row>

				<div className="flex min-h-0 flex-1 gap-4 overflow-hidden">
					<div className="w-64 flex-shrink-0">
						<DeptTreeBlock
							organizations={organizations}
							selectedDepartmentId={selectedOrganizationId}
							onSelect={setSelectedOrganizationId}
						/>
					</div>

					<Card
						className="min-h-0 flex-1"
						title={(
							<Space>
								<ApartmentOutlined />
								<span>平台用户管理</span>
							</Space>
						)}
						extra={(
							<Space>
								<AuthGuarded auth={PLATFORM_USER_TOOLBAR_ACTIONS[0].auth}>
									<Button type="primary" icon={<PlusCircleOutlined />} onClick={handleOpenCreate}>
										{PLATFORM_USER_TOOLBAR_ACTIONS[0].label}
									</Button>
								</AuthGuarded>
								<AuthGuarded auth={PLATFORM_USER_TOOLBAR_ACTIONS[1].auth}>
									<Button
										danger
										icon={<DeleteOutlined />}
										disabled={selectedRowKeys.length === 0}
										onClick={() => handleOffboardUsers(selectedRowKeys.map(Number))}
									>
										{PLATFORM_USER_TOOLBAR_ACTIONS[1].label}
									</Button>
								</AuthGuarded>
								<AuthGuarded auth={PLATFORM_USER_TOOLBAR_ACTIONS[2].auth}>
									<Button icon={<SwapOutlined />} onClick={handleImportExport}>
										{PLATFORM_USER_TOOLBAR_ACTIONS[2].label}
									</Button>
								</AuthGuarded>
							</Space>
						)}
					>
						<div className="flex h-full flex-col">
							<SearchPanel onSearch={setSearchValues} onReset={() => setSearchValues({})} />
							<Table<PlatformUserRow>
								rowKey="id"
								columns={columns}
								dataSource={tableRows}
								loading={loading}
								pagination={false}
								rowSelection={{
									selectedRowKeys,
									onChange: keys => setSelectedRowKeys(keys),
									getCheckboxProps: record => ({ disabled: record.status === "offboard" }),
								}}
								scroll={{ x: 1280 }}
							/>
						</div>
					</Card>
				</div>
			</div>

			<Detail
				open={detailOpen}
				mode={detailMode}
				user={selectedUserForDetail}
				roles={platformRoles}
				organizations={organizations}
				onClose={handleCloseDetail}
				onSuccess={user => syncUsers([user])}
			/>

			<ResetPasswordModal
				open={resetPasswordUserId != null}
				user={resetPasswordUser}
				onClose={handleCloseResetPassword}
				onSuccess={user => {
					syncUsers([user]);
				}}
			/>
		</BasicContent>
	);
}
