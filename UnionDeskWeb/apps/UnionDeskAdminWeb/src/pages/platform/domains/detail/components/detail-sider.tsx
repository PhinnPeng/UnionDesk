import {
	BellOutlined,
	FileSearchOutlined,
	FormOutlined,
	IdcardOutlined,
	LineChartOutlined,
	SafetyCertificateOutlined,
	SettingOutlined,
	ShopOutlined,
	StopOutlined,
	TeamOutlined,
	UserAddOutlined,
} from "@ant-design/icons";

import { useAuth } from "#src/hooks/use-auth";

import { useMemo } from "react";

import { DOMAIN_BLOCKED_WORD_READ_PERMISSION, DOMAIN_CUSTOMER_READ_PERMISSION, DOMAIN_CONTROL_OVERVIEW_PERMISSION, DOMAIN_ROLES_READ_PERMISSION, type DetailTabKey } from "./detail-shared";

import styles from "../index.module.less";

export interface DetailSiderProps {
	activeTab: DetailTabKey;
	onSelect: (tab: DetailTabKey) => void;
}

const NAV_ITEMS: { key: DetailTabKey; label: string; icon: React.ReactNode }[] = [
	{ key: "overview", label: "概览", icon: <LineChartOutlined /> },
	{ key: "basic", label: "通用", icon: <ShopOutlined /> },
	{ key: "members", label: "人员管理", icon: <TeamOutlined /> },
	{ key: "roles", label: "角色管理", icon: <SafetyCertificateOutlined /> },
	{ key: "customers", label: "客户管理", icon: <IdcardOutlined /> },
	{ key: "onboarding", label: "入域管理", icon: <UserAddOutlined /> },
	{ key: "tickets", label: "工单管理", icon: <FormOutlined /> },
	{ key: "blockwords", label: "屏蔽词库", icon: <StopOutlined /> },
	{ key: "notifications", label: "通知配置", icon: <BellOutlined /> },
	{ key: "config", label: "参数配置", icon: <SettingOutlined /> },
	{ key: "logs", label: "业务日志", icon: <FileSearchOutlined /> },
];

export function DetailSider({ activeTab, onSelect }: DetailSiderProps) {
	const { hasPermission } = useAuth();
	const navItems = useMemo(
		() => NAV_ITEMS.filter((item) => {
			if (item.key === "customers" && !hasPermission(DOMAIN_CUSTOMER_READ_PERMISSION)) {
				return false;
			}
			if (item.key === "overview" && !hasPermission(DOMAIN_CONTROL_OVERVIEW_PERMISSION)) {
				return false;
			}
			if (item.key === "roles" && !hasPermission(DOMAIN_ROLES_READ_PERMISSION)) {
				return false;
			}
			if (item.key === "blockwords" && !hasPermission(DOMAIN_BLOCKED_WORD_READ_PERMISSION)) {
				return false;
			}
			return true;
		}),
		[hasPermission],
	);

	return (
		<aside className={styles.sider}>
			<nav className={styles.siderNav}>
				{navItems.map(item => (
					<button
						key={item.key}
						type="button"
						className={`${styles.siderItem} ${activeTab === item.key ? styles.siderItemActive : ""}`}
						onClick={() => onSelect(item.key)}
					>
						{item.icon}
						<span>{item.label}</span>
					</button>
				))}
			</nav>
		</aside>
	);
}
