import type { PlatformAuditLogView } from "#src/api/platform/audit";
import { fetchDomainAuditLogs } from "#src/api/platform/audit";

import { AuthGuarded } from "#src/components/auth-guarded";
import { TableSearchForm } from "#src/components/table-search-form";

import { App, Card, DatePicker, Empty, Form, Input, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useCallback, useEffect, useState } from "react";

import { DOMAIN_CONTROL_AUDIT_LOG_READ_PERMISSION, resolveNumericDomainId } from "./detail-shared";

const { RangePicker } = DatePicker;

export interface DetailAuditLogsProps {
	domainId: string;
}

function buildDateRange(values: [Dayjs | null, Dayjs | null] | null | undefined) {
	if (!values || values.length !== 2) {
		return {};
	}
	const [start, end] = values;
	return {
		startTime: start ? start.format("YYYY-MM-DDTHH:mm:ss") : undefined,
		endTime: end ? end.format("YYYY-MM-DDTHH:mm:ss") : undefined,
	};
}

export function DetailAuditLogs({ domainId }: DetailAuditLogsProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm();
	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState<PlatformAuditLogView[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);

	const numericDomainId = resolveNumericDomainId(domainId);

	const loadLogs = useCallback(async (nextPage = page, nextPageSize = pageSize) => {
		if (numericDomainId === null) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const values = form.getFieldsValue();
			const result = await fetchDomainAuditLogs(numericDomainId, {
				page: nextPage,
				page_size: nextPageSize,
				operator: values.operator,
				action: values.action,
				...buildDateRange(values.timeRange),
			});
			setRows(result.list);
			setTotal(result.total);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载操作日志失败");
		}
		finally {
			setLoading(false);
		}
	}, [form, message, numericDomainId, page, pageSize]);

	useEffect(() => {
		void loadLogs(page, pageSize);
	}, [loadLogs, page, pageSize]);

	const columns: TableColumnsType<PlatformAuditLogView> = [
		{
			title: "操作者",
			dataIndex: "operatorName",
			width: 150,
			render: (_, row) => row.operatorName ?? "—",
		},
		{
			title: "动作",
			dataIndex: "action",
			width: 180,
			render: (_, row) => row.action ?? "—",
		},
		{
			title: "结果",
			dataIndex: "result",
			width: 110,
			render: (_, row) => (
				<Tag color={row.result === "success" ? "green" : "gold"}>{row.result ?? "—"}</Tag>
			),
		},
		{
			title: "目标",
			dataIndex: "target",
			width: 220,
			render: (_, row) => row.target ?? "—",
		},
		{
			title: "明细",
			dataIndex: "detail",
			ellipsis: true,
			render: (_, row) => row.detail ?? "—",
		},
		{
			title: "发生时间",
			dataIndex: "occurredAt",
			width: 180,
			render: (_, row) => row.occurredAt ? dayjs(row.occurredAt).format("YYYY-MM-DD HH:mm:ss") : "—",
		},
	];

	if (numericDomainId === null) {
		return <Empty description="无效的业务域 ID" />;
	}

	return (
		<AuthGuarded auth={DOMAIN_CONTROL_AUDIT_LOG_READ_PERMISSION} fallback={<Empty description="无权限查看操作日志" />}>
			<Card title="操作日志" bordered={false}>
				<TableSearchForm
					form={form}
					className="mb-4"
					onFinish={() => {
						setPage(1);
						void loadLogs(1, pageSize);
					}}
					onReset={() => {
						setPage(1);
						void loadLogs(1, pageSize);
					}}
				>
					<Form.Item name="operator" label="操作者">
						<Input allowClear placeholder="姓名 / ID" />
					</Form.Item>
					<Form.Item name="action" label="动作">
						<Input allowClear placeholder="如 ticket.reply" />
					</Form.Item>
					<Form.Item name="timeRange" label="时间范围">
						<RangePicker showTime className="w-full" />
					</Form.Item>
				</TableSearchForm>
				<Table
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={rows}
					pagination={{
						current: page,
						pageSize,
						total,
						showSizeChanger: true,
						onChange: (nextPage, nextPageSize) => {
							setPage(nextPage);
							setPageSize(nextPageSize);
						},
					}}
					scroll={{ x: 1100 }}
				/>
			</Card>
		</AuthGuarded>
	);
}
