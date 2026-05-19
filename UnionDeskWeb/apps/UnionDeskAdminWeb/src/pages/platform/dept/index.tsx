import type { ProColumns } from "@ant-design/pro-components";
import type { IamUser, PlatformOrganizationView } from "@uniondesk/shared";
import type { Key } from "react";

import { fetchPlatformUsers } from "#src/api/platform/iam";
import { fetchCreatePlatformOrganization, fetchDeletePlatformOrganization, fetchPlatformOrganizations, fetchUpdatePlatformOrganization } from "#src/api/platform/organization";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicButton } from "#src/components/basic-button";
import { BasicContent } from "#src/components/basic-content";
import { BasicTable } from "#src/components/basic-table";
import { getAllExpandedKeys } from "#src/utils/get-all-expanded-keys";

import { ApartmentOutlined, CompressOutlined, DeleteOutlined, DownOutlined, EditOutlined, ExpandOutlined, PlusOutlined, ReloadOutlined, RightOutlined } from "@ant-design/icons";
import { Alert, App, Button, Empty, Modal, Space, Tag, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import { Detail, type OrganizationFormValues } from "./components/detail";
import { SearchPanel } from "./components/search-panel";
import {
	buildLeaderOptionLabel,
	buildOrganizationTree,
	filterOrganizationTree,
	formatOrganizationDateTime,
	type OrganizationSearchValues,
	type OrganizationTreeNode,
} from "./utils";

function getExpandedTreeKeys(nodes: OrganizationTreeNode[]): Key[] {
	return getAllExpandedKeys(nodes, "id") as Key[];
}

export default function PlatformDept() {
	const { message } = App.useApp();
	const [organizations, setOrganizations] = useState<PlatformOrganizationView[]>([]);
	const [users, setUsers] = useState<IamUser[]>([]);
	const [searchValues, setSearchValues] = useState<OrganizationSearchValues>({});
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [loadError, setLoadError] = useState<string | null>(null);
	const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);
	const [modalOpen, setModalOpen] = useState(false);
	const [modalMode, setModalMode] = useState<"create" | "edit">("edit");
	const [editingOrganizationId, setEditingOrganizationId] = useState<number | null>(null);
	const [createParentId, setCreateParentId] = useState<number | null>(null);

	const reloadOrganizations = useCallback(async () => {
		setLoading(true);
		try {
			const list = await fetchPlatformOrganizations();
			setOrganizations(list);
			setLoadError(null);
		}
		catch (error) {
			setLoadError(error instanceof Error ? error.message : "组织数据加载失败");
		}
		finally {
			setLoading(false);
		}
	}, []);

	const reloadUsers = useCallback(async () => {
		try {
			const list = await fetchPlatformUsers();
			setUsers(list);
		}
		catch {
			setUsers([]);
		}
	}, []);

	useEffect(() => {
		void reloadOrganizations();
		void reloadUsers();
	}, [reloadOrganizations, reloadUsers]);

	const organizationTree = useMemo(() => buildOrganizationTree(organizations), [organizations]);
	const filteredTree = useMemo(() => filterOrganizationTree(organizationTree, searchValues), [organizationTree, searchValues]);
	const filteredNodeCount = useMemo(() => getExpandedTreeKeys(filteredTree).length, [filteredTree]);
	const organizationMap = useMemo(() => new Map(organizations.map(item => [item.id, item])), [organizations]);
	const leaderOptions = useMemo(() => users.map(user => ({
		label: buildLeaderOptionLabel(user.username, user.mobile),
		value: user.id,
	})), [users]);
	const selectedOrganization = editingOrganizationId == null ? null : organizationMap.get(editingOrganizationId) ?? null;
	const createParentOrganization = createParentId == null ? null : organizationMap.get(createParentId) ?? null;
	const parentOrganization = modalMode === "create"
		? createParentOrganization
		: (selectedOrganization?.parentId == null ? null : organizationMap.get(selectedOrganization.parentId) ?? null);
	const expandedTreeKeys = useMemo(() => getExpandedTreeKeys(filteredTree), [filteredTree]);

	useEffect(() => {
		setExpandedKeys(expandedTreeKeys);
	}, [expandedTreeKeys]);

	const closeModal = useCallback(() => {
		setModalOpen(false);
		setEditingOrganizationId(null);
		setCreateParentId(null);
	}, []);

	const openCreateModal = useCallback((parentId: number | null) => {
		setModalMode("create");
		setCreateParentId(parentId);
		setEditingOrganizationId(null);
		setModalOpen(true);
	}, []);

	const openEditModal = useCallback((organizationId: number) => {
		setModalMode("edit");
		setEditingOrganizationId(organizationId);
		setCreateParentId(null);
		setModalOpen(true);
	}, []);

	const handleSubmit = useCallback(async (values: OrganizationFormValues) => {
		setSaving(true);
		try {
			if (modalMode === "create") {
				await fetchCreatePlatformOrganization({
					code: values.code,
					name: values.name,
					parentId: values.parentId ?? null,
					leaderUserId: values.leaderUserId ?? null,
					orderNo: values.orderNo,
					status: values.status,
					remark: values.remark,
				});
				message.success("部门新增成功");
			}
			else {
				if (!selectedOrganization) {
					throw new Error("请选择要编辑的部门");
				}
				await fetchUpdatePlatformOrganization(selectedOrganization.id, {
					code: values.code,
					name: values.name,
					parentId: values.parentId ?? null,
					leaderUserId: values.leaderUserId ?? null,
					orderNo: values.orderNo,
					status: values.status,
					remark: values.remark,
				});
				message.success("部门保存成功");
			}
			await reloadOrganizations();
			closeModal();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存部门失败");
		}
		finally {
			setSaving(false);
		}
	}, [closeModal, message, modalMode, reloadOrganizations, selectedOrganization]);

	const handleDeleteOrganization = useCallback((target: PlatformOrganizationView) => {
		Modal.confirm({
			title: "删除部门",
			content: `确认删除部门“${target.name}”吗？`,
			okText: "删除",
			okButtonProps: { danger: true },
			cancelText: "取消",
			onOk: async () => {
				try {
					await fetchDeletePlatformOrganization(target.id);
					message.success("部门删除成功");
					await reloadOrganizations();
					if (editingOrganizationId === target.id) {
						closeModal();
					}
				}
				catch (error) {
					message.error(error instanceof Error ? error.message : "删除部门失败");
				}
			},
		});
	}, [closeModal, editingOrganizationId, message, reloadOrganizations]);

	const columns: ProColumns<OrganizationTreeNode>[] = useMemo(() => {
		return [
			{
				title: "部门名称",
				dataIndex: "name",
				width: 180,
				ellipsis: true,
				align: "left",
				search: false,
				render: (_, record) => <Typography.Text strong>{record.name}</Typography.Text>,
			},
			{
				title: "部门编码",
				dataIndex: "code",
				width: 160,
				ellipsis: true,
				align: "left",
				search: false,
				render: (_, record) => <Tag>{record.code}</Tag>,
			},
			{
				title: "上级部门",
				dataIndex: "parentName",
				width: 180,
				ellipsis: true,
				align: "left",
				search: false,
				render: (_, record) => record.parentName ?? "无上级部门",
			},
			{
				title: "负责人",
				dataIndex: "leaderName",
				width: 180,
				ellipsis: true,
				align: "left",
				search: false,
				render: (_, record) => record.leaderName ?? "-",
			},
			{
				title: "排序",
				dataIndex: "orderNo",
				width: 90,
				search: false,
			},
			{
				title: "创建时间",
				dataIndex: "createdAt",
				width: 160,
				search: false,
				render: (_, record) => formatOrganizationDateTime(record.createdAt),
			},
			{
				title: "状态",
				dataIndex: "status",
				width: 100,
				search: false,
				render: (_, record) => (
					<Tag color={record.status === 1 ? "success" : "default"}>
						{record.status === 1 ? "启用" : "停用"}
					</Tag>
				),
			},
			{
				title: "操作",
				key: "action",
				width: 180,
				fixed: "right",
				search: false,
				render: (_, record) => (
					<div className="flex items-center justify-center gap-2">
						<AuthGuarded auth="platform.organization.create">
							<BasicButton
								type="link"
								size="small"
								icon={<PlusOutlined />}
								onClick={() => openCreateModal(record.id)}
							>
								新增下级
							</BasicButton>
						</AuthGuarded>
						<AuthGuarded auth="platform.organization.update">
							<BasicButton
								type="link"
								size="small"
								icon={<EditOutlined />}
								onClick={() => openEditModal(record.id)}
							>
								编辑
							</BasicButton>
						</AuthGuarded>
						<AuthGuarded auth="platform.organization.delete">
							<BasicButton
								type="link"
								size="small"
								danger
								icon={<DeleteOutlined />}
								onClick={() => handleDeleteOrganization(record)}
							>
								删除
							</BasicButton>
						</AuthGuarded>
					</div>
				),
			},
		];
	}, [handleDeleteOrganization, openCreateModal, openEditModal]);

	const emptyDescription = searchValues.keyword || searchValues.createdRange ? "未找到符合条件的部门" : "暂无部门数据";

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4">
				{loadError ? <Alert type="error" showIcon message={loadError} /> : null}
				<BasicTable<OrganizationTreeNode>
					adaptive
					rowKey="id"
					search={false}
					pagination={false}
					loading={loading}
					dataSource={filteredTree}
					columns={columns}
					locale={{ emptyText: <Empty description={emptyDescription} /> }}
					tableExtraRender={() => (
						<SearchPanel onSearch={setSearchValues} onReset={() => setSearchValues({})} />
					)}
					expandable={{
						rowExpandable: record => Boolean(record.children?.length),
						expandedRowKeys: expandedKeys,
						onExpandedRowsChange: keys => setExpandedKeys(keys as Key[]),
						expandIcon: ({ expanded, onExpand, record }) => {
							if (!record.children?.length) {
								return null;
							}

							const Icon = expanded ? DownOutlined : RightOutlined;

							return (
								<Icon
									onClick={event => onExpand(record, event)}
								/>
							);
						},
					}}
					headerTitle={(
						<Space>
							<ApartmentOutlined />
							<span>组织架构</span>
							<Tag color={searchValues.keyword || searchValues.createdRange ? "processing" : "blue"}>{filteredNodeCount} 个节点</Tag>
						</Space>
					)}
					toolBarRender={() => [
						<AuthGuarded key="add-root" auth="platform.organization.create">
							<Button type="primary" icon={<PlusOutlined />} onClick={() => openCreateModal(null)}>
								新增部门
							</Button>
						</AuthGuarded>,
						<Button key="expand-all" icon={<ExpandOutlined />} onClick={() => setExpandedKeys(expandedTreeKeys)}>
							展开全部
						</Button>,
						<Button key="collapse-all" icon={<CompressOutlined />} onClick={() => setExpandedKeys([])}>
							收起全部
						</Button>,
						<Button key="refresh" icon={<ReloadOutlined />} onClick={() => void reloadOrganizations()}>
							刷新
						</Button>,
					]}
				/>
			</div>
			<Detail
				open={modalOpen}
				mode={modalMode}
				organization={modalMode === "edit" ? selectedOrganization : null}
				parentOrganization={parentOrganization}
				organizations={organizations}
				leaderOptions={leaderOptions}
				saving={saving}
				onSubmit={handleSubmit}
				onClose={closeModal}
				onDelete={async () => {
					if (modalMode !== "edit" || !selectedOrganization) {
						return;
					}
					handleDeleteOrganization(selectedOrganization);
				}}
			/>
		</BasicContent>
	);
}
