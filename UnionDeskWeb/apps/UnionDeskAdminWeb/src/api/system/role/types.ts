/** 后端 RoleView 对应类型 */
export interface RoleItemType {
	id: number
	code: string
	name: string
	scope: string
	system: boolean
}

/** 角色权限（菜单+按钮 ID 列表） */
export interface RolePermissions {
	roleId: number
	menuIds: number[]
	buttonIds: number[]
}

/** 创建/编辑角色请求体 */
export interface RolePayload {
	code: string
	name: string
	scope: string
}
