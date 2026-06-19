import type { TicketStatusFlow } from "@uniondesk/shared";

export const DEFAULT_FORM_SCHEMA: Record<string, unknown> = {
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

export const DEFAULT_STATUS_FLOW: TicketStatusFlow = {
	states: [
		{
			code: "pending",
			name: "待处理",
			state_type: "in_progress",
			allow_customer_withdraw: true,
			is_resolved: false,
		},
		{
			code: "processing",
			name: "处理中",
			state_type: "in_progress",
			allow_customer_withdraw: false,
			is_resolved: false,
		},
		{
			code: "closed",
			name: "已关闭",
			state_type: "terminal",
			allow_customer_withdraw: false,
			is_resolved: false,
		},
	],
	transitions: [
		{ from: "pending", to: "processing" },
		{ from: "processing", to: "closed" },
	],
};

export function countFormFields(schema: Record<string, unknown> | null | undefined): number {
	const properties = schema?.properties;
	if (!properties || typeof properties !== "object") {
		return 0;
	}
	return Object.keys(properties).length;
}

export function countFlowStates(flow: TicketStatusFlow | Record<string, unknown> | null | undefined): number {
	if (!flow || typeof flow !== "object") {
		return 0;
	}
	const states = (flow as TicketStatusFlow).states;
	return Array.isArray(states) ? states.length : 0;
}

export function mergeSystemFormSchema(schema: Record<string, unknown> | null | undefined): Record<string, unknown> {
	const base = structuredClone(DEFAULT_FORM_SCHEMA);
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
