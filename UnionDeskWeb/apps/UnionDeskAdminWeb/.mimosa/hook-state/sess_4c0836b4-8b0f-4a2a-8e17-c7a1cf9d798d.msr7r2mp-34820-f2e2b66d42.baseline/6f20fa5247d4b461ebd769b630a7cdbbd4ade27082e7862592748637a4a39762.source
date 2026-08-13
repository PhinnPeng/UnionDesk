import type { DomainMember, P0DomainCustomer, UpdateDomainCustomerRequest } from "@uniondesk/shared";
import {
	createDomainCustomerManual,
	createDomainCustomersFromStaff,
	fetchDomainCustomer,
	fetchDomainMembersPage,
	fetchP0DomainCustomersPage,
	resetDomainCustomerPassword,
	toErrorMessage,
	updateDomainCustomer,
	updateDomainCustomerStatus,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { TableSearchForm } from "#src/components/table-search-form";
import { useAuth } from "#src/hooks/use-auth";
import {
	DOMAIN_CUSTOMER_CREATE,
	DOMAIN_CUSTOMER_READ,
	DOMAIN_CUSTOMER_RESET_PASSWORD,
	DOMAIN_CUSTOMER_UPDATE,
	DOMAIN_CUSTOMER_UPDATE_STATUS,
} from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import {
	EditOutlined,
	EllipsisOutlined,
	EyeOutlined,
	KeyOutlined,
	PlayCircleOutlined,
	SearchOutlined,
	StopOutlined,
} from "@ant-design/icons";
import {
	Alert,
	App,
	Button,
	Card,
	Col,
	Descriptions,
	Dropdown,
	Empty,
	Form,
	Input,
	Modal,
	Radio,
	Row,
	Select,
	Space,
	Steps,
	Table,
	Tag,
	Tooltip,
	Typography,
} from "antd";
import type { FormInstance, TableColumnsType } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";

const { Text } = Typography;

type AddCustomerMode = "blank" | "staff";

interface CustomersSearchValues {
	keyword?: string;
	status?: string;
}

interface CustomersAddModalProps {
	open: boolean;
	domainId: string;
	confirmLoading?: boolean;
	onCancel: () => void;
	onSubmitBlank: (values: { display_name: string; login_name: string; phone: string; email: string }) => Promise<void>;
	onSubmitStaff: (staffAccountIds: string[]) => Promise<void>;
}

interface CustomerViewModalProps {
	open: boolean;
	domainId: string;
	customerId: string | null;
	onCancel: () => void;
}

function resolveBusinessDomainId(
	defaultBusinessDomainId: number,
	accessibleDomains: Array<{ id: number }>,
): string {
	if (defaultBusinessDomainId > 0) {
		return String(defaultBusinessDomainId);
	}
	const first = accessibleDomains[0];
	return first ? String(first.id) : "";
}

function formatSource(source?: string | null): string {
	if (source === "manual") {
		return "手动添加";
	}
	if (source === "staff_import") {
		return "员工导入";
	}
	if (source === "self_register") {
		return "自助注册";
	}
	if (source === "invitation") {
		return "邀请入域";
	}
	return source ?? "—";
}

function formatStatusTag(status?: string | null): { label: string; color: string } {
	if (status === "active") {
		return { label: "启用", color: "success" };
	}
	if (status === "disabled") {
		return { label: "禁用", color: "default" };
	}
	return { label: status ?? "—", color: "warning" };
}

function memberLabel(row: DomainMember): string {
	return row.login_name ?? row.phone ?? row.email ?? row.staff_account_id;
}

function CustomerViewModal({ open, domainId, customerId, onCancel }: CustomerViewModalProps) {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [customer, setCustomer] = useState<P0DomainCustomer | null>(null);

	useEffect(() => {
		if (!open || !domainId || !customerId) {
			setCustomer(null);
			return;
		}
		let cancelled = false;
		setLoading(true);
		void fetchDomainCustomer(domainId, customerId)
			.then((data) => {
				if (!cancelled) {
					setCustomer(data);
				}
			})
			.catch((error) => {
				if (!cancelled) {
					message.error(toErrorMessage(error));
				}
			})
			.finally(() => {
				if (!cancelled) {
					setLoading(false);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [customerId, domainId, message, open]);

	const statusTag = customer ? formatStatusTag(customer.status) : null;

	return (
		<Modal title="客户详情" open={open} footer={null} onCancel={onCancel} destroyOnHidden>
			{loading ? (
				<Empty description="加载中..." />
			) : customer ? (
				<Descriptions bordered size="small" column={1}>
					<Descriptions.Item label="展示名">{customer.display_name}</Descriptions.Item>
					<Descriptions.Item label="登录名">{customer.login_name ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="手机">{customer.phone ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="邮箱">{customer.email ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="真实姓名">{customer.real_name ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="身份证号">{customer.id_card_no ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="状态">
						{statusTag ? <Tag color={statusTag.color}>{statusTag.label}</Tag> : "—"}
					</Descriptions.Item>
					<Descriptions.Item label="来源">{formatSource(customer.source)}</Descriptions.Item>
					<Descriptions.Item label="创建时间">{customer.created_at ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="激活时间">{customer.activated_at ?? "—"}</Descriptions.Item>
				</Descriptions>
			) : (
				<Empty description="暂无数据" />
			)}
		</Modal>
	);
}

interface ResetPasswordResultModalProps {
	open: boolean;
	customerName: string;
	password: string;
	onClose: () => void;
}

interface CustomerEditModalProps {
	open: boolean;
	domainId: string;
	customerId: string | null;
	confirmLoading?: boolean;
	onCancel: () => void;
	onSubmit: (payload: UpdateDomainCustomerRequest) => Promise<void>;
}

interface CustomerEditFormValues {
	display_name: string;
	real_name?: string;
	login_name: string;
	phone: string;
	email?: string;
	id_card_no?: string;
}

function CustomerEditModal({ open, domainId, customerId, confirmLoading, onCancel, onSubmit }: CustomerEditModalProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<CustomerEditFormValues>();
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		if (!open || !domainId || !customerId) {
			return;
		}
		form.resetFields();
		let cancelled = false;
		setLoading(true);
		void fetchDomainCustomer(domainId, customerId)
			.then((data) => {
				if (!cancelled) {
					form.setFieldsValue({
						display_name: data.display_name,
						real_name: data.real_name ?? undefined,
						login_name: data.login_name ?? undefined,
						phone: data.phone ?? undefined,
						email: data.email ?? undefined,
						id_card_no: data.id_card_no ?? undefined,
					});
				}
			})
			.catch((error) => {
				if (!cancelled) {
					message.error(toErrorMessage(error));
				}
			})
			.finally(() => {
				if (!cancelled) {
					setLoading(false);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [customerId, domainId, form, message, open]);

	const handleSubmit = async () => {
		try {
			const values = await form.validateFields();
			const payload: UpdateDomainCustomerRequest = {
				display_name: values.display_name,
				phone: values.phone,
			};
			if (values.real_name) {
				payload.real_name = values.real_name;
			}
			if (values.email) {
				payload.email = values.email;
			}
			if (values.id_card_no && !values.id_card_no.includes("*")) {
				payload.id_card_no = values.id_card_no;
			}
			await onSubmit(payload);
		}
		catch {
			// 校验或提交失败
		}
	};

	return (
		<Modal
			title="编辑客户"
			open={open}
			confirmLoading={confirmLoading}
			onCancel={onCancel}
			onOk={() => void handleSubmit()}
			okText="保存"
			cancelText="取消"
			destroyOnHidden
		>
			<Form form={form} layout="vertical">
				<Row gutter={16}>
					<Col span={12}>
						<Form.Item name="display_name" label="展示名" rules={[{ required: true, message: "请输入展示名" }]}>
							<Input placeholder="客户展示名称" />
						</Form.Item>
					</Col>
					<Col span={12}>
						<Form.Item name="real_name" label="真实姓名">
							<Input placeholder="客户真实姓名" />
						</Form.Item>
					</Col>
					<Col span={12}>
						<Form.Item name="login_name" label="登录名">
							<Input disabled placeholder="登录名不可修改" />
						</Form.Item>
					</Col>
					<Col span={12}>
						<Form.Item name="phone" label="手机" rules={[{ required: true, message: "请输入手机号" }]}>
							<Input placeholder="手机号" />
						</Form.Item>
					</Col>
					<Col span={12}>
						<Form.Item name="email" label="邮箱" rules={[{ type: "email", message: "邮箱格式不正确" }]}>
							<Input placeholder="邮箱" />
						</Form.Item>
					</Col>
					<Col span={12}>
						<Form.Item name="id_card_no" label="身份证号">
							<Input placeholder="18 位身份证号（脱敏值可原样保存）" />
						</Form.Item>
					</Col>
				</Row>
			</Form>
			{loading ? <Alert type="info" showIcon message="正在加载客户资料..." className="mt-2" /> : null}
		</Modal>
	);
}

function ResetPasswordResultModal({ open, customerName, password, onClose }: ResetPasswordResultModalProps) {
	return (
		<Modal
			title="重置密码成功"
			open={open}
			okText="关闭"
			cancelButtonProps={{ style: { display: "none" } }}
			onOk={onClose}
			onCancel={onClose}
			destroyOnHidden
		>
			<Alert
				type="warning"
				showIcon
				className="mb-4"
				message={`客户「${customerName}」的新密码已生成，请复制并转交客户。`}
				description="客户使用新密码首次登录时将被强制要求修改密码，此密码仅展示一次。"
			/>
			<Space direction="vertical" className="w-full">
				<Text type="secondary">一次性密码</Text>
				<Typography.Text code copyable>{password}</Typography.Text>
			</Space>
		</Modal>
	);
}

function BlankCustomerForm({ form }: { form: FormInstance<{ display_name: string; login_name: string; phone: string; email: string }> }) {
	return (
		<Form form={form} layout="vertical">
			<Row gutter={16}>
				<Col span={12}>
					<Form.Item name="display_name" label="展示名" rules={[{ required: true, message: "请输入展示名" }]}>
						<Input placeholder="客户展示名称" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="login_name" label="登录名" rules={[{ required: true, message: "请输入登录名" }]}>
						<Input placeholder="登录账号" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="phone" label="手机" rules={[{ required: true, message: "请输入手机号" }]}>
						<Input placeholder="手机号" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item
						name="email"
						label="邮箱"
						rules={[
							{ required: true, message: "请输入邮箱" },
							{ type: "email", message: "邮箱格式不正确" },
						]}
					>
						<Input placeholder="邮箱" />
					</Form.Item>
				</Col>
			</Row>
		</Form>
	);
}

function CustomersAddModal({
	open,
	domainId,
	confirmLoading,
	onCancel,
	onSubmitBlank,
	onSubmitStaff,
}: CustomersAddModalProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<{ display_name: string; login_name: string; phone: string; email: string }>();
	const [step, setStep] = useState(0);
	const [mode, setMode] = useState<AddCustomerMode>("blank");
	const [staffKeyword, setStaffKeyword] = useState("");
	const [staffLoading, setStaffLoading] = useState(false);
	const [staffRows, setStaffRows] = useState<DomainMember[]>([]);
	const [staffTotal, setStaffTotal] = useState(0);
	const [staffPage, setStaffPage] = useState(1);
	const [staffPageSize, setStaffPageSize] = useState(10);
	const [selectedStaffIds, setSelectedStaffIds] = useState<string[]>([]);

	const reset = useCallback(() => {
		setStep(0);
		setMode("blank");
		setStaffKeyword("");
		setStaffPage(1);
		setSelectedStaffIds([]);
		form.resetFields();
	}, [form]);

	useEffect(() => {
		if (!open) {
			reset();
		}
	}, [open, reset]);

	const loadStaff = useCallback(async (page: number, pageSize: number, keyword: string) => {
		if (!domainId) {
			return;
		}
		setStaffLoading(true);
		try {
			const result = await fetchDomainMembersPage({
				domainId,
				page,
				page_size: pageSize,
				keyword: keyword.trim() || undefined,
			});
			setStaffRows(result.list);
			setStaffTotal(result.total);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setStaffLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		if (!open || step !== 1 || mode !== "staff") {
			return;
		}
		void loadStaff(staffPage, staffPageSize, staffKeyword);
	}, [loadStaff, mode, open, staffKeyword, staffPage, staffPageSize, step]);

	const staffColumns: TableColumnsType<DomainMember> = useMemo(() => [
		{ title: "姓名/账户", key: "name", render: (_, row) => memberLabel(row) },
		{ title: "手机", dataIndex: "phone", width: 130, render: v => v ?? "—" },
		{ title: "邮箱", dataIndex: "email", ellipsis: true, render: v => v ?? "—" },
		{ title: "在岗状态", dataIndex: "status", width: 100, render: v => v ?? "—" },
	], []);

	const selectedStaffRows = staffRows.filter(row => selectedStaffIds.includes(row.staff_account_id));

	const handleNext = async () => {
		if (step === 0) {
			setStep(1);
			return;
		}
		if (mode === "blank") {
			try {
				const values = await form.validateFields();
				await onSubmitBlank(values);
			}
			catch {
				// 校验或提交失败
			}
			return;
		}
		if (selectedStaffIds.length === 0) {
			message.warning("请至少选择一名员工");
			return;
		}
		await onSubmitStaff(selectedStaffIds);
	};

	return (
		<Modal
			title="添加客户"
			open={open}
			width={720}
			destroyOnHidden
			onCancel={onCancel}
			footer={(
				<Space>
					<Button onClick={onCancel}>取消</Button>
					{step > 0 ? <Button onClick={() => setStep(0)}>上一步</Button> : null}
					<Button type="primary" loading={confirmLoading} onClick={() => void handleNext()}>
						{step === 0 ? "下一步" : "确认添加"}
					</Button>
				</Space>
			)}
		>
			<Steps
				current={step}
				size="small"
				className="mb-6"
				items={[
					{ title: "选择方式" },
					{ title: mode === "blank" ? "填写信息" : "选择员工" },
				]}
			/>
			{step === 0 ? (
				<Radio.Group
					className="w-full"
					value={mode}
					onChange={event => setMode(event.target.value as AddCustomerMode)}
				>
					<Space direction="vertical" className="w-full">
						<Radio value="blank">
							<Space direction="vertical" size={0}>
								<Text strong>空白客户新增</Text>
								<Text type="secondary" className="text-xs">手工录入客户资料</Text>
							</Space>
						</Radio>
						<Radio value="staff">
							<Space direction="vertical" size={0}>
								<Text strong>选择员工新增</Text>
								<Text type="secondary" className="text-xs">从本业务域员工列表选择多名员工</Text>
							</Space>
						</Radio>
					</Space>
				</Radio.Group>
			) : null}
			{step === 1 && mode === "blank" ? (
				<BlankCustomerForm form={form} />
			) : null}
			{step === 1 && mode === "staff" ? (
				<Space direction="vertical" className="w-full" size="middle">
					<Space.Compact className="w-full">
						<Input
							placeholder="搜索员工：姓名、手机、邮箱"
							value={staffKeyword}
							onChange={e => setStaffKeyword(e.target.value)}
							onPressEnter={() => {
								setStaffPage(1);
								void loadStaff(1, staffPageSize, staffKeyword);
							}}
						/>
						<Button onClick={() => {
							setStaffPage(1);
							void loadStaff(1, staffPageSize, staffKeyword);
						}}
						>
							搜索
						</Button>
					</Space.Compact>
					<Table<DomainMember>
						rowKey="staff_account_id"
						size="small"
						loading={staffLoading}
						columns={staffColumns}
						dataSource={staffRows}
						rowSelection={{
							selectedRowKeys: selectedStaffIds,
							onChange: keys => setSelectedStaffIds(keys as string[]),
						}}
						pagination={{
							current: staffPage,
							pageSize: staffPageSize,
							total: staffTotal,
							showSizeChanger: true,
							onChange: (p, ps) => {
								setStaffPage(p);
								setStaffPageSize(ps);
							},
						}}
					/>
					{selectedStaffRows.length > 0 ? (
						<Descriptions bordered size="small" column={1} title="自动填充预览" className="mt-4">
							{selectedStaffRows.map(row => (
								<Descriptions.Item key={row.staff_account_id} label={memberLabel(row)}>
									{row.phone ?? "—"}
									{" / "}
									{row.email ?? "—"}
								</Descriptions.Item>
							))}
						</Descriptions>
					) : null}
				</Space>
			) : null}
		</Modal>
	);
}

export default function DomainCustomersPage() {
	const { message, modal } = App.useApp();
	const { hasPermission } = useAuth();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);
	const canResetPassword = hasPermission(DOMAIN_CUSTOMER_RESET_PASSWORD);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<P0DomainCustomer[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keyword, setKeyword] = useState("");
	const [statusFilter, setStatusFilter] = useState<string>("");
	const [addOpen, setAddOpen] = useState(false);
	const [editCustomerId, setEditCustomerId] = useState<string | null>(null);
	const [viewCustomerId, setViewCustomerId] = useState<string | null>(null);
	const [resetResult, setResetResult] = useState<{ customerName: string; password: string } | null>(null);

	const loadCustomers = useCallback(async (
		nextPage = page,
		nextPageSize = pageSize,
		nextKeyword = keyword,
		nextStatus = statusFilter,
	) => {
		if (!domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const result = await fetchP0DomainCustomersPage({
				domainId,
				page: nextPage,
				page_size: nextPageSize,
				keyword: nextKeyword.trim() || undefined,
				status: nextStatus || undefined,
			});
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
			setKeyword(nextKeyword);
			setStatusFilter(nextStatus);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, keyword, message, page, pageSize, statusFilter]);

	useEffect(() => {
		void loadCustomers(1, 20, "", "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- domainId 变化时初始化
	}, [domainId]);

	const handleSearch = useCallback((values: CustomersSearchValues) => {
		void loadCustomers(1, pageSize, values.keyword ?? "", values.status ?? "");
	}, [loadCustomers, pageSize]);

	const handleResetSearch = useCallback(() => {
		void loadCustomers(1, pageSize, "", "");
	}, [loadCustomers, pageSize]);

	const applyStatusChange = useCallback(async (id: string, nextStatus: "active" | "disabled") => {
		setSubmitting(true);
		try {
			await updateDomainCustomerStatus(domainId, id, nextStatus);
			message.success("已更新客户状态");
			await loadCustomers(page, pageSize, keyword, statusFilter);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	}, [domainId, keyword, loadCustomers, message, page, pageSize, statusFilter]);

	const confirmEnable = useCallback((
		id: string,
		title: string,
		content: string,
	) => {
		modal.confirm({
			title,
			content,
			okText: "确定",
			cancelText: "取消",
			onOk: () => applyStatusChange(id, "active"),
		});
	}, [applyStatusChange, modal]);

	const handleRowEnable = useCallback((row: P0DomainCustomer) => {
		confirmEnable(
			row.id,
			"确认启用客户",
			`确定将「${row.display_name}」重新启用吗？`,
		);
	}, [confirmEnable]);

	const handleResetPassword = useCallback(async (row: P0DomainCustomer) => {
		setSubmitting(true);
		try {
			const result = await resetDomainCustomerPassword(domainId, row.id);
			setResetResult({ customerName: row.display_name, password: result.password });
			message.success("密码已重置");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	}, [domainId, message]);

	const columns: TableColumnsType<P0DomainCustomer> = useMemo(() => [
		{ title: "展示名", dataIndex: "display_name", width: 180, align: "center", ellipsis: true },
		{ title: "登录名", dataIndex: "login_name", width: 140, align: "center", render: v => v ?? "—" },
		{ title: "手机", dataIndex: "phone", width: 130, align: "center", render: v => v ?? "—" },
		{ title: "邮箱", dataIndex: "email", width: 220, align: "center", ellipsis: true, render: v => v ?? "—" },
		{
			title: "状态",
			dataIndex: "status",
			width: 90,
			align: "center",
			render: status => {
				const { label, color } = formatStatusTag(status);
				return <Tag color={color}>{label}</Tag>;
			},
		},
		{
			title: "来源",
			dataIndex: "source",
			width: 110,
			align: "center",
			render: (_, row) => formatSource(row.source),
		},
		{
			title: "创建时间",
			dataIndex: "created_at",
			width: 150,
			align: "center",
			render: v => (v ? dayjs(v).format("YYYY-MM-DD HH:mm") : "—"),
		},
		{
			title: "操作",
			key: "actions",
			width: 120,
			align: "center",
			fixed: "right",
			render: (_, row) => (
				<Space size="small">
					<AuthGuarded auth={DOMAIN_CUSTOMER_UPDATE} fallback={null}>
						<Tooltip title="编辑">
							<Button type="link" size="small" icon={<EditOutlined />} onClick={() => setEditCustomerId(row.id)} />
						</Tooltip>
					</AuthGuarded>
					<AuthGuarded auth={DOMAIN_CUSTOMER_UPDATE_STATUS} fallback={null}>
						{row.status === "active" ? (
							<Tooltip title="禁用">
								<ConfirmPopover
									title="确认禁用客户"
									description={`确定将「${row.display_name}」设为禁用吗？`}
									onConfirm={() => applyStatusChange(row.id, "disabled")}
								>
									<Button type="link" size="small" icon={<StopOutlined />} />
								</ConfirmPopover>
							</Tooltip>
						) : null}
						{row.status === "disabled" ? (
							<Tooltip title="启用">
								<Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleRowEnable(row)} />
							</Tooltip>
						) : null}
					</AuthGuarded>
					<Dropdown
						trigger={["click"]}
						menu={{
							items: [
								...(canResetPassword
									? [{
										key: "reset-password",
										label: "重置密码",
										icon: <KeyOutlined />,
										onClick: () => handleResetPassword(row),
									}]
									: []),
								{
									key: "view-detail",
									label: "查看详情",
									icon: <EyeOutlined />,
									onClick: () => setViewCustomerId(row.id),
								},
							],
						}}
					>
						<Tooltip title="更多">
							<Button type="link" size="small" icon={<EllipsisOutlined />} />
						</Tooltip>
					</Dropdown>
				</Space>
			),
		},
	], [applyStatusChange, canResetPassword, handleResetPassword, handleRowEnable]);

	return (
		<BasicContent>
			<AuthGuarded auth={DOMAIN_CUSTOMER_READ} fallback={<Empty description="无权限查看客户管理" />}>
				{!domainId
					? <Empty description="暂无可用业务域" />
					: (
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
								<TableSearchForm<CustomersSearchValues>
									loading={loading}
									initialValues={{ keyword: "", status: "" }}
									onFinish={handleSearch}
									onReset={handleResetSearch}
								>
									<Form.Item name="keyword" label="关键字">
										<Input allowClear placeholder="名称、手机、邮箱" disabled={loading} />
									</Form.Item>
									<Form.Item name="status" label="状态">
										<Select
											allowClear
											placeholder="全部状态"
											options={[
												{ value: "active", label: "启用" },
												{ value: "disabled", label: "禁用" },
											]}
											disabled={loading}
										/>
									</Form.Item>
								</TableSearchForm>
							</Card>

							<Card
								bordered={false}
								title="客户列表"
								extra={(
									<AuthGuarded auth={DOMAIN_CUSTOMER_CREATE} fallback={null}>
										<Button type="primary" onClick={() => setAddOpen(true)}>添加客户</Button>
									</AuthGuarded>
								)}
							>
								<Table<P0DomainCustomer>
									rowKey="id"
									loading={loading}
									columns={columns}
									dataSource={rows}
									scroll={{ x: 1210 }}
									pagination={{
										current: page,
										pageSize,
										total,
										showSizeChanger: true,
										showTotal: t => `共 ${t} 条`,
										onChange: (nextPage, nextPageSize) => {
											void loadCustomers(nextPage, nextPageSize, keyword, statusFilter);
										},
									}}
									locale={{ emptyText: <Empty description="暂无客户" /> }}
								/>
							</Card>

							<CustomersAddModal
								open={addOpen}
								domainId={domainId}
								confirmLoading={submitting}
								onCancel={() => setAddOpen(false)}
								onSubmitBlank={async values => {
									setSubmitting(true);
									try {
										await createDomainCustomerManual(domainId, values);
										message.success("已添加客户");
										setAddOpen(false);
										await loadCustomers(1, pageSize, keyword, statusFilter);
									}
									catch (error) {
										message.error(toErrorMessage(error));
									}
									finally {
										setSubmitting(false);
									}
								}}
								onSubmitStaff={async staffAccountIds => {
									setSubmitting(true);
									try {
										const result = await createDomainCustomersFromStaff(domainId, {
											staff_account_ids: staffAccountIds,
										});
										if (result.added > 0 && result.skipped > 0) {
											message.success(`成功添加 ${result.added} 名，跳过 ${result.skipped} 名`);
										}
										else {
											message.success(`已添加 ${result.added} 名客户`);
										}
										setAddOpen(false);
										await loadCustomers(1, pageSize, keyword, statusFilter);
									}
									catch (error) {
										message.error(toErrorMessage(error));
									}
									finally {
										setSubmitting(false);
									}
								}}
							/>
							<CustomerViewModal
								open={viewCustomerId != null}
								domainId={domainId}
								customerId={viewCustomerId}
								onCancel={() => setViewCustomerId(null)}
							/>
							<CustomerEditModal
								open={editCustomerId != null}
								domainId={domainId}
								customerId={editCustomerId}
								confirmLoading={submitting}
								onCancel={() => setEditCustomerId(null)}
								onSubmit={async payload => {
									if (!editCustomerId) {
										return;
									}
									setSubmitting(true);
									try {
										await updateDomainCustomer(domainId, editCustomerId, payload);
										message.success("已更新客户资料");
										setEditCustomerId(null);
										await loadCustomers(page, pageSize, keyword, statusFilter);
									}
									catch (error) {
										message.error(toErrorMessage(error));
									}
									finally {
										setSubmitting(false);
									}
								}}
							/>
							<ResetPasswordResultModal
								open={resetResult != null}
								customerName={resetResult?.customerName ?? ""}
								password={resetResult?.password ?? ""}
								onClose={() => setResetResult(null)}
							/>
						</div>
					)}
			</AuthGuarded>
		</BasicContent>
	);
}
