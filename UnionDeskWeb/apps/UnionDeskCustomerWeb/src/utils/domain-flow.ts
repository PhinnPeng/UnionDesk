import {
	getCustomerPortalSnapshot,
	selectCustomerDomainLive,
	type CustomerPortalDomainView,
} from "@uniondesk/shared";

/** 专属链接登录后：进入指定业务域首页（live：走 switch-domain） */
export async function enterDedicatedDomain(
	domainCode: string,
): Promise<{ ok: true; path: "/home" } | { ok: false; message: string; path: "/domains" }> {
	const domain = getCustomerPortalSnapshot().domains.find(item => item.code === domainCode);
	if (!domain) {
		return { ok: false, message: "专属业务域不存在或未对当前账号开放", path: "/domains" };
	}

	if (!domain.joined) {
		return {
			ok: false,
			message: "当前账号尚未加入该业务域，请联系管理员开通或使用邀请码",
			path: "/domains",
		};
	}

	try {
		await selectCustomerDomainLive(domain.id);
		return { ok: true, path: "/home" };
	}
	catch (error) {
		return {
			ok: false,
			message: error instanceof Error ? error.message : "进入业务域失败",
			path: "/domains",
		};
	}
}

export function findDomainByCode(
	domains: CustomerPortalDomainView[],
	domainCode: string | undefined,
): CustomerPortalDomainView | null {
	if (!domainCode) {
		return null;
	}
	return domains.find(item => item.code === domainCode) ?? null;
}
