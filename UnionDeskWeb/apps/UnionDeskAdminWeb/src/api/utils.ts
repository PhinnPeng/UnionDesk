type ApiEnvelope<T> = {
	success?: boolean
	code?: number | string
	message?: string
	data?: T
	result?: T
};

function isApiStatusCode(code: unknown) {
	if (typeof code === "number") {
		return true;
	}
	if (typeof code === "string") {
		const normalized = code.trim();
		return normalized.length > 0 && /^\d+$/.test(normalized);
	}
	return false;
}

/** 区分统一响应包装与业务实体（如 RoleView 的 code 字段为角色编码，不是 API 状态码） */
function isApiEnvelope(payload: object): payload is ApiEnvelope<unknown> {
	if (typeof (payload as ApiEnvelope<unknown>).success === "boolean") {
		return true;
	}
	if ("data" in payload || "result" in payload) {
		return true;
	}
	if ("message" in payload && isApiStatusCode((payload as ApiEnvelope<unknown>).code)) {
		return true;
	}
	return false;
}

function isFailureEnvelope(envelope: ApiEnvelope<unknown>) {
	if (envelope.success === false) {
		return true;
	}
	if (typeof envelope.code === "number") {
		return envelope.code !== 0;
	}
	if (typeof envelope.code === "string") {
		const normalizedCode = envelope.code.trim().toUpperCase();
		if (!normalizedCode) {
			return false;
		}
		return !["0", "OK", "SUCCESS"].includes(normalizedCode);
	}
	return false;
}

export function parseApiResponse<T>(payload: unknown): T {
	if (payload && typeof payload === "object" && !Array.isArray(payload)) {
		const envelope = payload as ApiEnvelope<T>;
		if (isApiEnvelope(envelope)) {
			if (isFailureEnvelope(envelope)) {
				throw new Error(envelope.message?.trim() || "请求失败");
			}
			if ("data" in envelope && envelope.data !== undefined) {
				return envelope.data as T;
			}
			if ("result" in envelope && envelope.result !== undefined) {
				return envelope.result as T;
			}
		}
	}
	return payload as T;
}
