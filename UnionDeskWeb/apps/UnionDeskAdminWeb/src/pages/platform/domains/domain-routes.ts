/** 业务域列表（菜单页，页签常驻） */
export const PLATFORM_DOMAINS_LIST_PATH = "/platform/domains";

/** 新建业务域（历史路由，重定向至列表并打开 Drawer） */
export const PLATFORM_DOMAIN_CREATE_PATH = `${PLATFORM_DOMAINS_LIST_PATH}/create`;

/** 列表页打开新建 Drawer 的查询参数 */
export const PLATFORM_DOMAINS_CREATE_QUERY = "create";

/** 详情 Tab 键 */
export const DOMAIN_DETAIL_TAB_KEYS = ["overview", "basic", "config", "onboarding"] as const;
export type DomainDetailTabKey = (typeof DOMAIN_DETAIL_TAB_KEYS)[number];

/** 业务域详情页签路径（每个 domainId 对应独立页签） */
export function platformDomainDetailPath(domainId: string, tab?: DomainDetailTabKey): string {
	const base = `${PLATFORM_DOMAINS_LIST_PATH}/detail/${encodeURIComponent(domainId)}`;
	if (!tab || tab === "overview") {
		return base;
	}
	return `${base}?tab=${encodeURIComponent(tab)}`;
}
