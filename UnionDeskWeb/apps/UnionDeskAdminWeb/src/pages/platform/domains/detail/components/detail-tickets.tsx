import type { DomainTicketTemplate, DomainTicketType } from "@uniondesk/shared";
import {
	createDomainTicketType,
	deleteDomainTicketTemplate,
	deleteDomainTicketType,
	fetchDomainTicketTemplates,
	fetchDomainTicketTypes,
	toErrorMessage,
	updateDomainTicketType,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { IconPicker } from "#src/components/icon-picker";
import { TableSearchForm } from "#src/components/table-search-form";
import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";
import { appScopes } from "#src/router/extra-info/app-scope";
import { openAppScopeTab } from "#src/utils/tabbar-utils";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "../../platform-domain-permissions";

import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Empty,
	Form,
	Input,
	Modal,
	Select,
	Space,
	Switch,
	Table,
	Tag,
	Tabs,
	Tooltip,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

import { DetailTicketAttributes } from "./detail-ticket-attributes";
import { TicketTemplateModal } from "./ticket-template-modal";
import { isDraftUnpublished } from "./ticket-type-form-defaults";

import styles from "./detail-tickets.module.less";

const { Title, Text } = Typography;

export interface DetailTicketsProps {
	domainId: string;
}

interface TicketTemplateSearchValues {
	keyword?: string;
	type_id?: string;
}

interface TicketTypeActionHandlers {
	onEdit: (ticketType: DomainTicketType) => void;
	onConfig: (ticketType: DomainTicketType) => void;
	onToggleStatus: (ticketType: DomainTicketType) => void;
	onDelete: (ticketType: DomainTicketType) => void;
}

interface TicketTypesPanelProps {
	loading: boolean;
	dataSource: DomainTicketType[];
	onRefresh: () => void;
	onCreate: () => void;
	handlers: TicketTypeActionHandlers;
}

interface TicketTemplatesPanelProps {
	loading: boolean;
	dataSource: DomainTicketTemplate[];
	ticketTypes: DomainTicketType[];
	keyword: string;
	typeId?: string;
	columns: TableColumnsType<DomainTicketTemplate>;
	onSearch: (values: TicketTemplateSearchValues) => void;
	onResetSearch: () => void;
	onRefresh: () => void;
	onCreate: () => void;
}

function TicketTypeIcon({ icon }: { icon?: string | null }) {
	return (
		<div className={styles.typeIcon}>
			{icon?.trim()
				? resolveMenuIcon(icon, { fontSize: 20 })
				: <span className="text-colorTextQuaternary">—</span>}
		</div>
	);
}

function useTicketTypeColumns(handlers: TicketTypeActionHandlers): TableColumnsType<DomainTicketType> {
	return useMemo(() => [
		{
			title: "事项类型名称",
			width: "26%",
			ellipsis: true,
			render: (_, record) => {
				const unpublished = record.form_schema_has_unpublished
					?? isDraftUnpublished(record.form_schema_draft, record.form_schema);
				return (
					<div className={styles.nameCell}>
						<TicketTypeIcon icon={record.icon} />
						<div className={styles.nameMeta}>
							<div className={styles.nameTitleRow}>
								<Text strong ellipsis={{ tooltip: record.name }}>{record.name}</Text>
								{unpublished ? <Tag color="warning">未发布</Tag> : null}
							</div>
						</div>
					</div>
				);
			},
		},
		{
			title: "描述",
			width: "38%",
			ellipsis: true,
			render: (_, record) => {
				const text = record.description?.trim() || "暂无描述";
				return (
					<Text type="secondary" ellipsis={{ tooltip: text }}>
						{text}
					</Text>
				);
			},
		},
		{
			title: "状态",
			width: "12%",
			align: "center",
			render: (_, record) => {
				const active = record.status === "active";
				return (
					<div className={styles.statusCell}>
						<AuthGuarded
							auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE}
							fallback={(
								<Tooltip title={active ? "停用" : "启用"}>
									<Switch size="small" checked={active} disabled />
								</Tooltip>
							)}
						>
							<Tooltip title={active ? "停用" : "启用"}>
								<Switch
									size="small"
									checked={active}
									onChange={() => void handlers.onToggleStatus(record)}
								/>
							</Tooltip>
						</AuthGuarded>
					</div>
				);
			},
		},
		{
			title: "操作",
			width: "24%",
			align: "center",
			render: (_, record) => (
				<div className={styles.actionsCell}>
					<Space size={4}>
						<Tooltip title="配置">
							<Button
								type="link"
								size="small"
								icon={<EditOutlined />}
								onClick={() => handlers.onConfig(record)}
							>
								配置
							</Button>
						</Tooltip>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
							<Tooltip title="编辑">
								<Button
									type="text"
									size="small"
									icon={<EditOutlined />}
									onClick={() => handlers.onEdit(record)}
								/>
							</Tooltip>
						</AuthGuarded>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE} fallback={null}>
							<ConfirmPopover
								title="确认删除该事项类型？"
								description="若已有工单引用此类型将无法删除。"
								onConfirm={() => handlers.onDelete(record)}
							>
								<Tooltip title="删除">
									<Button type="text" size="small" danger icon={<DeleteOutlined />} />
								</Tooltip>
							</ConfirmPopover>
						</AuthGuarded>
					</Space>
				</div>
			),
		},
	], [handlers]);
}

function TicketTypesPanel({
	loading,
	dataSource,
	onRefresh,
	onCreate,
	handlers,
}: TicketTypesPanelProps) {
	const columns = useTicketTypeColumns(handlers);

	return (
		<Card
			bordered={false}
			title={(
				<div className={styles.listTitle}>
					<span className={styles.listTitleBar} />
					<span>事项类型</span>
				</div>
			)}
			extra={(
				<Space>
					<Button icon={<ReloadOutlined />} onClick={onRefresh}>
						刷新
					</Button>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE} fallback={null}>
						<Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
							新建事项类型
						</Button>
					</AuthGuarded>
				</Space>
			)}
		>
			<Table<DomainTicketType>
				rowKey="id"
				loading={loading}
				columns={columns}
				dataSource={dataSource}
				pagination={false}
				tableLayout="fixed"
				className={styles.typesTable}
				rowClassName={record => record.status === "active" ? "" : styles.rowDisabled}
				locale={{
					emptyText: <Empty description="暂无事项类型" />,
				}}
			/>
		</Card>
	);
}

function TicketTemplatesPanel({
	loading,
	dataSource,
	ticketTypes,
	keyword,
	typeId,
	columns,
	onSearch,
	onResetSearch,
	onRefresh,
	onCreate,
}: TicketTemplatesPanelProps) {
	const typeOptions = useMemo(
		() => ticketTypes.map(item => ({
			value: item.id,
			label: `${item.name}（${item.code}）`,
		})),
		[ticketTypes],
	);

	return (
		<div className="flex flex-col gap-4">
			<Card
				bordered={false}
				title={(
					<Space>
						<SearchOutlined />
						<span>筛选条件</span>
					</Space>
				)}
			>
				<TableSearchForm<TicketTemplateSearchValues>
					loading={loading}
					initialValues={{ keyword, type_id: typeId }}
					onFinish={onSearch}
					onReset={onResetSearch}
				>
					<Form.Item name="keyword" label="关键字">
						<Input allowClear placeholder="模板名称" disabled={loading} />
					</Form.Item>
					<Form.Item name="type_id" label="关联类型">
						<Select
							allowClear
							placeholder="全部"
							disabled={loading || ticketTypes.length === 0}
							options={typeOptions}
						/>
					</Form.Item>
				</TableSearchForm>
			</Card>

			<Card
				bordered={false}
				title="事项模板列表"
				extra={(
					<Space>
						<Button icon={<ReloadOutlined />} onClick={onRefresh}>
							刷新
						</Button>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE} fallback={null}>
							<Button
								icon={<PlusOutlined />}
								disabled={ticketTypes.length === 0}
								onClick={onCreate}
							>
								新建模板
							</Button>
						</AuthGuarded>
					</Space>
				)}
			>
				<Table<DomainTicketTemplate>
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={dataSource}
					pagination={false}
					locale={{
						emptyText: (
							<Empty
								description={ticketTypes.length === 0 ? "请先创建事项类型" : "暂无事项模板"}
							/>
						),
					}}
				/>
			</Card>
		</div>
	);
}

export function DetailTickets({ domainId }: DetailTicketsProps) {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	const [activeSubTab, setActiveSubTab] = useState<"types" | "attributes" | "templates">("types");
	const [templatesLoaded, setTemplatesLoaded] = useState(false);
	const [loadingTypes, setLoadingTypes] = useState(false);
	const [loadingTemplates, setLoadingTemplates] = useState(false);
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [templates, setTemplates] = useState<DomainTicketTemplate[]>([]);
	const [templateKeyword, setTemplateKeyword] = useState("");
	const [templateTypeId, setTemplateTypeId] = useState<string | undefined>();
	const [createOpen, setCreateOpen] = useState(false);
	const [createForm] = Form.useForm<{ code: string; name: string; description?: string; icon?: string }>();
	const [creating, setCreating] = useState(false);
	const [editOpen, setEditOpen] = useState(false);
	const [editForm] = Form.useForm<{ name: string; description?: string; icon?: string; status: string }>();
	const [editingType, setEditingType] = useState<DomainTicketType | null>(null);
	const [editing, setEditing] = useState(false);
	const [templateOpen, setTemplateOpen] = useState(false);
	const [editingTemplate, setEditingTemplate] = useState<DomainTicketTemplate | null>(null);

	const loadTicketTypes = useCallback(async () => {
		if (!domainId) {
			setTicketTypes([]);
			return;
		}
		setLoadingTypes(true);
		try {
			const list = await fetchDomainTicketTypes(domainId);
			setTicketTypes(list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoadingTypes(false);
		}
	}, [domainId, message]);

	const loadTemplates = useCallback(async () => {
		if (!domainId) {
			setTemplates([]);
			return;
		}
		setLoadingTemplates(true);
		try {
			const list = await fetchDomainTicketTemplates(domainId);
			setTemplates(list);
			setTemplatesLoaded(true);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoadingTemplates(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadTicketTypes();
	}, [loadTicketTypes]);

	useEffect(() => {
		if (activeSubTab === "templates" && !templatesLoaded) {
			void loadTemplates();
		}
	}, [activeSubTab, loadTemplates, templatesLoaded]);

	const filteredTemplates = useMemo(() => {
		const trimmed = templateKeyword.trim().toLowerCase();
		return templates.filter((item) => {
			if (templateTypeId && item.type_id !== templateTypeId) {
				return false;
			}
			if (trimmed && !item.name.toLowerCase().includes(trimmed)) {
				return false;
			}
			return true;
		});
	}, [templateKeyword, templateTypeId, templates]);

	const openConfigTab = useCallback((ticketType: DomainTicketType) => {
		const path = `/platform/domains/ticket-type-config/${encodeURIComponent(domainId)}/${encodeURIComponent(ticketType.id)}`;
		openAppScopeTab(appScopes.platform, navigate, path, {
			key: path,
			label: "事项类型配置",
			newTabTitle: `事项类型配置 - ${ticketType.name}`,
			closable: true,
			draggable: true,
		});
	}, [domainId, navigate]);

	const handleCreateType = async () => {
		try {
			const values = await createForm.validateFields();
			const code = values.code.trim();
			const name = values.name.trim();
			setCreating(true);
			const created = await createDomainTicketType(domainId, {
				code,
				name,
				description: values.description?.trim() || null,
				icon: values.icon?.trim() || null,
			});
			message.success("事项类型已创建");
			setCreateOpen(false);
			createForm.resetFields();
			await loadTicketTypes();
			modal.confirm({
				title: "是否进入配置？",
				content: "创建成功，可立即配置该类型的属性、工作流与描述模板。",
				okText: "进入配置",
				cancelText: "留在列表",
				onOk: () => openConfigTab(created),
			});
		}
		catch (error) {
			if (error && typeof error === "object" && "errorFields" in error) {
				return;
			}
			message.error(toErrorMessage(error));
		}
		finally {
			setCreating(false);
		}
	};

	const handleOpenEdit = (ticketType: DomainTicketType) => {
		setEditingType(ticketType);
		editForm.setFieldsValue({
			name: ticketType.name,
			description: ticketType.description ?? "",
			icon: ticketType.icon ?? "",
			status: ticketType.status === "disabled" ? "disabled" : "active",
		});
		setEditOpen(true);
	};

	const handleEditType = async () => {
		if (!editingType) {
			return;
		}
		try {
			const values = await editForm.validateFields();
			setEditing(true);
			await updateDomainTicketType(domainId, editingType.id, {
				name: values.name.trim(),
				description: values.description?.trim() || null,
				icon: values.icon?.trim() || null,
				status: values.status,
			});
			message.success("事项类型已更新");
			setEditOpen(false);
			setEditingType(null);
			editForm.resetFields();
			await loadTicketTypes();
		}
		catch (error) {
			if (error && typeof error === "object" && "errorFields" in error) {
				return;
			}
			message.error(toErrorMessage(error));
		}
		finally {
			setEditing(false);
		}
	};

	const handleToggleStatus = async (ticketType: DomainTicketType) => {
		const nextStatus = ticketType.status === "active" ? "disabled" : "active";
		const doToggle = async () => {
			try {
				await updateDomainTicketType(domainId, ticketType.id, { status: nextStatus });
				message.success(nextStatus === "active" ? "已启用" : "已停用");
				await loadTicketTypes();
			}
			catch (error) {
				message.error(toErrorMessage(error));
			}
		};
		if (nextStatus === "disabled") {
			modal.confirm({
				title: "确认停用该事项类型？",
				content: `事项类型「${ticketType.name}」停用后将无法在业务域中使用。`,
				okText: "确认停用",
				cancelText: "取消",
				onOk: doToggle,
			});
		}
		else {
			void doToggle();
		}
	};

	const handleDeleteType = async (ticketType: DomainTicketType) => {
		try {
			await deleteDomainTicketType(domainId, ticketType.id);
			message.success("事项类型已删除");
			await loadTicketTypes();
			if (templatesLoaded) {
				await loadTemplates();
			}
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const handleDeleteTemplate = async (template: DomainTicketTemplate) => {
		try {
			await deleteDomainTicketTemplate(domainId, template.id);
			message.success("模板已删除");
			await loadTemplates();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const typeHandlers: TicketTypeActionHandlers = {
		onEdit: handleOpenEdit,
		onConfig: openConfigTab,
		onToggleStatus: handleToggleStatus,
		onDelete: handleDeleteType,
	};

	const templateColumns: TableColumnsType<DomainTicketTemplate> = [
		{ title: "模板名称", dataIndex: "name" },
		{ title: "关联类型", dataIndex: "type", width: 140 },
		{ title: "排序", dataIndex: "sort_order", width: 80, render: value => value ?? "-" },
		{
			title: "操作",
			width: 160,
			render: (_, record) => (
				<Space size="small">
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<Button
							type="link"
							size="small"
							icon={<EditOutlined />}
							onClick={() => {
								setEditingTemplate(record);
								setTemplateOpen(true);
							}}
						>
							编辑
						</Button>
					</AuthGuarded>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE} fallback={null}>
						<ConfirmPopover
							title="确认删除该模板？"
							onConfirm={() => handleDeleteTemplate(record)}
						>
							<Button type="link" size="small" danger>
								删除
							</Button>
						</ConfirmPopover>
					</AuthGuarded>
				</Space>
			),
		},
	];

	return (
		<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ}>
			<div>
				<Title level={5} className="!mb-4">
					事项管理
				</Title>
				<Tabs
					type="card"
					activeKey={activeSubTab}
					onChange={key => setActiveSubTab(key as "types" | "attributes" | "templates")}
					items={[
						{
							key: "types",
							label: "事项类型",
							children: (
								<TicketTypesPanel
									loading={loadingTypes}
									dataSource={ticketTypes}
									onRefresh={() => void loadTicketTypes()}
									onCreate={() => setCreateOpen(true)}
									handlers={typeHandlers}
								/>
							),
						},
						{
							key: "attributes",
							label: "事项属性",
							children: <DetailTicketAttributes domainId={domainId} />,
						},
						{
							key: "templates",
							label: "事项模板",
							children: (
								<TicketTemplatesPanel
									loading={loadingTemplates}
									dataSource={filteredTemplates}
									ticketTypes={ticketTypes}
									keyword={templateKeyword}
									typeId={templateTypeId}
									columns={templateColumns}
									onSearch={(values) => {
										setTemplateKeyword(values.keyword ?? "");
										setTemplateTypeId(values.type_id || undefined);
									}}
									onResetSearch={() => {
										setTemplateKeyword("");
										setTemplateTypeId(undefined);
									}}
									onRefresh={() => void loadTemplates()}
									onCreate={() => {
										setEditingTemplate(null);
										setTemplateOpen(true);
									}}
								/>
							),
						},
					]}
				/>

				<Modal
					title="新建事项类型"
					open={createOpen}
					confirmLoading={creating}
					okText="创建"
					cancelText="取消"
					destroyOnHidden
					onCancel={() => {
						setCreateOpen(false);
						createForm.resetFields();
					}}
					onOk={() => void handleCreateType()}
				>
					<Form form={createForm} layout="vertical">
						<Form.Item name="code" label="类型编码" rules={[{ required: true, message: "请输入类型编码" }]}>
							<Input placeholder="例如 feedback" maxLength={32} />
						</Form.Item>
						<Form.Item name="name" label="类型名称" rules={[{ required: true, message: "请输入类型名称" }]}>
							<Input placeholder="例如 问题反馈" maxLength={64} />
						</Form.Item>
						<Form.Item name="description" label="描述">
							<Input.TextArea placeholder="可选" maxLength={500} rows={3} showCount />
						</Form.Item>
						<Form.Item name="icon" label="图标">
							<IconPicker />
						</Form.Item>
					</Form>
				</Modal>

				<Modal
					title="基础编辑"
					open={editOpen}
					confirmLoading={editing}
					okText="保存"
					cancelText="取消"
					destroyOnHidden
					onCancel={() => {
						setEditOpen(false);
						setEditingType(null);
						editForm.resetFields();
					}}
					onOk={() => void handleEditType()}
				>
					<Form form={editForm} layout="vertical">
						<Form.Item label="类型编码">
							<Input value={editingType?.code} disabled />
						</Form.Item>
						<Form.Item name="name" label="类型名称" rules={[{ required: true, message: "请输入类型名称" }]}>
							<Input maxLength={64} />
						</Form.Item>
						<Form.Item name="description" label="描述">
							<Input.TextArea maxLength={500} rows={3} showCount />
						</Form.Item>
						<Form.Item name="icon" label="图标">
							<IconPicker />
						</Form.Item>
						<Form.Item name="status" label="启用状态">
							<Select
								options={[
									{ value: "active", label: "启用" },
									{ value: "disabled", label: "停用" },
								]}
							/>
						</Form.Item>
					</Form>
				</Modal>

				<TicketTemplateModal
					open={templateOpen}
					domainId={domainId}
					ticketTypes={ticketTypes}
					template={editingTemplate}
					onClose={() => {
						setTemplateOpen(false);
						setEditingTemplate(null);
					}}
					onSaved={() => void loadTemplates()}
				/>
			</div>
		</AuthGuarded>
	);
}
