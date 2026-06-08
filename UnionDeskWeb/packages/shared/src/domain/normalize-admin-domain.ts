import type { AdminDomain, P0AccessPolicy, P0PageResult, P0VisibilityPolicyCode } from "../types";

const VISIBILITY_CODES: P0VisibilityPolicyCode[] = ["public", "domain_customer_only", "channel_only"];

function isVisibilityCode(value: string): value is P0VisibilityPolicyCode {
	return VISIBILITY_CODES.includes(value as P0VisibilityPolicyCode);
}

function normalizeVisibilityCodes(raw: unknown): P0VisibilityPolicyCode[] {
	if (Array.isArray(raw)) {
		const codes = raw
			.map(item => (typeof item === "string" ? item.trim() : ""))
			.filter(isVisibilityCode);
		return codes.length > 0 ? codes : ["public"];
	}
	if (typeof raw === "string" && raw.trim()) {
		try {
			const parsed = JSON.parse(raw) as unknown;
			return normalizeVisibilityCodes(parsed);
		}
		catch {
			return ["public"];
		}
	}
	return ["public"];
}

function normalizeAccessPolicy(raw: unknown): P0AccessPolicy {
	if (raw === "disallowed") {
		return "disallowed";
	}
	return "allowed";
}

function normalizeStatus(raw: unknown): string | undefined {
	if (raw == null) {
		return undefined;
	}
	if (typeof raw === "number") {
		return String(raw);
	}
	if (typeof raw === "string") {
		return raw;
	}
	return undefined;
}

function pickString(raw: Record<string, unknown>, snakeKey: string, camelKey: string): string | undefined {
	const snake = raw[snakeKey];
	if (typeof snake === "string") {
		return snake;
	}
	const camel = raw[camelKey];
	if (typeof camel === "string") {
		return camel;
	}
	if (typeof snake === "number") {
		return String(snake);
	}
	if (typeof camel === "number") {
		return String(camel);
	}
	return undefined;
}

/** 将接口返回的业务域行规范为 AdminDomain，兼容 snake_case / camelCase 与数字 status */
export function normalizeAdminDomain(raw: unknown): AdminDomain | null {
	if (!raw || typeof raw !== "object") {
		return null;
	}
	const row = raw as Record<string, unknown>;
	const id = pickString(row, "id", "id");
	const code = pickString(row, "code", "code");
	const name = pickString(row, "name", "name");
	if (!id || !code || !name) {
		return null;
	}
	return {
		id,
		code,
		name,
		description: pickString(row, "description", "description") ?? null,
		logo: pickString(row, "logo", "logo") ?? null,
		visibility_policy_codes: normalizeVisibilityCodes(
			row.visibility_policy_codes ?? row.visibilityPolicyCodes,
		),
		registration_enabled: normalizeAccessPolicy(
			row.registration_enabled ?? row.registrationEnabled,
		),
		invitation_enabled: normalizeAccessPolicy(
			row.invitation_enabled ?? row.invitationEnabled,
		),
		status: normalizeStatus(row.status),
		created_at: pickString(row, "created_at", "createdAt") ?? null,
		updated_at: pickString(row, "updated_at", "updatedAt") ?? null,
		created_by: pickString(row, "created_by", "createdBy") ?? null,
		updated_by: pickString(row, "updated_by", "updatedBy") ?? null,
		creator_name: pickString(row, "creator_name", "creatorName") ?? null,
		updater_name: pickString(row, "updater_name", "updaterName") ?? null,
	};
}

/** 解包并规范化业务域分页结果 */
export function normalizeAdminDomainsPageResult(raw: unknown): P0PageResult<AdminDomain> {
	if (!raw || typeof raw !== "object") {
		return { total: 0, list: [] };
	}
	const payload = raw as Record<string, unknown>;
	const totalRaw = payload.total;
	const total = typeof totalRaw === "number"
		? totalRaw
		: typeof totalRaw === "string"
			? Number(totalRaw) || 0
			: 0;
	const listRaw = payload.list ?? payload.items ?? payload.records;
	const list = Array.isArray(listRaw)
		? listRaw.map(normalizeAdminDomain).filter((item): item is AdminDomain => item != null)
		: [];
	return { total, list };
}
