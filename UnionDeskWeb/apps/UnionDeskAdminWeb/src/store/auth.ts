import type { AuthType, LoginInfo } from "#src/api/user/types";



import { fetchLogin, fetchLogout, fetchPermissionSnapshot, fetchSetDefaultDomain, fetchSwitchDomain } from "#src/api/auth";

import { syncAuthStoreToSharedApi } from "#src/api/sync-shared-session";

import { buildUserInfoFromLoginUser } from "#src/api/user/utils";

import { buildUserInfoFromPermissionSnapshot } from "#src/api/user/utils";

import { hasPlatformRoleHint } from "#src/router/extra-info/resolve-home-path";

import { useAccessStore } from "#src/store/access";

import { useTabsStore } from "#src/store/tabs";

import { useUserStore } from "#src/store/user";

import { getAppNamespace } from "#src/utils/get-app-namespace";



import { create } from "zustand";

import { persist } from "zustand/middleware";



const initialState: AuthType = {

	token: "",

	refreshToken: "",

	sid: "",

	role: "",

	clientCode: "ud-admin-web",

	tokenType: "",

	expiresInSeconds: 0,

	defaultBusinessDomainId: 0,

	preferredDefaultDomainId: null,

	accessibleDomains: [],

	user: null,

};



interface AuthAction {

	login: (loginPayload: LoginInfo) => Promise<void>

	logout: () => Promise<void>

	reset: () => void

	switchDomain: (domainId: number) => Promise<void>

	setDefaultDomain: (domainId: number) => Promise<void>

};



export const useAuthStore = create<AuthType & AuthAction>()(

	persist<AuthType & AuthAction>((set, get) => ({

		...initialState,



		login: async (loginPayload) => {

			// 避免沿用上次的动态路由 / 页签，导致登录后仍跳业务域首页

			useAccessStore.getState().reset();

			useTabsStore.getState().resetTabs();

			const response = await fetchLogin(loginPayload);

			const accessibleDomains = response.accessibleDomains ?? [];

			let userInfo = buildUserInfoFromLoginUser(

				response.user,

				response.user.roles,

				response.role,

				accessibleDomains,

			);

			const nextState: AuthType = {

				token: response.accessToken,

				refreshToken: response.refreshToken,

				sid: response.sid,

				role: response.role,

				clientCode: response.clientCode,

				tokenType: response.tokenType,

				expiresInSeconds: response.expiresInSeconds,

				defaultBusinessDomainId: response.defaultBusinessDomainId,

				preferredDefaultDomainId: response.preferredDefaultDomainId ?? null,

				accessibleDomains,

				user: userInfo,

			};

			set({

				...nextState,

			});

			syncAuthStoreToSharedApi();

			try {

				const businessDomainAccess = Boolean(userInfo.businessDomainAccess);

				const snapshot = await fetchPermissionSnapshot(

					businessDomainAccess

						? {

								menuScope: "business",

								domainId: response.defaultBusinessDomainId,

							}

						: { menuScope: "platform" },

				);

				userInfo = buildUserInfoFromPermissionSnapshot(snapshot, {

					accessibleDomains,

					businessDomainAccess,

				});

				if (hasPlatformRoleHint(response.user.roles, response.role) || userInfo.platformAccess) {

					userInfo = { ...userInfo, platformAccess: true };

				}

			}

			catch {

				// 登录链路允许快照请求暂时失败，后续由鉴权守卫兜底补齐用户信息。

			}

			set(state => ({

				...state,

				user: userInfo,

			}));

			useUserStore.getState().setUserInfo(userInfo);

		},



		logout: async () => {

			try {

				await fetchLogout();

			}

			finally {

				get().reset();

			}

		},



		reset: () => {

			set({

				...initialState,

			});

			syncAuthStoreToSharedApi();

			useUserStore.getState().reset();

			useAccessStore.getState().reset();

			useTabsStore.getState().resetTabs();

		},



		switchDomain: async (domainId) => {

			if (domainId === get().defaultBusinessDomainId) {

				return;

			}

			const response = await fetchSwitchDomain({ domainId });

			set(state => ({

				...state,

				token: response.accessToken,

				refreshToken: response.refreshToken,

				tokenType: response.tokenType,

				expiresInSeconds: response.expiresInSeconds,

				defaultBusinessDomainId: response.businessDomainId,

				accessibleDomains: response.accessibleDomains ?? state.accessibleDomains,

			}));

			syncAuthStoreToSharedApi();

		},



		setDefaultDomain: async (domainId) => {

			const response = await fetchSetDefaultDomain({ domainId });

			set(state => ({

				...state,

				preferredDefaultDomainId: response.preferredDefaultDomainId,

			}));

		},



	}), {

		name: getAppNamespace("access-token"),

		onRehydrateStorage: () => (state) => {

			if (state) {

				if (!Array.isArray(state.accessibleDomains)) {

					state.accessibleDomains = [];

				}

				if (state.preferredDefaultDomainId === undefined) {

					state.preferredDefaultDomainId = null;

				}

				syncAuthStoreToSharedApi();

			}

		},

	}),

);


