import type { TicketStatusFlow } from "@uniondesk/shared";

export {
	DEFAULT_TICKET_FORM_SCHEMA as DEFAULT_FORM_SCHEMA,
	mergeSystemFormSchema,
} from "#src/components/formily-form-designer";

/** 新建/空工作流：无状态、无步骤 */
export const EMPTY_STATUS_FLOW: TicketStatusFlow = {
	states: [],
	transitions: [],
};

/** @deprecated 使用 EMPTY_STATUS_FLOW；保留别名避免旧引用灌入三态假数据 */
export const DEFAULT_STATUS_FLOW: TicketStatusFlow = EMPTY_STATUS_FLOW;

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
