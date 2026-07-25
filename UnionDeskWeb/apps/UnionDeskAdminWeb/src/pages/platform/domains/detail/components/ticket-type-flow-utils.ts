import type { TicketStatusFlow, TicketStatusFlowState } from "@uniondesk/shared";
import type { Edge, Node } from "@xyflow/react";

type FlowNodeData = {
	label: string;
	state: TicketStatusFlowState;
	[key: string]: unknown;
};

export function statusFlowToGraph(flow: TicketStatusFlow | Record<string, unknown> | null | undefined): {
	nodes: Node<FlowNodeData>[];
	edges: Edge[];
} {
	if (!flow || typeof flow !== "object") {
		return { nodes: [], edges: [] };
	}
	const typedFlow = flow as TicketStatusFlow;
	const states = typedFlow.states ?? [];
	const transitions = typedFlow.transitions ?? [];
	const nodes: Node<FlowNodeData>[] = states.map((state, index) => ({
		id: state.code,
		position: state.position ?? { x: 80 + index * 200, y: 80 },
		data: {
			label: state.name,
			state,
		},
	}));
	const edges: Edge[] = transitions.map((transition, index) => ({
		id: `e-${transition.from}-${transition.to}-${index}`,
		source: transition.from,
		target: transition.to,
		label: transition.label,
	}));
	return { nodes, edges };
}

export function graphToStatusFlow(nodes: Node<FlowNodeData>[], edges: Edge[]): TicketStatusFlow {
	const states: TicketStatusFlowState[] = nodes.map((node) => {
		const state = node.data.state;
		return {
			code: node.id,
			name: state?.name ?? node.data.label ?? node.id,
			state_type: state?.state_type ?? "in_progress",
			allow_customer_withdraw: state?.allow_customer_withdraw,
			is_resolved: state?.is_resolved,
			position: { x: node.position.x, y: node.position.y },
		};
	});
	const transitions = edges.map((edge) => ({
		from: edge.source,
		to: edge.target,
		label: typeof edge.label === "string" ? edge.label : undefined,
	}));
	return {
		states,
		transitions,
		initial_state_code: states.length > 0 ? states[0]!.code : null,
	};
}
