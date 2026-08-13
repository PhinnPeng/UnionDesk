import type { UserInfoType } from "#src/api/user/types";

import { fetchUserInfo } from "#src/api/user";

import { create } from "zustand";

const initialState: UserInfoType = {
	id: 0,
	avatar: "",
	username: "",
	email: "",
	phoneNumber: "",
	description: "",
	roles: [],
	actions: [],
	platformAccess: false,
	businessDomainAccess: false,
	menus: [],
};

type UserState = UserInfoType;

interface UserAction {
	getUserInfo: () => Promise<UserInfoType>
	setUserInfo: (userInfo: UserInfoType) => void
	reset: () => void
};

export const useUserStore = create<UserState & UserAction>()(
	set => ({
		...initialState,

		getUserInfo: async () => {
			const userInfo = await fetchUserInfo();
			set({
				...userInfo,
			});
			return userInfo;
		},

		setUserInfo: (userInfo) => {
			set({
				...userInfo,
			});
		},

		reset: () => {
			return set({
				...initialState,
			});
		},

	}),

);
