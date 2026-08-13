import type { PlatformTicketTypeDetail } from "@uniondesk/shared";

import {
	buildTicketConfigPath,
	TICKET_TYPE_CONFIG_TABS,
	type PlatformTicketTypeConfigTab,
} from "#src/pages/platform/ticket-config/ticket-config-path";
import { cn } from "#src/utils/cn";

import { ArrowLeftOutlined } from "@ant-design/icons";
import { Tag, Typography } from "antd";
import { useNavigate } from "react-router";

import "./ticket-type-config-header.less";

const { Title } = Typography;

export interface TicketTypeConfigPageHeaderProps {
	ticketType: PlatformTicketTypeDetail | null;
	unpublished: boolean;
	activeTab: PlatformTicketTypeConfigTab;
	onTabChange: (key: string) => void;
	onBack?: () => void;
}

export function TicketTypeConfigPageHeader({
	ticketType,
	unpublished,
	activeTab,
	onTabChange,
	onBack,
}: TicketTypeConfigPageHeaderProps) {
	const navigate = useNavigate();

	const goToTypesList = () => {
		navigate(buildTicketConfigPath({ section: "types" }));
	};

	const handleBack = () => {
		if (onBack) {
			onBack();
			return;
		}
		goToTypesList();
	};

	return (
		<div className="ticket-type-config-page-header">
			<div className="ticket-type-config-page-header__breadcrumb-row">
				<div className="ticket-type-config-page-header__breadcrumb-nav">
					<button
						type="button"
						className="ticket-type-config-page-header__breadcrumb-item ticket-type-config-page-header__breadcrumb-back"
						aria-label="返回"
						onClick={handleBack}
					>
						<ArrowLeftOutlined />
					</button>
					<button
						type="button"
						className="ticket-type-config-page-header__breadcrumb-item"
						onClick={goToTypesList}
					>
						事项配置
					</button>
					<span className="ticket-type-config-page-header__breadcrumb-separator" aria-hidden="true">/</span>
					<button
						type="button"
						className="ticket-type-config-page-header__breadcrumb-item"
						onClick={handleBack}
					>
						事项类型
					</button>
				</div>
			</div>

			<div className="ticket-type-config-page-header__title-card">
				<Title level={4} className="ticket-type-config-page-header__title">
					{ticketType?.name ?? "事项类型配置"}
				</Title>
				{ticketType?.is_system ? <Tag className="ticket-type-config-page-header__tag" color="blue">系统</Tag> : null}
				{unpublished ? <Tag className="ticket-type-config-page-header__tag" color="warning">未发布</Tag> : null}
			</div>

			<nav className="ticket-type-config-page-header__tabs" aria-label="事项类型配置页签">
				{TICKET_TYPE_CONFIG_TABS.map(tab => (
					<button
						key={tab.key}
						type="button"
						className={cn(
							"ticket-type-config-page-header__tab-item",
							activeTab === tab.key && "ticket-type-config-page-header__tab-item--active",
						)}
						onClick={() => onTabChange(tab.key)}
					>
						{tab.label}
					</button>
				))}
			</nav>
		</div>
	);
}

/** @deprecated 使用 TicketTypeConfigPageHeader */
export type TicketTypeConfigHeaderProps = TicketTypeConfigPageHeaderProps;

/** @deprecated 使用 TicketTypeConfigPageHeader */
export const TicketTypeConfigHeader = TicketTypeConfigPageHeader;
