import type { TeamTemplate } from "@uniondesk/shared";
import { fetchTeamTemplate, toErrorMessage } from "@uniondesk/shared";

import { PLATFORM_TICKET_CONFIG_TEMPLATE_READ } from "#src/pages/platform/domains/platform-domain-permissions";
import { TicketConfigShell } from "#src/pages/platform/ticket-config/ticket-config-shell";
import {
	buildTeamTemplateConfigPath,
	buildTicketConfigPath,
	type TeamTemplateConfigModule,
} from "#src/pages/platform/ticket-config/ticket-config-path";

import shellStyles from "#src/pages/platform/ticket-config/index.module.less";

import { ArrowLeftOutlined } from "@ant-design/icons";
import { App, Button, Empty, Spin } from "antd";
import { useCallback, useEffect, useState } from "react";
import { Navigate, useNavigate } from "react-router";

import { BasicInfoPanel } from "./basic-info-panel";
import { CollaborationPanel } from "./collaboration-panel";

import "./team-template-config.less";

const MODULES: { key: TeamTemplateConfigModule; label: string }[] = [
	{ key: "basic", label: "基础信息" },
	{ key: "collaboration", label: "协作配置" },
];

export interface TeamTemplateConfigPageProps {
	templateId: string;
	module: TeamTemplateConfigModule;
}

export function TeamTemplateConfigPage({ templateId: templateIdProp, module }: TeamTemplateConfigPageProps) {
	const templateId = templateIdProp.trim();
	const navigate = useNavigate();
	const { message } = App.useApp();

	const [loading, setLoading] = useState(true);
	const [template, setTemplate] = useState<TeamTemplate | null>(null);

	const loadTemplate = useCallback(async () => {
		if (!templateId) {
			setTemplate(null);
			setLoading(false);
			return;
		}
		setLoading(true);
		try {
			const data = await fetchTeamTemplate(templateId);
			setTemplate(data);
		}
		catch (error) {
			message.error(toErrorMessage(error));
			setTemplate(null);
		}
		finally {
			setLoading(false);
		}
	}, [message, templateId]);

	useEffect(() => {
		void loadTemplate();
	}, [loadTemplate]);

	if (!templateId) {
		return <Navigate to={buildTicketConfigPath({ section: "templates" })} replace />;
	}

	const moduleSider = (
		<aside className={shellStyles.sider}>
			<nav className={shellStyles.siderNav}>
				{MODULES.map(item => (
					<button
						key={item.key}
						type="button"
						className={`${shellStyles.siderItem} ${module === item.key ? shellStyles.siderItemActive : ""}`}
						onClick={() => {
							navigate(buildTeamTemplateConfigPath(template?.id ?? templateId, item.key), { replace: true });
						}}
					>
						<span>{item.label}</span>
					</button>
				))}
			</nav>
		</aside>
	);

	return (
		<TicketConfigShell sider={moduleSider} auth={PLATFORM_TICKET_CONFIG_TEMPLATE_READ}>
			<div className="team-template-config">
				<Button
					type="link"
					icon={<ArrowLeftOutlined />}
					className="team-template-config__back"
					onClick={() => navigate(buildTicketConfigPath({ section: "templates" }), { replace: true })}
				>
					返回团队模板
				</Button>

				{loading
					? <div style={{ padding: 48, textAlign: "center" }}><Spin /></div>
					: !template
						? <Empty description="团队模板不存在" />
						: (
							<div className="team-template-config__content">
								{module === "basic"
									? (
										<BasicInfoPanel
											template={template}
											onUpdated={setTemplate}
										/>
									)
									: (
										<CollaborationPanel
											template={template}
											onUpdated={setTemplate}
										/>
									)}
							</div>
						)}
			</div>
		</TicketConfigShell>
	);
}
