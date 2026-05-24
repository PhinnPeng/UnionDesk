import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import {
	PLATFORM_DOMAINS_LIST_PATH,
	platformDomainDetailPath,
	type DomainDetailTabKey,
} from "./domain-routes";

/** 在平台页签栏打开业务域详情（保留列表页签） */
export function openPlatformDomainDetailTab(
	navigate: (path: string) => void,
	domainId: string,
	domainName?: string,
	tab: DomainDetailTabKey = "overview",
) {
	const path = platformDomainDetailPath(domainId, tab);
	if (domainName?.trim()) {
		useTabsStore.getState().addTab(appScopes.platform, path, {
			key: path.split("?")[0],
			label: "业务域详情",
			newTabTitle: `业务域详情 - ${domainName.trim()}`,
			closable: true,
			draggable: true,
		});
	}
	navigate(path);
}

export { PLATFORM_DOMAINS_LIST_PATH, platformDomainDetailPath };
export type { DomainDetailTabKey };
