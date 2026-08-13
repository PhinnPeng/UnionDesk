import { backendRequest } from "#src/utils/request";
import { requestBackendJson } from "#src/api/backend";

export interface AttachmentPresignRequest {
	file_name: string
	mime_type: string
	file_size: number
	target_type: string
	domain_id: number
}

export interface AttachmentPresignResponse {
	attachment_id: number
	upload_url: string
	expires_in: number
}

export interface AttachmentUploadResponse {
	attachment_id: number
	download_url: string
	storage_type: string
}

/** 契约保留：预签名直传（管理端默认不走此路径） */
export function presignAttachment(payload: AttachmentPresignRequest): Promise<AttachmentPresignResponse> {
	return requestBackendJson<AttachmentPresignResponse>("v1/attachments/presign", {
		method: "POST",
		json: payload,
	});
}

/** 契约保留：直传完成后确认 */
export function confirmAttachment(attachmentId: number): Promise<void> {
	return requestBackendJson<void>(`v1/attachments/${attachmentId}/confirm`, {
		method: "PUT",
	});
}

/** 默认上传：multipart 经后端代理写入 MinIO */
export async function uploadAttachment(
	domainId: number,
	file: File,
	targetType: string,
): Promise<AttachmentUploadResponse> {
	const form = new FormData();
	form.append("file", file);
	form.append("domain_id", String(domainId));
	form.append("target_type", targetType);
	const response = await backendRequest.post("v1/attachments/upload", {
		body: form,
	});
	return response.json<AttachmentUploadResponse>();
}
