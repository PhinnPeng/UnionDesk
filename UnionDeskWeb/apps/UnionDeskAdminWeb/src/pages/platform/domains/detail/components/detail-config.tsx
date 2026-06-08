import { DomainConfigPanel } from "#src/pages/platform/domain-config/config-panel";

import { Typography } from "antd";

const { Title } = Typography;

export interface DetailConfigProps {
	domainId: string;
}

export function DetailConfig({ domainId }: DetailConfigProps) {
	return (
		<div>
			<Title level={5} className="!mb-4">
				参数配置
			</Title>
			<DomainConfigPanel domainId={domainId} />
		</div>
	);
}
