import { buildTeamTemplateConfigPath, buildTicketConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { Navigate, useParams } from "react-router";

/** 旧路径 `.../collaboration` → query 协作配置 */
export default function TeamTemplateCollaborationLegacyRedirect() {
	const { templateId: templateIdParam } = useParams();
	const templateId = templateIdParam?.trim() ?? "";

	if (!templateId) {
		return <Navigate to={buildTicketConfigPath({ section: "templates" })} replace />;
	}

	return <Navigate to={buildTeamTemplateConfigPath(templateId, "collaboration")} replace />;
}
