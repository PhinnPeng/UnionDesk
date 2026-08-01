import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
} from "#src/pages/platform/domains/platform-domain-permissions";
import { TicketAttributesPanel } from "#src/pages/platform/ticket-config/attributes/ticket-attributes-panel";
import { TicketStatusesPanel } from "#src/pages/platform/ticket-config/statuses/ticket-statuses-panel";

import { AppstoreOutlined, FlagOutlined, ProfileOutlined } from "@ant-design/icons";
import { Empty } from "antd";
import { useMemo } from "react";

import { DomainTicketTypesPanel } from "./domain-ticket-types-panel";

import styles from "../index.module.less";

export type DomainTicketConfigSection = "attributes" | "types" | "statuses";

export interface DetailTicketConfigProps {
	domainId: string;
	section: DomainTicketConfigSection;
	onSectionChange: (section: DomainTicketConfigSection) => void;
}

const SECTION_ITEMS: {
	key: DomainTicketConfigSection
	label: string
	icon: React.ReactNode
	perm: string
}[] = [
	{ key: "types", label: "事项类型", icon: <AppstoreOutlined />, perm: PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ },
	{ key: "attributes", label: "事项属性", icon: <ProfileOutlined />, perm: PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_READ },
	{ key: "statuses", label: "事项状态", icon: <FlagOutlined />, perm: PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_READ },
];

export function parseDomainTicketConfigSection(value: string | null): DomainTicketConfigSection {
	if (value === "attributes" || value === "types" || value === "statuses") {
		return value;
	}
	return "types";
}

export function DetailTicketConfig({ domainId, section, onSectionChange }: DetailTicketConfigProps) {
	const navItems = useMemo(
		() => SECTION_ITEMS,
		[],
	);

	const entryAuth = [
		PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
		PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_READ,
		PLATFORM_DOMAIN_CONTROL_TICKET_STATUS_READ,
	];

	const renderContent = () => {
		if (section === "attributes") {
			return <TicketAttributesPanel scope="domain" domainId={domainId} />;
		}
		if (section === "statuses") {
			return <TicketStatusesPanel scope="domain" domainId={domainId} embedded />;
		}
		return <DomainTicketTypesPanel domainId={domainId} />;
	};

	return (
		<AuthGuarded
			auth={entryAuth}
			fallback={<Empty description="无权限查看事项配置" className="py-16" />}
		>
			<div className={`domain-ticket-config ${styles.detailBody}`} style={{ minHeight: 480, border: "none", borderRadius: 0, boxShadow: "none" }}>
				<aside className={styles.sider}>
					<nav className={styles.siderNav}>
						{navItems.map(item => (
							<button
								key={item.key}
								type="button"
								className={`${styles.siderItem} ${section === item.key ? styles.siderItemActive : ""}`}
								onClick={() => onSectionChange(item.key)}
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
	);
}
