import type { BusinessDomainView } from "@uniondesk/shared";

import { requestBackendJson } from "#src/api/backend";

export function fetchBusinessDomains(): Promise<BusinessDomainView[]> {
	return requestBackendJson<BusinessDomainView[]>("v1/domains");
}

