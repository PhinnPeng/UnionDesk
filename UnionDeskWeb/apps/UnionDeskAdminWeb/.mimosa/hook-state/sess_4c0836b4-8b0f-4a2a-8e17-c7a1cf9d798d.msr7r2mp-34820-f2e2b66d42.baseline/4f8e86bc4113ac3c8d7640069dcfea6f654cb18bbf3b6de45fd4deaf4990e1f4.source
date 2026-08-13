import { requestBackendJson } from "#src/api/backend";

export interface ConfigItemView {
	key: string
	value: string | null
	valueType?: string | null
	description?: string | null
	updatedAt?: string | null
}

export interface DomainConfigView {
	domainId: number
	items: ConfigItemView[]
}

export interface DomainConfigUpdateCommand {
	items: Array<{
		key: string
		value: string | null
		valueType?: string | null
		description?: string | null
	}>
}

export function fetchDomainConfig(domainId: number): Promise<DomainConfigView> {
	return requestBackendJson<DomainConfigView>(`v1/admin/domains/${domainId}/config`);
}

export function updateDomainConfig(domainId: number, payload: DomainConfigUpdateCommand): Promise<DomainConfigView> {
	return requestBackendJson<DomainConfigView>(`v1/admin/domains/${domainId}/config`, {
		method: "PUT",
		json: payload,
	});
}

