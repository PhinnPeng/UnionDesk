import type {
	CaptchaChallengeResponse,
	CaptchaVerifyRequest,
	CaptchaVerifyResponse,
	LoginConfig,
	LoginResponse,
	PermissionSnapshot,
	SessionView,
	SetDefaultDomainRequest,
	SetDefaultDomainResponse,
	SwitchDomainRequest,
	SwitchDomainResponse,
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
	SetDefaultDomainRequest,
	SetDefaultDomainResponse,
	SwitchDomainRequest,
	SwitchDomainResponse,
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

export type PermissionSnapshotQuery = {
	/** platform | business — 强制快照菜单/动作 scope（进端契约） */
	menuScope?: "platform" | "business"
	/** 当前业务域；业务端拉包时传入 */
	domainId?: number | string
};

export function fetchPermissionSnapshot(query?: PermissionSnapshotQuery): Promise<PermissionSnapshot> {
	const params = new URLSearchParams();
	if (query?.menuScope) {
		params.set("menuScope", query.menuScope);
	}
	if (query?.domainId != null && String(query.domainId).trim() !== "" && Number(query.domainId) > 0) {
		params.set("domainId", String(query.domainId));
	}
	const qs = params.toString();
	const path = qs ? `v1/iam/me/permission-snapshot?${qs}` : "v1/iam/me/permission-snapshot";
	return requestBackendJson<PermissionSnapshot>(path);
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

export function fetchSetDefaultDomain(data: SetDefaultDomainRequest): Promise<SetDefaultDomainResponse> {
	return requestBackendJson<SetDefaultDomainResponse>("v1/auth/me/default-domain", {
		method: "PUT",
		json: data,
	});
}

export function fetchSwitchDomain(data: SwitchDomainRequest): Promise<SwitchDomainResponse> {
	return requestBackendJson<SwitchDomainResponse>("v1/auth/switch-domain", {
		method: "POST",
		json: data,
	});
}
