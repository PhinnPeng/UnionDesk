import type { BusinessDomainView } from "@uniondesk/shared";

import { requestBackendJson } from "#src/api/backend";

type BusinessDomainListResponse = BusinessDomainView[] | { list?: BusinessDomainView[] };

function normalizeBusinessDomainList(response: BusinessDomainListResponse): BusinessDomainView[] {
	return Array.isArray(response) ? response : response.list ?? [];
}

export function fetchBusinessDomains(): Promise<BusinessDomainView[]> {
	return requestBackendJson<BusinessDomainListResponse>("v1/domains").then(normalizeBusinessDomainList);
}
