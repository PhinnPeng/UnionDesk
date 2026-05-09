import { requestBackendJson } from "#src/api/backend";

export interface PageResult<T> {
	total: number
	list: T[]
}

export interface PlatformAuditLogView {
	id: number
	businessDomainId?: number | null
	operatorSubjectId?: number | null
	operatorName?: string | null
	operatorActorType?: string | null
	target?: string | null
	action?: string | null
	detail?: string | null
	result?: string | null
	occurredAt?: string | null
	requestId?: string | null
}

export interface LoginLogView {
	id: number
	subjectId?: number | null
	operatorName?: string | null
	businessDomainId?: number | null
	loginName?: string | null
	portalType?: string | null
	ip?: string | null
	userAgent?: string | null
	result?: string | null
	failReason?: string | null
	createdAt?: string | null
}

export interface PlatformAuditLogQuery {
	page?: number
	page_size?: number
	domain_id?: number | string | null
	operator?: string
	action?: string
	startTime?: string | null
	endTime?: string | null
}

export interface LoginLogQuery {
	page?: number
	page_size?: number
	subject_id?: number | string | null
	portal_type?: string
	result?: string
	startTime?: string | null
	endTime?: string | null
}

function buildQuery(params: Record<string, unknown>) {
	const query = new URLSearchParams();
	for (const [key, value] of Object.entries(params)) {
		if (value === undefined || value === null || value === "") {
			continue;
		}
		query.set(key, String(value));
	}
	return query.toString();
}

export function fetchPlatformAuditLogs(params: PlatformAuditLogQuery): Promise<PageResult<PlatformAuditLogView>> {
	const query = buildQuery(params as Record<string, unknown>);
	return requestBackendJson<PageResult<PlatformAuditLogView>>(`v1/admin/audit-logs${query ? `?${query}` : ""}`);
}

export function fetchLoginLogsPage(params: LoginLogQuery): Promise<PageResult<LoginLogView>> {
	const query = buildQuery(params as Record<string, unknown>);
	return requestBackendJson<PageResult<LoginLogView>>(`v1/admin/login-logs${query ? `?${query}` : ""}`);
}
