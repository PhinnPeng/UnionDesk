import { requestBackendJson } from "#src/api/backend";
import { backendRequest } from "#src/utils/request";

import type { PageResult } from "./audit";
import type { AttachmentPresignRequest, AttachmentPresignResponse, AttachmentUploadResponse } from "./attachment";

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

export function presignAttachment(payload: AttachmentPresignRequest): Promise<AttachmentPresignResponse> {
	return requestBackendJson<AttachmentPresignResponse>("v1/attachments/presign", {
		method: "POST",
		json: payload,
	});
}

export async function uploadAttachmentLocal(form: FormData): Promise<AttachmentUploadResponse> {
	const response = await backendRequest.post("v1/attachments/upload", {
		body: form,
	});
	return response.json<AttachmentUploadResponse>();
}
