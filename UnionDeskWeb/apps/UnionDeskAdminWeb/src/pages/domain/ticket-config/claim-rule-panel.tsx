import {
	fetchDomainMembersPage,
	fetchDomainPriorityLevels,
	fetchDomainTicketTypes,
	type DomainMember,
	type DomainPriorityLevelView,
	type DomainTicketType,
} from "@uniondesk/shared";

import {
	createClaimRule,
	deleteClaimRule,
	fetchClaimRules,
	updateClaimRule,
	type ClaimRuleView,
} from "#src/api/platform/ticket-claim-rule";
import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";
import {
	DOMAIN_TICKET_CLAIM_RULE_CREATE,
	DOMAIN_TICKET_CLAIM_RULE_DELETE,
	DOMAIN_TICKET_CLAIM_RULE_READ,
	DOMAIN_TICKET_CLAIM_RULE_UPDATE,
} from "#src/pages/domain/domain-permissions";

import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Empty,
	Form,
	Input,
	InputNumber,
	Modal,
	Radio,
	Select,
	Space,
	Switch,
	Table,
	Tooltip,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

/** 表单中「全部」的占位值，提交时转换为 null */
const ALL_VALUE = "";

interface ClaimRulePanelProps {
	domainId: string;
}

interface ClaimRuleFormValues {
	name: string;
	enabled: boolean;
	matchTicketTypeId?: string;
	matchPriorityLevelId?: string;
	strategy: "least_loaded" | "fixed";
	assigneeStaffAccountId?: string;
	graceMinutes?: number;
}

function memberDisplayName(member: Pick<DomainMember, "real_name" | "nickname" | "username" | "id">): string {
	return member.real_name ?? member.nickname ?? member.username ?? member.id;
}

export function ClaimRulePanel({ domainId }: ClaimRulePanelProps) {
	const { message } = App.useApp();
	const [rows, setRows] = useState<ClaimRuleView[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [loading, setLoading] = useState(false);
	const [editorOpen, setEditorOpen] = useState(false);
	const [editingRule, setEditingRule] = useState<ClaimRuleView | null>(null);
	const [saving, setSaving] = useState(false);
	const [form] = Form.useForm<ClaimRuleFormValues>();

	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [priorityLevels, setPriorityLevels] = useState<DomainPriorityLevelView[]>([]);
	const [members, setMembers] = useState<DomainMember[]>([]);

	const loadMeta = useCallback(async () => {
		try {
			const [typeResult, priorityResult, memberResult] = await Promise.all([
				fetchDomainTicketTypes(domainId),
				fetchDomainPriorityLevels(domainId),
				fetchDomainMembersPage({ domainId, page: 1, page_size: 200, status: "active" }),
			]);
			setTicketTypes(typeResult);
			setPriorityLevels(priorityResult.items ?? []);
			setMembers(memberResult.list);
		}
		catch {
			// 选项加载失败不阻塞列表，选项留空兜底
		}
	}, [domainId]);

	const loadData = useCallback(async (nextPage = page, nextPageSize = pageSize) => {
		setLoading(true);
		try {
			const result = await fetchClaimRules(domainId, { page: nextPage, page_size: nextPageSize });
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
		}
		catch {
			// 列表加载失败静默处理
		}
		finally {
			setLoading(false);
		}
	}, [domainId, page, pageSize]);

	useEffect(() => {
		void loadData(1, pageSize);
		void loadMeta();
		// eslint-disable-next-line react-hooks/exhaustive-deps -- 域切换时回到第一页并重载元数据
	}, [domainId]);

	const typeNameMap = useMemo(() => {
		const map = new Map<string, string>();
		for (const type of ticketTypes) {
			map.set(type.id, type.name);
		}
		return map;
	}, [ticketTypes]);

	const priorityNameMap = useMemo(() => {
		const map = new Map<string, string>();
		for (const item of priorityLevels) {
			map.set(item.id, item.display_label ?? item.name);
		}
		return map;
	}, [priorityLevels]);

	const memberNameMap = useMemo(() => {
		const map = new Map<string, string>();
		for (const member of members) {
			map.set(member.staff_account_id, memberDisplayName(member));
		}
		return map;
	}, [members]);

	const openEditor = (row?: ClaimRuleView) => {
		setEditingRule(row ?? null);
		form.setFieldsValue({
			name: row?.name ?? "",
			enabled: row?.enabled ?? true,
			matchTicketTypeId: row?.matchTicketTypeId ?? ALL_VALUE,
			matchPriorityLevelId: row?.matchPriorityLevelId ?? ALL_VALUE,
			strategy: row?.strategy ?? "least_loaded",
			assigneeStaffAccountId: row?.assigneeStaffAccountId ?? undefined,
			graceMinutes: row?.graceMinutes ?? 0,
		});
		setEditorOpen(true);
	};

	const closeEditor = () => {
		setEditorOpen(false);
		setEditingRule(null);
		form.resetFields();
	};

	const submitEditor = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setSaving(true);
		try {
			const payload = {
				name: values.name.trim(),
				enabled: !!values.enabled,
				matchTicketTypeId: values.matchTicketTypeId || null,
				matchPriorityLevelId: values.matchPriorityLevelId || null,
				strategy: values.strategy,
				assigneeStaffAccountId: values.strategy === "fixed" ? (values.assigneeStaffAccountId || null) : null,
				graceMinutes: values.graceMinutes ?? 0,
			};
			if (editingRule) {
				await updateClaimRule(domainId, editingRule.id, payload);
				message.success("领取规则已更新");
			}
			else {
				await createClaimRule(domainId, payload);
				message.success("领取规则已创建");
			}
			closeEditor();
			await loadData(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
		finally {
			setSaving(false);
		}
	};

	const handleToggleEnabled = async (row: ClaimRuleView) => {
		try {
			await updateClaimRule(domainId, row.id, {
				name: row.name,
				enabled: !row.enabled,
				matchTicketTypeId: row.matchTicketTypeId ?? null,
				matchPriorityLevelId: row.matchPriorityLevelId ?? null,
				strategy: row.strategy,
				assigneeStaffAccountId: row.assigneeStaffAccountId ?? null,
				graceMinutes: row.graceMinutes,
			});
			message.success(row.enabled ? "已停用" : "已启用");
			await loadData(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "操作失败");
		}
	};

	const handleDelete = async (ruleId: string) => {
		try {
			await deleteClaimRule(domainId, ruleId);
			message.success("领取规则已删除");
			await loadData(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "删除失败");
		}
	};

	const columns: TableColumnsType<ClaimRuleView> = [
		{ title: "名称", dataIndex: "name", width: 180, ellipsis: true },
		{
			title: "启用",
			dataIndex: "enabled",
			width: 90,
			align: "center",
			render: (_, row) => (
				<AuthGuarded auth={DOMAIN_TICKET_CLAIM_RULE_UPDATE} fallback={(
					<Tooltip title={row.enabled ? "停用" : "启用"}>
						<Switch size="small" checked={row.enabled} disabled />
					</Tooltip>
				)}>
					<Tooltip title={row.enabled ? "停用" : "启用"}>
						<Switch size="small" checked={row.enabled} onChange={() => void handleToggleEnabled(row)} />
					</Tooltip>
				</AuthGuarded>
			),
		},
		{
			title: "匹配类型",
			dataIndex: "matchTicketTypeId",
			width: 150,
			render: value => (value ? (typeNameMap.get(value) ?? value) : "全部"),
		},
		{
			title: "匹配优先级",
			dataIndex: "matchPriorityLevelId",
			width: 150,
			render: value => (value ? (priorityNameMap.get(value) ?? value) : "全部"),
		},
		{
			title: "策略",
			dataIndex: "strategy",
			width: 100,
			render: value => (value === "fixed" ? "指定人" : "负载均衡"),
		},
		{
			title: "指定人",
			dataIndex: "assigneeStaffAccountId",
			width: 140,
			render: (value, row) => (row.strategy === "fixed" ? (memberNameMap.get(value) ?? value) : "—"),
		},
		{
			title: "操作",
			key: "actions",
			width: 100,
			render: (_, row) => (
				<Space>
					<AuthGuarded auth={DOMAIN_TICKET_CLAIM_RULE_UPDATE} fallback={null}>
						<Tooltip title="编辑">
							<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditor(row)} />
						</Tooltip>
					</AuthGuarded>
					<AuthGuarded auth={DOMAIN_TICKET_CLAIM_RULE_DELETE} fallback={null}>
						<Tooltip title="删除">
							<ConfirmPopover title="确认删除该领取规则？" onConfirm={() => void handleDelete(row.id)}>
								<Button type="link" size="small" danger icon={<DeleteOutlined />} />
							</ConfirmPopover>
						</Tooltip>
					</AuthGuarded>
				</Space>
			),
		},
	];

	return (
		<AuthGuarded auth={DOMAIN_TICKET_CLAIM_RULE_READ} fallback={<Empty description="无权限查看领取规则" />}>
			<Card
				bordered={false}
				title="领取规则"
				extra={(
					<Space>
						<Button icon={<ReloadOutlined />} onClick={() => void loadData(1, pageSize)}>
							刷新
						</Button>
						<AuthGuarded auth={DOMAIN_TICKET_CLAIM_RULE_CREATE} fallback={null}>
							<Button type="primary" icon={<PlusOutlined />} onClick={() => openEditor()}>
								新建规则
							</Button>
						</AuthGuarded>
					</Space>
				)}
			>
				<Table<ClaimRuleView>
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={rows}
					pagination={{
						current: page,
						pageSize,
						total,
						showSizeChanger: true,
						showTotal: value => `共 ${value} 条`,
						onChange: (nextPage, nextPageSize) => {
							void loadData(nextPage, nextPageSize);
						},
					}}
					scroll={{ x: 900 }}
					locale={{ emptyText: <Empty description="暂无领取规则" /> }}
				/>
			</Card>

			<Modal
				title={editingRule ? "编辑领取规则" : "新建领取规则"}
				open={editorOpen}
				confirmLoading={saving}
				okText="保存"
				cancelText="取消"
				destroyOnHidden
				onCancel={closeEditor}
				onOk={() => void submitEditor()}
			>
				<Form form={form} layout="vertical">
					<Form.Item name="name" label="规则名称" rules={[{ required: true, message: "请输入规则名称" }]}>
						<Input maxLength={128} placeholder="如 自动分配给受理最少员工" />
					</Form.Item>
					<Form.Item name="enabled" label="启用" valuePropName="checked">
						<Switch />
					</Form.Item>
					<div className="grid gap-4 lg:grid-cols-2">
						<Form.Item name="matchTicketTypeId" label="匹配类型">
							<Select
								options={[
									{ value: ALL_VALUE, label: "全部类型" },
									...ticketTypes.map(type => ({ value: type.id, label: type.name })),
								]}
							/>
						</Form.Item>
						<Form.Item name="matchPriorityLevelId" label="匹配优先级">
							<Select
								options={[
									{ value: ALL_VALUE, label: "全部优先级" },
									...priorityLevels.map(item => ({ value: item.id, label: item.display_label ?? item.name })),
								]}
							/>
						</Form.Item>
					</div>
					<Form.Item name="strategy" label="领取策略">
						<Radio.Group
							options={[
								{ value: "least_loaded", label: "负载均衡" },
								{ value: "fixed", label: "指定人" },
							]}
						/>
					</Form.Item>
					<Form.Item noStyle shouldUpdate={(prev, next) => prev.strategy !== next.strategy}>
						{({ getFieldValue }) => (
							getFieldValue("strategy") === "fixed" ? (
								<Form.Item
									name="assigneeStaffAccountId"
									label="指定人"
									rules={[{ required: true, message: "请选择指定人" }]}
								>
									<Select
										showSearch
										optionFilterProp="label"
										placeholder="请选择域内成员"
										options={members.map(member => ({
											value: member.staff_account_id,
											label: memberDisplayName(member),
										}))}
									/>
								</Form.Item>
							) : null
						)}
					</Form.Item>
					<Form.Item name="graceMinutes" label="延迟分钟" extra="定时兜底延迟（阶段二生效，当前仅保存）">
						<InputNumber className="w-full" min={0} addonAfter="分钟" />
					</Form.Item>
				</Form>
			</Modal>
		</AuthGuarded>
	);
}
