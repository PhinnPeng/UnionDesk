import { fetchDomainPriorityLevels, fetchDomainTicketStatuses, toErrorMessage, type DomainPriorityLevelView, type TicketStatusDefinition } from "@uniondesk/shared";

import { uploadAttachment } from "#src/api/platform/attachment";
import {
	assignAdminTicket,
	claimAdminTicket,
	fetchTicketDetail,
	mergeAdminTicket,
	replyAdminTicket,
	replaceAdminTicketWatchers,
	updateAdminTicketStatus,
	type TicketDetailResult,
} from "#src/api/platform/ticket";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { MemberPicker } from "#src/pages/platform/components/member-picker";
import { PriorityBadge } from "#src/pages/platform/components/priority-badge";
import { useAuthStore } from "#src/store/auth";

import { App, Button, Card, Descriptions, Empty, Form, Input, InputNumber, Modal, Select, Space, Tag, Timeline, Upload, Typography } from "antd";
import type { UploadProps } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";

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

export default function DomainTicketQueueDetailPage() {
	const { message } = App.useApp();
	const params = useParams<{ ticketId: string }>();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(() => {
		if (defaultBusinessDomainId > 0) {
			return defaultBusinessDomainId;
		}
		const first = accessibleDomains?.[0];
		return first ? Number(first.id) : 0;
	}, [accessibleDomains, defaultBusinessDomainId]);

	const ticketId = useMemo(() => {
		const parsed = Number(params.ticketId);
		return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
	}, [params.ticketId]);

	const [detail, setDetail] = useState<TicketDetailResult | null>(null);
	const [loading, setLoading] = useState(false);
	const [replyLoading, setReplyLoading] = useState(false);
	const [attachmentIds, setAttachmentIds] = useState<Array<{ id: number, name: string }>>([]);
	const [replyContent, setReplyContent] = useState("");
	const [statuses, setStatuses] = useState<TicketStatusDefinition[]>([]);
	const [priorityLevels, setPriorityLevels] = useState<DomainPriorityLevelView[]>([]);
	const [assignOpen, setAssignOpen] = useState(false);
	const [watchersOpen, setWatchersOpen] = useState(false);
	const [closeOpen, setCloseOpen] = useState(false);
	const [mergeOpen, setMergeOpen] = useState(false);
	const [replyForm] = Form.useForm();
	const [assignForm] = Form.useForm();
	const [watchersForm] = Form.useForm();
	const [closeForm] = Form.useForm();
	const [mergeForm] = Form.useForm();

	const statusMap = useMemo(() => {
		const map: Record<string, TicketStatusDefinition> = {};
		for (const item of statuses) {
			map[item.code] = item;
		}
		return map;
	}, [statuses]);

	const priorityMap = useMemo(() => {
		const map: Record<string, DomainPriorityLevelView> = {};
		for (const item of priorityLevels) {
			map[item.code] = item;
		}
		return map;
	}, [priorityLevels]);

	const statusOptions = useMemo(
		() => statuses
			.filter(item => item.status === "active")
			.map(item => ({ value: item.code, label: item.name })),
		[statuses],
	);

	const loadMeta = useCallback(async () => {
		if (!domainId) {
			return;
		}
		try {
			const [statusResult, priorityResult] = await Promise.all([
				fetchDomainTicketStatuses(String(domainId), { page: 1, page_size: 200 }),
				fetchDomainPriorityLevels(String(domainId)),
			]);
			setStatuses(statusResult.items ?? []);
			setPriorityLevels(priorityResult.items ?? []);
		}
		catch {
			// 下拉选项加载失败不阻塞详情，选项留空兜底
		}
	}, [domainId]);

	const loadDetail = useCallback(async () => {
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
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, replyForm, ticketId]);

	useEffect(() => {
		void loadDetail();
		void loadMeta();
	}, [loadDetail, loadMeta]);

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
			message.error(toErrorMessage(error));
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
			message.error(toErrorMessage(error));
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
			message.error(toErrorMessage(error));
		}
	};

	const onReplaceWatchers = async () => {
		if (!detail || !domainId || !ticketId) {
			return;
		}
		const values = await watchersForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await replaceAdminTicketWatchers(domainId, ticketId, {
				watcherStaffAccountIds: values.watcherStaffAccountIds ?? [],
			});
			message.success("关注人已更新");
			setWatchersOpen(false);
			await loadDetail();
		}
		catch (error) {
			message.error(toErrorMessage(error));
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
			message.error(toErrorMessage(error));
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
			message.error(toErrorMessage(error));
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
				message.error(toErrorMessage(error));
			}
			return false;
		},
	};

	const ticketPriority = detail ? priorityMap[detail.ticket.priority] : undefined;

	return (
		<BasicContent>
			<div className="mb-4">
				<Link to="/domain/ticket-queue">
					<Button type="link" className="!px-0">← 返回工单队列</Button>
				</Link>
			</div>
			<AuthGuarded auth="ticket.view.domain_all" fallback={<Empty description="无权限查看工单详情" className="py-16" />}>
				<Space direction="vertical" size={16} className="w-full">
					<Card
						title="工单详情"
						bordered={false}
						extra={(
							<Typography.Text type="secondary">
								业务域工单处理面板
							</Typography.Text>
						)}
					>
						<div className="flex flex-wrap items-center gap-3">
							<Space size="small">
								<AuthGuarded auth="ticket.claim" fallback={null}>
									<Button onClick={onClaim} disabled={!detail || loading}>领取</Button>
								</AuthGuarded>
								<AuthGuarded auth="ticket.assign" fallback={null}>
									<Button onClick={() => setAssignOpen(true)} disabled={!detail || loading}>指派</Button>
								</AuthGuarded>
								<AuthGuarded auth="ticket.assign" fallback={null}>
									<Button
										onClick={() => {
											watchersForm.setFieldsValue({
												watcherStaffAccountIds: detail?.watcherStaffAccountIds ?? [],
											});
											setWatchersOpen(true);
										}}
										disabled={!detail || loading}
									>
										关注人
									</Button>
								</AuthGuarded>
								<AuthGuarded auth="ticket.close" fallback={null}>
									<Button onClick={() => setCloseOpen(true)} disabled={!detail || loading}>关闭</Button>
								</AuthGuarded>
								<AuthGuarded auth="ticket.merge" fallback={null}>
									<Button onClick={() => setMergeOpen(true)} disabled={!detail || loading}>合并</Button>
								</AuthGuarded>
							</Space>
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
											<PriorityBadge
												code={detail.ticket.priority}
												name={ticketPriority?.display_label ?? ticketPriority?.name}
												color={ticketPriority?.color}
												icon={ticketPriority?.icon}
											/>
										</Descriptions.Item>
										<Descriptions.Item label="状态">
											<Tag color={statusMap[detail.ticket.status] ? "cyan" : "default"}>
												{statusMap[detail.ticket.status]?.name ?? detail.ticket.status}
											</Tag>
										</Descriptions.Item>
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
												placeholder="请输入回复内容"
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
										<Descriptions.Item label="关注人">
											{(detail.watcherStaffAccountIds?.length ?? 0) > 0
												? detail.watcherStaffAccountIds!.join(", ")
												: "-"}
										</Descriptions.Item>
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
							<Empty description="加载工单详情失败或工单不存在" />
						</Card>
					)}
				</Space>
			</AuthGuarded>

			<Modal title="指派工单" open={assignOpen} onCancel={() => setAssignOpen(false)} onOk={() => void onAssign()} destroyOnClose>
				<Form form={assignForm} layout="vertical">
					<Form.Item name="assigneeStaffAccountId" label="处理人" rules={[{ required: true, message: "请选择处理人" }]}>
						<MemberPicker domainId={domainId} />
					</Form.Item>
				</Form>
			</Modal>

			<Modal
				title="设置关注人"
				open={watchersOpen}
				onCancel={() => setWatchersOpen(false)}
				onOk={() => void onReplaceWatchers()}
				destroyOnClose
			>
				<Form form={watchersForm} layout="vertical">
					<Form.Item name="watcherStaffAccountIds" label="关注人">
						<MemberPicker domainId={domainId} multiple />
					</Form.Item>
				</Form>
			</Modal>

			<Modal title="更新工单状态" open={closeOpen} onCancel={() => setCloseOpen(false)} onOk={() => void onCloseTicket()} destroyOnClose>
				<Form form={closeForm} layout="vertical">
					<Form.Item name="status" label="目标状态" rules={[{ required: true, message: "请选择状态" }]}>
						<Select options={statusOptions} placeholder="请选择目标状态" />
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
