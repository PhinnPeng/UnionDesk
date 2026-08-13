import type { TicketStatusFlow } from "@uniondesk/shared";
import { describe, expect, it } from "vitest";

import { graphToStatusFlow, statusFlowToGraph } from "./ticket-type-flow-utils";

const SAMPLE_STATUS_FLOW: TicketStatusFlow = {
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

describe("ticket-type-flow-utils", () => {
	it("round-trips sample status flow with positions", () => {
		const graph = statusFlowToGraph(SAMPLE_STATUS_FLOW);
		expect(graph.nodes).toHaveLength(3);
		expect(graph.edges).toHaveLength(2);

		const restored = graphToStatusFlow(graph.nodes, graph.edges);
		expect(restored.states.map(state => state.code)).toEqual(["pending", "processing", "closed"]);
		expect(restored.transitions).toEqual(SAMPLE_STATUS_FLOW.transitions);
		expect(restored.initial_state_code).toBe("pending");
		expect(restored.states[0]?.position).toEqual(graph.nodes[0]?.position);
	});
});
