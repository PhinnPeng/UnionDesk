export const DEFAULT_TICKET_FORM_SCHEMA: Record<string, unknown> = {
	type: "object",
	properties: {
		title: {
			type: "string",
			title: "标题",
			"x-component": "Input",
			"x-decorator": "FormItem",
			required: true,
			"x-system-field": true,
			"x-index": 0,
		},
		description: {
			type: "string",
			title: "详细描述",
			"x-component": "Input.TextArea",
			"x-decorator": "FormItem",
			required: true,
			"x-component-props": { rows: 4, placeholder: "请描述您的问题或建议" },
			"x-system-field": true,
			"x-index": 1,
		},
	},
};

export function mergeSystemFormSchema(schema: Record<string, unknown> | null | undefined): Record<string, unknown> {
	const base = structuredClone(DEFAULT_TICKET_FORM_SCHEMA);
	const customProperties = schema?.properties;
	if (!customProperties || typeof customProperties !== "object") {
		return base;
	}
	const mergedProperties = {
		...(base.properties as Record<string, unknown>),
	};
	for (const [key, value] of Object.entries(customProperties as Record<string, unknown>)) {
		if (key === "title" || key === "description") {
			continue;
		}
		mergedProperties[key] = value;
	}
	return { ...base, properties: mergedProperties };
}

function validateSystemField(
	properties: Record<string, unknown>,
	key: string,
	label: string,
	component: string,
): string | null {
	const field = properties[key];
	if (!field || typeof field !== "object") {
		return `工单类型必须包含「${label}」系统字段`;
	}
	const fieldRecord = field as Record<string, unknown>;
	if (fieldRecord.required !== true) {
		return `系统字段「${label}」不可改为非必填`;
	}
	if (fieldRecord["x-component"] !== component) {
		return `系统字段「${label}」组件类型不可变更`;
	}
	return null;
}

export function validateFormSchemaForSave(schema: Record<string, unknown>): string | null {
	const properties = schema.properties;
	if (!properties || typeof properties !== "object") {
		return "表单 schema 格式无效";
	}
	const props = properties as Record<string, unknown>;
	const titleError = validateSystemField(props, "title", "标题", "Input");
	if (titleError) {
		return titleError;
	}
	return validateSystemField(props, "description", "详细描述", "Input.TextArea");
}

export function normalizeFormSchemaForSave(raw: unknown): Record<string, unknown> | null {
	const schemaCandidate = raw && typeof raw === "object" && "schema" in raw
		? (raw as { schema?: unknown }).schema
		: raw;
	if (!schemaCandidate || typeof schemaCandidate !== "object") {
		return null;
	}
	return mergeSystemFormSchema(schemaCandidate as Record<string, unknown>);
}
