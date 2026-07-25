import { FlagFilled, FlagOutlined } from "@ant-design/icons";

const PRIORITY_FALLBACK: Record<string, { color: string; icon: string; label: string }> = {
	urgent: { color: "#f5222d", icon: "urgent", label: "紧急" },
	high: { color: "#fa8c16", icon: "high", label: "高" },
	normal: { color: "#1677ff", icon: "normal", label: "中" },
	low: { color: "#8c8c8c", icon: "low", label: "低" },
};

export type PriorityDisplay = {
	code: string;
	label: string;
	color: string;
	icon: string;
};

export function resolvePriorityDisplay(
	code: string | null | undefined,
	meta?: { name?: string; color?: string | null; icon?: string | null } | null,
): PriorityDisplay {
	const normalized = (code ?? "normal").trim().toLowerCase() || "normal";
	const fallback = PRIORITY_FALLBACK[normalized] ?? PRIORITY_FALLBACK.normal;
	return {
		code: normalized,
		label: meta?.name?.trim() || fallback.label,
		color: meta?.color?.trim() || fallback.color,
		icon: meta?.icon?.trim() || fallback.icon,
	};
}

interface PriorityBadgeProps {
	code?: string | null;
	name?: string;
	color?: string | null;
	icon?: string | null;
	showLabel?: boolean;
}

/** 彩色优先级旗标（标准四档） */
export function PriorityBadge({
	code,
	name,
	color,
	icon,
	showLabel = true,
}: PriorityBadgeProps) {
	const display = resolvePriorityDisplay(code, { name, color, icon });
	const Icon = display.icon === "low" || display.icon === "normal" ? FlagOutlined : FlagFilled;

	return (
		<span style={{ display: "inline-flex", alignItems: "center", gap: 4, color: display.color }}>
			<Icon style={{ color: display.color, fontSize: 14 }} />
			{showLabel && <span style={{ color: "rgba(0,0,0,0.88)", fontSize: 13 }}>{display.label}</span>}
		</span>
	);
}
