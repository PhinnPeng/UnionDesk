import type { AppRouteRecordRaw } from "#src/router/types";
import type { BusinessDomainView } from "@uniondesk/shared";

export interface AuthType {
	token: string
	refreshToken: string
	sid: string
	role: string
	clientCode: string
	tokenType: string
	expiresInSeconds: number
	/** 当前会话活跃业务域（兼容登录字段 defaultBusinessDomainId） */
	defaultBusinessDomainId: number
	/** 用户跨登录默认域偏好；未设置或无效时为 null */
	preferredDefaultDomainId: number | null
	/** 登录返回的业务域访问名单 */
	accessibleDomains: BusinessDomainView[]
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
