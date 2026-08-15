import { requestBackendJson } from "#src/utils/request";
import { request } from "#src/utils/request";

import type { PageResult } from "./audit";

export type ClaimRuleStrategy = "least_loaded" | "fixed";

export interface ClaimRuleView {
	id: string
	businessDomainId: string
	name: string
	enabled: boolean
	matchTicketTypeId?: string | null
	matchPriorityLevelId?: string | null
	strategy: ClaimRuleStrategy
	assigneeStaffAccountId?: string | null
	graceMinutes: number
	createdAt?: string | null
	updatedAt?: string | null
}

export interface ClaimRuleCommand {
	name: string
	enabled: boolean
	matchTicketTypeId?: string | null
	matchPriorityLevelId?: string | null
	strategy: ClaimRuleStrategy
	assigneeStaffAccountId?: string | null
	graceMinutes: number
}

export function fetchClaimRules(domainId: string, params: { page?: number, page_size?: number } = {}): Promise<PageResult<ClaimRuleView>> {
	const query = new URLSearchParams();
	query.set("page", String(params.page ?? 1));
	query.set("page_size", String(params.page_size ?? 20));
	return requestBackendJson<PageResult<ClaimRuleView>>(`v1/admin/domains/${domainId}/ticket-claim-rules?${query.toString()}`);
}

export function createClaimRule(domainId: string, payload: ClaimRuleCommand): Promise<ClaimRuleView> {
	return requestBackendJson<ClaimRuleView>(`v1/admin/domains/${domainId}/ticket-claim-rules`, {
		method: "POST",
		json: payload,
	});
}

export function updateClaimRule(domainId: string, ruleId: string, payload: ClaimRuleCommand): Promise<ClaimRuleView> {
	return requestBackendJson<ClaimRuleView>(`v1/admin/domains/${domainId}/ticket-claim-rules/${ruleId}`, {
		method: "PUT",
		json: payload,
	});
}

export function deleteClaimRule(domainId: string, ruleId: string): Promise<void> {
	return request.delete(`v1/admin/domains/${domainId}/ticket-claim-rules/${ruleId}`).then(() => undefined);
}
