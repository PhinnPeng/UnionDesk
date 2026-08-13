import { buildTeamTemplateConfigPath, buildTicketConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { Navigate, useParams } from "react-router";

/** 旧路径 `/platform/ticket-config/templates/:templateId` → query 协作配置 */
export default function TeamTemplateConfigEntryRedirect() {
	const { templateId: templateIdParam } = useParams();
	const templateId = templateIdParam?.trim() ?? "";

	if (!templateId) {
		return <Navigate to={buildTicketConfigPath({ section: "templates" })} replace />;
	}

	return <Navigate to={buildTeamTemplateConfigPath(templateId, "collaboration")} replace />;
}
