import type { DomainTicketType } from "@uniondesk/shared";
import {
	createDomainTicketType,
	deleteDomainTicketType,
	fetchDomainTicketTypes,
	importDomainTicketTypesFromPlatform,
	toErrorMessage,
	updateDomainTicketType,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { IconPicker } from "#src/components/icon-picker";
import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";
import {
	DOMAIN_TICKET_TYPE_CREATE,
	DOMAIN_TICKET_TYPE_READ,
	DOMAIN_TICKET_TYPE_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { appScopes } from "#src/router/extra-info/app-scope";
import { openAppScopeTab } from "#src/utils/tabbar-utils";

import {
	EditOutlined,
	MoreOutlined,
	NodeIndexOutlined,
	PlusOutlined,
	ReloadOutlined,
	SettingOutlined,
} from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Dropdown,
	Empty,
	Form,
	Input,
	Modal,
	Select,
	Space,
	Switch,
	Table,
	Tag,
	Tooltip,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

import { DomainAddPlatformTicketTypesModal } from "#src/pages/platform/domains/detail/components/domain-add-platform-ticket-types-modal";
import { isDraftUnpublished } from "#src/pages/platform/domains/detail/components/ticket-type-form-defaults";

import { CreateTicketTypeDropdown } from "#src/pages/platform/ticket-config/types/components/create-ticket-type-dropdown";
import { TicketTypeFormModal } from "#src/pages/platform/ticket-config/types/components/ticket-type-form-modal";
import type { TicketTypeTemplateKey } from "#src/pages/platform/ticket-config/types/components/ticket-type-utils";

import styles from "./types-panel.module.less";

const { Text } = Typography;

type DomainTicketTypeConfigTab = "attributes" | "workflow" | "template";

export interface DomainTicketTypesPanelProps {
	domainId: string;
}

interface TicketTypeActionHandlers {
	onEdit: (ticketType: DomainTicketType) => void;
	onAttributeEdit: (ticketType: DomainTicketType) => void;
	onWorkflowEdit: (ticketType: DomainTicketType) => void;
	onCopy: (ticketType: DomainTicketType) => void;
	onToggleStatus: (ticketType: DomainTicketType) => void;
	onDelete: (ticketType: DomainTicketType) => void;
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
							auth={DOMAIN_TICKET_TYPE_UPDATE}
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
						<AuthGuarded auth={DOMAIN_TICKET_TYPE_UPDATE} fallback={null}>
							<Button type="link" size="small" icon={<EditOutlined />} onClick={() => handlers.onEdit(record)}>
								编辑
							</Button>
							<Button type="text" size="small" icon={<SettingOutlined />} onClick={() => handlers.onAttributeEdit(record)}>
								属性
							</Button>
							<Button type="text" size="small" icon={<NodeIndexOutlined />} onClick={() => handlers.onWorkflowEdit(record)}>
								工作流
							</Button>
						</AuthGuarded>
						<Dropdown
							menu={{
								items: [
									{
										key: "copy",
										label: "复制为新类型",
										onClick: () => handlers.onCopy(record),
									},
									{
										key: "delete",
										danger: true,
										label: "删除",
										onClick: () => handlers.onDelete(record),
									},
								],
							}}
						>
							<Button type="text" size="small" icon={<MoreOutlined />} />
						</Dropdown>
					</Space>
				</div>
			),
		},
	], [handlers]);
}

export function DomainTicketTypesPanel({ domainId }: DomainTicketTypesPanelProps) {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	const [loading, setLoading] = useState(false);
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [createOpen, setCreateOpen] = useState(false);
	const [addFromPlatformOpen, setAddFromPlatformOpen] = useState(false);
	const [importing, setImporting] = useState(false);
	const [creating, setCreating] = useState(false);
	const [templateKey, setTemplateKey] = useState<TicketTypeTemplateKey | null>(null);
	const [copyFrom, setCopyFrom] = useState<DomainTicketType | null>(null);
	const [editOpen, setEditOpen] = useState(false);
	const [editForm] = Form.useForm<{ name: string; description?: string; icon?: string; status: string }>();
	const [editingType, setEditingType] = useState<DomainTicketType | null>(null);
	const [editing, setEditing] = useState(false);

	const loadTicketTypes = useCallback(async () => {
		if (!domainId) {
			setTicketTypes([]);
			return;
		}
		setLoading(true);
		try {
			const list = await fetchDomainTicketTypes(domainId);
			setTicketTypes(list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadTicketTypes();
	}, [loadTicketTypes]);

	const openConfigTab = useCallback((ticketType: DomainTicketType, tab: DomainTicketTypeConfigTab = "attributes") => {
		const path = `/domain/ticket-config/types/${encodeURIComponent(ticketType.id)}?tab=${tab}`;
		openAppScopeTab(appScopes.business, navigate, path, {
			key: `/domain/ticket-config/types/${encodeURIComponent(ticketType.id)}`,
			label: "事项类型配置",
			newTabTitle: `事项类型配置 - ${ticketType.name}`,
			closable: true,
			draggable: true,
		});
	}, [navigate]);

	const handleOpenCreate = (key: TicketTypeTemplateKey) => {
		setCopyFrom(null);
		setTemplateKey(key);
		setCreateOpen(true);
	};

	const handleCreateType = async (values: { name: string; icon: string; description?: string }) => {
		const selectedTemplate = templateKey;
		setCreating(true);
		try {
			const created = await createDomainTicketType(domainId, {
				name: values.name.trim(),
				icon: values.icon.trim(),
				description: values.description?.trim() || null,
				template_key: copyFrom ? undefined : (selectedTemplate ?? undefined),
			});
			message.success("事项类型已创建");
			setCreateOpen(false);
			setTemplateKey(null);
			setCopyFrom(null);
			await loadTicketTypes();
			modal.confirm({
				title: "是否进入配置？",
				content: selectedTemplate === "simple_ticket"
					? "创建成功。该类型已自动关联系统属性「描述」（必填），可继续配置工作流或添加自定义属性。"
					: selectedTemplate === "standard_ticket"
						? "创建成功。该类型已自动关联系统属性「标题」「描述」（均必填），可继续配置工作流或添加自定义属性。"
						: "创建成功，可立即配置该类型的属性、工作流与描述模板。",
				okText: "进入配置",
				cancelText: "留在列表",
				onOk: () => openConfigTab(created),
			});
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setCreating(false);
		}
	};

	const excludePlatformTypeIds = useMemo(() => {
		const ids = new Set<string>();
		for (const type of ticketTypes) {
			if (type.source_global_type_id) {
				ids.add(type.source_global_type_id);
			}
		}
		return ids;
	}, [ticketTypes]);

	const handleImportFromPlatform = async (platformTypeIds: string[]) => {
		setImporting(true);
		try {
			const created = await importDomainTicketTypesFromPlatform(domainId, platformTypeIds);
			message.success(created.length > 1 ? `已添加 ${created.length} 个事项类型` : "事项类型已添加");
			setAddFromPlatformOpen(false);
			await loadTicketTypes();
			if (created.length === 1) {
				modal.confirm({
					title: "是否进入配置？",
					content: "添加成功，可立即配置该类型的属性、工作流与描述模板。",
					okText: "进入配置",
					cancelText: "留在列表",
					onOk: () => openConfigTab(created[0]),
				});
			}
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setImporting(false);
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

	const handleDeleteType = (ticketType: DomainTicketType) => {
		modal.confirm({
			title: "确认删除该事项类型？",
			content: `事项类型「${ticketType.name}」删除后不可恢复。若已有工单引用此类型将无法删除。`,
			okText: "确认删除",
			cancelText: "取消",
			okButtonProps: { danger: true },
			onOk: async () => {
				try {
					await deleteDomainTicketType(domainId, ticketType.id);
					message.success("事项类型已删除");
					await loadTicketTypes();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	};

	const handleCopyType = (ticketType: DomainTicketType) => {
		setTemplateKey(null);
		setCopyFrom(ticketType);
		setCreateOpen(true);
	};

	const typeHandlers: TicketTypeActionHandlers = {
		onEdit: handleOpenEdit,
		onAttributeEdit: type => openConfigTab(type, "attributes"),
		onWorkflowEdit: type => openConfigTab(type, "workflow"),
		onCopy: handleCopyType,
		onToggleStatus: handleToggleStatus,
		onDelete: handleDeleteType,
	};

	const columns = useTicketTypeColumns(typeHandlers);

	return (
		<AuthGuarded auth={DOMAIN_TICKET_TYPE_READ}>
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
						<Button icon={<ReloadOutlined />} onClick={() => void loadTicketTypes()}>
							刷新
						</Button>
						<AuthGuarded auth={DOMAIN_TICKET_TYPE_CREATE} fallback={null}>
							<Space>
								<Button icon={<PlusOutlined />} onClick={() => setAddFromPlatformOpen(true)}>
									添加事项类型
								</Button>
								<CreateTicketTypeDropdown onSelect={handleOpenCreate} />
							</Space>
						</AuthGuarded>
					</Space>
				)}
			>
				<Table<DomainTicketType>
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={ticketTypes}
					pagination={false}
					tableLayout="fixed"
					className={styles.typesTable}
					rowClassName={record => record.status === "active" ? "" : styles.rowDisabled}
					locale={{
						emptyText: <Empty description="暂无事项类型" />,
					}}
				/>
			</Card>

			<DomainAddPlatformTicketTypesModal
				open={addFromPlatformOpen}
				excludePlatformTypeIds={excludePlatformTypeIds}
				submitting={importing}
				onCancel={() => setAddFromPlatformOpen(false)}
				onAdd={handleImportFromPlatform}
			/>

			<TicketTypeFormModal
				open={createOpen}
				loading={creating}
				editing={null}
				templateKey={templateKey}
				copyFrom={copyFrom
					? {
							id: copyFrom.id,
							scope: "domain",
							name: copyFrom.name,
							icon: copyFrom.icon ?? "",
							description: copyFrom.description ?? "",
							status: copyFrom.status,
							code: copyFrom.code,
							category: copyFrom.category ?? "transaction",
							sort_order: 0,
							is_system: false,
							linked_domain_count: 0,
						}
					: null}
				onCancel={() => {
					setCreateOpen(false);
					setTemplateKey(null);
					setCopyFrom(null);
				}}
				onSubmit={handleCreateType}
			/>

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
		</AuthGuarded>
	);
}
