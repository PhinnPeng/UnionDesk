import type {
	CreateTicketStatusDefinitionBody,
	TicketStatusDefinition,
	UpdateTicketStatusDefinitionBody,
} from "@uniondesk/shared";
import {
	createDomainTicketStatus,
	createPlatformTicketStatus,
	deleteDomainTicketStatus,
	deletePlatformTicketStatus,
	fetchDomainTicketStatuses,
	fetchPlatformTicketStatuses,
	importDomainTicketStatusesFromPlatform,
	toErrorMessage,
	updateDomainTicketStatus,
	updatePlatformTicketStatus,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_CREATE,
	PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_DELETE,
	PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_UPDATE,
	PLATFORM_TICKET_CONFIG_STATUS_CREATE,
	PLATFORM_TICKET_CONFIG_STATUS_DELETE,
	PLATFORM_TICKET_CONFIG_STATUS_READ,
	PLATFORM_TICKET_CONFIG_STATUS_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { DomainAddPlatformTicketStatusesModal } from "./components/domain-add-platform-ticket-statuses-modal";
import { StatusFormModal } from "./components/status-form-modal";
import {
	formatDateTime,
	getStatusCategoryBadgeColor,
	getStatusCategoryLabel,
} from "./components/status-utils";

import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Badge, Button, Card, Empty, Input, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import "./ticket-statuses-panel.less";

export interface TicketStatusesPanelPermissions {
	read: string
	create: string
	update: string
	delete: string
}

export interface TicketStatusesPanelProps {
	scope?: "platform" | "domain";
	domainId?: string;
	/** 嵌入域配置壳时隐藏页头标题 */
	embedded?: boolean;
	/** 覆盖默认权限码（业务域端传入 domain.ticket_status.*） */
	permissions?: TicketStatusesPanelPermissions;
}

export function TicketStatusesPanel({
	scope = "platform",
	domainId,
	embedded = false,
	permissions,
}: TicketStatusesPanelProps) {
	const { message, modal } = App.useApp();

	const readPerm = permissions?.read ?? (scope === "platform"
		? PLATFORM_TICKET_CONFIG_STATUS_READ
		: PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_READ);
	const createPerm = permissions?.create ?? (scope === "platform"
		? PLATFORM_TICKET_CONFIG_STATUS_CREATE
		: PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_CREATE);
	const updatePerm = permissions?.update ?? (scope === "platform"
		? PLATFORM_TICKET_CONFIG_STATUS_UPDATE
		: PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_UPDATE);
	const deletePerm = permissions?.delete ?? (scope === "platform"
		? PLATFORM_TICKET_CONFIG_STATUS_DELETE
		: PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_DELETE);

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<TicketStatusDefinition[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keyword, setKeyword] = useState("");
	const [searchInput, setSearchInput] = useState("");
	const [modalOpen, setModalOpen] = useState(false);
	const [editing, setEditing] = useState<TicketStatusDefinition | null>(null);
	const [addFromPlatformOpen, setAddFromPlatformOpen] = useState(false);
	const [importing, setImporting] = useState(false);
	const [excludeCatalog, setExcludeCatalog] = useState<TicketStatusDefinition[]>([]);

	const loadStatuses = useCallback(async (nextPage = page, nextPageSize = pageSize, nextKeyword = keyword) => {
		if (scope === "domain" && !domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const params = {
				keyword: nextKeyword.trim() || undefined,
				...(total > 100 || nextPage > 1 ? { page: nextPage, page_size: nextPageSize } : {}),
			};
			const result = scope === "domain"
				? await fetchDomainTicketStatuses(domainId!, params)
				: await fetchPlatformTicketStatuses(params);
			setRows(result.items);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
			setKeyword(nextKeyword);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, keyword, message, page, pageSize, scope, total]);

	useEffect(() => {
		void loadStatuses(1, 20, "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- 初始化/切域
	}, [scope, domainId]);

	const handleSearch = () => {
		void loadStatuses(1, pageSize, searchInput);
	};

	const excludePlatformStatusIds = useMemo(() => {
		const ids = new Set<string>();
		for (const status of excludeCatalog) {
			if (status.source_status_id) {
				ids.add(status.source_status_id);
			}
		}
		return ids;
	}, [excludeCatalog]);

	const excludeStatusNames = useMemo(() => {
		const names = new Set<string>();
		for (const status of excludeCatalog) {
			names.add(status.name);
		}
		return names;
	}, [excludeCatalog]);

	const handleOpenAddFromPlatform = async () => {
		if (!domainId) {
			return;
		}
		try {
			const result = await fetchDomainTicketStatuses(domainId);
			setExcludeCatalog(result.items);
			setAddFromPlatformOpen(true);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const handleImportFromPlatform = async (platformStatusIds: string[]) => {
		if (scope !== "domain" || !domainId) {
			return;
		}
		setImporting(true);
		try {
			const created = await importDomainTicketStatusesFromPlatform(domainId, platformStatusIds);
			message.success(created.length > 1 ? `已添加 ${created.length} 个状态` : "状态已添加");
			setAddFromPlatformOpen(false);
			await loadStatuses();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setImporting(false);
		}
	};

	const handleSubmit = async (body: CreateTicketStatusDefinitionBody | UpdateTicketStatusDefinitionBody) => {
		setSubmitting(true);
		try {
			if (editing) {
				if (scope === "domain") {
					await updateDomainTicketStatus(domainId!, editing.id, body);
				}
				else {
					await updatePlatformTicketStatus(editing.id, body);
				}
				message.success("状态已更新");
			}
			else if (scope === "domain") {
				await createDomainTicketStatus(domainId!, body as CreateTicketStatusDefinitionBody);
				message.success("状态已创建");
			}
			else {
				await createPlatformTicketStatus(body as CreateTicketStatusDefinitionBody);
				message.success("状态已创建");
			}
			setModalOpen(false);
			setEditing(null);
			await loadStatuses();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleDelete = useCallback((record: TicketStatusDefinition) => {
		modal.confirm({
			title: "确认删除该状态？",
			content: `状态「${record.name}」删除后不可恢复。`,
			okText: "删除",
			okButtonProps: { danger: true },
			cancelText: "取消",
			onOk: async () => {
				try {
					if (scope === "domain") {
						await deleteDomainTicketStatus(domainId!, record.id);
					}
					else {
						await deletePlatformTicketStatus(record.id);
					}
					message.success("状态已删除");
					await loadStatuses();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	}, [domainId, loadStatuses, message, modal, scope]);

	const columns = useMemo<TableColumnsType<TicketStatusDefinition>>(() => [
		{
			title: "状态名称",
			dataIndex: "name",
			key: "name",
			render: (_value, record) => (
				<Space size={8}>
					<span>{record.name}</span>
					{record.is_system ? <Tag color="default">系统</Tag> : null}
				</Space>
			),
		},
		{
			title: "状态类型",
			dataIndex: "category",
			key: "category",
			width: 120,
			render: value => (
				<Badge
					color={getStatusCategoryBadgeColor(value)}
					text={getStatusCategoryLabel(value)}
				/>
			),
		},
		{
			title: "描述",
			dataIndex: "description",
			key: "description",
			ellipsis: true,
			render: value => value || "—",
		},
		{
			title: "创建时间",
			dataIndex: "created_at",
			key: "created_at",
			width: 180,
			render: value => formatDateTime(value),
		},
		{
			title: "更新时间",
			dataIndex: "updated_at",
			key: "updated_at",
			width: 180,
			render: value => formatDateTime(value),
		},
		{
			title: "操作",
			key: "actions",
			width: 140,
			render: (_value, record) => (
				<Space size={8}>
					<AuthGuarded auth={updatePerm} fallback={null}>
						<Button
							type="link"
							size="small"
							onClick={() => {
								setEditing(record);
								setModalOpen(true);
							}}
						>
							编辑
						</Button>
					</AuthGuarded>
					{!record.is_system
						? (
							<AuthGuarded auth={deletePerm} fallback={null}>
								<Button type="link" size="small" danger onClick={() => handleDelete(record)}>
									删除
								</Button>
							</AuthGuarded>
						)
						: null}
				</Space>
			),
		},
	], [deletePerm, handleDelete, updatePerm]);

	return (
		<AuthGuarded auth={readPerm} fallback={<Empty description="无权限查看事项状态" className="py-16" />}>
			<div className="ticket-statuses-panel">
				{embedded
					? null
					: (
						<div className="ticket-statuses-panel__header">
							<div>
								<Typography.Title level={4} className="ticket-statuses-panel__title">事项状态</Typography.Title>
								<Typography.Paragraph type="secondary" className="ticket-statuses-panel__subtitle">
									定义可复用的处理状态，供各事项类型工作流选用（本阶段仅维护字典）
								</Typography.Paragraph>
							</div>
						</div>
					)}

				<Card bordered={false}>
					<div className="ticket-statuses-panel__toolbar">
						<Input
							allowClear
							prefix={<SearchOutlined />}
							placeholder="搜索名称或描述"
							value={searchInput}
							onChange={event => setSearchInput(event.target.value)}
							onPressEnter={handleSearch}
							className="ticket-statuses-panel__search"
						/>
						<Space>
							<Button icon={<ReloadOutlined />} onClick={() => void loadStatuses()}>刷新</Button>
							<AuthGuarded auth={createPerm} fallback={null}>
								{scope === "domain"
									? (
										<Space>
											<Button icon={<PlusOutlined />} onClick={() => void handleOpenAddFromPlatform()}>
												添加状态
											</Button>
											<Button
												type="primary"
												icon={<PlusOutlined />}
												onClick={() => {
													setEditing(null);
													setModalOpen(true);
												}}
											>
												创建状态
											</Button>
										</Space>
									)
									: (
										<Button
											type="primary"
											icon={<PlusOutlined />}
											onClick={() => {
												setEditing(null);
												setModalOpen(true);
											}}
										>
											创建状态
										</Button>
									)}
							</AuthGuarded>
						</Space>
					</div>

					<Table
						rowKey="id"
						loading={loading}
						columns={columns}
						dataSource={rows}
						pagination={{
							current: page,
							pageSize,
							total,
							showSizeChanger: true,
							onChange: (nextPage, nextPageSize) => void loadStatuses(nextPage, nextPageSize, keyword),
						}}
					/>
				</Card>

				<StatusFormModal
					open={modalOpen}
					editing={editing}
					submitting={submitting}
					onCancel={() => {
						setModalOpen(false);
						setEditing(null);
					}}
					onSubmit={handleSubmit}
				/>

				{scope === "domain"
					? (
						<DomainAddPlatformTicketStatusesModal
							open={addFromPlatformOpen}
							excludePlatformStatusIds={excludePlatformStatusIds}
							excludeNames={excludeStatusNames}
							submitting={importing}
							onCancel={() => setAddFromPlatformOpen(false)}
							onAdd={handleImportFromPlatform}
						/>
					)
					: null}
			</div>
		</AuthGuarded>
	);
}
