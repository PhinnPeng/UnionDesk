import type { BusinessDomainView } from "@uniondesk/shared";

import { fetchBusinessDomains } from "#src/api/platform/domain";
import { BasicContent } from "#src/components/basic-content";

import { App, Card, Select, Space, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router";

import { DomainConfigPanel } from "./config-panel";

export default function PlatformDomainConfig() {
	const { message } = App.useApp();
	const [searchParams] = useSearchParams();
	const presetDomainId = searchParams.get("domainId")?.trim();
	const [domains, setDomains] = useState<BusinessDomainView[]>([]);
	const [domainId, setDomainId] = useState<string>();

	useEffect(() => {
		let ignore = false;
		void (async () => {
			try {
				const list = await fetchBusinessDomains();
				if (ignore) {
					return;
				}
				setDomains(list);
				const matched = presetDomainId
					? list.find(item => String(item.id) === presetDomainId)?.id
					: undefined;
				const initial = matched ?? list[0]?.id;
				setDomainId(initial != null ? String(initial) : undefined);
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "加载业务域失败");
			}
		})();
		return () => {
			ignore = true;
		};
	}, [message, presetDomainId]);

	const domainOptions = useMemo(() => {
		return domains.map(domain => ({
			label: `${domain.name} / ${domain.code}`,
			value: String(domain.id),
		}));
	}, [domains]);

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card
				title="域配置"
				bordered={false}
				extra={(
					<Space>
						<Typography.Text type="secondary">按业务域独立维护工作时间、时区和语言等基础配置</Typography.Text>
					</Space>
				)}
			>
				<Space direction="vertical" size={16} className="w-full">
					<div className="flex flex-wrap items-center gap-3">
						<Typography.Text className="text-slate-600">业务域</Typography.Text>
						<Select
							className="min-w-72"
							value={domainId}
							options={domainOptions}
							onChange={setDomainId}
							placeholder="请选择业务域"
						/>
					</div>
					{domainId ? <DomainConfigPanel domainId={domainId} /> : null}
				</Space>
			</Card>
		</BasicContent>
	);
}

