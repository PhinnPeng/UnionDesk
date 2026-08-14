import type { LoginLogView } from "#src/api/platform/audit";
import { fetchDomainLoginLogs } from "#src/api/platform/audit";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import {
	formatLoginPortalType,
	formatLoginResult,
} from "#src/pages/platform/audit-logs/audit-log-labels";
import { DOMAIN_LOGIN_LOG_READ } from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import { SearchOutlined } from "@ant-design/icons";
import { App, Card, DatePicker, Empty, Form, Select, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";

const { RangePicker } = DatePicker;

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

export default function DomainLoginLogsPage() {
	const { message } = App.useApp();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);
	const [form] = Form.useForm();

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);
	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState<LoginLogView[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);

	const loadLogs = useCallback(async (nextPage = page, nextPageSize = pageSize) => {
		if (!domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const values = form.getFieldsValue();
			const result = await fetchDomainLoginLogs(domainId, {
				page: nextPage,
				page_size: nextPageSize,
				portal_type: values.portalType,
				result: values.result,
				...buildDateRange(values.timeRange),
			});
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载登录日志失败");
		}
		finally {
			setLoading(false);
		}
	}, [domainId, form, message, page, pageSize]);

	useEffect(() => {
		void loadLogs(1, 20);
	// eslint-disable-next-line react-hooks/exhaustive-deps -- domainId 变化时初始化
	}, [domainId]);

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
			render: (_, row) => formatLoginPortalType(row.portalType),
		},
		{
			title: "结果",
			dataIndex: "result",
			width: 110,
			render: (_, row) => (
				<Tag color={row.result === "success" ? "green" : "red"}>{formatLoginResult(row.result)}</Tag>
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

	if (!domainId) {
		return (
			<BasicContent>
				<Empty description="暂无可用业务域" />
			</BasicContent>
		);
	}

	return (
		<AuthGuarded auth={DOMAIN_LOGIN_LOG_READ} fallback={<BasicContent><Empty description="无权限查看登录日志" /></BasicContent>}>
			<BasicContent>
				<div className="flex flex-col gap-4">
					<Card
						bordered={false}
						title={(
							<>
								<SearchOutlined />
								{" "}
								筛选条件
							</>
						)}
					>
						<TableSearchForm
							form={form}
							onFinish={() => {
								void loadLogs(1, pageSize);
							}}
							onReset={() => {
								void loadLogs(1, pageSize);
							}}
						>
							<Form.Item name="portalType" label="门户">
								<Select
									allowClear
									placeholder="全部门户"
									options={[
										{ label: "员工端", value: "staff" },
										{ label: "客户端", value: "customer" },
									]}
								/>
							</Form.Item>
							<Form.Item name="result" label="结果">
								<Select
									allowClear
									placeholder="全部结果"
									options={[
										{ label: "成功", value: "success" },
										{ label: "失败", value: "failure" },
									]}
								/>
							</Form.Item>
							<Form.Item name="timeRange" label="时间范围">
								<RangePicker showTime className="w-full" allowEmpty={[true, true]} />
							</Form.Item>
						</TableSearchForm>
					</Card>
					<Card bordered={false} title="登录日志列表">
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
									void loadLogs(nextPage, nextPageSize);
								},
							}}
							scroll={{ x: 1000 }}
						/>
					</Card>
				</div>
			</BasicContent>
		</AuthGuarded>
	);
}
