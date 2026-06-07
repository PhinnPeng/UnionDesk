import type { DomainMember, P0DomainCustomer } from "@uniondesk/shared";
import {
	createDomainCustomerManual,
	createDomainCustomersFromStaff,
	fetchDomainCustomer,
	fetchDomainMembersPage,
	fetchP0DomainCustomersPage,
	toErrorMessage,
	updateDomainCustomerStatus,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";

import {
	PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE,
	PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS,
} from "../../platform-domain-permissions";

import { DOMAIN_CUSTOMER_READ_PERMISSION } from "./detail-shared";

import {
	App,
	Button,
	Card,
	Col,
	Descriptions,
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
	Typography,
} from "antd";
import type { FormInstance, TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

const { Title, Text } = Typography;

type AddCustomerMode = "blank" | "staff";

export interface DetailCustomersProps {
	domainId: string;
}

interface CustomersAddModalProps {
	open: boolean;
	domainId: string;
	confirmLoading?: boolean;
	onCancel: () => void;
	onSubmitBlank: (values: { display_name: string; login_name: string; phone: string; email: string }) => Promise<void>;
	onSubmitStaff: (staffAccountId: string) => Promise<void>;
}

interface CustomersBatchStaffModalProps {
	open: boolean;
	domainId: string;
	confirmLoading?: boolean;
	onCancel: () => void;
	onSubmit: (staffAccountIds: string[]) => Promise<void>;
}

interface CustomerViewModalProps {
	open: boolean;
	domainId: string;
	customerId: string | null;
	onCancel: () => void;
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
		<Modal title="查看客户" open={open} footer={null} onCancel={onCancel} destroyOnHidden>
			{loading ? (
				<Empty description="加载中..." />
			) : customer ? (
				<Descriptions bordered size="small" column={1}>
					<Descriptions.Item label="展示名">{customer.display_name}</Descriptions.Item>
					<Descriptions.Item label="登录名">{customer.login_name ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="手机">{customer.phone ?? "—"}</Descriptions.Item>
					<Descriptions.Item label="邮箱">{customer.email ?? "—"}</Descriptions.Item>
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
	const [selectedStaffId, setSelectedStaffId] = useState<string | null>(null);

	const reset = useCallback(() => {
		setStep(0);
		setMode("blank");
		setStaffKeyword("");
		setStaffPage(1);
		setSelectedStaffId(null);
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

	const selectedStaff = staffRows.find(row => row.staff_account_id === selectedStaffId)
		?? (selectedStaffId ? { staff_account_id: selectedStaffId } as DomainMember : undefined);

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
		if (!selectedStaffId) {
			message.warning("请选择一名员工");
			return;
		}
		await onSubmitStaff(selectedStaffId);
	};

	return (
		<Modal
			title="添加客户"
			open={open}
			width={720}
			destroyOnClose
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
								<Text type="secondary" className="text-xs">从本业务域员工列表选择一名员工</Text>
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
							type: "radio",
							selectedRowKeys: selectedStaffId ? [selectedStaffId] : [],
							onChange: keys => setSelectedStaffId(String(keys[0] ?? "")),
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
					{selectedStaff ? (
						<Descriptions bordered size="small" column={1} title="自动填充预览">
							<Descriptions.Item label="账户">{memberLabel(selectedStaff)}</Descriptions.Item>
							<Descriptions.Item label="手机">{selectedStaff.phone ?? "—"}</Descriptions.Item>
							<Descriptions.Item label="邮箱">{selectedStaff.email ?? "—"}</Descriptions.Item>
						</Descriptions>
					) : null}
				</Space>
			) : null}
		</Modal>
	);
}

function CustomersBatchStaffModal({
	open,
	domainId,
	confirmLoading,
	onCancel,
	onSubmit,
}: CustomersBatchStaffModalProps) {
	const { message } = App.useApp();
	const [keyword, setKeyword] = useState("");
	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState<DomainMember[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [selectedKeys, setSelectedKeys] = useState<string[]>([]);

	const loadStaff = useCallback(async (nextPage: number, nextPageSize: number, nextKeyword: string) => {
		if (!domainId) {
			return;
		}
		setLoading(true);
		try {
			const result = await fetchDomainMembersPage({
				domainId,
				page: nextPage,
				page_size: nextPageSize,
				keyword: nextKeyword.trim() || undefined,
			});
			setRows(result.list);
			setTotal(result.total);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		if (!open) {
			setKeyword("");
			setPage(1);
			setSelectedKeys([]);
			return;
		}
		void loadStaff(page, pageSize, keyword);
	}, [open, loadStaff, page, pageSize, keyword]);

	const columns: TableColumnsType<DomainMember> = useMemo(() => [
		{ title: "姓名/账户", key: "name", render: (_, row) => memberLabel(row) },
		{ title: "手机", dataIndex: "phone", width: 130, render: v => v ?? "—" },
		{ title: "邮箱", dataIndex: "email", ellipsis: true, render: v => v ?? "—" },
		{ title: "在岗状态", dataIndex: "status", width: 100, render: v => v ?? "—" },
	], []);

	const previewRows = rows.filter(row => selectedKeys.includes(row.staff_account_id));

	return (
		<Modal
			title="批量添加员工"
			open={open}
			width={720}
			destroyOnClose
			onCancel={onCancel}
			okText="确认添加"
			cancelText="取消"
			confirmLoading={confirmLoading}
			onOk={() => {
				if (selectedKeys.length === 0) {
					message.warning("请至少选择一名员工");
					return;
				}
				void onSubmit(selectedKeys);
			}}
		>
			<Text type="secondary" className="mb-3 block text-xs">多选域内员工，一次提交为客户</Text>
			<Space.Compact className="mb-3 w-full">
				<Input
					placeholder="搜索员工：姓名、手机、邮箱"
					value={keyword}
					onChange={e => setKeyword(e.target.value)}
					onPressEnter={() => {
						setPage(1);
						void loadStaff(1, pageSize, keyword);
					}}
				/>
				<Button onClick={() => {
					setPage(1);
					void loadStaff(1, pageSize, keyword);
				}}
				>
					搜索
				</Button>
			</Space.Compact>
			<Table<DomainMember>
				rowKey="staff_account_id"
				size="small"
				loading={loading}
				columns={columns}
				dataSource={rows}
				rowSelection={{
					selectedRowKeys: selectedKeys,
					onChange: keys => setSelectedKeys(keys as string[]),
				}}
				pagination={{
					current: page,
					pageSize,
					total,
					showSizeChanger: true,
					onChange: (p, ps) => {
						setPage(p);
						setPageSize(ps);
					},
				}}
			/>
			{previewRows.length > 0 ? (
				<Descriptions bordered size="small" column={1} title="自动填充预览" className="mt-4">
					{previewRows.map(row => (
						<Descriptions.Item key={row.staff_account_id} label={memberLabel(row)}>
							{row.phone ?? "—"}
							{" / "}
							{row.email ?? "—"}
						</Descriptions.Item>
					))}
				</Descriptions>
			) : null}
		</Modal>
	);
}

export function DetailCustomers({ domainId }: DetailCustomersProps) {
	const { message, modal } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<P0DomainCustomer[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keywordInput, setKeywordInput] = useState("");
	const [keyword, setKeyword] = useState("");
	const [statusFilter, setStatusFilter] = useState<string>("");
	const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
	const [addOpen, setAddOpen] = useState(false);
	const [batchStaffOpen, setBatchStaffOpen] = useState(false);
	const [viewCustomerId, setViewCustomerId] = useState<string | null>(null);

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

	const applyStatusChange = useCallback(async (ids: string[], nextStatus: "active" | "disabled") => {
		setSubmitting(true);
		try {
			for (const id of ids) {
				await updateDomainCustomerStatus(domainId, id, nextStatus);
			}
			message.success(`已更新 ${ids.length} 名客户状态`);
			setSelectedRowKeys([]);
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
		ids: string[],
		title: string,
		content: string,
	) => {
		modal.confirm({
			title,
			content,
			okText: "确定",
			cancelText: "取消",
			onOk: () => applyStatusChange(ids, "active"),
		});
	}, [applyStatusChange, modal]);

	const handleRowEnable = useCallback((row: P0DomainCustomer) => {
		confirmEnable(
			[row.id],
			"确认启用客户",
			`确定将「${row.display_name}」重新启用吗？`,
		);
	}, [confirmEnable]);

	const columns: TableColumnsType<P0DomainCustomer> = useMemo(() => [
		{ title: "展示名", dataIndex: "display_name", ellipsis: true },
		{ title: "登录名", dataIndex: "login_name", width: 120, render: v => v ?? "—" },
		{ title: "手机", dataIndex: "phone", width: 130, render: v => v ?? "—" },
		{ title: "邮箱", dataIndex: "email", ellipsis: true, render: v => v ?? "—" },
		{
			title: "状态",
			dataIndex: "status",
			width: 90,
			render: status => {
				const { label, color } = formatStatusTag(status);
				return <Tag color={color}>{label}</Tag>;
			},
		},
		{
			title: "来源",
			dataIndex: "source",
			width: 110,
			render: (_, row) => formatSource(row.source),
		},
		{ title: "创建时间", dataIndex: "created_at", width: 170, render: v => v ?? "—" },
		{
			title: "操作",
			key: "actions",
			width: 140,
			fixed: "right",
			render: (_, row) => (
				<Space size="small">
					<Button type="link" size="small" onClick={() => setViewCustomerId(row.id)}>查看</Button>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS} fallback={null}>
						{row.status === "active" ? (
							<ConfirmPopover
								title="确认禁用客户"
								description={`确定将「${row.display_name}」设为禁用吗？`}
								onConfirm={() => applyStatusChange([row.id], "disabled")}
							>
								<Button type="link" size="small">禁用</Button>
							</ConfirmPopover>
						) : null}
						{row.status === "disabled" ? (
							<Button type="link" size="small" onClick={() => handleRowEnable(row)}>启用</Button>
						) : null}
					</AuthGuarded>
				</Space>
			),
		},
	], [applyStatusChange, handleRowEnable]);

	return (
		<AuthGuarded auth={DOMAIN_CUSTOMER_READ_PERMISSION} fallback={<Empty description="无权限查看客户管理" />}>
			<div>
				<Title level={5} className="!mb-4">客户管理</Title>
				<Card className="!mb-4">
					<Space wrap>
						<Input.Search
							className="w-64"
							placeholder="名称、手机、邮箱"
							allowClear
							value={keywordInput}
							onChange={e => setKeywordInput(e.target.value)}
							onSearch={value => void loadCustomers(1, pageSize, value, statusFilter)}
						/>
						<Select
							style={{ width: 120 }}
							value={statusFilter}
							options={[
								{ value: "", label: "全部状态" },
								{ value: "active", label: "启用" },
								{ value: "disabled", label: "禁用" },
							]}
							onChange={value => setStatusFilter(value)}
						/>
						<Button onClick={() => void loadCustomers(1, pageSize, keywordInput, statusFilter)}>查询</Button>
						<Button onClick={() => {
							setKeywordInput("");
							setStatusFilter("");
							void loadCustomers(1, pageSize, "", "");
						}}
						>
							重置
						</Button>
					</Space>
				</Card>
				<div className="mb-4 flex flex-wrap items-center justify-between gap-2">
					<Space wrap>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE} fallback={null}>
							<Button type="primary" onClick={() => setAddOpen(true)}>添加客户</Button>
							<Button onClick={() => setBatchStaffOpen(true)}>批量添加员工</Button>
						</AuthGuarded>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS} fallback={null}>
							<ConfirmPopover
								title="确认批量禁用"
								description={`确定将选中的 ${selectedRowKeys.length} 名客户设为禁用吗？`}
								onConfirm={() => applyStatusChange(selectedRowKeys, "disabled")}
							>
								<Button disabled={selectedRowKeys.length === 0}>批量禁用</Button>
							</ConfirmPopover>
							<Button
								disabled={selectedRowKeys.length === 0}
								onClick={() => confirmEnable(
									selectedRowKeys,
									"确认批量启用",
									`确定将选中的 ${selectedRowKeys.length} 名客户重新启用吗？`,
								)}
							>
								批量启用
							</Button>
						</AuthGuarded>
					</Space>
					<Text type="secondary" className="text-xs">
						{selectedRowKeys.length > 0
							? `已选 ${selectedRowKeys.length} 项 · 共 ${total} 条`
							: `共 ${total} 条`}
					</Text>
				</div>
				<Table<P0DomainCustomer>
				rowKey="id"
				loading={loading}
				columns={columns}
				dataSource={rows}
				scroll={{ x: 960 }}
				rowSelection={{
					selectedRowKeys,
					onChange: keys => setSelectedRowKeys(keys as string[]),
				}}
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
				onSubmitStaff={async staffAccountId => {
					setSubmitting(true);
					try {
						const result = await createDomainCustomersFromStaff(domainId, {
							staff_account_ids: [staffAccountId],
						});
						message.success(`已添加 ${result.added} 名客户`);
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
			<CustomersBatchStaffModal
				open={batchStaffOpen}
				domainId={domainId}
				confirmLoading={submitting}
				onCancel={() => setBatchStaffOpen(false)}
				onSubmit={async staffAccountIds => {
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
						setBatchStaffOpen(false);
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
			</div>
		</AuthGuarded>
	);
}
