import type { LoginInfo } from "#src/api/user";

import { normalizeAccessRoles } from "#src/api/user/utils";

export { normalizeAccessRoles };

export function buildPasswordLoginPayload(values: Pick<LoginInfo, "username" | "password">, captchaToken?: string | null): LoginInfo {
	const payload: LoginInfo = {
		...values,
	};
	if (captchaToken) {
		payload.captchaToken = captchaToken;
	}
	return payload;
}
