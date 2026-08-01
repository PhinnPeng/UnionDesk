import type { AdminDomain, P0AccessPolicy, P0InvitationCode } from "@uniondesk/shared";
import {
	createP0InvitationCode,
	deleteP0InvitationCode,
	fetchAdminDomain,
	fetchP0InvitationCodes,
	toErrorMessage,
	updateAdminDomain,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { useAuth } from "#src/hooks/use-auth";
import {
	DOMAIN_GENERAL_READ,
	DOMAIN_GENERAL_UPDATE,
	DOMAIN_INVITATION_CODE_CREATE,
	DOMAIN_INVITATION_CODE_DELETE,
	DOMAIN_INVITATION_CODE_READ,
} from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import {
	App,
	Button,
	Card,
	DatePicker,
	Empty,
	Form,
	Input,
	InputNumber,
	Modal,
	Spin,
	Switch,
	Table,
	Tabs,
	Tag,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";

import styles from "./index.module.less";

const { Text } = Typography;

interface AccessPolicySwitchRowProps {
	title: string;
	description: string;
	checked: boolean;
	loading?: boolean;
	disabled?: boolean;
	onChange: (checked: boolean) => void;
}

interface CreateInviteFormValues {
	channel?: string;
	expires_at?: Dayjs | null;
	max_uses?: number | null;
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

function isAccessAllowed(value: P0AccessPolicy | undefined): boolean {
	return value === "allowed";
}

function toAccessPolicy(enabled: boolean): P0AccessPolicy {
	return enabled ? "allowed" : "disallowed";
}

function AccessPolicySwitchRow({
	title,
	description,
	checked,
	loading = false,
	disabled = false,
	onChange,
}: AccessPolicySwitchRowProps) {
	return (
		<div className={styles.policySwitchRow}>
			<div className={styles.policySwitchMain}>
				<Text strong>{title}</Text>
				<Text type="secondary" className={styles.policySwitchDesc}>
					{description}
				</Text>
			</div>
			<Switch
				checked={checked}
				loading={loading}
				disabled={disabled}
				checkedChildren="已开启"
				unCheckedChildren="已关闭"
				onChange={onChange}
			/>
		</div>
	);
}

export default function DomainOnboardingPage() {
	const { message, modal } = App.useApp();
	const { hasPermission } = useAuth();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const canUpdatePolicy = hasPermission(DOMAIN_GENERAL_UPDATE);
	const canReadInvites = hasPermission(DOMAIN_INVITATION_CODE_READ);
	const canCreateInvite = hasPermission(DOMAIN_INVITATION_CODE_CREATE);
	const canDeleteInvite = hasPermission(DOMAIN_INVITATION_CODE_DELETE);

	const [loading, setLoading] = useState(false);
	const [domain, setDomain] = useState<AdminDomain | null>(null);
	const [regLoading, setRegLoading] = useState(false);
	const [invLoading, setInvLoading] = useState(false);

	const [invites, setInvites] = useState<P0InvitationCode[]>([]);
	const [inviteTotal, setInviteTotal] = useState(0);
	const [invitePage, setInvitePage] = useState(1);
	const [invitePageSize, setInvitePageSize] = useState(20);
	const [inviteLoading, setInviteLoading] = useState(false);
	const [createOpen, setCreateOpen] = useState(false);
	const [createSubmitting, setCreateSubmitting] = useState(false);
	const [createForm] = Form.useForm<CreateInviteFormValues>();

	const loadDomain = useCallback(async () => {
		if (!domainId) {
			setDomain(null);
			return;
		}
		setLoading(true);
		try {
			const data = await fetchAdminDomain(domainId);
			setDomain(data);
		}
		catch (error) {
			setDomain(null);
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	const loadInvites = useCallback(async (page: number, pageSize: number) => {
		if (!domainId || !canReadInvites) {
			setInvites([]);
			setInviteTotal(0);
			return;
		}
		setInviteLoading(true);
		try {
			const result = await fetchP0InvitationCodes(domainId, { page, page_size: pageSize });
			setInvites(result.list);
			setInviteTotal(result.total);
			setInvitePage(page);
			setInvitePageSize(pageSize);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setInviteLoading(false);
		}
	}, [canReadInvites, domainId, message]);

	useEffect(() => {
		void loadDomain();
	}, [loadDomain]);

	useEffect(() => {
		if (!canReadInvites || !domainId) {
			setInvites([]);
			setInviteTotal(0);
			return;
		}
		void loadInvites(1, 20);
	}, [canReadInvites, domainId, loadInvites]);

	const registrationEnabled = isAccessAllowed(domain?.registration_enabled);
	const invitationEnabled = isAccessAllowed(domain?.invitation_enabled);

	const applyRegistrationPolicy = useCallback(async (checked: boolean) => {
		if (!domain) {
			return;
		}
		setRegLoading(true);
		const nextPolicy = toAccessPolicy(checked);
		try {
			await updateAdminDomain(domain.id, { registration_enabled: nextPolicy });
			message.success(checked ? "已开启客户自助注册" : "已关闭客户自助注册");
			setDomain({ ...domain, registration_enabled: nextPolicy });
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setRegLoading(false);
		}
	}, [domain, message]);

	const applyInvitationPolicy = useCallback(async (checked: boolean) => {
		if (!domain) {
			return;
		}
		setInvLoading(true);
		const nextPolicy = toAccessPolicy(checked);
		try {
			await updateAdminDomain(domain.id, { invitation_enabled: nextPolicy });
			message.success(checked ? "已开启邀请码入域" : "已关闭邀请码入域");
			setDomain({ ...domain, invitation_enabled: nextPolicy });
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setInvLoading(false);
		}
	}, [domain, message]);

	const handleRegistrationChange = useCallback((checked: boolean) => {
		if (!canUpdatePolicy) {
			return;
		}
		if (!checked && registrationEnabled) {
			modal.confirm({
				title: "确认关闭客户自助注册",
				content: "关闭后，客户端将无法自助注册加入该业务域，确定继续吗？",
				okText: "确定关闭",
				cancelText: "取消",
				onOk: () => applyRegistrationPolicy(false),
			});
			return;
		}
		void applyRegistrationPolicy(checked);
	}, [applyRegistrationPolicy, canUpdatePolicy, modal, registrationEnabled]);

	const handleInvitationChange = useCallback((checked: boolean) => {
		if (!canUpdatePolicy) {
			return;
		}
		if (!checked && invitationEnabled) {
			modal.confirm({
				title: "确认关闭邀请码入域",
				content: "关闭后，客户将无法通过邀请码加入该业务域，确定继续吗？",
				okText: "确定关闭",
				cancelText: "取消",
				onOk: () => applyInvitationPolicy(false),
			});
			return;
		}
		void applyInvitationPolicy(checked);
	}, [applyInvitationPolicy, canUpdatePolicy, invitationEnabled, modal]);

	const handleCreateInvite = useCallback(async () => {
		if (!domainId) {
			return;
		}
		if (!invitationEnabled) {
			message.warning("请先开启「邀请码入域」后再创建邀请码");
			return;
		}
		try {
			const values = await createForm.validateFields();
			setCreateSubmitting(true);
			const created = await createP0InvitationCode(domainId, {
				channel: values.channel?.trim() || null,
				expires_at: values.expires_at ? values.expires_at.format("YYYY-MM-DDTHH:mm:ss") : null,
				max_uses: values.max_uses ?? null,
			});
			message.success(`已创建邀请码 ${created.code}`);
			setCreateOpen(false);
			createForm.resetFields();
			await loadInvites(1, invitePageSize);
		}
		catch (error) {
			if (error && typeof error === "object" && "errorFields" in error) {
				return;
			}
			message.error(toErrorMessage(error));
		}
		finally {
			setCreateSubmitting(false);
		}
	}, [createForm, domainId, invitationEnabled, invitePageSize, loadInvites, message]);

	const handleDeleteInvite = useCallback(async (codeId: string) => {
		if (!domainId) {
			return;
		}
		try {
			await deleteP0InvitationCode(domainId, codeId);
			message.success("已删除邀请码");
			const nextTotal = Math.max(inviteTotal - 1, 0);
			const maxPage = Math.max(Math.ceil(nextTotal / invitePageSize), 1);
			const nextPage = Math.min(invitePage, maxPage);
			await loadInvites(nextPage, invitePageSize);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, invitePage, invitePageSize, inviteTotal, loadInvites, message]);

	const inviteColumns: TableColumnsType<P0InvitationCode> = useMemo(() => [
		{ title: "邀请码", dataIndex: "code", width: 160 },
		{ title: "渠道", dataIndex: "channel", render: value => value ?? "—" },
		{
			title: "过期时间",
			dataIndex: "expires_at",
			width: 180,
			render: value => (value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "—"),
		},
		{
			title: "用量",
			key: "uses",
			width: 120,
			render: (_, row) => `${row.used_count ?? 0}/${row.max_uses ?? "∞"}`,
		},
		{
			title: "状态",
			dataIndex: "status",
			width: 100,
			render: status => <Tag>{status ?? "—"}</Tag>,
		},
		{
			title: "创建时间",
			dataIndex: "created_at",
			width: 180,
			render: value => (value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "—"),
		},
		...(canDeleteInvite
			? [{
					title: "操作",
					key: "actions",
					width: 100,
					fixed: "right" as const,
					render: (_: unknown, row: P0InvitationCode) => (
						<ConfirmPopover
							title="确认删除该邀请码？"
							onConfirm={() => handleDeleteInvite(row.id)}
						>
							<Button type="link" size="small" danger>
								删除
							</Button>
						</ConfirmPopover>
					),
				}]
			: []),
	], [canDeleteInvite, handleDeleteInvite]);

	const tabItems = useMemo(() => {
		const items = [
			{
				key: "registration",
				label: "客户注册配置",
				children: (
					<AccessPolicySwitchRow
						title="开启客户自助注册"
						description="允许客户在客户端自助注册并加入该业务域。"
						checked={registrationEnabled}
						loading={regLoading}
						disabled={!canUpdatePolicy || invLoading}
						onChange={handleRegistrationChange}
					/>
				),
			},
			{
				key: "invitation",
				label: "客户邀请配置",
				children: (
					<AccessPolicySwitchRow
						title="开启邀请码入域"
						description="允许客户通过邀请码加入该业务域。"
						checked={invitationEnabled}
						loading={invLoading}
						disabled={!canUpdatePolicy || regLoading}
						onChange={handleInvitationChange}
					/>
				),
			},
		];

		if (canReadInvites) {
			items.push({
				key: "codes",
				label: "邀请码",
				children: (
					<div className="flex flex-col gap-4">
						{canCreateInvite
							? (
								<div className="flex justify-end">
									<Button
										type="primary"
										disabled={!invitationEnabled}
										onClick={() => {
											if (!invitationEnabled) {
												message.warning("请先开启「邀请码入域」后再创建邀请码");
												return;
											}
											setCreateOpen(true);
										}}
									>
										新建邀请码
									</Button>
								</div>
							)
							: null}
						{!invitationEnabled
							? (
								<Text type="secondary">
									当前未开启邀请码入域，已有邀请码仍可查看与删除；新建需先开启策略。
								</Text>
							)
							: null}
						<Table<P0InvitationCode>
							rowKey="id"
							loading={inviteLoading}
							columns={inviteColumns}
							dataSource={invites}
							scroll={{ x: 900 }}
							pagination={{
								current: invitePage,
								pageSize: invitePageSize,
								total: inviteTotal,
								showSizeChanger: true,
								onChange: (page, pageSize) => {
									void loadInvites(page, pageSize);
								},
							}}
						/>
					</div>
				),
			});
		}

		return items;
	}, [
		canCreateInvite,
		canReadInvites,
		canUpdatePolicy,
		handleInvitationChange,
		handleRegistrationChange,
		invLoading,
		invitationEnabled,
		inviteColumns,
		inviteLoading,
		invitePage,
		invitePageSize,
		inviteTotal,
		invites,
		loadInvites,
		message,
		regLoading,
		registrationEnabled,
	]);

	return (
		<BasicContent>
			<AuthGuarded
				auth={[DOMAIN_GENERAL_READ, DOMAIN_INVITATION_CODE_READ]}
				fallback={<Empty description="无权限查看入域管理" className="py-16" />}
			>
				<Card title="入域管理" bordered={false}>
					{!domainId
						? <Empty description="暂无可用业务域" className="py-16" />
						: loading
							? (
								<div className="flex justify-center py-16">
									<Spin />
								</div>
							)
							: !domain
								? <Empty description="业务域信息加载失败" className="py-16" />
								: <Tabs type="card" items={tabItems} />}
				</Card>
			</AuthGuarded>

			<Modal
				title="新建邀请码"
				open={createOpen}
				okText="创建"
				cancelText="取消"
				confirmLoading={createSubmitting}
				destroyOnHidden
				onCancel={() => {
					setCreateOpen(false);
					createForm.resetFields();
				}}
				onOk={() => {
					void handleCreateInvite();
				}}
			>
				<Form form={createForm} layout="vertical" initialValues={{ channel: "", max_uses: null }}>
					<Form.Item name="channel" label="渠道">
						<Input allowClear placeholder="可选，如 wechat / email" maxLength={64} />
					</Form.Item>
					<Form.Item name="expires_at" label="过期时间">
						<DatePicker
							showTime
							className="w-full"
							placeholder="可选，不填则不过期"
							disabledDate={current => !!current && current.isBefore(dayjs().startOf("day"))}
						/>
					</Form.Item>
					<Form.Item name="max_uses" label="最大使用次数">
						<InputNumber className="w-full" min={1} precision={0} placeholder="可选，不填则不限次数" />
					</Form.Item>
					<Text type="secondary">邀请码由系统自动生成，创建后可复制分发。</Text>
				</Form>
			</Modal>
		</BasicContent>
	);
}
