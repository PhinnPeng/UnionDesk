import type { ClientCode } from "@uniondesk/shared";

import { clearAuthSession, saveAuthSession, setClientCode } from "@uniondesk/shared";

import { useAuthStore } from "#src/store/auth";

const DEFAULT_ADMIN_CLIENT_CODE = "ud-admin-web" as ClientCode;

/** 将管理端 Zustand 登录态同步到 @uniondesk/shared 的 axios，避免缺少 X-UD-Client-Code / Token */
export function syncAuthStoreToSharedApi() {
	const state = useAuthStore.getState();
	const clientCode = (state.clientCode || DEFAULT_ADMIN_CLIENT_CODE) as ClientCode;
	setClientCode(clientCode);

	if (!state.token) {
		clearAuthSession();
		return;
	}

	const username = state.user?.username ?? "";
	saveAuthSession({
		username,
		accessToken: state.token,
		refreshToken: state.refreshToken,
		role: state.role,
		clientCode,
		authenticatedAt: new Date().toISOString(),
		sid: state.sid || null,
		userId: state.user?.id ?? null,
		businessDomainId: state.defaultBusinessDomainId || null,
	});
}
