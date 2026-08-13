import { requestBackendJson } from "#src/api/backend";

import type { PageResult } from "./audit";

export type AdminTicketListQuery = {
	page: number
	page_size: number
	status?: string
	/** 按受理人（员工账号 ID）筛选 */
	assignee?: number
	priority?: string
	keyword?: string
	/** 只看分配给我的待办 */
	assigned_to_me?: boolean
}

export interface TicketRow {
	id: number
	ticketNo: string
	businessDomainId: number
	businessDomainCode: string
	businessDomainName: string
	ticketTypeId: number
	ticketTypeName: string
	customerId: number
	assignedTo?: number | null
	title: string
	description?: string | null
	status: string
	priority: string
	source: string
	result?: string | null
	version: number
	customFieldsJson?: string | null
	slaFirstResponseDeadline?: string | null
	slaResolutionDeadline?: string | null
	slaFirstRespondedAt?: string | null
	slaResolvedAt?: string | null
	slaStatus?: string | null
	slaPausedDuration?: number
	slaPauseStartedAt?: string | null
	breachActionJson?: string | null
	createdAt?: string | null
	updatedAt?: string | null
	lastReplyAt?: string | null
	replyCount: number
}

export interface TicketReplyRow {
	id: number
	senderType?: string | null
	senderRole?: string | null
	staffAccountId?: number | null
	customerAccountId?: number | null
	replyType?: string | null
	content?: string | null
	createdAt?: string | null
}

export interface TicketHistoryRow {
	id: number
	action?: string | null
	fromValue?: string | null
	toValue?: string | null
	operatorSubjectId?: number | null
	operatorActorType?: string | null
	payloadJson?: string | null
	createdAt?: string | null
}

export interface TicketDetailResult {
	ticket: TicketRow
	replies: TicketReplyRow[]
	history: TicketHistoryRow[]
	watcherStaffAccountIds?: number[]
}

export interface ReplyTicketCommand {
	version: number
	content: string
	quickReplyTemplateId?: number | null
	attachmentIds?: number[]
}

export interface ClaimTicketCommand {
	version: number
}

export interface AssignTicketCommand {
	version: number
	assigneeStaffAccountId: number
}

export interface ReplaceWatchersCommand {
	watcherStaffAccountIds: number[]
}

export interface ChangeTicketStatusCommand {
	status: string
	version: number
	quickReplyTemplateId?: number | null
	content?: string | null
}

export interface MergeTicketCommand {
	version: number
	targetTicketId: number
	note?: string | null
}

function withDomainPath(domainId: number, ticketId: number, suffix = "") {
	return `v1/admin/domains/${domainId}/tickets/${ticketId}${suffix}`;
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

export function fetchAdminDomainTicketsPage(
	domainId: number,
	params: AdminTicketListQuery,
): Promise<PageResult<TicketRow>> {
	const query = buildQuery(params as Record<string, unknown>);
	return requestBackendJson<PageResult<TicketRow>>(`v1/admin/domains/${domainId}/tickets${query ? `?${query}` : ""}`);
}

export function fetchTicketDetail(domainId: number, ticketId: number): Promise<TicketDetailResult> {
	return requestBackendJson<TicketDetailResult>(withDomainPath(domainId, ticketId));
}

export function replyAdminTicket(domainId: number, ticketId: number, payload: ReplyTicketCommand): Promise<{ id: number }> {
	return requestBackendJson<{ id: number }>(withDomainPath(domainId, ticketId, "/replies"), {
		method: "POST",
		json: payload,
	});
}

export function claimAdminTicket(domainId: number, ticketId: number, payload: ClaimTicketCommand): Promise<{ id: number }> {
	return requestBackendJson<{ id: number }>(withDomainPath(domainId, ticketId, "/claim"), {
		method: "POST",
		json: payload,
	});
}

export function assignAdminTicket(domainId: number, ticketId: number, payload: AssignTicketCommand): Promise<{ id: number }> {
	return requestBackendJson<{ id: number }>(withDomainPath(domainId, ticketId, "/assign"), {
		method: "POST",
		json: payload,
	});
}

export function replaceAdminTicketWatchers(
	domainId: number,
	ticketId: number,
	payload: ReplaceWatchersCommand,
): Promise<{ id: number }> {
	return requestBackendJson<{ id: number }>(withDomainPath(domainId, ticketId, "/watchers"), {
		method: "POST",
		json: payload,
	});
}

export function updateAdminTicketStatus(domainId: number, ticketId: number, payload: ChangeTicketStatusCommand): Promise<{ id: number }> {
	return requestBackendJson<{ id: number }>(withDomainPath(domainId, ticketId, "/status"), {
		method: "PATCH",
		json: payload,
	});
}

export function mergeAdminTicket(domainId: number, ticketId: number, payload: MergeTicketCommand): Promise<{ id: number }> {
	return requestBackendJson<{ id: number }>(withDomainPath(domainId, ticketId, "/merge"), {
		method: "POST",
		json: payload,
	});
}

export {
	confirmAttachment,
	presignAttachment,
	uploadAttachment,
} from "./attachment";
export type { AttachmentPresignRequest, AttachmentPresignResponse, AttachmentUploadResponse } from "./attachment";
