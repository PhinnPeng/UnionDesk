import { requestBackendJson } from "#src/utils/request";

import type { PageResult } from "./audit";

export interface NotificationTemplateView {
	id: number
	scopeType: string
	scopeId: number
	eventCategory: string
	channel: string
	code: string
	titleTemplate: string
	contentTemplate: string
	isSecurity: boolean
	isUnsubscribable: boolean
	status: string
	createdAt?: string | null
	updatedAt?: string | null
}

export interface UpdateNotificationTemplateCommand {
	eventCategory: string
	channel: string
	code: string
	titleTemplate: string
	contentTemplate: string
	isSecurity?: boolean | null
	isUnsubscribable?: boolean | null
	status: string
}

export function fetchNotificationTemplates(domainId: number, params: { page?: number, page_size?: number } = {}): Promise<PageResult<NotificationTemplateView>> {
	const query = new URLSearchParams();
	query.set("page", String(params.page ?? 1));
	query.set("page_size", String(params.page_size ?? 20));
	return requestBackendJson<PageResult<NotificationTemplateView>>(`v1/admin/domains/${domainId}/notification-templates?${query.toString()}`);
}

export function updateNotificationTemplate(domainId: number, templateId: number, payload: UpdateNotificationTemplateCommand): Promise<NotificationTemplateView> {
	return requestBackendJson<NotificationTemplateView>(`v1/admin/domains/${domainId}/notification-templates/${templateId}`, {
		method: "PUT",
		json: payload,
	});
}
