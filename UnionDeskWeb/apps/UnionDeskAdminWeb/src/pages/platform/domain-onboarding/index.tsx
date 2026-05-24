import type { AdminDomain } from "@uniondesk/shared";
import { fetchAdminDomainsPage, toErrorMessage } from "@uniondesk/shared";

import { BasicContent } from "#src/components/basic-content";

import { App, Alert, Select, Space } from "antd";
import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router";

import { DomainOnboardingPanel } from "./onboarding-panel";

export default function PlatformDomainOnboarding() {
	const { message } = App.useApp();
	const [searchParams] = useSearchParams();
	const presetDomainId = searchParams.get("domainId")?.trim() || undefined;
	const [domains, setDomains] = useState<AdminDomain[]>([]);
	const [domainId, setDomainId] = useState<string | undefined>(presetDomainId);

	const loadDomains = useCallback(async () => {
		try {
			const page = await fetchAdminDomainsPage({ page: 1, page_size: 100 });
			setDomains(page.list);
			setDomainId((prev) => {
				if (presetDomainId && page.list.some(item => item.id === presetDomainId)) {
					return presetDomainId;
				}
				return prev ?? page.list[0]?.id;
			});
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [message, presetDomainId]);

	useEffect(() => {
		void loadDomains();
	}, [loadDomains]);

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Space direction="vertical" size="large" className="w-full">
				<Alert
					type="info"
					showIcon
					message="客户入域（P0）"
					description="邀请码与域客户列表；后端未发布时表格为空。"
				/>
				<Space wrap>
					<span>业务域</span>
					<Select
						className="min-w-56"
						value={domainId}
						options={domains.map(domain => ({ value: domain.id, label: `${domain.name} (${domain.code})` }))}
						onChange={value => setDomainId(value)}
					/>
				</Space>
				{domainId ? <DomainOnboardingPanel domainId={domainId} /> : null}
			</Space>
		</BasicContent>
	);
}
