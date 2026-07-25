import {
	buildTicketConfigPath,
	parsePlatformTicketTypeConfigTab,
	type PlatformTicketTypeConfigTab,
} from "#src/pages/platform/ticket-config/ticket-config-path";

import { Navigate, useParams, useSearchParams } from "react-router";

/** 旧路径 `/platform/ticket-config/types/:typeId` 重定向到统一 query 路由 */
export default function PlatformTicketTypeConfigLegacyRedirect() {
	const { typeId: typeIdParam } = useParams();
	const [searchParams] = useSearchParams();
	const typeId = typeIdParam?.trim() ?? "";
	const tab = parsePlatformTicketTypeConfigTab(searchParams.get("tab")) as PlatformTicketTypeConfigTab;

	if (!typeId) {
		return <Navigate to={buildTicketConfigPath({ section: "types" })} replace />;
	}

	return <Navigate to={buildTicketConfigPath({ section: "types", typeId, tab })} replace />;
}
