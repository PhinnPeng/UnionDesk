import { AUTH_HEADER, AUTH_REFRESH_PATH, AUTH_WHITE_LIST_PATHS, CLIENT_CODE_HEADER, LANG_HEADER } from "#src/utils/request/constants";
import { handleErrorResponse } from "#src/utils/request/error-response";
import { useAuthStore } from "#src/store/auth";
import { usePreferencesStore } from "#src/store/preferences";

import { HttpRequestError } from "#src/utils/http-request-error";

import { parseApiResponse } from "./utils";

const BACKEND_API_BASE_URL = "http://localhost:8080/api";

const requestWhiteList = [
	...AUTH_WHITE_LIST_PATHS.map(path => `v1${path}`),
	`v1${AUTH_REFRESH_PATH}`,
];

type BackendJsonRequestOptions = {
	method?: string
	json?: unknown
	headers?: HeadersInit
	/** 为 true 时不弹出全局错误提示，由调用方自行展示友好文案 */
	silentError?: boolean
};

function buildBackendUrl(path: string) {
	return `${BACKEND_API_BASE_URL}/${path.replace(/^\/+/, "")}`;
}

export async function requestBackendJson<T>(path: string, options: BackendJsonRequestOptions = {}): Promise<T> {
	const headers = new Headers(options.headers);
	headers.set(CLIENT_CODE_HEADER, "ud-admin-web");
	headers.set(LANG_HEADER, usePreferencesStore.getState().language);

	const token = useAuthStore.getState().token;
	const isWhiteRequest = requestWhiteList.some(url => path.endsWith(url));
	if (token && !isWhiteRequest) {
		headers.set(AUTH_HEADER, `Bearer ${token}`);
	}

	let body: string | undefined;
	if (options.json !== undefined) {
		headers.set("Content-Type", "application/json");
		body = JSON.stringify(options.json);
	}

	const response = await fetch(buildBackendUrl(path), {
		method: options.method ?? (options.json !== undefined ? "POST" : "GET"),
		headers,
		body,
	});

	if (!response.ok) {
		let apiCode: string | undefined;
		let apiMessage = response.statusText || "请求失败";
		try {
			const errorPayload = await response.clone().json() as {
				code?: number | string
				message?: string
			};
			if (typeof errorPayload.message === "string" && errorPayload.message.trim()) {
				apiMessage = errorPayload.message.trim();
			}
			if (errorPayload.code !== undefined && errorPayload.code !== null) {
				apiCode = String(errorPayload.code);
			}
		}
		catch {
			// 非 JSON 错误体时保留默认文案
		}
		if (!options.silentError) {
			await handleErrorResponse(response);
		}
		throw new HttpRequestError(response.status, apiMessage, apiCode);
	}

	const payload = await response.json().catch(() => null);
	return parseApiResponse<T>(payload);
}
