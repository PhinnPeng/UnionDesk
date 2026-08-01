import type { AdminDomain, P0AccessPolicy } from "@uniondesk/shared";
import { fetchAdminDomain, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { DOMAIN_INVITATION_CODE_READ } from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import { App, Card, Empty, Spin, Switch, Tabs, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";

import styles from "./index.module.less";

const { Text } = Typography;

interface AccessPolicySwitchRowProps {
	title: string;
	description: string;
	checked: boolean;
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

function AccessPolicySwitchRow({ title, description, checked }: AccessPolicySwitchRowProps) {
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
				disabled
				checkedChildren="已开启"
				unCheckedChildren="已关闭"
			/>
		</div>
	);
}

export default function DomainOnboardingPage() {
	const { message } = App.useApp();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const [loading, setLoading] = useState(false);
	const [domain, setDomain] = useState<AdminDomain | null>(null);

	useEffect(() => {
		if (!domainId) {
			setDomain(null);
			return;
		}
		let cancelled = false;
		setLoading(true);
		void (async () => {
			try {
				const data = await fetchAdminDomain(domainId);
				if (!cancelled) {
					setDomain(data);
				}
			}
			catch (error) {
				if (!cancelled) {
					setDomain(null);
					message.error(toErrorMessage(error));
				}
			}
			finally {
				if (!cancelled) {
					setLoading(false);
				}
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [domainId, message]);

	const registrationEnabled = isAccessAllowed(domain?.registration_enabled);
	const invitationEnabled = isAccessAllowed(domain?.invitation_enabled);

	return (
		<BasicContent>
			<AuthGuarded
				auth={DOMAIN_INVITATION_CODE_READ}
				fallback={<Empty description="无权限查看入域管理" className="py-16" />}
			>
				<Card title="入域管理" bordered={false}>
					{!domainId ? (
						<Empty description="暂无可用业务域" className="py-16" />
					) : loading ? (
						<div className="flex justify-center py-16">
							<Spin />
						</div>
					) : !domain ? (
						<Empty description="业务域信息加载失败" className="py-16" />
					) : (
						<Tabs
							type="card"
							items={[
								{
									key: "registration",
									label: "客户注册配置",
									children: (
										<AccessPolicySwitchRow
											title="开启客户自助注册"
											description="允许客户在客户端自助注册并加入该业务域。（只读）"
											checked={registrationEnabled}
										/>
									),
								},
								{
									key: "invitation",
									label: "客户邀请配置",
									children: (
										<AccessPolicySwitchRow
											title="开启邀请码入域"
											description="允许客户通过邀请码加入该业务域。（只读）"
											checked={invitationEnabled}
										/>
									),
								},
							]}
						/>
					)}
				</Card>
			</AuthGuarded>
		</BasicContent>
	);
}
