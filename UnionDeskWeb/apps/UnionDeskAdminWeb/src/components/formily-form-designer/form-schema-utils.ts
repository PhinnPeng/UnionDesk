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
