import type {
	CreateTicketStatusDefinitionBody,
	TicketStatusDefinition,
	UpdateTicketStatusDefinitionBody,
} from "@uniondesk/shared";
import {
	createPlatformTicketStatus,
	deletePlatformTicketStatus,
	fetchPlatformTicketStatuses,
	toErrorMessage,
	updatePlatformTicketStatus,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_TICKET_CONFIG_STATUS_CREATE,
	PLATFORM_TICKET_CONFIG_STATUS_DELETE,
	PLATFORM_TICKET_CONFIG_STATUS_READ,
	PLATFORM_TICKET_CONFIG_STATUS_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

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

export function TicketStatusesPanel() {
	const { message, modal } = App.useApp();

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

	const loadStatuses = useCallback(async (nextPage = page, nextPageSize = pageSize, nextKeyword = keyword) => {
		setLoading(true);
		try {
			const params = {
				keyword: nextKeyword.trim() || undefined,
				...(total > 100 || nextPage > 1 ? { page: nextPage, page_size: nextPageSize } : {}),
			};
			const result = await fetchPlatformTicketStatuses(params);
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
	}, [keyword, message, page, pageSize, total]);

	useEffect(() => {
		void loadStatuses(1, 20, "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- 初始化加载
	}, []);

	const handleSearch = () => {
		void loadStatuses(1, pageSize, searchInput);
	};

	const handleSubmit = async (body: CreateTicketStatusDefinitionBody | UpdateTicketStatusDefinitionBody) => {
		setSubmitting(true);
		try {
			if (editing) {
				await updatePlatformTicketStatus(editing.id, body);
				message.success("状态已更新");
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
					await deletePlatformTicketStatus(record.id);
					message.success("状态已删除");
					await loadStatuses();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	}, [loadStatuses, message, modal]);

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
					<AuthGuarded auth={PLATFORM_TICKET_CONFIG_STATUS_UPDATE} fallback={null}>
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
					{!record.is_system ? (
						<AuthGuarded auth={PLATFORM_TICKET_CONFIG_STATUS_DELETE} fallback={null}>
							<Button type="link" size="small" danger onClick={() => handleDelete(record)}>
								删除
							</Button>
						</AuthGuarded>
					) : null}
				</Space>
			),
		},
	], [handleDelete]);

	return (
		<AuthGuarded auth={PLATFORM_TICKET_CONFIG_STATUS_READ} fallback={<Empty description="无权限查看事项状态" className="py-16" />}>
			<div className="ticket-statuses-panel">
				<div className="ticket-statuses-panel__header">
					<div>
						<Typography.Title level={4} className="ticket-statuses-panel__title">事项状态</Typography.Title>
						<Typography.Paragraph type="secondary" className="ticket-statuses-panel__subtitle">
							定义可复用的处理状态，供各事项类型工作流选用（本阶段仅维护字典）
						</Typography.Paragraph>
					</div>
				</div>

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
							<AuthGuarded auth={PLATFORM_TICKET_CONFIG_STATUS_CREATE} fallback={null}>
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
			</div>
		</AuthGuarded>
	);
}
