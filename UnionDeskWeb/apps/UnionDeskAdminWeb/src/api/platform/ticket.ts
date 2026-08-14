import { requestBackendJson } from "#src/utils/request";

import type { PageResult } from "./audit";

export type AdminTicketListQuery = {
	page: number
	page_size: number
	status?: string
	/** 按受理人（员工账号 ID）筛选 */
	assignee?: string
	priority?: string
	keyword?: string
	/** 只看分配给我的待办 */
	assigned_to_me?: boolean
}

export interface TicketRow {
	id: string
	ticketNo: string
	businessDomainId: string
	businessDomainCode: string
	businessDomainName: string
	ticketTypeId: string
	ticketTypeName: string
	customerId: string
	customerName?: string | null
	assignedTo?: number | null
	/** 受理人姓名（员工账号），后端联查不到时为空，前端兜底「员工 #id」 */
	assigneeName?: string | null
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
	id: string
	senderType?: string | null
	senderRole?: string | null
	staffAccountId?: string | null
	customerAccountId?: string | null
	replyType?: string | null
	content?: string | null
	createdAt?: string | null
}

export interface TicketHistoryRow {
	id: string
	action?: string | null
	fromValue?: string | null
	toValue?: string | null
	operatorSubjectId?: string | null
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
	quickReplyTemplateId?: string | null
	attachmentIds?: number[]
}

export interface ClaimTicketCommand {
	version: number
}

export interface AssignTicketCommand {
	version: number
	assigneeStaffAccountId: string
}

export interface ReplaceWatchersCommand {
	watcherStaffAccountIds: number[]
}

export interface ChangeTicketStatusCommand {
	status: string
	version: number
	quickReplyTemplateId?: string | null
	content?: string | null
}

export interface MergeTicketCommand {
	version: number
	targetTicketId: string
	note?: string | null
}

function withDomainPath(domainId: string, ticketId: string, suffix = "") {
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
	domainId: string,
	params: AdminTicketListQuery,
): Promise<PageResult<TicketRow>> {
	const query = buildQuery(params as Record<string, unknown>);
	return requestBackendJson<PageResult<TicketRow>>(`v1/admin/domains/${domainId}/tickets${query ? `?${query}` : ""}`);
}

export function fetchTicketDetail(domainId: string, ticketId: string): Promise<TicketDetailResult> {
	return requestBackendJson<TicketDetailResult>(withDomainPath(domainId, ticketId));
}

export function replyAdminTicket(domainId: string, ticketId: string, payload: ReplyTicketCommand): Promise<{ id: string }> {
	return requestBackendJson<{ id: string }>(withDomainPath(domainId, ticketId, "/replies"), {
		method: "POST",
		json: payload,
	});
}

export function claimAdminTicket(domainId: string, ticketId: string, payload: ClaimTicketCommand): Promise<{ id: string }> {
	return requestBackendJson<{ id: string }>(withDomainPath(domainId, ticketId, "/claim"), {
		method: "POST",
		json: payload,
	});
}

export function assignAdminTicket(domainId: string, ticketId: string, payload: AssignTicketCommand): Promise<{ id: string }> {
	return requestBackendJson<{ id: string }>(withDomainPath(domainId, ticketId, "/assign"), {
		method: "POST",
		json: payload,
	});
}

export function replaceAdminTicketWatchers(
	domainId: string,
	ticketId: string,
	payload: ReplaceWatchersCommand,
): Promise<{ id: string }> {
	return requestBackendJson<{ id: string }>(withDomainPath(domainId, ticketId, "/watchers"), {
		method: "POST",
		json: payload,
	});
}

export function updateAdminTicketStatus(domainId: string, ticketId: string, payload: ChangeTicketStatusCommand): Promise<{ id: string }> {
	return requestBackendJson<{ id: string }>(withDomainPath(domainId, ticketId, "/status"), {
		method: "PATCH",
		json: payload,
	});
}

export function mergeAdminTicket(domainId: string, ticketId: string, payload: MergeTicketCommand): Promise<{ id: string }> {
	return requestBackendJson<{ id: string }>(withDomainPath(domainId, ticketId, "/merge"), {
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
