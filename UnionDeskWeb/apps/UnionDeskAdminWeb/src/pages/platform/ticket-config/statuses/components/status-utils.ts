import type { TicketStatusDefinitionCategory } from "@uniondesk/shared";

export const STATUS_CATEGORY_OPTIONS: { value: TicketStatusDefinitionCategory; label: string }[] = [
	{ value: "not_started", label: "未开始" },
	{ value: "in_progress", label: "进行中" },
	{ value: "completed", label: "已完成" },
];

export function getStatusCategoryLabel(category: TicketStatusDefinitionCategory | string): string {
	return STATUS_CATEGORY_OPTIONS.find(item => item.value === category)?.label ?? category;
}

export function getStatusCategoryBadgeColor(category: TicketStatusDefinitionCategory | string): string {
	switch (category) {
		case "not_started":
			return "blue";
		case "in_progress":
			return "orange";
		case "completed":
			return "green";
		default:
			return "default";
	}
}

export function formatDateTime(value?: string | null): string {
	if (!value) {
		return "—";
	}
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) {
		return value;
	}
	const pad = (n: number) => String(n).padStart(2, "0");
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}
