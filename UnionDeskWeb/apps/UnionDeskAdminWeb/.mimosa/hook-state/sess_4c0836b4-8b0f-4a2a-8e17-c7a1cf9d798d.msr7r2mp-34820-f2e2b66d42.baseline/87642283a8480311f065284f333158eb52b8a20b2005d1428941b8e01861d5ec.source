import type { TicketStatusFlow, TicketStatusFlowState } from "@uniondesk/shared";
import {
	Background,
	Controls,
	MiniMap,
	ReactFlow,
	addEdge,
	useEdgesState,
	useNodesState,
	type Connection,
	type Edge,
	type Node,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";

import { PlusOutlined } from "@ant-design/icons";
import { Button, Form, Input, Select, Space } from "antd";
import { useCallback, useMemo, useState } from "react";

import { DEFAULT_STATUS_FLOW } from "./ticket-type-form-defaults";
import { graphToStatusFlow, statusFlowToGraph } from "./ticket-type-flow-utils";

type FlowNodeData = {
	label: string;
	state: TicketStatusFlowState;
	[key: string]: unknown;
};

export interface TicketTypeFlowDesignerProps {
	value: TicketStatusFlow | Record<string, unknown> | null | undefined;
	onChange: (flow: TicketStatusFlow) => void;
	disabled?: boolean;
}

const STATE_TYPE_OPTIONS = [
	{ value: "in_progress", label: "进行中" },
	{ value: "paused", label: "暂停" },
	{ value: "terminal", label: "终态" },
];

function createStateCode(existing: Node<FlowNodeData>[]): string {
	let index = existing.length + 1;
	let code = `state_${index}`;
	while (existing.some(node => node.id === code)) {
		index += 1;
		code = `state_${index}`;
	}
	return code;
}

export function TicketTypeFlowDesigner({ value, onChange, disabled }: TicketTypeFlowDesignerProps) {
	const initialGraph = useMemo(
		() => statusFlowToGraph(value ?? DEFAULT_STATUS_FLOW),
		// eslint-disable-next-line react-hooks/exhaustive-deps -- 仅首次挂载
		[],
	);
	const [nodes, setNodes, onNodesChange] = useNodesState<Node<FlowNodeData>>(initialGraph.nodes);
	const [edges, setEdges, onEdgesChange] = useEdgesState(initialGraph.edges);
	const [selectedId, setSelectedId] = useState<string | null>(null);

	const syncFlow = useCallback((nextNodes: Node<FlowNodeData>[], nextEdges: Edge[]) => {
		onChange(graphToStatusFlow(nextNodes, nextEdges));
	}, [onChange]);

	const selectedNode = useMemo(
		() => nodes.find(node => node.id === selectedId) ?? null,
		[nodes, selectedId],
	);

	const handleConnect = useCallback((connection: Connection) => {
		setEdges((current) => {
			const nextEdges = addEdge(connection, current);
			syncFlow(nodes, nextEdges);
			return nextEdges;
		});
	}, [nodes, setEdges, syncFlow]);

	const handleNodeDragStop = useCallback(() => {
		syncFlow(nodes, edges);
	}, [edges, nodes, syncFlow]);

	const handleAddState = () => {
		const code = createStateCode(nodes);
		const nextNode: Node<FlowNodeData> = {
			id: code,
			position: { x: 80 + nodes.length * 180, y: 120 },
			data: {
				label: `新状态${nodes.length + 1}`,
				state: {
					code,
					name: `新状态${nodes.length + 1}`,
					state_type: "in_progress",
				},
			},
		};
		const nextNodes = [...nodes, nextNode];
		setNodes(nextNodes);
		setSelectedId(code);
		syncFlow(nextNodes, edges);
	};

	const handleUpdateSelected = (patch: Partial<TicketStatusFlowState>) => {
		if (!selectedNode) {
			return;
		}
		const nextNodes = nodes.map((node) => {
			if (node.id !== selectedNode.id) {
				return node;
			}
			const nextState = { ...node.data.state, ...patch };
			return {
				...node,
				data: {
					label: nextState.name,
					state: nextState,
				},
			};
		});
		setNodes(nextNodes);
		syncFlow(nextNodes, edges);
	};

	return (
		<div className="flex flex-col gap-3">
			<Space>
				<Button icon={<PlusOutlined />} disabled={disabled} onClick={handleAddState}>
					添加状态
				</Button>
				<span className="text-xs text-gray-500">拖拽节点调整位置；从节点边缘拖线建立流转</span>
			</Space>
			<div className="grid grid-cols-[1fr_280px] gap-3">
				<div style={{ height: 360, border: "1px solid var(--ant-color-border-secondary)", borderRadius: 8 }}>
					<ReactFlow
						nodes={nodes}
						edges={edges}
						onNodesChange={disabled ? undefined : onNodesChange}
						onEdgesChange={disabled ? undefined : onEdgesChange}
						onConnect={disabled ? undefined : handleConnect}
						onNodeDragStop={disabled ? undefined : handleNodeDragStop}
						onNodeClick={(_, node) => setSelectedId(node.id)}
						fitView
					>
						<Background />
						<MiniMap />
						<Controls />
					</ReactFlow>
				</div>
				<div className="rounded border border-gray-200 p-3">
					{selectedNode ? (
						<Form layout="vertical" disabled={disabled}>
							<Form.Item label="状态编码">
								<Input value={selectedNode.data.state.code} disabled />
							</Form.Item>
							<Form.Item label="显示名称">
								<Input
									value={selectedNode.data.state.name}
									onChange={event => handleUpdateSelected({ name: event.target.value })}
								/>
							</Form.Item>
							<Form.Item label="状态类型">
								<Select
									value={selectedNode.data.state.state_type}
									options={STATE_TYPE_OPTIONS}
									onChange={stateType => handleUpdateSelected({ state_type: stateType })}
								/>
							</Form.Item>
						</Form>
					) : (
						<p className="text-sm text-gray-500">点击画布中的状态节点以编辑属性</p>
					)}
				</div>
			</div>
		</div>
	);
}
