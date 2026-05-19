import type { P0AdminDomain, P0CreateAdminDomainPayload, P0RegistrationPolicy, P0VisibilityPolicyCode } from "@uniondesk/shared";
import {
	P0_STEP_UP_OPERATION,
	createP0AdminDomain,
	deleteP0AdminDomain,
	fetchP0AdminDomainsPage,
	updateP0AdminDomain,
	toErrorMessage,
} from "@uniondesk/shared";

import StepUpModal from "#src/components/step-up-modal";
import { BasicContent } from "#src/components/basic-content";
import { App, Alert, Button, Card, Checkbox, Form, Input, Modal, Select, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useState } from "react";

const registrationOptions: { value: P0RegistrationPolicy; label: string }[] = [
	{ value: "open", label: "开放注册" },
	{ value: "invitation_only", label: "仅邀请" },
	{ value: "admin_only", label: "仅管理员添加" },
];

const visibilityOptions: { value: P0VisibilityPolicyCode; label: string }[] = [
	{ value: "public", label: "public" },
	{ value: "domain_customer_only", label: "domain_customer_only" },
	{ value: "channel_only", label: "channel_only" },
];

function normalizeVisibility(codes: P0VisibilityPolicyCode[]): P0VisibilityPolicyCode[] {
	if (codes.includes("public")) {
		return ["public"];
	}
	return codes.length > 0 ? codes : ["public"];
}

export default function PlatformBusinessDomains() {
	const { message } = App.useApp();
	const [rows, setRows] = useState<P0AdminDomain[]>([]);
	const [loading, setLoading] = useState(false);
	const [createOpen, setCreateOpen] = useState(false);
	const [editRow, setEditRow] = useState<P0AdminDomain | null>(null);
	const [stepUpOpen, setStepUpOpen] = useState(false);
	const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
	const [createForm] = Form.useForm<P0CreateAdminDomainPayload>();
	const [editForm] = Form.useForm<P0CreateAdminDomainPayload>();

	const reload = useCallback(async () => {
		setLoading(true);
		try {
			const page = await fetchP0AdminDomainsPage({ page: 1, page_size: 100 });
			setRows(page.list);
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
			setLoading(false);
		}
	}, [message]);

	useEffect(() => {
		void reload();
	}, [reload]);

	const openCreate = () => {
		createForm.setFieldsValue({
			name: "",
			code: "",
			visibility_policy_codes: ["public"],
			registration_policy: "open",
		});
		setCreateOpen(true);
	};

	const submitCreate = async () => {
		const v = await createForm.validateFields().catch(() => null);
		if (!v) {
			return;
		}
		const payload: P0CreateAdminDomainPayload = {
			name: v.name,
			code: v.code,
			visibility_policy_codes: normalizeVisibility(v.visibility_policy_codes ?? ["public"]),
			registration_policy: v.registration_policy,
		};
		try {
			await createP0AdminDomain(payload);
			message.success("已创建业务域");
			setCreateOpen(false);
			await reload();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	const openEdit = (row: P0AdminDomain) => {
		setEditRow(row);
		editForm.setFieldsValue({
			name: row.name,
			code: row.code,
			logo: row.logo ?? undefined,
			visibility_policy_codes: row.visibility_policy_codes,
			registration_policy: row.registration_policy,
		});
	};

	const submitEdit = async () => {
		if (!editRow) {
			return;
		}
		const v = await editForm.validateFields().catch(() => null);
		if (!v) {
			return;
		}
		try {
			await updateP0AdminDomain(editRow.id, {
				name: v.name,
				logo: v.logo,
				visibility_policy_codes: normalizeVisibility(v.visibility_policy_codes ?? ["public"]),
				registration_policy: v.registration_policy,
			});
			message.success("已更新业务域");
			setEditRow(null);
			await reload();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	const requestDelete = (id: string) => {
		setPendingDeleteId(id);
		setStepUpOpen(true);
	};

	const afterStepUp = async (token: string) => {
		setStepUpOpen(false);
		if (!pendingDeleteId) {
			return;
		}
		const id = pendingDeleteId;
		setPendingDeleteId(null);
		try {
			await deleteP0AdminDomain(id, { stepUpToken: token });
			message.success("已发起软删除（若后端未开放接口将失败）");
			await reload();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	const columns: TableColumnsType<P0AdminDomain> = [
		{ title: "名称", dataIndex: "name", width: 200 },
		{ title: "短码", dataIndex: "code", width: 140 },
		{
			title: "可见策略",
			dataIndex: "visibility_policy_codes",
			render: (codes: P0VisibilityPolicyCode[]) => (
				<Space size={4} wrap>
					{codes.map(c => <Tag key={c}>{c}</Tag>)}
				</Space>
			),
		},
		{
			title: "注册策略",
			dataIndex: "registration_policy",
			width: 140,
			render: (p: P0RegistrationPolicy) => registrationOptions.find(o => o.value === p)?.label ?? p,
		},
		{ title: "状态", dataIndex: "status", width: 100, render: s => s ?? "-" },
		{
			title: "操作",
			key: "actions",
			width: 200,
			render: (_, row) => (
				<Space>
					<Button type="link" size="small" onClick={() => openEdit(row)}>编辑</Button>
					<Button type="link" size="small" danger onClick={() => requestDelete(row.id)}>删除</Button>
				</Space>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card title="业务域管理（P0）" extra={<Button type="primary" onClick={openCreate}>新建业务域</Button>}>
				<Alert
					type="info"
					showIcon
					className="mb-4"
					message="接口对齐说明"
					description="列表优先请求 `GET /api/v1/admin/domains`；若后端尚未发布该接口，将回退到演示 `GET /api/v1/domains` 只读数据。创建 / 更新 / 删除依赖完整 P0 管理域接口与 step-up。"
				/>
				<Table<P0AdminDomain>
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={rows}
					pagination={false}
				/>
			</Card>

			<Modal
				title="新建业务域"
				open={createOpen}
				onOk={submitCreate}
				onCancel={() => setCreateOpen(false)}
				okText="创建"
				destroyOnClose
			>
				<Form form={createForm} layout="vertical">
					<Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
						<Input />
					</Form.Item>
					<Form.Item name="code" label="短码" rules={[{ required: true, message: "请输入短码" }]}>
						<Input />
					</Form.Item>
					<Form.Item name="registration_policy" label="注册策略" rules={[{ required: true }]}>
						<Select options={registrationOptions} />
					</Form.Item>
					<Form.Item
						name="visibility_policy_codes"
						label="可见策略编码（public 与其他互斥）"
						rules={[{ required: true }]}
					>
						<Checkbox.Group
							options={visibilityOptions.map(o => ({ label: o.label, value: o.value }))}
							onChange={(vals) => {
								const next = vals as P0VisibilityPolicyCode[];
								if (next.includes("public") && next.length > 1) {
									createForm.setFieldValue("visibility_policy_codes", ["public"]);
								}
							}}
						/>
					</Form.Item>
				</Form>
			</Modal>

			<Modal
				title="编辑业务域"
				open={!!editRow}
				onOk={submitEdit}
				onCancel={() => setEditRow(null)}
				okText="保存"
				destroyOnClose
			>
				<Form form={editForm} layout="vertical">
					<Form.Item name="name" label="名称" rules={[{ required: true }]}>
						<Input />
					</Form.Item>
					<Typography.Paragraph type="secondary" className="!text-sm">
						短码（code）创建后通常不可改，此处不展示编辑项。
					</Typography.Paragraph>
					<Form.Item name="registration_policy" label="注册策略" rules={[{ required: true }]}>
						<Select options={registrationOptions} />
					</Form.Item>
					<Form.Item name="visibility_policy_codes" label="可见策略" rules={[{ required: true }]}>
						<Checkbox.Group options={visibilityOptions.map(o => ({ label: o.label, value: o.value }))} />
					</Form.Item>
				</Form>
			</Modal>

			<StepUpModal
				open={stepUpOpen}
				title="删除业务域前二次验证"
				description="删除业务域为一次性敏感操作，需校验密码并携带 step-up 令牌。"
				operationCode={P0_STEP_UP_OPERATION.DELETE_BUSINESS_DOMAIN}
				onCancel={() => {
					setStepUpOpen(false);
					setPendingDeleteId(null);
				}}
				onVerified={afterStepUp}
			/>
		</BasicContent>
	);
}
