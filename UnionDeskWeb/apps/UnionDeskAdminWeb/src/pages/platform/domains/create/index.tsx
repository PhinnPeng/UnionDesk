import { Navigate, useSearchParams } from "react-router";

import { PLATFORM_DOMAINS_CREATE_QUERY, PLATFORM_DOMAINS_LIST_PATH } from "../domain-routes";

/** 历史新建路由：重定向至列表并打开新建 Drawer */
export default function PlatformDomainCreateRedirect() {
	const [searchParams] = useSearchParams();
	const next = new URLSearchParams(searchParams);
	next.set(PLATFORM_DOMAINS_CREATE_QUERY, "1");
	return <Navigate to={`${PLATFORM_DOMAINS_LIST_PATH}?${next.toString()}`} replace />;
}
