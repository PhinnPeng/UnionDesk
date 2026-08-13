import { TicketAttributesPanel } from "./attributes/ticket-attributes-panel";
import { TicketStatusesPanel } from "./statuses/ticket-statuses-panel";
import { TeamTemplatesPanel } from "./templates/team-templates-panel";
import { TeamTemplateConfigPage } from "./templates/config/team-template-config-page";
import { TicketConfigShell } from "./ticket-config-shell";
import {
	parseTeamTemplateConfigModule,
	parseTicketConfigSection,
	resolveEffectiveTicketConfigSection,
} from "./ticket-config-path";
import { PlatformTicketTypeConfigContent } from "./types/config/platform-ticket-type-config-content";
import { navigatePlatformTicketTypeConfig } from "./types/open-config-tab";
import { TicketTypesPanel } from "./types/ticket-types-panel";

import { useAuth } from "#src/hooks/use-auth";
import {
	PLATFORM_TICKET_CONFIG_ATTR_READ,
	PLATFORM_TICKET_CONFIG_STATUS_READ,
	PLATFORM_TICKET_CONFIG_TEMPLATE_READ,
	PLATFORM_TICKET_CONFIG_TYPE_READ,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router";

export default function PlatformTicketConfigPage() {
	const navigate = useNavigate();
	const [searchParams, setSearchParams] = useSearchParams();
	const { hasPermission } = useAuth();

	const canViewAttributes = hasPermission(PLATFORM_TICKET_CONFIG_ATTR_READ);
	const canViewTypes = hasPermission(PLATFORM_TICKET_CONFIG_TYPE_READ);
	const canViewStatuses = hasPermission(PLATFORM_TICKET_CONFIG_STATUS_READ);
	const canViewTemplates = hasPermission(PLATFORM_TICKET_CONFIG_TEMPLATE_READ);

	const sectionParam = parseTicketConfigSection(searchParams.get("section"));
	const effectiveSection = resolveEffectiveTicketConfigSection(
		sectionParam,
		canViewAttributes,
		canViewTypes,
		canViewStatuses,
		canViewTemplates,
	);
	const typeId = searchParams.get("typeId")?.trim() ?? "";
	const templateId = searchParams.get("templateId")?.trim() ?? "";
	const templateModule = parseTeamTemplateConfigModule(searchParams.get("module"));

	useEffect(() => {
		if (sectionParam === effectiveSection) {
			return;
		}
		setSearchParams((prev) => {
			const next = new URLSearchParams(prev);
			next.set("section", effectiveSection);
			if (effectiveSection !== "types") {
				next.delete("typeId");
				next.delete("tab");
			}
			if (effectiveSection !== "templates") {
				next.delete("templateId");
				next.delete("module");
			}
			return next;
		}, { replace: true });
	}, [effectiveSection, sectionParam, setSearchParams]);

	const handleBackToTypesList = () => {
		setSearchParams((prev) => {
			const next = new URLSearchParams(prev);
			next.set("section", "types");
			next.delete("typeId");
			next.delete("tab");
			return next;
		}, { replace: true });
	};

	if (effectiveSection === "templates" && templateId) {
		return <TeamTemplateConfigPage templateId={templateId} module={templateModule} />;
	}

	const renderContent = () => {
		if (effectiveSection === "attributes") {
			return <TicketAttributesPanel scope="platform" />;
		}
		if (effectiveSection === "statuses") {
			return <TicketStatusesPanel />;
		}
		if (effectiveSection === "templates") {
			return <TeamTemplatesPanel />;
		}
		if (typeId) {
			return (
				<PlatformTicketTypeConfigContent
					typeId={typeId}
					onBack={handleBackToTypesList}
				/>
			);
		}
		return (
			<TicketTypesPanel
				onAttributeEdit={record => navigatePlatformTicketTypeConfig(navigate, record, "attributes")}
				onWorkflowEdit={record => navigatePlatformTicketTypeConfig(navigate, record, "workflow")}
			/>
		);
	};

	return (
		<TicketConfigShell>
			{renderContent()}
		</TicketConfigShell>
	);
}
