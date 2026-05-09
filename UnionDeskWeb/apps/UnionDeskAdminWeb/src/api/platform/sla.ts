import { requestBackendJson } from "#src/api/backend";
import { backendRequest } from "#src/utils/request";

import type { PageResult } from "./audit";

export interface SlaRuleView {
	id: number
	businessDomainId: number
	name: string
	ticketTypeId?: number | null
	priorityLevelId?: number | null
	calendarId?: number | null
	firstResponseMinutes?: number | null
	resolutionMinutes?: number | null
	isUrgentConfig: boolean
	breachAction?: Record<string, unknown>
	createdAt?: string | null
	updatedAt?: string | null
}

export interface SlaCalendarView {
	id: number
	businessDomainId: number
	name: string
	config?: Record<string, unknown>
	createdAt?: string | null
	updatedAt?: string | null
}

export interface SlaRuleCommand {
	name: string
	ticketTypeId?: number | null
	priorityLevelId?: number | null
	calendarId?: number | null
	firstResponseMinutes?: number | null
	resolutionMinutes?: number | null
	isUrgentConfig?: boolean | null
	breachAction?: Record<string, unknown> | null
}

export interface SlaCalendarCommand {
	name: string
	config?: Record<string, unknown> | null
}

export function fetchSlaRules(domainId: number, params: { page?: number, page_size?: number } = {}): Promise<PageResult<SlaRuleView>> {
	const query = new URLSearchParams();
	query.set("page", String(params.page ?? 1));
	query.set("page_size", String(params.page_size ?? 20));
	return requestBackendJson<PageResult<SlaRuleView>>(`v1/admin/domains/${domainId}/sla-rules?${query.toString()}`);
}

export function createSlaRule(domainId: number, payload: SlaRuleCommand): Promise<SlaRuleView> {
	return requestBackendJson<SlaRuleView>(`v1/admin/domains/${domainId}/sla-rules`, {
		method: "POST",
		json: payload,
	});
}

export function updateSlaRule(domainId: number, ruleId: number, payload: SlaRuleCommand): Promise<SlaRuleView> {
	return requestBackendJson<SlaRuleView>(`v1/admin/domains/${domainId}/sla-rules/${ruleId}`, {
		method: "PUT",
		json: payload,
	});
}

export function deleteSlaRule(domainId: number, ruleId: number): Promise<void> {
	return backendRequest.delete(`v1/admin/domains/${domainId}/sla-rules/${ruleId}`).then(() => undefined);
}

export function fetchSlaCalendars(domainId: number, params: { page?: number, page_size?: number } = {}): Promise<PageResult<SlaCalendarView>> {
	const query = new URLSearchParams();
	query.set("page", String(params.page ?? 1));
	query.set("page_size", String(params.page_size ?? 20));
	return requestBackendJson<PageResult<SlaCalendarView>>(`v1/admin/domains/${domainId}/sla-calendars?${query.toString()}`);
}

export function createSlaCalendar(domainId: number, payload: SlaCalendarCommand): Promise<SlaCalendarView> {
	return requestBackendJson<SlaCalendarView>(`v1/admin/domains/${domainId}/sla-calendars`, {
		method: "POST",
		json: payload,
	});
}

export function updateSlaCalendar(domainId: number, calendarId: number, payload: SlaCalendarCommand): Promise<SlaCalendarView> {
	return requestBackendJson<SlaCalendarView>(`v1/admin/domains/${domainId}/sla-calendars/${calendarId}`, {
		method: "PUT",
		json: payload,
	});
}
