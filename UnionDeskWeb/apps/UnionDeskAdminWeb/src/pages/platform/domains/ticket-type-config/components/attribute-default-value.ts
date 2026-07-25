import type { TicketAttribute } from "@uniondesk/shared";

import { resolveAttributeTypeKey } from "#src/pages/platform/ticket-config/attributes/components/attribute-utils";

import dayjs from "dayjs";

export type SlotDefaultValueMode = "literal" | "expression";

export type SlotDefaultValue = {
	mode: SlotDefaultValueMode;
	value: string | number | boolean | string[] | null;
};

const UNSET_LABEL = "未指定";

export function parseSlotDefaultValue(raw?: string | null): SlotDefaultValue | null {
	if (raw == null || raw.trim() === "") {
		return null;
	}
	try {
		const parsed = JSON.parse(raw) as Partial<SlotDefaultValue>;
		if (parsed && typeof parsed === "object" && "mode" in parsed) {
			if (parsed.mode === "expression") {
				return { mode: "expression", value: parsed.value ?? null };
			}
			return { mode: "literal", value: parsed.value ?? null };
		}
	}
	catch {
		// legacy plain string
	}
	return { mode: "literal", value: raw };
}

export function serializeSlotDefaultValue(value: SlotDefaultValue | null): string | undefined {
	if (!value || value.value === null || value.value === undefined) {
		return undefined;
	}
	if (value.mode === "literal" && typeof value.value === "string" && value.value.trim() === "") {
		return undefined;
	}
	if (value.mode === "literal" && Array.isArray(value.value) && value.value.length === 0) {
		return undefined;
	}
	return JSON.stringify(value);
}

export function formatSlotDefaultValueLabel(
	raw: string | null | undefined,
	attribute: TicketAttribute,
): string {
	const parsed = parseSlotDefaultValue(raw);
	if (!parsed || parsed.value === null || parsed.value === undefined) {
		return UNSET_LABEL;
	}
	if (parsed.mode === "expression") {
		return String(parsed.value ?? "");
	}

	const typeKey = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);

	if (typeKey === "switch") {
		return parsed.value === true ? "是" : parsed.value === false ? "否" : UNSET_LABEL;
	}

	if (typeKey === "single_select" || typeKey === "multi_select") {
		const options = attribute.type_config?.options ?? [];
		const resolveLabel = (val: string) =>
			options.find(item => item.value === val)?.label ?? val;

		if (Array.isArray(parsed.value)) {
			const labels = parsed.value.map(item => resolveLabel(String(item)));
			return labels.length > 0 ? labels.join("、") : UNSET_LABEL;
		}
		return resolveLabel(String(parsed.value));
	}

	if (typeKey === "date" || typeKey === "datetime") {
		const text = String(parsed.value);
		const formatted = dayjs(text);
		return formatted.isValid()
			? formatted.format(typeKey === "datetime" ? "YYYY-MM-DD HH:mm" : "YYYY-MM-DD")
			: text;
	}

	if (Array.isArray(parsed.value)) {
		return parsed.value.map(item => String(item)).join("、");
	}

	return String(parsed.value);
}

export function createEmptyLiteralDefault(attribute: TicketAttribute): SlotDefaultValue {
	const typeKey = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);
	if (typeKey === "switch") {
		return { mode: "literal", value: false };
	}
	if (typeKey === "multi_select") {
		return { mode: "literal", value: [] };
	}
	return { mode: "literal", value: "" };
}
