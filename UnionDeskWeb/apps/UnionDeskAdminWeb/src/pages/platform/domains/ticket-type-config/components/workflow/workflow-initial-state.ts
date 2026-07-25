import type { TicketStatusFlow } from "@uniondesk/shared";

/** Normalize API / draft flow: ensure unique initial_state_code when states exist. */
export function normalizeStatusFlow(
	flow: TicketStatusFlow | Record<string, unknown> | null | undefined,
): TicketStatusFlow {
	const raw = (flow && typeof flow === "object" ? flow : {}) as TicketStatusFlow;
	const states = Array.isArray(raw.states) ? raw.states : [];
	const transitions = Array.isArray(raw.transitions) ? raw.transitions : [];
	if (states.length === 0) {
		return { states: [], transitions: [], initial_state_code: null };
	}
	const current = typeof raw.initial_state_code === "string" ? raw.initial_state_code.trim() : "";
	const initial_state_code = states.some(s => s.code === current)
		? current
		: states[0]!.code;
	return { states, transitions, initial_state_code };
}

/** House icon: only the explicit unique initial state. */
export function isInitialState(flow: TicketStatusFlow, stateCode: string): boolean {
	return Boolean(flow.initial_state_code) && flow.initial_state_code === stateCode;
}

export function pickFallbackInitialCode(
	states: TicketStatusFlow["states"],
	preferCode?: string | null,
): string | null {
	if (states.length === 0) {
		return null;
	}
	if (preferCode && states.some(s => s.code === preferCode)) {
		return preferCode;
	}
	return states[0]!.code;
}
