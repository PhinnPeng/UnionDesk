import type { Options } from "ky";

import { loginPath } from "#src/router/extra-info";
import { useAuthStore } from "#src/store/auth";
import { usePreferencesStore } from "#src/store/preferences";
import ky from "ky";

import { AUTH_HEADER, AUTH_REFRESH_PATH, AUTH_WHITE_LIST_PATHS, CLIENT_CODE_HEADER, LANG_HEADER } from "./constants";
import { handleErrorResponse } from "./error-response";
import { globalProgress } from "./global-progress";
import { goLogin } from "./go-login";
import { refreshTokenAndRetry } from "./refresh";

type RequestOptions = Options & {
	ignoreLoading?: boolean
};

type Hooks = NonNullable<Options["hooks"]>;
type BeforeRequestHook = NonNullable<Hooks["beforeRequest"]>[number];
type AfterResponseHook = NonNullable<Hooks["afterResponse"]>[number];

const requestWhiteList = [
	loginPath,
	...AUTH_WHITE_LIST_PATHS,
	AUTH_REFRESH_PATH,
];

const API_TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT) || 10000;

const beforeRequestHook = ((request: Request, options: RequestOptions) => {
	const requestOptions = options;
	const clientCode = useAuthStore.getState().clientCode || "ud-admin-web";
	const requestUrl = request.url ?? "";
	if (typeof request.headers?.set === "function") {
		request.headers.set(CLIENT_CODE_HEADER, clientCode);
		request.headers.set(LANG_HEADER, usePreferencesStore.getState().language);
	}
	else if (requestOptions) {
		const headers = new Headers(requestOptions.headers as HeadersInit | undefined);
		headers.set(CLIENT_CODE_HEADER, clientCode);
		headers.set(LANG_HEADER, usePreferencesStore.getState().language);
		requestOptions.headers = headers;
	}
	if (!requestOptions?.ignoreLoading) {
		globalProgress.start();
	}
	const isWhiteRequest = requestWhiteList.some(url => requestUrl.endsWith(url));
	const token = useAuthStore.getState().token;
	if (!isWhiteRequest && token) {
		if (typeof request.headers?.set === "function") {
			request.headers.set(AUTH_HEADER, `Bearer ${token}`);
		}
		else if (requestOptions) {
			const headers = new Headers(requestOptions.headers as HeadersInit | undefined);
			headers.set(AUTH_HEADER, `Bearer ${token}`);
			requestOptions.headers = headers;
		}
	}
	else if (typeof request.headers?.delete === "function") {
		request.headers.delete(AUTH_HEADER);
	}
}) as unknown as BeforeRequestHook;

const afterResponseHook = (async (request: Request, options: RequestOptions, response: Response) => {
	const requestUrl = request.url ?? "";
	if (!options?.ignoreLoading) {
		globalProgress.done();
	}

	if (!response) {
		return response;
	}

	const isWhiteRequest = requestWhiteList.some(url => requestUrl.endsWith(url));
	if (!response.ok) {
		if (isWhiteRequest) {
			return handleErrorResponse(response);
		}
		if (response.status === 401) {
			if ([AUTH_REFRESH_PATH].some(url => requestUrl.endsWith(url))) {
				goLogin();
				return response;
			}

			const { refreshToken } = useAuthStore.getState();
			if (!refreshToken) {
				if (location.pathname === loginPath) {
					return response;
				}
				goLogin();
				return response;
			}

			return refreshTokenAndRetry(request, options, refreshToken);
		}

		return handleErrorResponse(response);
	}

	return response;
}) as unknown as AfterResponseHook;

function createRequestClient(prefix: string) {
	return ky.create({
		prefix,
		timeout: API_TIMEOUT,
		retry: {
			limit: 3,
		},
		hooks: {
			beforeRequest: [beforeRequestHook],
			afterResponse: [afterResponseHook],
		},
	});
}

export const request = createRequestClient(import.meta.env.VITE_API_BASE_URL);
export const backendRequest = createRequestClient("http://localhost:8080/api");
