import { describe, expect, it } from "vitest";

import type { TicketStatusFlow } from "@uniondesk/shared";

import { isInitialState, normalizeStatusFlow, pickFallbackInitialCode } from "./workflow-initial-state";

const sample: TicketStatusFlow = {
	states: [
		{ code: "a", name: "A", state_type: "in_progress" },
		{ code: "b", name: "B", state_type: "terminal" },
	],
	transitions: [],
	initial_state_code: "a",
};

describe("normalizeStatusFlow", () => {
	it("keeps explicit initial when valid", () => {
		expect(normalizeStatusFlow(sample).initial_state_code).toBe("a");
	});

	it("fills initial with first state when missing", () => {
		expect(normalizeStatusFlow({
			states: sample.states,
			transitions: [],
		}).initial_state_code).toBe("a");
	});

	it("clears initial for empty flow", () => {
		expect(normalizeStatusFlow({ states: [], transitions: [] })).toEqual({
			states: [],
			transitions: [],
			initial_state_code: null,
		});
	});
});

describe("isInitialState", () => {
	it("only matches explicit initial_state_code", () => {
		expect(isInitialState(sample, "a")).toBe(true);
		expect(isInitialState(sample, "b")).toBe(false);
	});
});

describe("pickFallbackInitialCode", () => {
	it("prefers surviving code else first", () => {
		expect(pickFallbackInitialCode(sample.states, "b")).toBe("b");
		expect(pickFallbackInitialCode(sample.states, "gone")).toBe("a");
		expect(pickFallbackInitialCode([], "a")).toBeNull();
	});
});
