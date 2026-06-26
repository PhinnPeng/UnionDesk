import type { TicketStatusFlow } from "@uniondesk/shared";

export {
	DEFAULT_TICKET_FORM_SCHEMA as DEFAULT_FORM_SCHEMA,
	mergeSystemFormSchema,
} from "#src/components/formily-form-designer";

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

export function isDraftUnpublished(
	draft: Record<string, unknown> | null | undefined,
	published: Record<string, unknown> | null | undefined,
): boolean {
	if (draft == null) {
		return false;
	}
	return JSON.stringify(draft) !== JSON.stringify(published ?? null);
}
