import type { RoleTemplateAppliedDomain, RoleTemplateBatchResult, RoleTemplateItem, RoleTemplatePermissionItem } from "#src/api/platform/role-template";
import {
	applyRoleTemplate,
	createRoleTemplate,
	deleteRoleTemplate,
	fetchRoleTemplateDetail,
	fetchRoleTemplateList,
	fetchRoleTemplatePermissionItems,
	syncRoleTemplate,
	unapplyRoleTemplate,
	updateRoleTemplate,
} from "#src/api/platform/role-template";
import { fetchBusinessDomains } from "#src/api/platform/domain";
import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";

import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SendOutlined, SyncOutlined } from "@ant-design/icons";
import { App, Button, Checkbox, Form, Input, Modal, Select, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

const { Text } = Typography;

const TEMPLATE_CREATE = "platform.role_template.create";
const TEMPLATE_UPDATE = "platform.role_template.update";
const TEMPLATE_DELETE = "platform.role_template.delete";
const TEMPLATE_APPLY = "platform.role_template.apply";
const TEMPLATE_SYNC = "platform.role_template.sync";

const SYNC_STRATEGY_LABELS: Record<string, string> = {
	immediate: "立即同步",
	manual: "手动同步",
	none: "不同步",
};

const LOCKED_FIELD_LABELS: Record<string, string> = {
	permissions: "权限包",
};

interface TemplateFormValues {
	code: string
	name: string
	description?: string
	sync_strategy: "immediate" | "manual" | "none"
	locked_fields: string[]
	permission_ids: number[]
}

interface TemplateEditState {
	item?: RoleTemplateItem
}

interface ApplyModalState {
	template: RoleTemplateItem
	appliedDomains: RoleTemplateAppliedDomain[]
}

function groupPermissionItemsByModule(items: RoleTemplatePermissionItem[]): Map<string, RoleTemplatePermissionItem[]> {
	const grouped = new Map<string, RoleTemplatePermissionItem[]>();
	for (const item of items) {
		const moduleKey = item.module?.trim() || "其他";
		const list = grouped.get(moduleKey) ?? [];
		list.push(item);
		grouped.set(moduleKey, list);
	}
	return grouped;
}

function formatLockedFields(fields: string[]): string {
	if (!fields || fields.length === 0) {
		return "—";
	}
	return fields.map(field => LOCKED_FIELD_LABELS[field] ?? field).join("、");
}

/** 批量结果摘要：成功/跳过/失败 */
function formatBatchResult(result: RoleTemplateBatchResult): string {
	const parts: string[] = [];
	parts.push(`成功 ${result.success.length} 个域`);
	if (result.skipped.length > 0) {
		const reasons = result.skipped.map(item => `域${item.domain_id}：${item.reason}`).join("；");
		parts.push(`跳过 ${result.skipped.length} 个域（${reasons}）`);
	}
	if (result.failed.length > 0) {
		const reasons = result.failed.map(item => `域${item.domain_id}：${item.reason}`).join("；");
		parts.push(`失败 ${result.failed.length} 个域（${reasons}）`);
	}
	return parts.join("\n");
}

export function TemplateTab() {
	const { message, modal } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [templates, setTemplates] = useState<RoleTemplateItem[]>([]);
	const [permissionItems, setPermissionItems] = useState<RoleTemplatePermissionItem[]>([]);
	const [domains, setDomains] = useState<Array<{ id: number; name: string }>>([]);

	const [editOpen, setEditOpen] = useState(false);
	const [editSubmitting, setEditSubmitting] = useState(false);
	const [editState, setEditState] = useState<TemplateEditState>({});
	const [templateForm] = Form.useForm<TemplateFormValues>();

	const [applyOpen, setApplyOpen] = useState(false);
	const [applySubmitting, setApplySubmitting] = useState(false);
	const [applyState, setApplyState] = useState<ApplyModalState | null>(null);
	const [selectedDomainIds, setSelectedDomainIds] = useState<number[]>([]);

	const loadTemplates = useCallback(async () => {
		setLoading(true);
		try {
			setTemplates(await fetchRoleTemplateList());
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "模板列表加载失败");
		}
		finally {
			setLoading(false);
		}
	}, [message]);

	const loadPermissionItems = useCallback(async () => {
		try {
			setPermissionItems(await fetchRoleTemplatePermissionItems());
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "权限目录加载失败");
		}
	}, [message]);

	useEffect(() => {
		void loadTemplates();
		void loadPermissionItems();
	}, [loadTemplates, loadPermissionItems]);

	const permissionGroups = useMemo(() => groupPermissionItemsByModule(permissionItems), [permissionItems]);

	const handleOpenCreate = () => {
		setEditState({});
		templateForm.resetFields();
		templateForm.setFieldsValue({
			sync_strategy: "immediate",
			locked_fields: ["permissions"],
			permission_ids: [],
		});
		setEditOpen(true);
	};

	const handleOpenEdit = async (item: RoleTemplateItem) => {
		try {
			const detail = await fetchRoleTemplateDetail(item.id);
			setEditState({ item });
			const template = detail.template;
			templateForm.setFieldsValue({
				code: template.code,
				name: template.name,
				description: template.description ?? "",
				sync_strategy: template.sync_strategy,
				locked_fields: template.locked_fields,
				permission_ids: detail.permission_items.map(permission => permission.id),
			});
			setEditOpen(true);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "模板详情加载失败");
		}
	};

	const handleSaveTemplate = async () => {
		const values = await templateForm.validateFields();
		if (editSubmitting) {
			return;
		}
		setEditSubmitting(true);
		try {
			const permissionIds = values.permission_ids ?? [];
			const payload = {
				name: values.name.trim(),
				description: values.description?.trim() || null,
				sync_strategy: values.sync_strategy,
				locked_fields: values.locked_fields,
				permission_item_ids: permissionIds,
			};
			if (editState.item) {
				await updateRoleTemplate(editState.item.id, payload);
				message.success("模板已更新");
			}
			else {
				await createRoleTemplate({
					code: values.code.trim(),
					...payload,
				});
				message.success("模板已创建");
			}
			setEditOpen(false);
			await loadTemplates();
		}
		catch (error) {
			if (error && typeof error === "object" && "errorFields" in error) {
				return;
			}
			message.error(error instanceof Error ? error.message : "保存失败");
		}
		finally {
			setEditSubmitting(false);
		}
	};

	const handleDeleteTemplate = async (item: RoleTemplateItem) => {
		try {
			await deleteRoleTemplate(item.id);
			message.success("模板已删除");
			await loadTemplates();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "删除失败");
		}
	};

	const handleOpenApply = async (item: RoleTemplateItem) => {
		try {
			const [detail, domainList] = await Promise.all([
				fetchRoleTemplateDetail(item.id),
				fetchBusinessDomains(),
			]);
			setApplyState({ template: detail.template, appliedDomains: detail.applied_domains });
			setSelectedDomainIds([]);
			setDomains(domainList.map(domain => ({ id: domain.id, name: domain.name })));
			setApplyOpen(true);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "业务域列表加载失败");
		}
	};

	const handleApply = async () => {
		if (!applyState || selectedDomainIds.length === 0) {
			message.warning("请选择需要下发的业务域");
			return;
		}
		if (applySubmitting) {
			return;
		}
		setApplySubmitting(true);
		try {
			const result = await applyRoleTemplate(applyState.template.id, {
				domain_ids: selectedDomainIds,
				sync_mode: "immediate",
			});
			modal.info({
				title: "下发完成",
				content: <pre style={{ whiteSpace: "pre-wrap" }}>{formatBatchResult(result)}</pre>,
			});
			setApplyOpen(false);
			await loadTemplates();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "下发失败");
		}
		finally {
			setApplySubmitting(false);
		}
	};

	const handleSync = async (item: RoleTemplateItem) => {
		modal.confirm({
			title: "确认同步该模板？",
			content: `将模板「${item.name}」当前权限包与版本同步到已下发的 ${item.applied_domain_count} 个业务域实例。`,
			okText: "确认",
			cancelText: "取消",
			onOk: async () => {
				try {
					const result = await syncRoleTemplate(item.id);
					modal.info({
						title: "同步完成",
						content: <pre style={{ whiteSpace: "pre-wrap" }}>{formatBatchResult(result)}</pre>,
					});
					await loadTemplates();
				}
				catch (error) {
					message.error(error instanceof Error ? error.message : "同步失败");
				}
			},
		});
	};

	const handleUnapply = async (templateId: number, applied: RoleTemplateAppliedDomain) => {
		modal.confirm({
			title: "确认解绑该域？",
			content: `解绑后域 ${applied.domain_id} 的角色实例将转为独立角色，不再随模板同步。`,
			okText: "确认",
			cancelText: "取消",
			onOk: async () => {
				try {
					const result = await unapplyRoleTemplate(templateId, { domain_ids: [applied.domain_id] });
					modal.info({
						title: "解绑完成",
						content: <pre style={{ whiteSpace: "pre-wrap" }}>{formatBatchResult(result)}</pre>,
					});
					if (applyState) {
						setApplyState({
							...applyState,
							appliedDomains: applyState.appliedDomains.filter(item => item.domain_id !== applied.domain_id),
						});
					}
					await loadTemplates();
				}
				catch (error) {
					message.error(error instanceof Error ? error.message : "解绑失败");
				}
			},
		});
	};

	const columns: TableColumnsType<RoleTemplateItem> = useMemo(() => [
		{
			title: "模板名称",
			dataIndex: "name",
			render: (_, row) => <Text strong>{row.name}</Text>,
		},
		{
			title: "模板编码",
			dataIndex: "code",
			render: (_, row) => <Text code>{row.code}</Text>,
		},
		{
			title: "同步策略",
			dataIndex: "sync_strategy",
			width: 110,
			render: strategy => <Tag color={strategy === "immediate" ? "blue" : strategy === "manual" ? "orange" : "default"}>{SYNC_STRATEGY_LABELS[strategy] ?? strategy}</Tag>,
		},
		{
			title: "锁定字段",
			dataIndex: "locked_fields",
			width: 120,
			render: fields => formatLockedFields(fields ?? []),
		},
		{
			title: "版本",
			dataIndex: "version",
			width: 80,
			align: "center",
			render: version => <Text code>v{version}</Text>,
		},
		{
			title: "已下发域数",
			dataIndex: "applied_domain_count",
			width: 110,
			align: "center",
			render: count => count > 0 ? <Tag color="green">{count} 个</Tag> : <Text type="secondary">未下发</Text>,
		},
		{
			title: "操作",
			key: "actions",
			width: 240,
			render: (_, row) => (
				<Space size="small" wrap>
					<AuthGuarded auth={TEMPLATE_UPDATE}>
						<Button type="link" size="small" icon={<EditOutlined />} onClick={() => void handleOpenEdit(row)}>
							编辑
						</Button>
					</AuthGuarded>
					<AuthGuarded auth={TEMPLATE_APPLY}>
						<Button type="link" size="small" icon={<SendOutlined />} onClick={() => void handleOpenApply(row)}>
							推送选域
						</Button>
					</AuthGuarded>
					<AuthGuarded auth={TEMPLATE_SYNC}>
						<Button type="link" size="small" icon={<SyncOutlined />} onClick={() => handleSync(row)}>
							同步
						</Button>
					</AuthGuarded>
					<AuthGuarded auth={TEMPLATE_DELETE}>
						<ConfirmPopover
							title="确认删除该模板？"
							onConfirm={() => handleDeleteTemplate(row)}
							okText="确认"
							cancelText="取消"
						>
							<Button type="link" size="small" danger icon={<DeleteOutlined />}>
								删除
							</Button>
						</ConfirmPopover>
					</AuthGuarded>
				</Space>
			),
		},
	], [handleSync, loadTemplates, message, modal]);

	return (
		<div className="flex flex-col gap-4">
			<div className="flex justify-end">
				<AuthGuarded auth={TEMPLATE_CREATE}>
					<Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate}>
						新建模板
					</Button>
				</AuthGuarded>
				<Button icon={<ReloadOutlined />} onClick={() => void loadTemplates()} style={{ marginLeft: 8 }}>
					刷新
				</Button>
			</div>
			<Table<RoleTemplateItem>
				rowKey="id"
				loading={loading}
				columns={columns}
				dataSource={templates}
				pagination={false}
				locale={{ emptyText: "暂无角色模板" }}
			/>

			{/* 创建 / 编辑模板 */}
			<Modal
				title={editState.item ? "编辑模板" : "新建模板"}
				open={editOpen}
				confirmLoading={editSubmitting}
				destroyOnHidden
				width={640}
				onCancel={() => setEditOpen(false)}
				onOk={() => void handleSaveTemplate()}
				okText="保存"
				cancelText="取消"
			>
				<Form form={templateForm} layout="vertical">
					<Form.Item
						name="code"
						label="模板编码"
						rules={[
							{ required: true, message: "请输入模板编码" },
							{ pattern: /^[a-z][a-z0-9_]{1,31}$/, message: "编码需小写字母开头，仅含小写字母/数字/下划线" },
						]}
					>
						<Input allowClear placeholder="例如 ops_admin" disabled={Boolean(editState.item)} />
					</Form.Item>
					<Form.Item name="name" label="模板名称" rules={[{ required: true, message: "请输入模板名称" }]}>
						<Input allowClear placeholder="例如 运营管理员" />
					</Form.Item>
					<Form.Item name="description" label="模板描述">
						<Input.TextArea rows={2} allowClear placeholder="模板用途说明（可选）" />
					</Form.Item>
					<Space size="large" wrap>
						<Form.Item name="sync_strategy" label="同步策略" rules={[{ required: true }]}>
							<Select
								style={{ width: 200 }}
								options={[
									{ value: "immediate", label: "立即同步（模板变更自动下发实例）" },
									{ value: "manual", label: "手动同步（平台手动触发 sync）" },
									{ value: "none", label: "不同步（一次性模板，创建后与实例解耦）" },
								]}
							/>
						</Form.Item>
						<Form.Item name="locked_fields" label="锁定字段（域端不可修改）" rules={[{ required: true }]}>
							<Checkbox.Group options={[{ value: "permissions", label: "权限包" }]} />
						</Form.Item>
					</Space>
					<Form.Item
						name="permission_ids"
						label="权限包（permission_item 目录）"
						rules={[{ required: true, message: "请选择至少一个权限项" }]}
					>
						<Checkbox.Group style={{ width: "100%" }}>
							{permissionItems.length === 0
								? <Text type="secondary">暂无权限项</Text>
								: [...permissionGroups.entries()].map(([moduleKey, items]) => (
									<div key={moduleKey} style={{ marginBottom: 12 }}>
										<Text strong>{moduleKey}</Text>
										<div style={{ display: "flex", flexDirection: "column", gap: 4, marginTop: 4 }}>
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
					</Form.Item>
				</Form>
			</Modal>

			{/* 推送选域 */}
			<Modal
				title={applyState ? `推送选域 - ${applyState.template.name}` : "推送选域"}
				open={applyOpen}
				confirmLoading={applySubmitting}
				destroyOnHidden
				width={640}
				onCancel={() => setApplyOpen(false)}
				onOk={() => void handleApply()}
				okText="下发"
				cancelText="取消"
			>
				<div style={{ marginBottom: 8 }}>
					<Text type="secondary">
						每次下发将按域生成角色实例（编码为模板编码）；满额域（自定义角色达 20 个）自动跳过并提示。
					</Text>
				</div>
				<Form.Item label="选择业务域" required style={{ marginBottom: 8 }}>
					<Select
						mode="multiple"
						style={{ width: "100%" }}
						placeholder="请选择需要下发的业务域"
						value={selectedDomainIds}
						onChange={setSelectedDomainIds}
						options={domains.map(domain => ({ value: domain.id, label: domain.name }))}
					/>
				</Form.Item>
				{applyState && applyState.appliedDomains.length > 0 ? (
					<div>
						<Text strong>已下发域（{applyState.appliedDomains.length} 个）</Text>
						{applyState.appliedDomains.map(applied => {
							const templateVersion = applyState.template.version;
							const behind = applied.instance_version != null && applied.instance_version < templateVersion
								? templateVersion - applied.instance_version
								: 0;
							return (
								<div
									key={applied.domain_id}
									style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 6 }}
								>
									<Text>域 {applied.domain_id}</Text>
									<Text code>v{applied.instance_version ?? "?"}</Text>
									{behind > 0
										? <Tag color="orange">落后 {behind} 版本</Tag>
										: <Tag color="green">已同步</Tag>}
									<AuthGuarded auth={TEMPLATE_APPLY}>
										<Button
											type="link"
											size="small"
											danger
											onClick={() => handleUnapply(applyState.template.id, applied)}
										>
											解绑
										</Button>
									</AuthGuarded>
								</div>
							);
						})}
					</div>
				) : null}
			</Modal>
		</div>
	);
}
