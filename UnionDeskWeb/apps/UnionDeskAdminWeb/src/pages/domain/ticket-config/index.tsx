import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import {
	DOMAIN_TICKET_ATTRIBUTE_CREATE,
	DOMAIN_TICKET_ATTRIBUTE_DELETE,
	DOMAIN_TICKET_ATTRIBUTE_READ,
	DOMAIN_TICKET_ATTRIBUTE_UPDATE,
	DOMAIN_TICKET_CLAIM_RULE_READ,
	DOMAIN_TICKET_STATUS_CREATE,
	DOMAIN_TICKET_STATUS_DELETE,
	DOMAIN_TICKET_STATUS_READ,
	DOMAIN_TICKET_STATUS_UPDATE,
	DOMAIN_TICKET_TYPE_READ,
} from "#src/pages/domain/domain-permissions";
import { TicketAttributesPanel } from "#src/pages/platform/ticket-config/attributes/ticket-attributes-panel";
import { TicketStatusesPanel } from "#src/pages/platform/ticket-config/statuses/ticket-statuses-panel";
import { useAuthStore } from "#src/store/auth";

import { AppstoreOutlined, FlagOutlined, ProfileOutlined, RocketOutlined } from "@ant-design/icons";
import { Empty } from "antd";
import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router";

import { ClaimRulePanel } from "./claim-rule-panel";
import { DomainTicketTypesPanel } from "./types-panel";

import styles from "./index.module.less";

type TicketConfigSection = "attributes" | "types" | "statuses" | "claim-rule";

const SECTION_ITEMS: {
	key: TicketConfigSection
	label: string
	icon: React.ReactNode
	perm: string
}[] = [
	{ key: "types", label: "事项类型", icon: <AppstoreOutlined />, perm: DOMAIN_TICKET_TYPE_READ },
	{ key: "attributes", label: "事项属性", icon: <ProfileOutlined />, perm: DOMAIN_TICKET_ATTRIBUTE_READ },
	{ key: "statuses", label: "事项状态", icon: <FlagOutlined />, perm: DOMAIN_TICKET_STATUS_READ },
	{ key: "claim-rule", label: "领取规则", icon: <RocketOutlined />, perm: DOMAIN_TICKET_CLAIM_RULE_READ },
];

const ATTRIBUTE_PERMISSIONS = {
	read: DOMAIN_TICKET_ATTRIBUTE_READ,
	create: DOMAIN_TICKET_ATTRIBUTE_CREATE,
	update: DOMAIN_TICKET_ATTRIBUTE_UPDATE,
	delete: DOMAIN_TICKET_ATTRIBUTE_DELETE,
};

const STATUS_PERMISSIONS = {
	read: DOMAIN_TICKET_STATUS_READ,
	create: DOMAIN_TICKET_STATUS_CREATE,
	update: DOMAIN_TICKET_STATUS_UPDATE,
	delete: DOMAIN_TICKET_STATUS_DELETE,
};

function parseSection(value: string | null): TicketConfigSection {
	if (value === "attributes" || value === "types" || value === "statuses" || value === "claim-rule") {
		return value;
	}
	return "types";
}

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

export default function DomainTicketConfigPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);
	const section = parseSection(searchParams.get("section"));

	const handleSectionChange = useCallback((next: TicketConfigSection) => {
		setSearchParams((prev) => {
			const nextParams = new URLSearchParams(prev);
			nextParams.set("section", next);
			return nextParams;
		}, { replace: true });
	}, [setSearchParams]);

	const entryAuth = [
		DOMAIN_TICKET_TYPE_READ,
		DOMAIN_TICKET_ATTRIBUTE_READ,
		DOMAIN_TICKET_STATUS_READ,
		DOMAIN_TICKET_CLAIM_RULE_READ,
	];

	const renderContent = () => {
		if (!domainId) {
			return <Empty description="未绑定当前业务域" className="py-16" />;
		}
		if (section === "attributes") {
			return (
				<TicketAttributesPanel
					scope="domain"
					domainId={domainId}
					permissions={ATTRIBUTE_PERMISSIONS}
				/>
			);
		}
		if (section === "statuses") {
			return (
				<TicketStatusesPanel
					scope="domain"
					domainId={domainId}
					embedded
					permissions={STATUS_PERMISSIONS}
				/>
			);
		}
		if (section === "claim-rule") {
			return <ClaimRulePanel domainId={domainId} />;
		}
		return <DomainTicketTypesPanel domainId={domainId} />;
	};

	return (
		<BasicContent className="h-full !p-0">
			<AuthGuarded
				auth={entryAuth}
				fallback={<Empty description="无权限查看事项配置" className="py-16" />}
			>
				<div className={`domain-ticket-config ${styles.detailBody}`}>
					<aside className={styles.sider}>
						<nav className={styles.siderNav}>
							{SECTION_ITEMS.map(item => (
								<button
									key={item.key}
									type="button"
									className={`${styles.siderItem} ${section === item.key ? styles.siderItemActive : ""}`}
									onClick={() => handleSectionChange(item.key)}
								>
									{item.icon}
									<span>{item.label}</span>
								</button>
							))}
						</nav>
					</aside>
					<div className={styles.contentCard}>
						<div className={styles.contentScroll}>
							{renderContent()}
						</div>
					</div>
				</div>
			</AuthGuarded>
		</BasicContent>
	);
}
