import type { AdminDomain, P0AccessPolicy, P0VisibilityPolicyCode } from "@uniondesk/shared";

export const detailTabs = [
	"overview",
	"basic",
	"members",
	"roles",
	"customers",
	"onboarding",
	"tickets",
	"blockwords",
	"notifications",
	"config",
	"audit_logs",
	"login_logs",
] as const;

export type DetailTabKey = (typeof detailTabs)[number];

export {
	PLATFORM_DOMAIN_CONTROL_ENTRY as DOMAIN_CONTROL_ENTRY_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_GENERAL_DELETE as DOMAIN_CONTROL_GENERAL_DELETE_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE as DOMAIN_CONTROL_GENERAL_UPDATE_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE_STATUS as DOMAIN_CONTROL_GENERAL_UPDATE_STATUS_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_OVERVIEW as DOMAIN_CONTROL_OVERVIEW_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_CUSTOMER_READ as DOMAIN_CUSTOMER_READ_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE as DOMAIN_CUSTOMER_CREATE_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS as DOMAIN_CUSTOMER_UPDATE_STATUS_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_READ as DOMAIN_BLOCKED_WORD_READ_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE as DOMAIN_BLOCKED_WORD_CREATE_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_DELETE as DOMAIN_BLOCKED_WORD_DELETE_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_AUDIT_LOG_READ as DOMAIN_CONTROL_AUDIT_LOG_READ_PERMISSION,
	PLATFORM_DOMAIN_CONTROL_LOGIN_LOG_READ as DOMAIN_CONTROL_LOGIN_LOG_READ_PERMISSION,
	PLATFORM_DOMAIN_ROLES_READ as DOMAIN_ROLES_READ_PERMISSION,
	PLATFORM_DOMAIN_ROLES_PERMISSIONS_READ as DOMAIN_ROLE_PERMISSION_READ_PERMISSION,
} from "../../platform-domain-permissions";

export const DEFAULT_DOMAIN_LOGO = "/default-domain-logo.svg";

export const accessPolicyOptions: { value: P0AccessPolicy; label: string }[] = [
	{ value: "allowed", label: "允许" },
	{ value: "disallowed", label: "不允许" },
];

export const visibilityOptions: { value: P0VisibilityPolicyCode; label: string }[] = [
	{ value: "public", label: "公开" },
	{ value: "domain_customer_only", label: "仅域内客户" },
	{ value: "channel_only", label: "仅渠道" },
];

export function parseDetailTab(value: string | null): DetailTabKey {
	if (value === "logs") {
		return "audit_logs";
	}
	if (value && (detailTabs as readonly string[]).includes(value)) {
		return value as DetailTabKey;
	}
	return "overview";
}

export function normalizeVisibility(codes: P0VisibilityPolicyCode[]): P0VisibilityPolicyCode[] {
	if (codes.includes("public")) {
		return ["public"];
	}
	return codes.length > 0 ? codes : ["public"];
}

export function formatAccessPolicyLabel(value: P0AccessPolicy): string {
	return accessPolicyOptions.find(item => item.value === value)?.label ?? value;
}

export function isDomainEnabled(domain: Pick<AdminDomain, "status">): boolean {
	const status = domain.status;
	return status === "1" || status === "active" || status === "enabled";
}

function isValidLogoUrl(value: string | undefined): boolean {
	if (!value?.trim()) {
		return false;
	}
	try {
		const url = new URL(value.trim());
		return url.protocol === "http:" || url.protocol === "https:" || value.startsWith("/");
	}
	catch {
		return value.startsWith("/");
	}
}

export function resolveDomainLogoUrl(logo?: string | null): string {
	const trimmed = logo?.trim();
	return trimmed && isValidLogoUrl(trimmed) ? trimmed : DEFAULT_DOMAIN_LOGO;
}

export function derivePortalUrl(code: string): string {
	return `https://${code}.uniondesk.com`;
}

export function resolveNumericDomainId(domainId: string): number | null {
	const trimmed = domainId.trim();
	if (!trimmed || !/^\d+$/.test(trimmed)) {
		return null;
	}
	const numeric = Number(trimmed);
	return Number.isSafeInteger(numeric) ? numeric : null;
}
