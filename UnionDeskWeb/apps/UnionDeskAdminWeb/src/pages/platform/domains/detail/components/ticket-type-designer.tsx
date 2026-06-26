import type { DomainTicketType, TicketStatusFlow } from "@uniondesk/shared";
import { updateDomainTicketType, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "../../platform-domain-permissions";

import { App, Button, Form, Input, Space, Switch, Tabs, Tag, Typography } from "antd";
import { lazy, Suspense, useEffect, useState } from "react";

import { FormilyFormDesignerFallback } from "#src/components/formily-form-designer";

import { DEFAULT_STATUS_FLOW, mergeSystemFormSchema } from "./ticket-type-form-defaults";
import { TicketTypeFlowDesigner } from "./ticket-type-flow-designer";

const FormilyFormDesigner = lazy(() => import("#src/components/formily-form-designer").then(module => ({
	default: module.FormilyFormDesigner,
})));

const { Text } = Typography;

export interface TicketTypeDesignerProps {
	domainId: string;
	ticketType: DomainTicketType;
	onCancel: () => void;
	onSaved: (ticketType: DomainTicketType) => void;
}

export function TicketTypeDesigner({
	domainId,
	ticketType,
	onCancel,
	onSaved,
}: TicketTypeDesignerProps) {
	const { message } = App.useApp();
	const [submitting, setSubmitting] = useState(false);
	const [name, setName] = useState("");
	const [status, setStatus] = useState("active");
	const [formSchema, setFormSchema] = useState<Record<string, unknown>>(mergeSystemFormSchema(null));
	const [statusFlow, setStatusFlow] = useState<TicketStatusFlow>(DEFAULT_STATUS_FLOW);

	useEffect(() => {
		setName(ticketType.name);
		setStatus(ticketType.status === "disabled" ? "disabled" : "active");
		setFormSchema(mergeSystemFormSchema(ticketType.form_schema));
		setStatusFlow((ticketType.status_flow as TicketStatusFlow | null) ?? DEFAULT_STATUS_FLOW);
	}, [ticketType]);

	const handleSave = async () => {
		const trimmedName = name.trim();
		if (!trimmedName) {
			message.warning("请输入类型名称");
			return;
		}
		setSubmitting(true);
		try {
			const saved = await updateDomainTicketType(domainId, ticketType.id, {
				name: trimmedName,
				status,
				status_flow: statusFlow,
			});
			message.success("工单类型已保存");
			onSaved(saved);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	return (
		<div className="flex flex-col gap-4">
			<Tabs
				destroyOnHidden
				items={[
					{
						key: "basic",
						label: "基础信息",
						children: (
							<Form layout="vertical">
								<Form.Item label="类型编码">
									<Input value={ticketType.code} disabled />
								</Form.Item>
								<Form.Item label="类型名称" required>
									<Input value={name} onChange={event => setName(event.target.value)} />
								</Form.Item>
								<Form.Item label="启用状态">
									<Space>
										<Switch
											checked={status === "active"}
											onChange={checked => setStatus(checked ? "active" : "disabled")}
										/>
										<Tag color={status === "active" ? "success" : "default"}>
											{status === "active" ? "启用" : "停用"}
										</Tag>
									</Space>
								</Form.Item>
								<Text type="secondary">系统字段 title / description 为所有工单类型必填，不可删除。</Text>
							</Form>
						),
					},
					{
						key: "form",
						label: "表单设计",
						children: (
							<Suspense fallback={<FormilyFormDesignerFallback />}>
								<FormilyFormDesigner
									key={ticketType.id}
									value={formSchema}
									onChange={setFormSchema}
									hint="系统字段「标题」「详细描述」为必填且不可删除；可从左侧拖入控件扩展表单。"
								/>
							</Suspense>
						),
					},
					{
						key: "flow",
						label: "状态流",
						children: (
							<TicketTypeFlowDesigner
								key={ticketType.id}
								value={statusFlow}
								onChange={setStatusFlow}
							/>
						),
					},
				]}
			/>
			<div className="flex justify-end border-t border-colorBorderSecondary pt-4">
				<Space>
					<Button onClick={onCancel}>取消</Button>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<Button type="primary" loading={submitting} onClick={() => void handleSave()}>
							保存
						</Button>
					</AuthGuarded>
				</Space>
			</div>
		</div>
	);
}
