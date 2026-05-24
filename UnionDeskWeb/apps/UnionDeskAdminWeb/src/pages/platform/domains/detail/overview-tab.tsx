import type { AdminDomain } from "@uniondesk/shared";

import { Avatar, Descriptions, Space, Tag } from "antd";
import dayjs from "dayjs";
import { registrationOptions, visibilityOptions } from "../constants";
import { isDomainEnabled, resolveDomainLogoUrl } from "../domain-utils";

interface DomainOverviewTabProps {
	domain: AdminDomain;
}

export function DomainOverviewTab({ domain }: DomainOverviewTabProps) {
	const enabled = isDomainEnabled(domain);
	const registrationLabel = registrationOptions.find(item => item.value === domain.registration_policy)?.label
		?? domain.registration_policy;

	return (
		<Descriptions bordered column={1} size="small">
			<Descriptions.Item label="Logo">
				<Avatar size={48} src={resolveDomainLogoUrl(domain.logo)} />
			</Descriptions.Item>
			<Descriptions.Item label="ID">{domain.id}</Descriptions.Item>
			<Descriptions.Item label="名称">{domain.name}</Descriptions.Item>
			<Descriptions.Item label="描述">{domain.description?.trim() || "-"}</Descriptions.Item>
			<Descriptions.Item label="短码">{domain.code}</Descriptions.Item>
			<Descriptions.Item label="状态">
				<Tag color={enabled ? "success" : "default"}>{enabled ? "已启用" : "已禁用"}</Tag>
			</Descriptions.Item>
			<Descriptions.Item label="注册策略">{registrationLabel}</Descriptions.Item>
			<Descriptions.Item label="可见策略">
				<Space wrap>
					{domain.visibility_policy_codes.map(code => (
						<Tag key={code}>
							{visibilityOptions.find(item => item.value === code)?.label ?? code}
						</Tag>
					))}
				</Space>
			</Descriptions.Item>
			<Descriptions.Item label="创建时间">
				{domain.created_at ? dayjs(domain.created_at).format("YYYY-MM-DD HH:mm:ss") : "-"}
			</Descriptions.Item>
			<Descriptions.Item label="创建者">{domain.creator_name ?? "-"}</Descriptions.Item>
			<Descriptions.Item label="更新时间">
				{domain.updated_at ? dayjs(domain.updated_at).format("YYYY-MM-DD HH:mm:ss") : "-"}
			</Descriptions.Item>
			<Descriptions.Item label="更新者">{domain.updater_name ?? "-"}</Descriptions.Item>
		</Descriptions>
	);
}
