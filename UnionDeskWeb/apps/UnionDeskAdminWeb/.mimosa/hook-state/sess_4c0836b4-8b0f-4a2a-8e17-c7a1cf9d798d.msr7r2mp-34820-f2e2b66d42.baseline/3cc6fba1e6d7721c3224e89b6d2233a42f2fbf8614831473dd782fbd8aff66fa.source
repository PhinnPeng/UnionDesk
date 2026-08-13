export function formatAuditDetail(detail?: string | null): string {
	if (!detail) {
		return "—";
	}
	const trimmed = detail.trimStart();
	if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
		try {
			return JSON.stringify(JSON.parse(detail), null, 2);
		}
		catch {
			return detail;
		}
	}
	return detail;
}

export const AUDIT_ACTION_FILTER_OPTIONS = [
	{ value: "platform.domain.create", label: "业务域创建" },
	{ value: "platform.domain.update", label: "业务域更新" },
	{ value: "platform.domain.update_status", label: "业务域状态变更" },
	{ value: "platform.domain.delete", label: "业务域删除" },
	{ value: "platform.role.permissions.update", label: "角色权限更新" },
	{ value: "platform.domain.member.update_status", label: "域成员状态变更" },
] as const;

export function formatLoginPortalType(portalType?: string | null): string {
	if (portalType === "staff") {
		return "员工端";
	}
	if (portalType === "customer") {
		return "客户端";
	}
	return portalType ?? "—";
}

export function formatLoginResult(result?: string | null): string {
	if (!result) {
		return "—";
	}
	const normalized = result.trim().toLowerCase();
	if (normalized === "success") {
		return "成功";
	}
	if (normalized === "failure" || normalized === "failed") {
		return "失败";
	}
	return result;
}

export function resolveAuditActionLabel(row: { action?: string | null, actionLabel?: string | null }): string {
	return row.actionLabel ?? row.action ?? "—";
}
