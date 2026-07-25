import { AppstoreOutlined, FlagOutlined, ProfileOutlined } from "@ant-design/icons";

import { useAuth } from "#src/hooks/use-auth";
import {
	PLATFORM_TICKET_CONFIG_ATTR_READ,
	PLATFORM_TICKET_CONFIG_STATUS_READ,
	PLATFORM_TICKET_CONFIG_TYPE_READ,
} from "#src/pages/platform/domains/platform-domain-permissions";
import {
	parseTicketConfigSection,
	resolveEffectiveTicketConfigSection,
	type TicketConfigSection,
} from "#src/pages/platform/ticket-config/ticket-config-path";

import { useMemo } from "react";
import { useSearchParams } from "react-router";

import styles from "../index.module.less";

const NAV_ITEMS: { key: TicketConfigSection; label: string; icon: React.ReactNode; perm: string }[] = [
	{
		key: "attributes",
		label: "事项属性",
		icon: <ProfileOutlined />,
		perm: PLATFORM_TICKET_CONFIG_ATTR_READ,
	},
	{
		key: "types",
		label: "事项类型",
		icon: <AppstoreOutlined />,
		perm: PLATFORM_TICKET_CONFIG_TYPE_READ,
	},
	{
		key: "statuses",
		label: "事项状态",
		icon: <FlagOutlined />,
		perm: PLATFORM_TICKET_CONFIG_STATUS_READ,
	},
];

export function TicketConfigSider() {
	const { hasPermission } = useAuth();
	const [searchParams, setSearchParams] = useSearchParams();

	const canViewAttributes = hasPermission(PLATFORM_TICKET_CONFIG_ATTR_READ);
	const canViewTypes = hasPermission(PLATFORM_TICKET_CONFIG_TYPE_READ);
	const canViewStatuses = hasPermission(PLATFORM_TICKET_CONFIG_STATUS_READ);

	const navItems = useMemo(
		() => NAV_ITEMS.filter(item => hasPermission(item.perm)),
		[hasPermission],
	);

	const sectionParam = parseTicketConfigSection(searchParams.get("section"));
	const activeKey = resolveEffectiveTicketConfigSection(
		sectionParam,
		canViewAttributes,
		canViewTypes,
		canViewStatuses,
	);

	const handleSelect = (key: TicketConfigSection) => {
		setSearchParams((prev) => {
			const next = new URLSearchParams(prev);
			next.set("section", key);
			next.delete("typeId");
			next.delete("tab");
			return next;
		}, { replace: true });
	};

	return (
		<aside className={styles.sider}>
			<nav className={styles.siderNav}>
				{navItems.map(item => (
					<button
						key={item.key}
						type="button"
						className={`${styles.siderItem} ${activeKey === item.key ? styles.siderItemActive : ""}`}
						onClick={() => handleSelect(item.key)}
					>
						{item.icon}
						<span>{item.label}</span>
					</button>
				))}
			</nav>
		</aside>
	);
}
