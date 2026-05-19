import type { IamUser, LoginLogView } from "@uniondesk/shared";

import { requestBackendJson } from "#src/api/backend";

import { fetchBusinessDomains } from "./domain";
import { fetchPlatformOffboardPoolUsers, fetchPlatformUsers } from "./iam";

export interface PlatformOverview {
	domainCount: number;
	activeUserCount: number;
	disabledUserCount: number;
	offboardUserCount: number;
	pendingImportTaskCount: number;
	announcementCount: number;
	recentAuditCount: number;
	loginLogs: LoginLogView[];
}

function fetchPlatformLoginLogs(limit = 5): Promise<LoginLogView[]> {
	return requestBackendJson<LoginLogView[]>(`v1/auth/login-logs?limit=${limit}`);
}

function countActiveUsers(users: IamUser[]): number {
	return users.filter(user => user.employmentStatus !== "offboarded").length;
}

function countDisabledUsers(users: IamUser[]): number {
	return users.filter(user => user.employmentStatus !== "offboarded" && user.status !== 1).length;
}

export async function fetchPlatformOverview(): Promise<PlatformOverview> {
	const [domains, users, offboardUsers, loginLogs] = await Promise.all([
		fetchBusinessDomains(),
		fetchPlatformUsers(),
		fetchPlatformOffboardPoolUsers(),
		fetchPlatformLoginLogs(),
	]);

	return {
		domainCount: domains.length,
		activeUserCount: countActiveUsers(users),
		disabledUserCount: countDisabledUsers(users),
		offboardUserCount: offboardUsers.length,
		pendingImportTaskCount: 0,
		announcementCount: 0,
		recentAuditCount: loginLogs.length,
		loginLogs,
	};
}
