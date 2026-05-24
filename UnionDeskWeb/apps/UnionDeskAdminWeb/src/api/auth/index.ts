import type {
	CaptchaChallengeResponse,
	CaptchaVerifyRequest,
	CaptchaVerifyResponse,
	LoginConfig,
	LoginResponse,
	PermissionSnapshot,
	SessionView,
} from "@uniondesk/shared";

import type { LoginInfo } from "#src/api/user/types";

import { requestBackendJson } from "#src/api/backend";

export type {
	CaptchaChallengeResponse,
	CaptchaVerifyRequest,
	CaptchaVerifyResponse,
	LoginConfig,
	LoginResponse,
	PermissionSnapshot,
	SessionView,
};

export function fetchLoginConfig(): Promise<LoginConfig> {
	return requestBackendJson<LoginConfig>("v1/auth/login-config");
}

export function createCaptchaChallenge(): Promise<CaptchaChallengeResponse> {
	return requestBackendJson<CaptchaChallengeResponse>("v1/auth/captcha/challenge", {
		method: "POST",
	});
}

export function verifyCaptcha(data: CaptchaVerifyRequest): Promise<CaptchaVerifyResponse> {
	return requestBackendJson<CaptchaVerifyResponse>("v1/auth/captcha/verify", {
		method: "POST",
		json: data,
	});
}

export function fetchPermissionSnapshot(): Promise<PermissionSnapshot> {
	return requestBackendJson<PermissionSnapshot>("v1/iam/me/permission-snapshot");
}

export function fetchSessionStatus(): Promise<SessionView> {
	return requestBackendJson<SessionView>("v1/auth/session");
}

export function fetchLogin(data: LoginInfo): Promise<LoginResponse> {
	return requestBackendJson<LoginResponse>("v1/auth/login", {
		method: "POST",
		json: data,
		silentError: true,
	});
}

export function fetchLogout(): Promise<void> {
	return requestBackendJson<void>("v1/auth/logout", {
		method: "POST",
	}).then(() => undefined);
}
