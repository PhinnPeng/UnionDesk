import type { P0RegistrationPolicy, P0VisibilityPolicyCode } from "@uniondesk/shared";

export const registrationOptions: { value: P0RegistrationPolicy; label: string }[] = [
	{ value: "open", label: "开放注册" },
	{ value: "invitation_only", label: "仅邀请" },
	{ value: "admin_only", label: "仅管理员添加" },
];

export const visibilityOptions: { value: P0VisibilityPolicyCode; label: string }[] = [
	{ value: "public", label: "公开" },
	{ value: "domain_customer_only", label: "仅域内客户" },
	{ value: "channel_only", label: "仅渠道" },
];

export function normalizeVisibility(codes: P0VisibilityPolicyCode[]): P0VisibilityPolicyCode[] {
	if (codes.includes("public")) {
		return ["public"];
	}
	return codes.length > 0 ? codes : ["public"];
}
