import type { CreateTeamTemplateBody, TeamTemplate } from "@uniondesk/shared";
import {
	createTeamTemplate,
	deleteTeamTemplate,
	fetchTeamTemplates,
	toErrorMessage,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_TICKET_CONFIG_TEMPLATE_CREATE,
	PLATFORM_TICKET_CONFIG_TEMPLATE_DELETE,
	PLATFORM_TICKET_CONFIG_TEMPLATE_READ,
	PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";
import { buildTeamTemplateConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { TeamTemplateFormModal } from "./components/team-template-form-modal";

import { DeleteOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SettingOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Input, Space, Table, Tag, Tooltip, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

import "./team-templates-panel.less";

function formatDateTime(value?: string | null): string {
	if (!value) {
		return "—";
	}
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) {
		return value;
	}
	const pad = (n: number) => String(n).padStart(2, "0");
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function TeamTemplatesPanel() {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<TeamTemplate[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keyword, setKeyword] = useState("");
	const [searchInput, setSearchInput] = useState("");
	const [modalOpen, setModalOpen] = useState(false);

	const loadTemplates = useCallback(async (nextPage = page, nextPageSize = pageSize, nextKeyword = keyword) => {
		setLoading(true);
		try {
			const params = {
				keyword: nextKeyword.trim() || undefined,
				...(total > 100 || nextPage > 1 ? { page: nextPage, page_size: nextPageSize } : {}),
			};
			const result = await fetchTeamTemplates(params);
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
		void loadTemplates(1, 20, "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- 初始化加载
	}, []);

	const handleSearch = () => {
		void loadTemplates(1, pageSize, searchInput);
	};

	const openConfig = useCallback((nextTemplateId: string) => {
		navigate(buildTeamTemplateConfigPath(nextTemplateId, "collaboration"), { replace: true });
	}, [navigate]);

	const handleSubmit = async (body: CreateTeamTemplateBody) => {
		setSubmitting(true);
		try {
			const created = await createTeamTemplate(body);
			message.success("团队模板已创建");
			setModalOpen(false);
			navigate(buildTeamTemplateConfigPath(created.id, "collaboration"), { replace: true });
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleDelete = useCallback((record: TeamTemplate) => {
		modal.confirm({
			title: "确认删除该团队模板？",
			content: `模板「${record.name}」删除后不可恢复。已套用过该模板的业务域不受影响。`,
			okText: "删除",
			okButtonProps: { danger: true },
			cancelText: "取消",
			onOk: async () => {
				try {
					await deleteTeamTemplate(record.id);
					message.success("团队模板已删除");
					await loadTemplates();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	}, [loadTemplates, message, modal]);

	const columns = useMemo<TableColumnsType<TeamTemplate>>(() => [
		{
			title: "名称",
			dataIndex: "name",
			key: "name",
			render: (_value, record) => (
				<Space size={8}>
					<span>{record.name}</span>
					{record.is_system ? <Tag>系统</Tag> : null}
					{record.status === "disabled" ? <Tag color="default">停用</Tag> : null}
				</Space>
			),
		},
		{
			title: "描述",
			dataIndex: "description",
			key: "description",
			render: value => (
				<span className="team-templates-panel__description">{value || "—"}</span>
			),
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
					<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE} fallback={null}>
						<Tooltip title="配置">
							<Button
								type="link"
								size="small"
								icon={<SettingOutlined />}
								onClick={() => openConfig(record.id)}
							/>
						</Tooltip>
					</AuthGuarded>
					{!record.is_system
						? (
							<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_DELETE} fallback={null}>
								<Tooltip title="删除">
									<Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)} />
								</Tooltip>
							</AuthGuarded>
						)
						: null}
				</Space>
			),
		},
	], [handleDelete, openConfig]);

	return (
		<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_READ} fallback={<Empty description="无权限查看团队模板" className="py-16" />}>
			<div className="team-templates-panel">
				<div className="team-templates-panel__header">
					<div>
						<Typography.Title level={4} className="team-templates-panel__title">团队模板</Typography.Title>
						<Typography.Paragraph type="secondary" className="team-templates-panel__subtitle">
							团队模板是提供一次性创建事项类型的打包方案
						</Typography.Paragraph>
					</div>
				</div>

				<Card bordered={false}>
					<div className="team-templates-panel__toolbar">
						<Input
							allowClear
							prefix={<SearchOutlined />}
							placeholder="搜索名称或描述"
							value={searchInput}
							onChange={event => setSearchInput(event.target.value)}
							onPressEnter={handleSearch}
							className="team-templates-panel__search"
						/>
						<Space>
							<Button icon={<ReloadOutlined />} onClick={() => void loadTemplates()}>刷新</Button>
							<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_CREATE} fallback={null}>
								<Button
									type="primary"
									icon={<PlusOutlined />}
									onClick={() => setModalOpen(true)}
								>
									创建模板
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
							onChange: (nextPage, nextPageSize) => void loadTemplates(nextPage, nextPageSize, keyword),
						}}
					/>
				</Card>

				<TeamTemplateFormModal
					open={modalOpen}
					submitting={submitting}
					onCancel={() => setModalOpen(false)}
					onSubmit={handleSubmit}
				/>
			</div>
		</AuthGuarded>
	);
}
