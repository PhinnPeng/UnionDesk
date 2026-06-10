import type { AdminDomain, P0AccessPolicy } from "@uniondesk/shared";
import { toErrorMessage, updateAdminDomain } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { useAuth } from "#src/hooks/use-auth";

import { App, Switch, Tabs, Typography } from "antd";
import { useCallback, useState } from "react";

import { PLATFORM_DOMAIN_CONTROL_ENTRY, PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE } from "../../platform-domain-permissions";

import styles from "./detail-onboarding.module.less";

const { Title, Text } = Typography;

export interface DetailOnboardingProps {
	domain: AdminDomain;
	onSaved: (domain: AdminDomain) => void;
}

interface AccessPolicySwitchRowProps {
	title: string;
	description: string;
	checked: boolean;
	loading?: boolean;
	disabled?: boolean;
	onChange: (checked: boolean) => void;
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

export function DetailOnboarding({ domain, onSaved }: DetailOnboardingProps) {
	const { message, modal } = App.useApp();
	const { hasPermission } = useAuth();
	const canUpdate = hasPermission(PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE);

	const [regLoading, setRegLoading] = useState(false);
	const [invLoading, setInvLoading] = useState(false);

	const registrationEnabled = isAccessAllowed(domain.registration_enabled);
	const invitationEnabled = isAccessAllowed(domain.invitation_enabled);

	const applyRegistrationPolicy = useCallback(async (checked: boolean) => {
		setRegLoading(true);
		const nextPolicy = toAccessPolicy(checked);
		try {
			await updateAdminDomain(domain.id, { registration_enabled: nextPolicy });
			message.success(checked ? "已开启客户自助注册" : "已关闭客户自助注册");
			onSaved({ ...domain, registration_enabled: nextPolicy });
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setRegLoading(false);
		}
	}, [domain, message, onSaved]);

	const applyInvitationPolicy = useCallback(async (checked: boolean) => {
		setInvLoading(true);
		const nextPolicy = toAccessPolicy(checked);
		try {
			await updateAdminDomain(domain.id, { invitation_enabled: nextPolicy });
			message.success(checked ? "已开启邀请码入域" : "已关闭邀请码入域");
			onSaved({ ...domain, invitation_enabled: nextPolicy });
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setInvLoading(false);
		}
	}, [domain, message, onSaved]);

	const handleRegistrationChange = useCallback((checked: boolean) => {
		if (!canUpdate) {
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
	}, [applyRegistrationPolicy, canUpdate, modal, registrationEnabled]);

	const handleInvitationChange = useCallback((checked: boolean) => {
		if (!canUpdate) {
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
	}, [applyInvitationPolicy, canUpdate, invitationEnabled, modal]);

	return (
		<div>
			<Title level={5} className="!mb-4">
				客户入域
			</Title>
			<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_ENTRY}>
				<Tabs
					type="card"
					items={[
						{
							key: "registration",
							label: "客户注册配置",
							children: (
								<AccessPolicySwitchRow
									title="开启客户自助注册"
									description="允许客户在客户端自助注册并加入该业务域。"
									checked={registrationEnabled}
									loading={regLoading}
									disabled={!canUpdate || invLoading}
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
									disabled={!canUpdate || regLoading}
									onChange={handleInvitationChange}
								/>
							),
						},
					]}
				/>
			</AuthGuarded>
		</div>
	);
}
