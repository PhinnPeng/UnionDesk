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
import { buildFormDesignPath } from "#src/pages/common/form-design";
import { appScopes } from "#src/router/extra-info/app-scope";
import { openAppScopeTab } from "#src/utils/tabbar-utils";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "../../platform-domain-permissions";

import { EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Col,
	Empty,
	Form,
	Input,
	Modal,
	Row,
	Select,
	Space,
	Table,
	Tag,
	Tabs,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

import { TicketTemplateModal } from "./ticket-template-modal";
import { countFlowStates, countFormFields, isDraftUnpublished } from "./ticket-type-form-defaults";

const { Title, Paragraph, Text } = Typography;

export interface DetailTicketsProps {
	domainId: string;
}

interface TicketTypeSearchValues {
	keyword?: string;
}

interface TicketTemplateSearchValues {
	keyword?: string;
	type_id?: string;
}

interface TicketTypesPanelProps {
	domainId: string;
	loading: boolean;
	dataSource: DomainTicketType[];
	keyword: string;
	onSearch: (values: TicketTypeSearchValues) => void;
	onResetSearch: () => void;
	onRefresh: () => void;
	onCreate: () => void;
	onEdit: (ticketType: DomainTicketType) => void;
	onFormDesign: (ticketType: DomainTicketType) => void;
	onFlowConfig: (ticketType: DomainTicketType) => void;
	onToggleStatus: (ticketType: DomainTicketType) => void;
	onDelete: (ticketType: DomainTicketType) => void;
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

function TicketTypeCard({
	record,
	onEdit,
	onFormDesign,
	onFlowConfig,
	onToggleStatus,
	onDelete,
}: {
	record: DomainTicketType;
	onEdit: (ticketType: DomainTicketType) => void;
	onFormDesign: (ticketType: DomainTicketType) => void;
	onFlowConfig: (ticketType: DomainTicketType) => void;
	onToggleStatus: (ticketType: DomainTicketType) => void;
	onDelete: (ticketType: DomainTicketType) => void;
}) {
	const schemaForCount = record.form_schema_draft ?? record.form_schema;
	const unpublished = isDraftUnpublished(record.form_schema_draft, record.form_schema);

	return (
		<Card hoverable className="h-full">
			<div className="flex flex-col items-center gap-3 text-center">
				<div className="flex h-12 w-12 items-center justify-center rounded-lg bg-colorFillTertiary text-2xl">
					{record.icon?.trim()
						? resolveMenuIcon(record.icon, { fontSize: 28 })
						: <span className="text-colorTextQuaternary">—</span>}
				</div>
				<div className="w-full">
					<div className="flex flex-wrap items-center justify-center gap-2">
						<Text strong>{record.name}</Text>
						<Tag color={record.status === "active" ? "success" : "default"}>
							{record.status === "active" ? "启用" : "停用"}
						</Tag>
						{unpublished ? <Tag color="warning">未发布</Tag> : null}
					</div>
					<Text type="secondary" className="text-xs">{record.code}</Text>
				</div>
				{record.description?.trim() ? (
					<Paragraph
						type="secondary"
						className="!mb-0 line-clamp-2 w-full text-xs"
						ellipsis={{ rows: 2 }}
					>
						{record.description}
					</Paragraph>
				) : (
					<Text type="secondary" className="text-xs">暂无描述</Text>
				)}
				<div className="flex gap-4 text-xs text-colorTextSecondary">
					<span>字段 {countFormFields(schemaForCount)}</span>
					<span>状态 {countFlowStates(record.status_flow)}</span>
				</div>
				<Space wrap className="justify-center">
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<Button size="small" type="primary" onClick={() => onFormDesign(record)}>
							表单设计
						</Button>
						<Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>
							编辑
						</Button>
						<Button size="small" onClick={() => onFlowConfig(record)}>
							状态流
						</Button>
						<Button size="small" onClick={() => void onToggleStatus(record)}>
							{record.status === "active" ? "停用" : "启用"}
						</Button>
					</AuthGuarded>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE} fallback={null}>
						<ConfirmPopover
							title="确认删除该工单类型？"
							description="若已有工单引用此类型将无法删除。"
							onConfirm={() => onDelete(record)}
						>
							<Button size="small" danger>
								删除
							</Button>
						</ConfirmPopover>
					</AuthGuarded>
				</Space>
			</div>
		</Card>
	);
}

function TicketTypesPanel({
	domainId: _domainId,
	loading,
	dataSource,
	keyword,
	onSearch,
	onResetSearch,
	onRefresh,
	onCreate,
	onEdit,
	onFormDesign,
	onFlowConfig,
	onToggleStatus,
	onDelete,
}: TicketTypesPanelProps) {
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
				<TableSearchForm<TicketTypeSearchValues>
					loading={loading}
					initialValues={{ keyword }}
					onFinish={onSearch}
					onReset={onResetSearch}
				>
					<Form.Item name="keyword" label="关键字">
						<Input allowClear placeholder="编码或名称" disabled={loading} />
					</Form.Item>
				</TableSearchForm>
			</Card>

			<Card
				bordered={false}
				title="工单列表"
				extra={(
					<Space>
						<Button icon={<ReloadOutlined />} onClick={onRefresh}>
							刷新
						</Button>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE} fallback={null}>
							<Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
								新建类型
							</Button>
						</AuthGuarded>
					</Space>
				)}
			>
				{dataSource.length === 0 && !loading ? (
					<Empty description="暂无工单类型" />
				) : (
					<Row gutter={[16, 16]} justify="center">
						{dataSource.map(record => (
							<Col key={record.id} xs={24} sm={12} md={10} lg={8} xl={6}>
								<TicketTypeCard
									record={record}
									onEdit={onEdit}
									onFormDesign={onFormDesign}
									onFlowConfig={onFlowConfig}
									onToggleStatus={onToggleStatus}
									onDelete={onDelete}
								/>
							</Col>
						))}
					</Row>
				)}
			</Card>
		</div>
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
				title="工单模板列表"
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
								description={ticketTypes.length === 0 ? "请先创建工单类型" : "暂无工单模板"}
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
	const [activeSubTab, setActiveSubTab] = useState<"types" | "templates">("types");
	const [templatesLoaded, setTemplatesLoaded] = useState(false);
	const [loadingTypes, setLoadingTypes] = useState(false);
	const [loadingTemplates, setLoadingTemplates] = useState(false);
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [templates, setTemplates] = useState<DomainTicketTemplate[]>([]);
	const [typeKeyword, setTypeKeyword] = useState("");
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

	const filteredTypes = useMemo(() => {
		const trimmed = typeKeyword.trim().toLowerCase();
		if (!trimmed) {
			return ticketTypes;
		}
		return ticketTypes.filter(item =>
			item.code.toLowerCase().includes(trimmed)
			|| item.name.toLowerCase().includes(trimmed)
			|| (item.description ?? "").toLowerCase().includes(trimmed),
		);
	}, [typeKeyword, ticketTypes]);

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

	const openFormDesignTab = useCallback((ticketType: DomainTicketType) => {
		const path = buildFormDesignPath(domainId, ticketType.id);
		openAppScopeTab(appScopes.platform, navigate, path, {
			key: path,
			label: "表单设计",
			newTabTitle: `表单设计 - ${ticketType.name}`,
			closable: true,
			draggable: true,
		});
	}, [domainId, navigate]);

	const openFlowConfigTab = useCallback((ticketType: DomainTicketType) => {
		const path = `/platform/domains/ticket-type-config/${encodeURIComponent(domainId)}/${encodeURIComponent(ticketType.id)}/flow`;
		openAppScopeTab(appScopes.platform, navigate, path, {
			key: path,
			label: "状态流",
			newTabTitle: `状态流 - ${ticketType.name}`,
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
			message.success("工单类型已创建");
			setCreateOpen(false);
			createForm.resetFields();
			await loadTicketTypes();
			modal.confirm({
				title: "是否进入表单设计？",
				content: "创建成功，可立即配置该类型的表单字段。",
				okText: "进入表单设计",
				cancelText: "留在列表",
				onOk: () => openFormDesignTab(created),
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
			message.success("工单类型已更新");
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
		try {
			await updateDomainTicketType(domainId, ticketType.id, { status: nextStatus });
			message.success(nextStatus === "active" ? "已启用" : "已停用");
			await loadTicketTypes();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const handleDeleteType = async (ticketType: DomainTicketType) => {
		try {
			await deleteDomainTicketType(domainId, ticketType.id);
			message.success("工单类型已删除");
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
					工单配置
				</Title>
				<Tabs
					type="card"
					activeKey={activeSubTab}
					onChange={key => setActiveSubTab(key as "types" | "templates")}
					items={[
						{
							key: "types",
							label: "工单列表",
							children: (
								<TicketTypesPanel
									domainId={domainId}
									loading={loadingTypes}
									dataSource={filteredTypes}
									keyword={typeKeyword}
									onSearch={values => setTypeKeyword(values.keyword ?? "")}
									onResetSearch={() => setTypeKeyword("")}
									onRefresh={() => void loadTicketTypes()}
									onCreate={() => setCreateOpen(true)}
									onEdit={handleOpenEdit}
									onFormDesign={openFormDesignTab}
									onFlowConfig={openFlowConfigTab}
									onToggleStatus={handleToggleStatus}
									onDelete={handleDeleteType}
								/>
							),
						},
						{
							key: "templates",
							label: "工单模板",
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
					title="新建工单类型"
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
					title="编辑工单类型"
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
