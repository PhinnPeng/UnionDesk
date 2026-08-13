import type { LoginLogView, PageResult } from "#src/api/platform/audit";

import { requestBackendJson } from "#src/api/backend";
import { fetchLoginLogsPage } from "#src/api/platform/audit";

/** 后端 /api/v1/dashboard/overview 聚合返回的实时计数。 */
interface OverviewCounts {
	domainCount: number;
	staffCount: number;
	activeUserCount: number;
	disabledUserCount: number;
	offboardUserCount: number;
	customerCount: number;
	ticketCount: number;
	consultationCount: number;
	recentAuditCount: number;
}

export interface PlatformOverview extends OverviewCounts {
	pendingImportTaskCount: number;
	announcementCount: number;
	loginLogs: LoginLogView[];
}

function fetchPlatformLoginLogs(limit = 5): Promise<LoginLogView[]> {
	return fetchLoginLogsPage({
		page: 1,
		page_size: limit,
		event_type: "LOGIN",
	}).then((page: PageResult<LoginLogView>) => page.list);
}

export async function fetchPlatformOverview(): Promise<PlatformOverview> {
	const [counts, loginLogs] = await Promise.all([
		requestBackendJson<OverviewCounts>("v1/dashboard/overview"),
		fetchPlatformLoginLogs(),
	]);

	return {
		...counts,
		pendingImportTaskCount: 0,
		announcementCount: 0,
		loginLogs,
	};
}
