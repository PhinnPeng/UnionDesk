import { requestBackendJson } from "#src/utils/request";

export interface ConfigItemView {
	key: string
	value: string | null
	valueType?: string | null
	description?: string | null
	updatedAt?: string | null
}

export interface DomainConfigView {
	domainId: string
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

export function fetchDomainConfig(domainId: string): Promise<DomainConfigView> {
	return requestBackendJson<DomainConfigView>(`v1/admin/domains/${domainId}/config`);
}

export function updateDomainConfig(domainId: string, payload: DomainConfigUpdateCommand): Promise<DomainConfigView> {
	return requestBackendJson<DomainConfigView>(`v1/admin/domains/${domainId}/config`, {
		method: "PUT",
		json: payload,
	});
}

