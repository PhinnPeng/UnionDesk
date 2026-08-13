import { requestBackendJson } from "#src/api/backend";

export interface ConfigItemView {
	key: string
	value: string | null
	valueType?: string | null
	description?: string | null
	updatedAt?: string | null
}

export interface SystemConfigView {
	items: ConfigItemView[]
}

export interface SystemConfigUpdateCommand {
	items: Array<{
		key: string
		value: string | null
		valueType?: string | null
		description?: string | null
	}>
}

export function fetchSystemConfig(): Promise<SystemConfigView> {
	return requestBackendJson<SystemConfigView>("v1/admin/system-config");
}

export function updateSystemConfig(payload: SystemConfigUpdateCommand): Promise<SystemConfigView> {
	return requestBackendJson<SystemConfigView>("v1/admin/system-config", {
		method: "PUT",
		json: payload,
	});
}

