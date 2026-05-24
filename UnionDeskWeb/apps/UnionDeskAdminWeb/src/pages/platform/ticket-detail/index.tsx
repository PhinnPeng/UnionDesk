import type { BusinessDomainView } from "@uniondesk/shared";

import { fetchBusinessDomains } from "#src/api/platform/domain";
import { uploadAttachment } from "#src/api/platform/attachment";
import {
	claimAdminTicket,
	fetchTicketDetail,
	mergeAdminTicket,
	replyAdminTicket,
	updateAdminTicketStatus,
	assignAdminTicket,
	type TicketDetailResult,
} from "#src/api/platform/ticket";
import { BasicContent } from "#src/components/basic-content";

import { App, Button, Card, Descriptions, Divider, Empty, Form, Input, InputNumber, Modal, Select, Space, Tag, Timeline, Upload, Typography } from "antd";
import type { UploadProps } from "antd";
import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";

type ReplyPreset = {
	label: string
	value: string
};

const replyPresets: ReplyPreset[] = [
	{ label: "已收到，正在处理中", value: "已收到您的反馈，我们已经开始处理。" },
	{ label: "请补充信息", value: "麻烦补充一下相关截图或复现步骤，便于我们进一步排查。" },
	{ label: "问题已关闭", value: "问题已处理完成，工单将进行关闭。" },
];

async function uploadTicketAttachment(domainId: number, file: File) {
	return uploadAttachment(domainId, file, "ticket");
}

function getSlaLabel(ticket: TicketDetailResult["ticket"]) {
	if (!ticket.slaStatus) {
		return "-";
	}
	const colors: Record<string, string> = {
		tracking: "blue",
		breached: "red",
		stopped: "default",
		resolved: "green",
	};
	return <Tag color={colors[ticket.slaStatus] ?? "blue"}>{ticket.slaStatus}</Tag>;
}

function formatTime(value?: string | null) {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "-";
}

export default function PlatformTicketDetail() {
	const { message } = App.useApp();
	const [domains, setDomains] = useState<BusinessDomainView[]>([]);
	const [domainId, setDomainId] = useState<number>();
	const [ticketId, setTicketId] = useState<number>();
	const [detail, setDetail] = useState<TicketDetailResult | null>(null);
	const [loading, setLoading] = useState(false);
	const [replyLoading, setReplyLoading] = useState(false);
	const [attachmentIds, setAttachmentIds] = useState<Array<{ id: number, name: string }>>([]);
	const [replyContent, setReplyContent] = useState("");
	const [assignOpen, setAssignOpen] = useState(false);
	const [closeOpen, setCloseOpen] = useState(false);
	const [mergeOpen, setMergeOpen] = useState(false);
	const [replyForm] = Form.useForm();
	const [assignForm] = Form.useForm();
	const [closeForm] = Form.useForm();
	const [mergeForm] = Form.useForm();

	useEffect(() => {
		let ignore = false;
		void (async () => {
			try {
				const list = await fetchBusinessDomains();
				if (ignore) {
					return;
				}
				setDomains(list);
				setDomainId(list[0]?.id);
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "加载业务域失败");
			}
		})();
		return () => {
			ignore = true;
		};
	}, [message]);

	const domainOptions = useMemo(() => {
		return domains.map(domain => ({
			label: `${domain.name} / ${domain.code}`,
			value: domain.id,
		}));
	}, [domains]);

	const loadDetail = async () => {
		if (!domainId || !ticketId) {
			setDetail(null);
			return;
		}
		setLoading(true);
		try {
			const result = await fetchTicketDetail(domainId, ticketId);
			setDetail(result);
			setAttachmentIds([]);
			setReplyContent("");
			replyForm.resetFields();
		}
		catch (error) {
			setDetail(null);
			message.error(error instanceof Error ? error.message : "加载工单失败");
		}
		finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		void loadDetail();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [domainId, ticketId]);

	const onReply = async () => {
		if (!detail || !domainId || !ticketId) {
			return;
		}
		const values = await replyForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setReplyLoading(true);
		try {
			await replyAdminTicket(domainId, ticketId, {
				version: detail.ticket.version,
				content: values.content,
				attachmentIds: attachmentIds.map(item => item.id),
			});
			message.success("回复已发送");
			setReplyContent("");
			setAttachmentIds([]);
			replyForm.resetFields();
			await loadDetail();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "回复失败");
		}
		finally {
			setReplyLoading(false);
		}
	};

	const onClaim = async () => {
		if (!detail || !domainId || !ticketId) {
			return;
		}
		try {
			await claimAdminTicket(domainId, ticketId, { version: detail.ticket.version });
			message.success("工单已领取");
			await loadDetail();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "领取失败");
		}
	};

	const onAssign = async () => {
		if (!detail || !domainId || !ticketId) {
			return;
		}
		const values = await assignForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await assignAdminTicket(domainId, ticketId, {
				version: detail.ticket.version,
				assigneeStaffAccountId: values.assigneeStaffAccountId,
			});
			message.success("工单已指派");
			setAssignOpen(false);
			await loadDetail();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "指派失败");
		}
	};

	const onCloseTicket = async () => {
		if (!detail || !domainId || !ticketId) {
			return;
		}
		const values = await closeForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await updateAdminTicketStatus(domainId, ticketId, {
				status: values.status,
				version: detail.ticket.version,
				content: values.content,
			});
			message.success("工单状态已更新");
			setCloseOpen(false);
			await loadDetail();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "更新状态失败");
		}
	};

	const onMergeTicket = async () => {
		if (!detail || !domainId || !ticketId) {
			return;
		}
		const values = await mergeForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await mergeAdminTicket(domainId, ticketId, {
				version: detail.ticket.version,
				targetTicketId: values.targetTicketId,
				note: values.note,
			});
			message.success("工单已合并");
			setMergeOpen(false);
			await loadDetail();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "合并失败");
		}
	};

	const uploadProps: UploadProps = {
		multiple: false,
		showUploadList: false,
		beforeUpload: async (file) => {
			if (!domainId) {
				message.warning("请先选择业务域");
				return false;
			}
			try {
				const result = await uploadTicketAttachment(domainId, file as File);
				setAttachmentIds(prev => [...prev, { id: result.attachment_id, name: file.name }]);
				message.success(`附件已上传: ${file.name}`);
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "附件上传失败");
			}
			return false;
		},
	};

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Space direction="vertical" size={16} className="w-full">
				<Card
					title="工单详情"
					bordered={false}
					extra={<Typography.Text type="secondary">管理端视角的工单处理面板</Typography.Text>}
				>
					<div className="flex flex-wrap items-center gap-3">
						<Typography.Text className="text-slate-600">业务域</Typography.Text>
						<Select className="min-w-72" value={domainId} options={domainOptions} onChange={setDomainId} />
						<InputNumber
							className="w-40"
							placeholder="工单 ID"
							value={ticketId}
							onChange={value => setTicketId(typeof value === "number" ? value : undefined)}
						/>
						<Button type="primary" onClick={() => void loadDetail()} loading={loading}>
							加载工单
						</Button>
						<Button onClick={onClaim} disabled={!detail || loading}>
							领取
						</Button>
						<Button onClick={() => setAssignOpen(true)} disabled={!detail || loading}>
							指派
						</Button>
						<Button onClick={() => setCloseOpen(true)} disabled={!detail || loading}>
							关闭
						</Button>
						<Button onClick={() => setMergeOpen(true)} disabled={!detail || loading}>
							合并
						</Button>
					</div>
				</Card>

				{detail ? (
					<div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
						<div className="space-y-4">
							<Card title="工单信息" bordered={false}>
								<Descriptions column={2} size="small">
									<Descriptions.Item label="编号">{detail.ticket.ticketNo}</Descriptions.Item>
									<Descriptions.Item label="标题">{detail.ticket.title}</Descriptions.Item>
									<Descriptions.Item label="类型">{detail.ticket.ticketTypeName}</Descriptions.Item>
									<Descriptions.Item label="优先级">
										<Tag color={detail.ticket.priority === "urgent" ? "red" : detail.ticket.priority === "high" ? "orange" : "blue"}>{detail.ticket.priority}</Tag>
									</Descriptions.Item>
									<Descriptions.Item label="状态"><Tag color="cyan">{detail.ticket.status}</Tag></Descriptions.Item>
									<Descriptions.Item label="SLA 状态">{getSlaLabel(detail.ticket)}</Descriptions.Item>
									<Descriptions.Item label="SLA 首响截止">{formatTime(detail.ticket.slaFirstResponseDeadline)}</Descriptions.Item>
									<Descriptions.Item label="SLA 解决截止">{formatTime(detail.ticket.slaResolutionDeadline)}</Descriptions.Item>
									<Descriptions.Item label="最后回复">{formatTime(detail.ticket.lastReplyAt)}</Descriptions.Item>
									<Descriptions.Item label="创建时间">{formatTime(detail.ticket.createdAt)}</Descriptions.Item>
									<Descriptions.Item label="更新时间">{formatTime(detail.ticket.updatedAt)}</Descriptions.Item>
									<Descriptions.Item label="版本">{detail.ticket.version}</Descriptions.Item>
								</Descriptions>
							</Card>

							<Card title={`对话流 (${detail.replies.length})`} bordered={false}>
								{detail.replies.length > 0 ? (
									<Space direction="vertical" size={12} className="w-full">
										{detail.replies.map(reply => (
											<Card key={reply.id} size="small" className="bg-slate-50">
												<div className="mb-2 flex items-center justify-between gap-3">
													<div>
														<Tag color={reply.senderType === "staff" ? "blue" : "green"}>{reply.senderType ?? "unknown"}</Tag>
														<Typography.Text className="ml-2">{reply.senderRole ?? "-"}</Typography.Text>
													</div>
													<Typography.Text type="secondary">{formatTime(reply.createdAt)}</Typography.Text>
												</div>
												<Typography.Paragraph className="!mb-0 whitespace-pre-wrap">
													{reply.content || "-"}
												</Typography.Paragraph>
											</Card>
										))}
									</Space>
								) : (
									<Empty description="暂无回复" />
								)}
							</Card>

							<Card title="回复工单" bordered={false}>
								<Form form={replyForm} layout="vertical">
								<Form.Item name="content" rules={[{ required: true, message: "请输入回复内容" }]}>
										<Input.TextArea
											rows={6}
											value={replyContent}
											onChange={event => {
												const next = event.target.value;
												setReplyContent(next);
												replyForm.setFieldValue("content", next);
											}}
											placeholder="请输入管理端回复内容"
										/>
									</Form.Item>
								</Form>
								<div className="mb-3 flex flex-wrap gap-2">
									{replyPresets.map(preset => (
										<Button
											key={preset.value}
											size="small"
											onClick={() => {
												setReplyContent(preset.value);
												replyForm.setFieldValue("content", preset.value);
											}}
										>
											{preset.label}
										</Button>
									))}
								</div>
								<Space direction="vertical" size={12} className="w-full">
									<Upload {...uploadProps}>
										<Button>上传附件</Button>
									</Upload>
									{attachmentIds.length > 0 ? (
										<Space wrap>
											{attachmentIds.map(item => (
												<Tag
													key={item.id}
													closable
													onClose={() => {
														setAttachmentIds(prev => prev.filter(current => current.id !== item.id));
													}}
												>
													{item.name}
												</Tag>
											))}
										</Space>
									) : null}
									<div className="flex justify-end">
										<Button type="primary" loading={replyLoading} onClick={() => void onReply()}>
											发送回复
										</Button>
									</div>
								</Space>
							</Card>
						</div>

						<div className="space-y-4">
							<Card title="工单摘要" bordered={false}>
								<Descriptions column={1} size="small">
									<Descriptions.Item label="业务域">
										{detail.ticket.businessDomainName} / {detail.ticket.businessDomainCode}
									</Descriptions.Item>
									<Descriptions.Item label="客户 ID">{detail.ticket.customerId}</Descriptions.Item>
									<Descriptions.Item label="处理人 ID">{detail.ticket.assignedTo ?? "-"}</Descriptions.Item>
									<Descriptions.Item label="回复数">{detail.ticket.replyCount}</Descriptions.Item>
									<Descriptions.Item label="来源">{detail.ticket.source}</Descriptions.Item>
									<Descriptions.Item label="结果">{detail.ticket.result ?? "-"}</Descriptions.Item>
								</Descriptions>
							</Card>

							<Card title="历史轨迹" bordered={false}>
								<Timeline
									items={detail.history.map(item => ({
										children: (
											<div>
												<div className="font-medium">{item.action}</div>
												<div className="text-sm text-slate-500">
													{item.fromValue ?? "-"} → {item.toValue ?? "-"}
												</div>
												<div className="text-xs text-slate-400">{formatTime(item.createdAt)}</div>
											</div>
										),
									}))}
								/>
							</Card>
						</div>
					</div>
				) : (
					<Card bordered={false}>
						<Empty description="请选择业务域和工单 ID 后加载详情" />
					</Card>
				)}
			</Space>

			<Modal title="指派工单" open={assignOpen} onCancel={() => setAssignOpen(false)} onOk={() => void onAssign()} destroyOnClose>
				<Form form={assignForm} layout="vertical">
					<Form.Item name="assigneeStaffAccountId" label="处理人 ID" rules={[{ required: true, message: "请输入处理人 ID" }]}>
						<InputNumber className="w-full" min={1} />
					</Form.Item>
				</Form>
			</Modal>

			<Modal title="更新工单状态" open={closeOpen} onCancel={() => setCloseOpen(false)} onOk={() => void onCloseTicket()} destroyOnClose>
				<Form form={closeForm} layout="vertical">
					<Form.Item name="status" label="目标状态" rules={[{ required: true, message: "请选择状态" }]}>
						<Select
							options={[
								{ label: "processing", value: "processing" },
								{ label: "resolved", value: "resolved" },
								{ label: "closed", value: "closed" },
							]}
						/>
					</Form.Item>
					<Form.Item name="content" label="说明">
						<Input.TextArea rows={4} placeholder="可填写关闭说明" />
					</Form.Item>
				</Form>
			</Modal>

			<Modal title="合并工单" open={mergeOpen} onCancel={() => setMergeOpen(false)} onOk={() => void onMergeTicket()} destroyOnClose>
				<Form form={mergeForm} layout="vertical">
					<Form.Item name="targetTicketId" label="目标工单 ID" rules={[{ required: true, message: "请输入目标工单 ID" }]}>
						<InputNumber className="w-full" min={1} />
					</Form.Item>
					<Form.Item name="note" label="合并备注">
						<Input.TextArea rows={4} placeholder="说明为什么合并" />
					</Form.Item>
				</Form>
			</Modal>
		</BasicContent>
	);
}
