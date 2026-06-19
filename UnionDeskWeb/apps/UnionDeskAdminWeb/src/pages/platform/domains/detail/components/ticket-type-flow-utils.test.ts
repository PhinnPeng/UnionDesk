import { describe, expect, it } from "vitest";

import { DEFAULT_STATUS_FLOW } from "./ticket-type-form-defaults";
import { graphToStatusFlow, statusFlowToGraph } from "./ticket-type-flow-utils";

describe("ticket-type-flow-utils", () => {
	it("round-trips default status flow with positions", () => {
		const graph = statusFlowToGraph(DEFAULT_STATUS_FLOW);
		expect(graph.nodes).toHaveLength(3);
		expect(graph.edges).toHaveLength(2);

		const restored = graphToStatusFlow(graph.nodes, graph.edges);
		expect(restored.states.map(state => state.code)).toEqual(["pending", "processing", "closed"]);
		expect(restored.transitions).toEqual(DEFAULT_STATUS_FLOW.transitions);
		expect(restored.states[0]?.position).toEqual(graph.nodes[0]?.position);
	});
});
