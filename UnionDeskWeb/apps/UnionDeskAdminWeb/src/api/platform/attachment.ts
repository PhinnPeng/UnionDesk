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
	expires_in_seconds: number
}

export interface AttachmentUploadResponse {
	attachment_id: number
	download_url: string
	storage_type: string
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

