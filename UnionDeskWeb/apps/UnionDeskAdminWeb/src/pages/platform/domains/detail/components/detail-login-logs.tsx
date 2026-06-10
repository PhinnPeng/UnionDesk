import type { LoginLogView } from "#src/api/platform/audit";
import { fetchDomainLoginLogs } from "#src/api/platform/audit";

import { AuthGuarded } from "#src/components/auth-guarded";
import { TableSearchForm } from "#src/components/table-search-form";

import { App, Card, DatePicker, Empty, Form, Select, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useCallback, useEffect, useState } from "react";

import { DOMAIN_CONTROL_LOGIN_LOG_READ_PERMISSION, resolveNumericDomainId } from "./detail-shared";

const { RangePicker } = DatePicker;

export interface DetailLoginLogsProps {
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

export function DetailLoginLogs({ domainId }: DetailLoginLogsProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm();
	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState<LoginLogView[]>([]);
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
			const result = await fetchDomainLoginLogs(numericDomainId, {
				page: nextPage,
				page_size: nextPageSize,
				portal_type: values.portalType,
				result: values.result,
				...buildDateRange(values.timeRange),
			});
			setRows(result.list);
			setTotal(result.total);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载登录日志失败");
		}
		finally {
			setLoading(false);
		}
	}, [form, message, numericDomainId, page, pageSize]);

	useEffect(() => {
		void loadLogs(page, pageSize);
	}, [loadLogs, page, pageSize]);

	const columns: TableColumnsType<LoginLogView> = [
		{
			title: "账号",
			dataIndex: "operatorName",
			width: 150,
			render: (_, row) => row.operatorName ?? row.loginName ?? "—",
		},
		{
			title: "门户",
			dataIndex: "portalType",
			width: 120,
			render: (_, row) => row.portalType ?? "—",
		},
		{
			title: "结果",
			dataIndex: "result",
			width: 110,
			render: (_, row) => (
				<Tag color={row.result === "success" ? "green" : "red"}>{row.result ?? "—"}</Tag>
			),
		},
		{
			title: "IP",
			dataIndex: "ip",
			width: 150,
			render: (_, row) => row.ip ?? "—",
		},
		{
			title: "失败原因",
			dataIndex: "failReason",
			ellipsis: true,
			render: (_, row) => row.failReason ?? "—",
		},
		{
			title: "登录时间",
			dataIndex: "createdAt",
			width: 180,
			render: (_, row) => row.createdAt ? dayjs(row.createdAt).format("YYYY-MM-DD HH:mm:ss") : "—",
		},
	];

	if (numericDomainId === null) {
		return <Empty description="无效的业务域 ID" />;
	}

	return (
		<AuthGuarded auth={DOMAIN_CONTROL_LOGIN_LOG_READ_PERMISSION} fallback={<Empty description="无权限查看登录日志" />}>
			<Card title="登录日志" bordered={false}>
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
					<Form.Item name="portalType" label="门户">
						<Select
							allowClear
							placeholder="全部门户"
							options={[
								{ label: "admin", value: "admin" },
								{ label: "customer", value: "customer" },
							]}
						/>
					</Form.Item>
					<Form.Item name="result" label="结果">
						<Select
							allowClear
							placeholder="全部结果"
							options={[
								{ label: "success", value: "success" },
								{ label: "failure", value: "failure" },
							]}
						/>
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
					scroll={{ x: 1000 }}
				/>
			</Card>
		</AuthGuarded>
	);
}
