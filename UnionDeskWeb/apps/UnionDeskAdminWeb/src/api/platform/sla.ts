import { requestBackendJson } from "#src/utils/request";
import { request } from "#src/utils/request";

import type { PageResult } from "./audit";

export interface SlaRuleView {
	id: string
	businessDomainId: string
	name: string
	ticketTypeId?: string | null
	priorityLevelId?: string | null
	calendarId?: string | null
	firstResponseMinutes?: number | null
	resolutionMinutes?: number | null
	isUrgentConfig: boolean
	breachAction?: Record<string, unknown>
	createdAt?: string | null
	updatedAt?: string | null
}

export interface SlaCalendarView {
	id: string
	businessDomainId: string
	name: string
	config?: Record<string, unknown>
	createdAt?: string | null
	updatedAt?: string | null
}

export interface SlaRuleCommand {
	name: string
	ticketTypeId?: string | null
	priorityLevelId?: string | null
	calendarId?: string | null
	firstResponseMinutes?: number | null
	resolutionMinutes?: number | null
	isUrgentConfig?: boolean | null
	breachAction?: Record<string, unknown> | null
}

export interface SlaCalendarCommand {
	name: string
	config?: Record<string, unknown> | null
}

export function fetchSlaRules(domainId: string, params: { page?: number, page_size?: number } = {}): Promise<PageResult<SlaRuleView>> {
	const query = new URLSearchParams();
	query.set("page", String(params.page ?? 1));
	query.set("page_size", String(params.page_size ?? 20));
	return requestBackendJson<PageResult<SlaRuleView>>(`v1/admin/domains/${domainId}/sla-rules?${query.toString()}`);
}

export function createSlaRule(domainId: string, payload: SlaRuleCommand): Promise<SlaRuleView> {
	return requestBackendJson<SlaRuleView>(`v1/admin/domains/${domainId}/sla-rules`, {
		method: "POST",
		json: payload,
	});
}

export function updateSlaRule(domainId: string, ruleId: string, payload: SlaRuleCommand): Promise<SlaRuleView> {
	return requestBackendJson<SlaRuleView>(`v1/admin/domains/${domainId}/sla-rules/${ruleId}`, {
		method: "PUT",
		json: payload,
	});
}

export function deleteSlaRule(domainId: string, ruleId: string): Promise<void> {
	return request.delete(`v1/admin/domains/${domainId}/sla-rules/${ruleId}`).then(() => undefined);
}

export function fetchSlaCalendars(domainId: string, params: { page?: number, page_size?: number } = {}): Promise<PageResult<SlaCalendarView>> {
	const query = new URLSearchParams();
	query.set("page", String(params.page ?? 1));
	query.set("page_size", String(params.page_size ?? 20));
	return requestBackendJson<PageResult<SlaCalendarView>>(`v1/admin/domains/${domainId}/sla-calendars?${query.toString()}`);
}

export function createSlaCalendar(domainId: string, payload: SlaCalendarCommand): Promise<SlaCalendarView> {
	return requestBackendJson<SlaCalendarView>(`v1/admin/domains/${domainId}/sla-calendars`, {
		method: "POST",
		json: payload,
	});
}

export function updateSlaCalendar(domainId: string, calendarId: string, payload: SlaCalendarCommand): Promise<SlaCalendarView> {
	return requestBackendJson<SlaCalendarView>(`v1/admin/domains/${domainId}/sla-calendars/${calendarId}`, {
		method: "PUT",
		json: payload,
	});
}

export function deleteSlaCalendar(domainId: string, calendarId: string): Promise<void> {
	return request.delete(`v1/admin/domains/${domainId}/sla-calendars/${calendarId}`).then(() => undefined);
}
