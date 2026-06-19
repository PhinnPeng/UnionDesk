import type { DomainTicketTemplate, DomainTicketType, TicketStatusFlow } from "@uniondesk/shared";
import { updateDomainTicketType, toErrorMessage } from "@uniondesk/shared";

import { App, Button, Drawer, Form, Input, Space, Switch, Tabs, Tag, Typography } from "antd";
import { useEffect, useState } from "react";

import { AuthGuarded } from "#src/components/auth-guarded";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "../../platform-domain-permissions";

import { DEFAULT_STATUS_FLOW, mergeSystemFormSchema } from "./ticket-type-form-defaults";
import { TicketTypeFlowDesigner } from "./ticket-type-flow-designer";
import { TicketTypeFormDesigner } from "./ticket-type-form-designer";

const { Text } = Typography;

export interface TicketTypeDesignerDrawerProps {
	open: boolean;
	domainId: string;
	ticketType: DomainTicketType | null;
	onClose: () => void;
	onSaved: (ticketType: DomainTicketType) => void;
}

export function TicketTypeDesignerDrawer({
	open,
	domainId,
	ticketType,
	onClose,
	onSaved,
}: TicketTypeDesignerDrawerProps) {
	const { message } = App.useApp();
	const [submitting, setSubmitting] = useState(false);
	const [name, setName] = useState("");
	const [status, setStatus] = useState("active");
	const [formSchema, setFormSchema] = useState<Record<string, unknown>>(mergeSystemFormSchema(null));
	const [statusFlow, setStatusFlow] = useState<TicketStatusFlow>(DEFAULT_STATUS_FLOW);

	useEffect(() => {
		if (!open || !ticketType) {
			return;
		}
		setName(ticketType.name);
		setStatus(ticketType.status === "disabled" ? "disabled" : "active");
		setFormSchema(mergeSystemFormSchema(ticketType.form_schema));
		setStatusFlow((ticketType.status_flow as TicketStatusFlow | null) ?? DEFAULT_STATUS_FLOW);
	}, [open, ticketType]);

	const handleSave = async () => {
		if (!ticketType) {
			return;
		}
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
				form_schema: formSchema,
				status_flow: statusFlow,
			});
			message.success("工单类型已保存");
			onSaved(saved);
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	return (
		<Drawer
			title={ticketType ? `配置工单类型：${ticketType.code}` : "配置工单类型"}
			open={open}
			width={960}
			destroyOnClose
			onClose={onClose}
			footer={(
				<div className="flex justify-end">
					<Space>
						<Button onClick={onClose}>取消</Button>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
							<Button type="primary" loading={submitting} onClick={() => void handleSave()}>
								保存
							</Button>
						</AuthGuarded>
					</Space>
				</div>
			)}
		>
			{ticketType ? (
				<Tabs
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
								<TicketTypeFormDesigner
									key={ticketType.id}
									value={formSchema}
									onChange={setFormSchema}
								/>
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
			) : null}
		</Drawer>
	);
}
