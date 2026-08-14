import type { Options } from "ky";

import { parseApiResponse } from "#src/api/utils";
import { HttpRequestError } from "#src/utils/http-request-error";

import { request } from "./index";

export type BackendJsonRequestOptions = {
	method?: string
	json?: unknown
	headers?: HeadersInit
	/** 为 true 时不弹出全局错误提示，由调用方自行展示友好文案 */
	silentError?: boolean
};

/**
 * 统一请求处理层入口（自 api/backend.ts 并入，2026-08-14 收敛）。
 *
 * 语义与旧版 backend.ts 保持一致：
 * - HTTP 错误 → 解析响应体 {code, message} → 抛 HttpRequestError(status, message, code)
 * - 成功响应 → parseApiResponse 信封解包（success/code/message/data）
 * - silentError=true 时跳过全局错误提示（由调用方处理）
 */
export async function requestBackendJson<T>(path: string, options: BackendJsonRequestOptions = {}): Promise<T> {
	const response = await request(path, {
		method: options.method ?? (options.json !== undefined ? "POST" : "GET"),
		json: options.json,
		headers: options.headers,
		silentError: options.silentError,
		throwHttpErrors: false,
	} as Options);

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
		throw new HttpRequestError(response.status, apiMessage, apiCode);
	}

	const payload = await response.json().catch(() => null);
	return parseApiResponse<T>(payload);
}
