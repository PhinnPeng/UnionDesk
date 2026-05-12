import type { AppRouteRecordRaw } from "#src/router/types";

export interface AuthType {
	token: string
	refreshToken: string
	sid: string
	role: string
	clientCode: string
	tokenType: string
	expiresInSeconds: number
	defaultBusinessDomainId: number
	user: UserInfoType | null
}

export interface LoginInfo {
	username: string
	password: string
	captchaToken?: string
}

export interface UserInfoType {
	id: number
	avatar: string
	username: string
	email: string
	phoneNumber: string
	description: string
	roles: Array<string>
	actions: Array<string>
	platformAccess: boolean
	businessDomainAccess?: boolean
	// 路由可以在此处动态添加
	menus?: AppRouteRecordRaw[]
}

export interface AuthListProps {
	label: string
	name: string
	auth: string[]
}
