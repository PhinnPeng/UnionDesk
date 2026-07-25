import type { AdditionalAttributeItem, TransitionRule } from "@uniondesk/shared";

/** 从规则解析附加属性列表；旧数据仅有 required_slot_ids 时自动hydrate */
export function normalizeAdditionalAttributes(
	rule: Pick<TransitionRule, "additional_attributes" | "required_slot_ids"> | null | undefined,
): AdditionalAttributeItem[] {
	const listed = rule?.additional_attributes;
	if (listed && listed.length > 0) {
		return listed.map(item => ({
			slot_id: item.slot_id,
			required: Boolean(item.required),
			default_mode: item.default_mode === "set" ? "set" : "keep",
			default_value: item.default_value,
		}));
	}
	return (rule?.required_slot_ids ?? []).map(slot_id => ({
		slot_id,
		required: true,
		default_mode: "keep" as const,
	}));
}

export function syncRequiredSlotIds(
	items: AdditionalAttributeItem[],
): string[] {
	return items.filter(item => item.required).map(item => item.slot_id);
}

export function upsertAdditionalAttribute(
	items: AdditionalAttributeItem[],
	next: AdditionalAttributeItem,
): AdditionalAttributeItem[] {
	const index = items.findIndex(item => item.slot_id === next.slot_id);
	if (index < 0) {
		return [...items, next];
	}
	const copy = [...items];
	copy[index] = next;
	return copy;
}

export function removeAdditionalAttribute(
	items: AdditionalAttributeItem[],
	slotId: string,
): AdditionalAttributeItem[] {
	return items.filter(item => item.slot_id !== slotId);
}

export function hasAdditionalAttributeRule(
	rule: Pick<TransitionRule, "additional_attributes" | "required_slot_ids"> | null | undefined,
): boolean {
	return normalizeAdditionalAttributes(rule).length > 0;
}

export function summarizeAdditionalAttributes(count: number): string {
	return count > 0 ? `${count} 项附加属性` : "未配置附加属性";
}
