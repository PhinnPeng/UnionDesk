import {
	fetchNotificationTemplates,
	updateNotificationTemplate,
	type NotificationTemplateView,
} from "#src/api/platform/notification-template";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import {
	DOMAIN_NOTIFICATION_TEMPLATE_READ,
	DOMAIN_NOTIFICATION_TEMPLATE_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import { EditOutlined, ReloadOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Empty,
	Form,
	Input,
	Modal,
	Select,
	Space,
	Switch,
	Table,
	Tag,
	Tooltip,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

const STATUS_OPTIONS = [
	{ label: "启用", value: "active" },
	{ label: "停用", value: "disabled" },
];

function resolveBusinessDomainId(
	defaultBusinessDomainId: string,
	accessibleDomains: Array<{ id: string }>,
): string {
	if (defaultBusinessDomainId) {
		return defaultBusinessDomainId;
	}
	const first = accessibleDomains[0];
	return first ? first.id : "";
}

function resolveStatusLabel(status: string) {
	return status === "active" ? "启用" : "停用";
}

export default function DomainNotificationsPage() {
	const { message } = App.useApp();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);
	const [rows, setRows] = useState<NotificationTemplateView[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [loading, setLoading] = useState(false);
	const [editing, setEditing] = useState<NotificationTemplateView | null>(null);
	const [submitting, setSubmitting] = useState(false);
	const [form] = Form.useForm();

	const loadTemplates = useCallback(async (nextPage = page, nextPageSize = pageSize) => {
		if (!domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const result = await fetchNotificationTemplates(domainId, { page: nextPage, page_size: nextPageSize });
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载通知模板失败");
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, page, pageSize]);

	useEffect(() => {
		void loadTemplates(1, pageSize);
		// eslint-disable-next-line react-hooks/exhaustive-deps -- 域变化时回到第一页
	}, [domainId]);

	const openEditor = (row: NotificationTemplateView) => {
		setEditing(row);
		form.setFieldsValue({
			eventCategory: row.eventCategory,
			channel: row.channel,
			code: row.code,
			titleTemplate: row.titleTemplate,
			contentTemplate: row.contentTemplate,
			isSecurity: row.isSecurity,
			isUnsubscribable: row.isUnsubscribable,
			status: row.status === "active" ? "active" : "disabled",
		});
	};

	const closeEditor = () => {
		setEditing(null);
	};

	const submitEditor = async () => {
		if (!domainId || !editing) {
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setSubmitting(true);
		try {
			await updateNotificationTemplate(domainId, editing.id, {
				eventCategory: values.eventCategory,
				channel: values.channel,
				code: values.code,
				titleTemplate: values.titleTemplate,
				contentTemplate: values.contentTemplate,
				isSecurity: !!values.isSecurity,
				isUnsubscribable: !!values.isUnsubscribable,
				status: values.status,
			});
			message.success("通知模板已更新");
			closeEditor();
			await loadTemplates(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
		finally {
			setSubmitting(false);
		}
	};

	const columns: TableColumnsType<NotificationTemplateView> = [
		{ title: "事件分类", dataIndex: "eventCategory", width: 140, render: value => value || "-" },
		{ title: "渠道", dataIndex: "channel", width: 90, render: value => value || "-" },
		{ title: "模板编码", dataIndex: "code", width: 160, render: value => value || "-" },
		{ title: "标题模板", dataIndex: "titleTemplate", ellipsis: true, render: value => value || "-" },
		{ title: "内容模板", dataIndex: "contentTemplate", ellipsis: true, render: value => value || "-" },
		{
			title: "状态",
			dataIndex: "status",
			width: 80,
			render: value => (
				<Tag color={value === "active" ? "success" : "default"}>
					{resolveStatusLabel(value)}
				</Tag>
			),
		},
		{
			title: "操作",
			key: "actions",
			width: 80,
			render: (_, row) => (
				<AuthGuarded auth={DOMAIN_NOTIFICATION_TEMPLATE_UPDATE} fallback={null}>
					<Tooltip title="编辑">
						<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEditor(row)} />
					</Tooltip>
				</AuthGuarded>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<AuthGuarded auth={DOMAIN_NOTIFICATION_TEMPLATE_READ} fallback={<Empty description="无权限查看通知配置" />}>
				{!domainId
					? <Empty description="暂无可用业务域" />
					: (
						<Card title="通知配置" bordered={false} extra={(
							<Button icon={<ReloadOutlined />} onClick={() => void loadTemplates(page, pageSize)}>
								刷新
							</Button>
						)}>
							<Table<NotificationTemplateView>
								rowKey="id"
								loading={loading}
								columns={columns}
								dataSource={rows}
								pagination={{
									current: page,
									pageSize,
									total,
									showSizeChanger: true,
									showTotal: value => `共 ${value} 条`,
									onChange: (nextPage, nextPageSize) => {
										void loadTemplates(nextPage, nextPageSize);
									},
								}}
								scroll={{ x: 1000 }}
								locale={{ emptyText: <Empty description="暂无通知模板" /> }}
							/>
						</Card>
					)}
			</AuthGuarded>

			<Modal
				title="编辑通知模板"
				open={editing !== null}
				onCancel={closeEditor}
				onOk={() => void submitEditor()}
				confirmLoading={submitting}
				destroyOnClose
				width={640}
			>
				<Form form={form} layout="vertical">
					<div className="grid gap-4 lg:grid-cols-3">
						<Form.Item name="eventCategory" label="事件分类">
							<Input disabled />
						</Form.Item>
						<Form.Item name="channel" label="渠道">
							<Input disabled />
						</Form.Item>
						<Form.Item name="code" label="模板编码">
							<Input disabled />
						</Form.Item>
					</div>
					<Form.Item name="titleTemplate" label="标题模板" rules={[{ required: true, message: "请输入标题模板" }]}>
						<Input placeholder="如 【工单提醒】您的工单有新回复" />
					</Form.Item>
					<Form.Item name="contentTemplate" label="内容模板" rules={[{ required: true, message: "请输入内容模板" }]}>
						<Input.TextArea rows={6} placeholder="支持占位符，如 {ticket_no}、{customer_name}" />
					</Form.Item>
					<div className="grid gap-4 lg:grid-cols-3">
						<Form.Item name="status" label="状态" rules={[{ required: true, message: "请选择状态" }]}>
							<Select options={STATUS_OPTIONS} />
						</Form.Item>
						<Form.Item name="isSecurity" label="安全通知" valuePropName="checked">
							<Switch />
						</Form.Item>
						<Form.Item name="isUnsubscribable" label="可退订" valuePropName="checked">
							<Switch />
						</Form.Item>
					</div>
				</Form>
			</Modal>
		</BasicContent>
	);
}
