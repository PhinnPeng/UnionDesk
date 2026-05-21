import type { AdminDomain, CreateAdminDomainPayload, P0RegistrationPolicy, P0VisibilityPolicyCode } from "@uniondesk/shared";
import {
	P0_STEP_UP_OPERATION,
	createAdminDomain,
	deleteAdminDomain,
	fetchAdminDomainsPage,
	updateAdminDomain,
	toErrorMessage,
} from "@uniondesk/shared";

import StepUpModal from "#src/components/step-up-modal";
import { BasicContent } from "#src/components/basic-content";
import { App, Alert, Button, Card, Checkbox, Form, Input, Modal, Select, Space, Typography, List, Empty, Pagination } from "antd";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router";

import { SearchPanel, type DomainSearchValues } from "./components/search-panel";
import { DomainCard } from "./components/domain-card";

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
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	
	const [rows, setRows] = useState<AdminDomain[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [searchValues, setSearchValues] = useState<DomainSearchValues>({});

	const [createOpen, setCreateOpen] = useState(false);
	const [editRow, setEditRow] = useState<AdminDomain | null>(null);
	const [stepUpOpen, setStepUpOpen] = useState(false);
	const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
	
	const [createForm] = Form.useForm<CreateAdminDomainPayload>();
	const [editForm] = Form.useForm<CreateAdminDomainPayload>();

	const reload = useCallback(async (p = page, ps = pageSize, sv = searchValues) => {
		setLoading(true);
		try {
			const res = await fetchAdminDomainsPage({
				page: p,
				page_size: ps,
				keyword: sv.keyword,
				created_from: sv.createdRange?.[0]?.format("YYYY-MM-DDTHH:mm:ss"),
				created_to: sv.createdRange?.[1]?.format("YYYY-MM-DDTHH:mm:ss"),
			});
			setRows(res.list);
			setTotal(res.total);
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
			setLoading(false);
		}
	}, [message, page, pageSize, searchValues]);

	useEffect(() => {
		void reload();
	}, [reload]);

	const handleSearch = (values: DomainSearchValues) => {
		setSearchValues(values);
		setPage(1);
		void reload(1, pageSize, values);
	};

	const handleReset = () => {
		setSearchValues({});
		setPage(1);
		void reload(1, pageSize, {});
	};

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
		const payload: CreateAdminDomainPayload = {
			name: v.name,
			code: v.code,
			visibility_policy_codes: normalizeVisibility(v.visibility_policy_codes ?? ["public"]),
			registration_policy: v.registration_policy,
		};
		try {
			await createAdminDomain(payload);
			message.success("已创建业务域");
			setCreateOpen(false);
			await reload();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	const openEdit = (row: AdminDomain) => {
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
			await updateAdminDomain(editRow.id, {
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

	const handleManage = (domain: AdminDomain) => {
		navigate(`/platform/domains/${domain.id}`);
	};

	const handleToggleStatus = (domain: AdminDomain) => {
		const isEnabled = domain.status === "1" || domain.status === "active" || domain.status === "enabled";
		const nextStatus = isEnabled ? 0 : 1;
		const actionText = isEnabled ? "禁用" : "启用";

		modal.confirm({
			title: `${actionText}业务域`,
			content: `确认${actionText}业务域“${domain.name}”吗？`,
			onOk: async () => {
				try {
					await updateAdminDomain(domain.id, { status: nextStatus });
					message.success(`已${actionText}业务域`);
					await reload();
				} catch (e) {
					message.error(toErrorMessage(e));
				}
			},
		});
	};

	const requestDelete = (domain: AdminDomain) => {
		setPendingDeleteId(domain.id);
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
			await deleteAdminDomain(id, { stepUpToken: token });
			message.success("业务域已删除");
			await reload();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	return (
		<BasicContent className="h-full bg-colorBgLayout overflow-auto">
			<SearchPanel onSearch={handleSearch} onReset={handleReset} loading={loading} />

			<Card
				title="业务域管理"
				extra={(
					<Space>
						<Button icon={<ReloadOutlined />} onClick={() => reload()}>刷新</Button>
						<Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建业务域</Button>
					</Space>
				)}
				bordered={false}
			>
				<List
					grid={{
						gutter: 16,
						xs: 1,
						sm: 2,
						md: 2,
						lg: 3,
						xl: 3,
						xxl: 4,
					}}
					loading={loading}
					dataSource={rows}
					locale={{ emptyText: <Empty description="暂无业务域数据" /> }}
					renderItem={item => (
						<List.Item>
							<DomainCard
								domain={item}
								onEdit={openEdit}
								onManage={handleManage}
								onToggleStatus={handleToggleStatus}
								onDelete={requestDelete}
								registrationOptions={registrationOptions}
							/>
						</List.Item>
					)}
				/>
				
				<div className="mt-4 flex justify-end">
					<Pagination
						current={page}
						pageSize={pageSize}
						total={total}
						onChange={(p, ps) => {
							setPage(p);
							setPageSize(ps);
							void reload(p, ps);
						}}
						showSizeChanger
						showTotal={t => `共 ${t} 条`}
					/>
				</div>
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
						label="可见策略编码"
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
						短码（code）创建后通常不可改。
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
				title="安全验证"
				description="删除业务域为高危操作，请验证身份。"
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
