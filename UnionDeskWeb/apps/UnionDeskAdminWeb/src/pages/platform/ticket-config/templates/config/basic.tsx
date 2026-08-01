import { buildTeamTemplateConfigPath, buildTicketConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { Navigate, useParams } from "react-router";

/** 旧路径 `.../basic` → query 基础信息 */
export default function TeamTemplateBasicLegacyRedirect() {
	const { templateId: templateIdParam } = useParams();
	const templateId = templateIdParam?.trim() ?? "";

	if (!templateId) {
		return <Navigate to={buildTicketConfigPath({ section: "templates" })} replace />;
	}

	return <Navigate to={buildTeamTemplateConfigPath(templateId, "basic")} replace />;
}
