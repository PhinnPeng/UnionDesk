import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import {
	DOMAIN_CONFIG_READ,
	DOMAIN_CONFIG_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { DomainConfigPanel } from "#src/pages/platform/domain-config/config-panel";
import { useAuthStore } from "#src/store/auth";

import { Card, Empty } from "antd";
import { useMemo } from "react";

function resolveBusinessDomainId(
	defaultBusinessDomainId: string,
	accessibleDomains: Array<{ id: string }>,
): string {
	if (defaultBusinessDomainId) {
		return defaultBusinessDomainId;
	}
	const first = accessibleDomains[0];
	return first ? first.id : "";
}

export default function DomainConfigPage() {
	const { hasPermission } = useAuth();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const canUpdate = hasPermission(DOMAIN_CONFIG_UPDATE);

	return (
		<BasicContent>
			<AuthGuarded auth={DOMAIN_CONFIG_READ} fallback={<Empty description="无权限查看参数配置" />}>
				<Card bordered={false} title="参数配置">
					{domainId
						? <DomainConfigPanel domainId={domainId} canUpdate={canUpdate} />
						: <Empty description="暂无可用业务域" />}
				</Card>
			</AuthGuarded>
		</BasicContent>
	);
}
