import dayjs from "dayjs";

/** SLA 状态展示元信息（中文文案 + 语义色，success/error/warning 语义） */
export function slaStatusMeta(status?: string | null): { text: string, color: string } {
	if (!status) {
		return { text: "—", color: "default" };
	}
	const map: Record<string, { text: string, color: string }> = {
		tracking: { text: "正常", color: "green" },
		breached: { text: "已超时", color: "red" },
		stopped: { text: "已结束", color: "default" },
	};
	return map[status] ?? { text: status, color: "default" };
}

/** 相对时长文案（自然时间）：X 分钟 / X 小时 X 分 / X.X 天 */
export function formatSlaDuration(totalMinutes: number): string {
	const minutes = Math.max(0, Math.round(totalMinutes));
	if (minutes < 60) {
		return `${minutes} 分钟`;
	}
	const hours = Math.floor(minutes / 60);
	const restMinutes = minutes % 60;
	if (hours < 24) {
		return restMinutes > 0 ? `${hours} 小时 ${restMinutes} 分` : `${hours} 小时`;
	}
	return `${(hours / 24).toFixed(1)} 天`;
}

export type SlaDeadlineTone = "ok" | "warn" | "danger" | "none";

/** 单个 SLA 期限（首响/解决）的相对展示：已完成 / 剩余 X / 超时 X / — */
export function describeSlaDeadline(
	deadline?: string | null,
	completedAt?: string | null,
	completedLabel = "已完成",
): { text: string, tone: SlaDeadlineTone } {
	if (!deadline) {
		return { text: "—", tone: "none" };
	}
	if (completedAt) {
		return { text: completedLabel, tone: "ok" };
	}
	const diffMinutes = dayjs(deadline).diff(dayjs(), "minute");
	if (diffMinutes < 0) {
		return { text: `超时 ${formatSlaDuration(-diffMinutes)}`, tone: "danger" };
	}
	return { text: `剩余 ${formatSlaDuration(diffMinutes)}`, tone: "warn" };
}

export const slaDeadlineToneClass: Record<SlaDeadlineTone, string> = {
	ok: "text-green-600",
	warn: "text-orange-500",
	danger: "text-red-500",
	none: "text-slate-500",
};

/** 超时动作 JSON → 中文提示列表（如「升级优先级」），结构见 SlaService.evaluateTicket */
export function parseBreachActionHints(breachActionJson?: string | null): string[] {
	if (!breachActionJson) {
		return [];
	}
	try {
		const parsed = JSON.parse(breachActionJson) as Record<string, unknown>;
		const hints: string[] = [];
		const raiseTo = parsed["raise_priority_to"];
		if (raiseTo !== undefined && raiseTo !== null && String(raiseTo) !== "") {
			hints.push(`升级优先级至 ${String(raiseTo)}`);
		}
		if (parsed["escalate_priority"] === true) {
			hints.push("按序升级优先级");
		}
		const assignTo = parsed["assign_to_staff_account_id"];
		if (assignTo !== undefined && assignTo !== null && Number(assignTo) > 0) {
			hints.push("更换处理人");
		}
		const watchers = parsed["add_watcher_staff_account_ids"];
		if (Array.isArray(watchers) && watchers.length > 0) {
			hints.push("添加关注人");
		}
		const nextStatus = parsed["sla_status"];
		if (nextStatus !== undefined && nextStatus !== null && String(nextStatus) !== "") {
			hints.push(`状态置为 ${String(nextStatus)}`);
		}
		return hints;
	}
	catch {
		return [];
	}
}
