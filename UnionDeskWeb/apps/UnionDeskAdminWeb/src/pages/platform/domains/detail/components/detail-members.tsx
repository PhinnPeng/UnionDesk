import type { DomainMember, DomainRole, DomainStaffCandidate } from "@uniondesk/shared";
import {
	createDomainMember,
	createDomainMemberWithStaff,
	deleteDomainMember,
	fetchDomainMembersPage,
	fetchDomainStaffCandidates,
	fetchPlatformDomainRoles,
	toErrorMessage,
	updateDomainMemberRoles,
	updateDomainMemberStatus,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { TableSearchForm } from "#src/components/table-search-form";

import {
	DOMAIN_MEMBER_CREATE,
	DOMAIN_MEMBER_DELETE,
	DOMAIN_MEMBER_READ,
	DOMAIN_MEMBER_UPDATE_ROLES,
	DOMAIN_MEMBER_UPDATE_STATUS,
} from "../../platform-domain-permissions";

import { SearchOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Col,
	DatePicker,
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
import dayjs, { type Dayjs } from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";

import styles from "./detail-members.module.less";

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

type AddMemberMode = "staff" | "new_staff";

interface MemberSearchValues {
	keyword?: string;
	status?: string;
	createdRange?: [Dayjs | string | Date | null, Dayjs | string | Date | null] | null;
}

const EMPTY_MEMBER_SEARCH: MemberSearchValues = {
	keyword: "",
	status: "",
	createdRange: null,
};

function toSearchDayjs(value: Dayjs | string | Date | null | undefined): Dayjs | null {
	if (value == null || value === "") {
		return null;
	}
	const parsed = dayjs(value);
	return parsed.isValid() ? parsed : null;
}

function normalizeCreatedRange(
	range?: MemberSearchValues["createdRange"],
): [Dayjs, Dayjs] | null {
	if (!range) {
		return null;
	}
	const start = toSearchDayjs(range[0]);
	const end = toSearchDayjs(range[1]);
	if (!start || !end) {
		return null;
	}
	return [start, end];
}

function normalizeMemberSearch(values: MemberSearchValues): MemberSearchValues {
	const createdRange = normalizeCreatedRange(values.createdRange);
	return {
		keyword: values.keyword ?? "",
		status: values.status ?? "",
		createdRange: createdRange ? [createdRange[0], createdRange[1]] : null,
	};
}

export interface DetailMembersProps {
	domainId: string;
}

interface MembersAddModalProps {
	open: boolean;
	domainId: string;
	roleOptions: { label: string; value: string }[];
	confirmLoading?: boolean;
	onCancel: () => void;
	onSubmitStaff: (staffAccountId: string, roleIds: string[]) => Promise<void>;
	onSubmitNewStaff: (values: {
		username: string;
		real_name?: string;
		nickname?: string;
		phone: string;
		email?: string;
		password: string;
		role_ids: string[];
	}) => Promise<void>;
}

interface MembersEditRolesModalProps {
	open: boolean;
	member: DomainMember | null;
	roleOptions: { label: string; value: string }[];
	confirmLoading?: boolean;
	onCancel: () => void;
	onSubmit: (roleIds: string[]) => Promise<void>;
}

interface MembersBatchStatusModalProps {
	open: boolean;
	selectedCount: number;
	confirmLoading?: boolean;
	onCancel: () => void;
	onConfirm: (status: "active" | "disabled") => void;
}

function formatDomainRoleLabel(role: { code?: string | null; name?: string | null }): string {
	if (role.code === "super_admin") {
		return "所有人";
	}
	return role.name ?? role.code ?? "—";
}

function formatMemberStatus(status?: string | null): { label: string; color: string } {
	if (status === "active" || status === "1" || status === "enabled") {
		return { label: "启用", color: "success" };
	}
	if (status === "disabled" || status === "0") {
		return { label: "禁用", color: "default" };
	}
	return { label: status ?? "—", color: "default" };
}

function memberDisplayName(row: Pick<DomainMember, "real_name" | "nickname" | "username" | "id">): string {
	return row.real_name ?? row.nickname ?? row.username ?? row.id;
}

function candidateLabel(row: DomainStaffCandidate): string {
	return row.real_name ?? row.nickname ?? row.username ?? row.id;
}

function NewStaffForm({ form, roleOptions }: {
	form: FormInstance<{
		username: string;
		real_name?: string;
		nickname?: string;
		phone: string;
		email?: string;
		password: string;
		role_ids: string[];
	}>;
	roleOptions: { label: string; value: string }[];
}) {
	return (
		<Form form={form} layout="vertical">
			<Row gutter={16}>
				<Col span={12}>
					<Form.Item name="username" label="登录账号" rules={[{ required: true, message: "请输入登录账号" }]}>
						<Input placeholder="登录账号" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="phone" label="手机号" rules={[{ required: true, message: "请输入手机号" }]}>
						<Input placeholder="手机号" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="real_name" label="真实姓名">
						<Input placeholder="真实姓名" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="nickname" label="昵称">
						<Input placeholder="昵称" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="email" label="邮箱" rules={[{ type: "email", message: "邮箱格式不正确" }]}>
						<Input placeholder="邮箱" />
					</Form.Item>
				</Col>
				<Col span={12}>
					<Form.Item name="password" label="初始密码" rules={[{ required: true, message: "请输入初始密码" }]}>
						<Input.Password placeholder="初始密码" />
					</Form.Item>
				</Col>
				<Col span={24}>
					<Form.Item name="role_ids" label="角色" rules={[{ required: true, message: "请选择角色" }]}>
						<Select mode="multiple" placeholder="选择角色" options={roleOptions} />
					</Form.Item>
				</Col>
			</Row>
		</Form>
	);
}

function MembersAddModal({
	open,
	domainId,
	roleOptions,
	confirmLoading,
	onCancel,
	onSubmitStaff,
	onSubmitNewStaff,
}: MembersAddModalProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<{
		username: string;
		real_name?: string;
		nickname?: string;
		phone: string;
		email?: string;
		password: string;
		role_ids: string[];
	}>();
	const [step, setStep] = useState(0);
	const [mode, setMode] = useState<AddMemberMode>("staff");
	const [staffKeyword, setStaffKeyword] = useState("");
	const [staffLoading, setStaffLoading] = useState(false);
	const [staffRows, setStaffRows] = useState<DomainStaffCandidate[]>([]);
	const [staffTotal, setStaffTotal] = useState(0);
	const [staffPage, setStaffPage] = useState(1);
	const [staffPageSize, setStaffPageSize] = useState(10);
	const [selectedStaffId, setSelectedStaffId] = useState<string | null>(null);
	const [selectedRoleIds, setSelectedRoleIds] = useState<string[]>([]);

	const reset = useCallback(() => {
		setStep(0);
		setMode("staff");
		setStaffKeyword("");
		setStaffPage(1);
		setSelectedStaffId(null);
		setSelectedRoleIds([]);
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
			const result = await fetchDomainStaffCandidates({
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

	const staffColumns: TableColumnsType<DomainStaffCandidate> = useMemo(() => [
		{ title: "姓名/账号", key: "name", render: (_, row) => candidateLabel(row) },
		{ title: "手机", dataIndex: "phone", width: 130, render: v => v ?? "—" },
		{ title: "邮箱", dataIndex: "email", ellipsis: true, render: v => v ?? "—" },
	], []);

	const handleNext = async () => {
		if (step === 0) {
			setStep(1);
			return;
		}
		if (mode === "new_staff") {
			try {
				const values = await form.validateFields();
				await onSubmitNewStaff(values);
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
		if (selectedRoleIds.length === 0) {
			message.warning("请选择角色");
			return;
		}
		await onSubmitStaff(selectedStaffId, selectedRoleIds);
	};

	return (
		<Modal
			title="添加员工"
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
					{ title: mode === "new_staff" ? "填写信息" : "选择员工" },
				]}
			/>
			{step === 0 ? (
				<Radio.Group
					className="w-full"
					value={mode}
					onChange={event => setMode(event.target.value as AddMemberMode)}
				>
					<Space direction="vertical" className="w-full">
						<Radio value="staff">
							<Space direction="vertical" size={0}>
								<Text strong>从平台员工添加</Text>
								<Text type="secondary" className="text-xs">选择尚未加入本域的平台员工</Text>
							</Space>
						</Radio>
						<Radio value="new_staff">
							<Space direction="vertical" size={0}>
								<Text strong>新建员工</Text>
								<Text type="secondary" className="text-xs">创建平台员工账号并加入本域</Text>
							</Space>
						</Radio>
					</Space>
				</Radio.Group>
			) : null}
			{step === 1 && mode === "new_staff" ? (
				<NewStaffForm form={form} roleOptions={roleOptions} />
			) : null}
			{step === 1 && mode === "staff" ? (
				<Space direction="vertical" className="w-full" size="middle">
					<Space.Compact className="w-full">
						<Input
							placeholder="搜索：姓名、昵称、手机、邮箱"
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
					<Table<DomainStaffCandidate>
						rowKey="id"
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
							onChange: (nextPage, nextPageSize) => {
								setStaffPage(nextPage);
								setStaffPageSize(nextPageSize);
							},
						}}
					/>
					<Select
						mode="multiple"
						className="w-full"
						placeholder="选择角色"
						options={roleOptions}
						value={selectedRoleIds}
						onChange={setSelectedRoleIds}
					/>
				</Space>
			) : null}
		</Modal>
	);
}

function MembersEditRolesModal({
	open,
	member,
	roleOptions,
	confirmLoading,
	onCancel,
	onSubmit,
}: MembersEditRolesModalProps) {
	const [roleIds, setRoleIds] = useState<string[]>([]);

	useEffect(() => {
		if (open && member?.roles) {
			setRoleIds(member.roles.map(role => role.id));
		}
		else if (!open) {
			setRoleIds([]);
		}
	}, [member, open]);

	return (
		<Modal
			title={member ? `编辑角色 - ${memberDisplayName(member)}` : "编辑角色"}
			open={open}
			destroyOnHidden
			onCancel={onCancel}
			onOk={() => void onSubmit(roleIds)}
			confirmLoading={confirmLoading}
			okText="保存"
			cancelText="取消"
		>
			<Select
				mode="multiple"
				className="w-full"
				placeholder="选择角色"
				options={roleOptions}
				value={roleIds}
				onChange={setRoleIds}
			/>
		</Modal>
	);
}

function MembersBatchStatusModal({
	open,
	selectedCount,
	confirmLoading,
	onCancel,
	onConfirm,
}: MembersBatchStatusModalProps) {
	const [status, setStatus] = useState<"active" | "disabled">("disabled");

	useEffect(() => {
		if (open) {
			setStatus("disabled");
		}
	}, [open]);

	return (
		<Modal
			title="批量更新成员状态"
			open={open}
			destroyOnHidden
			onCancel={onCancel}
			onOk={() => onConfirm(status)}
			confirmLoading={confirmLoading}
			okText="确认"
			cancelText="取消"
		>
			<Text className="mb-4 block">
				已选择
				{selectedCount}
				{" "}
				名成员，请选择目标状态：
			</Text>
			<Radio.Group value={status} onChange={event => setStatus(event.target.value)}>
				<Space direction="vertical">
					<Radio value="active">启用</Radio>
					<Radio value="disabled">禁用</Radio>
				</Space>
			</Radio.Group>
		</Modal>
	);
}

interface MembersSearchPanelProps {
	loading?: boolean;
	onSearch: (values: MemberSearchValues) => void;
	onReset: () => void;
}

function MembersSearchPanel({ loading, onSearch, onReset }: MembersSearchPanelProps) {
	return (
		<Card
			className={styles.filterCard}
			bordered={false}
			title={(
				<Space>
					<SearchOutlined />
					<span>筛选条件</span>
				</Space>
			)}
		>
			<TableSearchForm<MemberSearchValues>
				loading={loading}
				initialValues={EMPTY_MEMBER_SEARCH}
				onFinish={onSearch}
				onReset={() => {
					onReset();
				}}
			>
				<Form.Item name="keyword" label="关键字">
					<Input allowClear placeholder="昵称、姓名、手机、邮箱" prefix={<SearchOutlined />} disabled={loading} />
				</Form.Item>
				<Form.Item name="status" label="状态">
					<Select
						allowClear
						placeholder="全部状态"
						disabled={loading}
						options={[
							{ value: "active", label: "启用" },
							{ value: "disabled", label: "禁用" },
						]}
					/>
				</Form.Item>
				<Form.Item name="createdRange" label="加入时间">
					<RangePicker style={{ width: "100%" }} placeholder={["加入开始", "加入结束"]} disabled={loading} />
				</Form.Item>
			</TableSearchForm>
		</Card>
	);
}

export function DetailMembers({ domainId }: DetailMembersProps) {
	const { message, modal } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<DomainMember[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [searchValues, setSearchValues] = useState<MemberSearchValues>(EMPTY_MEMBER_SEARCH);
	const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
	const [roles, setRoles] = useState<DomainRole[]>([]);
	const [addOpen, setAddOpen] = useState(false);
	const [editMember, setEditMember] = useState<DomainMember | null>(null);
	const [batchStatusOpen, setBatchStatusOpen] = useState(false);

	const roleOptions = useMemo(
		() => roles.map(role => ({ label: formatDomainRoleLabel(role), value: role.id })),
		[roles],
	);

	const loadRoles = useCallback(async () => {
		if (!domainId) {
			return;
		}
		try {
			const list = await fetchPlatformDomainRoles(domainId);
			setRoles(list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, message]);

	const loadMembers = useCallback(async (
		nextPage = page,
		nextPageSize = pageSize,
		nextSearch = searchValues,
	) => {
		if (!domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const normalizedSearch = normalizeMemberSearch(nextSearch);
			const keyword = normalizedSearch.keyword?.trim() ?? "";
			const status = normalizedSearch.status ?? "";
			const createdRange = normalizeCreatedRange(normalizedSearch.createdRange);
			const result = await fetchDomainMembersPage({
				domainId,
				page: nextPage,
				page_size: nextPageSize,
				keyword: keyword || undefined,
				status: status || undefined,
				created_from: createdRange?.[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss"),
				created_to: createdRange?.[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss"),
			});
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
			setSearchValues(normalizedSearch);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, page, pageSize, searchValues]);

	useEffect(() => {
		void loadMembers(1, 20, EMPTY_MEMBER_SEARCH);
		void loadRoles();
	// eslint-disable-next-line react-hooks/exhaustive-deps -- domainId 变化时初始化
	}, [domainId]);

	const refreshCurrentPage = useCallback(async () => {
		await loadMembers(page, pageSize, searchValues);
	}, [loadMembers, page, pageSize, searchValues]);

	const handleSearch = useCallback((values: MemberSearchValues) => {
		void loadMembers(1, pageSize, normalizeMemberSearch(values));
	}, [loadMembers, pageSize]);

	const handleResetSearch = useCallback(() => {
		void loadMembers(1, pageSize, EMPTY_MEMBER_SEARCH);
	}, [loadMembers, pageSize]);

	const confirmStatusChange = useCallback((
		ids: string[],
		nextStatus: "active" | "disabled",
		title: string,
		content: string,
	) => {
		modal.confirm({
			title,
			content,
			okText: "确定",
			cancelText: "取消",
			onOk: async () => {
				setSubmitting(true);
				try {
					for (const id of ids) {
						await updateDomainMemberStatus(domainId, id, nextStatus);
					}
					message.success(`已更新 ${ids.length} 名成员状态`);
					setSelectedRowKeys([]);
					await refreshCurrentPage();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
				finally {
					setSubmitting(false);
				}
			},
		});
	}, [domainId, message, modal, refreshCurrentPage]);

	const confirmDelete = useCallback((ids: string[], title: string, content: string) => {
		modal.confirm({
			title,
			content,
			okText: "确定",
			cancelText: "取消",
			okButtonProps: { danger: true },
			onOk: async () => {
				setSubmitting(true);
				try {
					for (const id of ids) {
						await deleteDomainMember(domainId, id);
					}
					message.success(`已移除 ${ids.length} 名成员`);
					setSelectedRowKeys([]);
					await refreshCurrentPage();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
				finally {
					setSubmitting(false);
				}
			},
		});
	}, [domainId, message, modal, refreshCurrentPage]);

	const handleRowStatus = useCallback((row: DomainMember) => {
		const name = memberDisplayName(row);
		if (row.status === "active") {
			confirmStatusChange([row.id], "disabled", "确认禁用成员", `确定将「${name}」设为禁用吗？`);
			return;
		}
		if (row.status === "disabled") {
			confirmStatusChange([row.id], "active", "确认启用成员", `确定将「${name}」重新启用吗？`);
		}
	}, [confirmStatusChange]);

	const columns: TableColumnsType<DomainMember> = useMemo(() => [
		{
			title: "登录账号",
			dataIndex: "username",
			width: 120,
			render: (_, row) => row.username ?? row.login_name ?? "—",
		},
		{
			title: "真实姓名",
			dataIndex: "real_name",
			width: 100,
			render: (_, row) => row.real_name ?? "—",
		},
		{
			title: "昵称",
			dataIndex: "nickname",
			width: 100,
			render: (_, row) => row.nickname ?? "—",
		},
		{
			title: "角色",
			dataIndex: "roles",
			render: (_, row) => (
				row.roles && row.roles.length > 0
					? row.roles.map(role => <Tag key={role.id}>{formatDomainRoleLabel(role)}</Tag>)
					: "—"
			),
		},
		{
			title: "状态",
			dataIndex: "status",
			width: 90,
			render: (_, row) => {
				const { label, color } = formatMemberStatus(row.status);
				return <Tag color={color}>{label}</Tag>;
			},
		},
		{
			title: "加入时间",
			dataIndex: "created_at",
			width: 160,
			render: (_, row) => {
				const value = row.created_at ?? row.activated_at;
				return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "—";
			},
		},
		{
			title: "操作",
			key: "actions",
			width: 220,
			fixed: "right",
			render: (_, row) => (
				<Space size="small">
					<AuthGuarded auth={DOMAIN_MEMBER_UPDATE_ROLES} fallback={null}>
						<Button type="link" size="small" onClick={() => setEditMember(row)}>编辑角色</Button>
					</AuthGuarded>
					<AuthGuarded auth={DOMAIN_MEMBER_UPDATE_STATUS} fallback={null}>
						{row.status === "active" ? (
							<Button type="link" size="small" onClick={() => handleRowStatus(row)}>禁用</Button>
						) : null}
						{row.status === "disabled" ? (
							<Button type="link" size="small" onClick={() => handleRowStatus(row)}>启用</Button>
						) : null}
					</AuthGuarded>
					<AuthGuarded auth={DOMAIN_MEMBER_DELETE} fallback={null}>
						<ConfirmPopover
							title="确认移除成员"
							description={`确定将「${memberDisplayName(row)}」从本域移除吗？`}
							onConfirm={async () => {
								setSubmitting(true);
								try {
									await deleteDomainMember(domainId, row.id);
									message.success("已移除成员");
									await refreshCurrentPage();
								}
								catch (error) {
									message.error(toErrorMessage(error));
								}
								finally {
									setSubmitting(false);
								}
							}}
						>
							<Button type="link" size="small" danger>删除</Button>
						</ConfirmPopover>
					</AuthGuarded>
				</Space>
			),
		},
	], [domainId, message, refreshCurrentPage]);

	return (
		<AuthGuarded auth={DOMAIN_MEMBER_READ} fallback={<Empty description="无权限查看员工管理" />}>
			<div>
				<Title level={5} className={styles.pageTitle}>员工管理</Title>
				<MembersSearchPanel
					loading={loading}
					onSearch={handleSearch}
					onReset={handleResetSearch}
				/>
				<div className={styles.toolbar}>
					<Space className={styles.toolbarActions} wrap>
						<AuthGuarded auth={DOMAIN_MEMBER_CREATE} fallback={null}>
							<Button type="primary" onClick={() => setAddOpen(true)}>添加员工</Button>
						</AuthGuarded>
						<AuthGuarded auth={DOMAIN_MEMBER_UPDATE_STATUS} fallback={null}>
							<Button
								disabled={selectedRowKeys.length === 0}
								onClick={() => setBatchStatusOpen(true)}
							>
								批量启停
							</Button>
						</AuthGuarded>
						<AuthGuarded auth={DOMAIN_MEMBER_DELETE} fallback={null}>
							<Button
								danger
								disabled={selectedRowKeys.length === 0}
								onClick={() => confirmDelete(
									selectedRowKeys,
									"确认批量移除",
									`确定将选中的 ${selectedRowKeys.length} 名成员从本域移除吗？`,
								)}
							>
								批量删除
							</Button>
						</AuthGuarded>
					</Space>
					<Text type="secondary" className={styles.toolbarMeta}>
						{selectedRowKeys.length > 0 ? `已选 ${selectedRowKeys.length} 项 · 共 ${total} 条` : `共 ${total} 条`}
					</Text>
				</div>
				<Table<DomainMember>
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={rows}
					scroll={{ x: 1100 }}
					rowSelection={{
						selectedRowKeys,
						onChange: keys => setSelectedRowKeys(keys.map(String)),
					}}
					pagination={{
						current: page,
						pageSize,
						total,
						showSizeChanger: true,
						showTotal: t => `共 ${t} 条`,
						onChange: (nextPage, nextPageSize) => {
							void loadMembers(nextPage, nextPageSize, searchValues);
						},
					}}
					locale={{ emptyText: <Empty description="暂无员工" /> }}
				/>
				<MembersAddModal
					open={addOpen}
					domainId={domainId}
					roleOptions={roleOptions}
					confirmLoading={submitting}
					onCancel={() => setAddOpen(false)}
					onSubmitStaff={async (staffAccountId, roleIds) => {
						setSubmitting(true);
						try {
							await createDomainMember(domainId, { staff_account_id: staffAccountId, role_ids: roleIds });
							message.success("添加成功");
							setAddOpen(false);
							await refreshCurrentPage();
						}
						catch (error) {
							message.error(toErrorMessage(error));
						}
						finally {
							setSubmitting(false);
						}
					}}
					onSubmitNewStaff={async (values) => {
						setSubmitting(true);
						try {
							await createDomainMemberWithStaff(domainId, values);
							message.success("创建并添加成功");
							setAddOpen(false);
							await refreshCurrentPage();
						}
						catch (error) {
							message.error(toErrorMessage(error));
						}
						finally {
							setSubmitting(false);
						}
					}}
				/>
				<MembersEditRolesModal
					open={editMember != null}
					member={editMember}
					roleOptions={roleOptions}
					confirmLoading={submitting}
					onCancel={() => setEditMember(null)}
					onSubmit={async (roleIds) => {
						if (!editMember) {
							return;
						}
						setSubmitting(true);
						try {
							await updateDomainMemberRoles(domainId, editMember.id, roleIds);
							message.success("角色已更新");
							setEditMember(null);
							await refreshCurrentPage();
						}
						catch (error) {
							message.error(toErrorMessage(error));
						}
						finally {
							setSubmitting(false);
						}
					}}
				/>
				<MembersBatchStatusModal
					open={batchStatusOpen}
					selectedCount={selectedRowKeys.length}
					confirmLoading={submitting}
					onCancel={() => setBatchStatusOpen(false)}
					onConfirm={(nextStatus) => {
						setBatchStatusOpen(false);
						confirmStatusChange(
							selectedRowKeys,
							nextStatus,
							nextStatus === "disabled" ? "确认批量禁用" : "确认批量启用",
							`确定将选中的 ${selectedRowKeys.length} 名成员${nextStatus === "disabled" ? "设为禁用" : "重新启用"}吗？`,
						);
					}}
				/>
			</div>
		</AuthGuarded>
	);
}
