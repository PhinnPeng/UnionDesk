import type {
	TicketAttribute,
	TicketAttributeFieldType,
	TicketAttributeTypeConfig,
} from "@uniondesk/shared";

/** 新建可选的 8 种属性类型（不含成员、迭代） */
export type AttributeTypeKey =
	| "single_select"
	| "multi_select"
	| "single_line_text"
	| "multi_line_text"
	| "date"
	| "datetime"
	| "integer"
	| "decimal";

const ATTRIBUTE_TYPE_LABELS: Record<AttributeTypeKey, string> = {
	single_select: "单选菜单",
	multi_select: "多选菜单",
	single_line_text: "单行文本输入",
	multi_line_text: "多行文本输入",
	date: "日期选择",
	datetime: "日期时间选择",
	integer: "整数输入",
	decimal: "小数输入",
};

const LEGACY_FIELD_TYPE_LABELS: Record<string, string> = {
	switch: "开关类",
	member: "成员",
};

const FORMAT_LABELS: Record<string, string> = {
	text: "文本",
	email: "邮箱",
	phone: "手机",
};

export function resolveAttributeTypeKey(
	fieldType: TicketAttributeFieldType,
	typeConfig: TicketAttributeTypeConfig = {},
): AttributeTypeKey | "switch" | "email_input" | "phone_input" {
	if (fieldType === "switch") {
		return "switch";
	}
	if (fieldType === "member") {
		return typeConfig.multiple ? "multi_select" : "single_select";
	}
	if (fieldType === "select") {
		return typeConfig.multiple ? "multi_select" : "single_select";
	}
	if (fieldType === "date") {
		return typeConfig.withTime ? "datetime" : "date";
	}
	if (fieldType === "input") {
		const format = typeConfig.format ?? "text";
		if (format === "email") {
			return "email_input";
		}
		if (format === "phone") {
			return "phone_input";
		}
		if (format === "integer") {
			return "integer";
		}
		if (format === "decimal") {
			return "decimal";
		}
		return typeConfig.multiline ? "multi_line_text" : "single_line_text";
	}
	return "single_line_text";
}

export function toFieldTypePayload(
	key: AttributeTypeKey,
	typeConfig: Partial<TicketAttributeTypeConfig> = {},
): { field_type: TicketAttributeFieldType; type_config: TicketAttributeTypeConfig } {
	switch (key) {
		case "single_select":
			return {
				field_type: "select",
				type_config: {
					options: typeConfig.options ?? [],
				},
			};
		case "multi_select":
			return {
				field_type: "select",
				type_config: {
					multiple: true,
					options: typeConfig.options ?? [],
				},
			};
		case "single_line_text":
			return {
				field_type: "input",
				type_config: { format: "text" },
			};
		case "multi_line_text":
			return {
				field_type: "input",
				type_config: { format: "text", multiline: true },
			};
		case "date":
			return {
				field_type: "date",
				type_config: {},
			};
		case "datetime":
			return {
				field_type: "date",
				type_config: { withTime: true },
			};
		case "integer":
			return {
				field_type: "input",
				type_config: {
					format: "integer",
					...(typeConfig.unit?.trim() ? { unit: typeConfig.unit.trim() } : {}),
				},
			};
		case "decimal":
			return {
				field_type: "input",
				type_config: {
					format: "decimal",
					...(typeConfig.unit?.trim() ? { unit: typeConfig.unit.trim() } : {}),
				},
			};
		default:
			return { field_type: "input", type_config: { format: "text" } };
	}
}

export function getAttributeTypeLabel(key: AttributeTypeKey): string {
	return ATTRIBUTE_TYPE_LABELS[key];
}

export function formatFieldTypeLabel(fieldType: string): string {
	return LEGACY_FIELD_TYPE_LABELS[fieldType] ?? fieldType;
}

export function formatAttributeTypeLabel(attribute: TicketAttribute): string {
	if (attribute.field_type === "member") {
		return attribute.type_config?.multiple ? "成员（多选）" : "成员";
	}
	if (isPriorityLevelsAttribute(attribute)) {
		return "优先级档位";
	}
	const key = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);
	if (key === "switch") {
		return LEGACY_FIELD_TYPE_LABELS.switch;
	}
	if (key === "email_input") {
		return "邮箱输入";
	}
	if (key === "phone_input") {
		return "手机输入";
	}
	return ATTRIBUTE_TYPE_LABELS[key];
}

export function isPriorityLevelsAttribute(attribute: TicketAttribute | null | undefined): boolean {
	return !!attribute
		&& attribute.field_type === "select"
		&& attribute.type_config?.options_source === "priority_levels";
}

export function formatTypeConfigSummary(attribute: TicketAttribute): string {
	if (isPriorityLevelsAttribute(attribute)) {
		return "域标准四档";
	}
	if (attribute.field_type === "member") {
		return attribute.type_config?.multiple ? "多选成员" : "单选成员";
	}
	const key = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);
	if (key === "single_select" || key === "multi_select") {
		const count = attribute.type_config?.options?.length ?? 0;
		return `${count} 个选项`;
	}
	if (key === "integer" || key === "decimal") {
		return attribute.type_config?.unit?.trim() || "—";
	}
	if (key === "email_input" || key === "phone_input") {
		const format = attribute.type_config?.format ?? "text";
		return FORMAT_LABELS[format] ?? format;
	}
	return "—";
}

export function isMenuAttributeType(key: AttributeTypeKey): boolean {
	return key === "single_select" || key === "multi_select";
}

export function isNumberAttributeType(key: AttributeTypeKey): boolean {
	return key === "integer" || key === "decimal";
}
