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
import { TableSearchForm } from "#src/components/table-search-form";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "../../platform-domain-permissions";

import { EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, SettingOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Empty,
	Form,
	Input,
	Modal,
	Space,
	Table,
	Tag,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import { TicketTemplateModal } from "./ticket-template-modal";
import { TicketTypeDesignerDrawer } from "./ticket-type-designer-drawer";
import { countFlowStates, countFormFields } from "./ticket-type-form-defaults";

const { Title } = Typography;

export interface DetailTicketsProps {
	domainId: string;
}

interface TicketTypeSearchValues {
	keyword?: string;
}

export function DetailTickets({ domainId }: DetailTicketsProps) {
	const { message } = App.useApp();
	const [loadingTypes, setLoadingTypes] = useState(false);
	const [loadingTemplates, setLoadingTemplates] = useState(false);
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [templates, setTemplates] = useState<DomainTicketTemplate[]>([]);
	const [keyword, setKeyword] = useState("");
	const [createOpen, setCreateOpen] = useState(false);
	const [createCode, setCreateCode] = useState("");
	const [createName, setCreateName] = useState("");
	const [creating, setCreating] = useState(false);
	const [designerOpen, setDesignerOpen] = useState(false);
	const [editingType, setEditingType] = useState<DomainTicketType | null>(null);
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
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoadingTemplates(false);
		}
	}, [domainId, message]);

	const reloadAll = useCallback(async () => {
		await Promise.all([loadTicketTypes(), loadTemplates()]);
	}, [loadTicketTypes, loadTemplates]);

	useEffect(() => {
		void reloadAll();
	}, [reloadAll]);

	const filteredTypes = useMemo(() => {
		const trimmed = keyword.trim().toLowerCase();
		if (!trimmed) {
			return ticketTypes;
		}
		return ticketTypes.filter(item =>
			item.code.toLowerCase().includes(trimmed)
			|| item.name.toLowerCase().includes(trimmed),
		);
	}, [keyword, ticketTypes]);

	const handleSearch = (values: TicketTypeSearchValues) => {
		setKeyword(values.keyword ?? "");
	};

	const handleResetSearch = () => {
		setKeyword("");
	};

	const openDesigner = (ticketType: DomainTicketType) => {
		setEditingType(ticketType);
		setDesignerOpen(true);
	};

	const handleCreateType = async () => {
		const code = createCode.trim();
		const name = createName.trim();
		if (!code || !name) {
			message.warning("请填写类型编码和名称");
			return;
		}
		setCreating(true);
		try {
			const created = await createDomainTicketType(domainId, { code, name });
			message.success("工单类型已创建");
			setCreateOpen(false);
			setCreateCode("");
			setCreateName("");
			await loadTicketTypes();
			openDesigner(created);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setCreating(false);
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
			await reloadAll();
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

	const typeColumns: TableColumnsType<DomainTicketType> = [
		{ title: "编码", dataIndex: "code", width: 140 },
		{ title: "名称", dataIndex: "name" },
		{
			title: "状态",
			dataIndex: "status",
			width: 90,
			render: (value: string) => (
				<Tag color={value === "active" ? "success" : "default"}>
					{value === "active" ? "启用" : "停用"}
				</Tag>
			),
		},
		{
			title: "字段数",
			width: 80,
			render: (_, record) => countFormFields(record.form_schema),
		},
		{
			title: "状态数",
			width: 80,
			render: (_, record) => countFlowStates(record.status_flow),
		},
		{
			title: "操作",
			width: 220,
			render: (_, record) => (
				<Space size="small">
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<Button type="link" size="small" icon={<SettingOutlined />} onClick={() => openDesigner(record)}>
							配置
						</Button>
						<Button type="link" size="small" onClick={() => void handleToggleStatus(record)}>
							{record.status === "active" ? "停用" : "启用"}
						</Button>
					</AuthGuarded>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE} fallback={null}>
						<ConfirmPopover
							title="确认删除该工单类型？"
							description="若已有工单引用此类型将无法删除。"
							onConfirm={() => handleDeleteType(record)}
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
							loading={loadingTypes}
							initialValues={{ keyword: "" }}
							onFinish={handleSearch}
							onReset={handleResetSearch}
						>
							<Form.Item name="keyword" label="关键字">
								<Input allowClear placeholder="编码或名称" disabled={loadingTypes} />
							</Form.Item>
						</TableSearchForm>
					</Card>

					<Card
						bordered={false}
						title="工单类型列表"
						extra={(
							<Space>
								<Button icon={<ReloadOutlined />} onClick={() => void reloadAll()}>
									刷新
								</Button>
								<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE} fallback={null}>
									<Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
										新建类型
									</Button>
								</AuthGuarded>
							</Space>
						)}
					>
						<Table<DomainTicketType>
							rowKey="id"
							loading={loadingTypes}
							columns={typeColumns}
							dataSource={filteredTypes}
							pagination={false}
							locale={{ emptyText: <Empty description="暂无工单类型" /> }}
						/>
					</Card>

					<Card
						bordered={false}
						title="工单模板列表"
						extra={(
							<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE} fallback={null}>
								<Button
									icon={<PlusOutlined />}
									disabled={ticketTypes.length === 0}
									onClick={() => {
										setEditingTemplate(null);
										setTemplateOpen(true);
									}}
								>
									新建模板
								</Button>
							</AuthGuarded>
						)}
					>
						<Table<DomainTicketTemplate>
							rowKey="id"
							loading={loadingTemplates}
							columns={templateColumns}
							dataSource={templates}
							pagination={false}
							locale={{ emptyText: <Empty description="暂无工单模板" /> }}
						/>
					</Card>
				</div>

				<Modal
					title="新建工单类型"
					open={createOpen}
					confirmLoading={creating}
					okText="创建并配置"
					cancelText="取消"
					onCancel={() => {
						setCreateOpen(false);
						setCreateCode("");
						setCreateName("");
					}}
					onOk={() => void handleCreateType()}
				>
					<Form layout="vertical">
						<Form.Item label="类型编码" required>
							<Input
								value={createCode}
								placeholder="例如 feedback"
								maxLength={32}
								onChange={event => setCreateCode(event.target.value)}
							/>
						</Form.Item>
						<Form.Item label="类型名称" required>
							<Input
								value={createName}
								placeholder="例如 问题反馈"
								maxLength={64}
								onChange={event => setCreateName(event.target.value)}
							/>
						</Form.Item>
					</Form>
				</Modal>

				<TicketTypeDesignerDrawer
					open={designerOpen}
					domainId={domainId}
					ticketType={editingType}
					onClose={() => {
						setDesignerOpen(false);
						setEditingType(null);
					}}
					onSaved={(saved) => {
						setTicketTypes(prev => prev.map(item => (item.id === saved.id ? saved : item)));
					}}
				/>

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
