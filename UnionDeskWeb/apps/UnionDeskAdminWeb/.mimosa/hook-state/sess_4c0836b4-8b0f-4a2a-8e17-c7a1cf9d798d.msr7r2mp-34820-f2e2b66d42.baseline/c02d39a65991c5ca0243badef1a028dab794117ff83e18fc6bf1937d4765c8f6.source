import { Form, FormItem, Input } from "@formily/antd-v5";
import { createForm } from "@formily/core";
import { FormProvider, Field } from "@formily/react";
import { Card, Typography } from "antd";
import { useMemo } from "react";
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

const { Title, Paragraph } = Typography;

const initialNodes: Node[] = [
	{ id: "pending", position: { x: 80, y: 80 }, data: { label: "待处理" } },
	{ id: "closed", position: { x: 280, y: 80 }, data: { label: "已关闭" } },
];

const initialEdges: Edge[] = [{ id: "e-pending-closed", source: "pending", target: "closed" }];

/**
 * Dev-only POC：验证 Formily 渲染 + React Flow DAG 与 antd 6 共存。
 * 路由未注册；typecheck 引用本模块即可。
 */
export function TicketTypeDesignerPoc() {
	const form = useMemo(
		() =>
			createForm({
				values: { title: "", description: "" },
			}),
		[],
	);
	const [nodes, , onNodesChange] = useNodesState(initialNodes);
	const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

	const onConnect = (connection: Connection) => {
		setEdges((eds) => addEdge(connection, eds));
	};

	return (
		<div className="flex flex-col gap-4 p-4">
			<Title level={4}>工单类型设计器 POC</Title>
			<Paragraph type="secondary">
				Formily 表单预览（title / description 系统字段）+ React Flow 状态流占位。
			</Paragraph>
			<div className="grid grid-cols-2 gap-4">
				<Card title="Formily 预览" bordered={false}>
					<FormProvider form={form}>
						<Form layout="vertical">
							<Field
								name="title"
								title="标题"
								required
								decorator={[FormItem]}
								component={[Input, { placeholder: "请输入标题" }]}
							/>
							<Field
								name="description"
								title="详细描述"
								required
								decorator={[FormItem]}
								component={[Input.TextArea, { rows: 4, placeholder: "请描述问题" }]}
							/>
						</Form>
					</FormProvider>
				</Card>
				<Card title="状态流 DAG" bordered={false} styles={{ body: { height: 320 } }}>
					<div style={{ width: "100%", height: 280 }}>
						<ReactFlow
							nodes={nodes}
							edges={edges}
							onNodesChange={onNodesChange}
							onEdgesChange={onEdgesChange}
							onConnect={onConnect}
							fitView
						>
							<Background />
							<MiniMap />
							<Controls />
						</ReactFlow>
					</div>
				</Card>
			</div>
		</div>
	);
}
