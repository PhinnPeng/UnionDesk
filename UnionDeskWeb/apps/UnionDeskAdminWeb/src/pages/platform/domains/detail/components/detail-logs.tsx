import type { PlatformAuditLogView } from "#src/api/platform/audit";
import { fetchPlatformAuditLogs } from "#src/api/platform/audit";

import { AuthGuarded } from "#src/components/auth-guarded";

import { App, Empty, Table, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useState } from "react";

import { resolveNumericDomainId } from "./detail-shared";

const { Title } = Typography;

export interface DetailLogsProps {
	domainId: string;
}

export function DetailLogs({ domainId }: DetailLogsProps) {
	const { message } = App.useApp();
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
			const result = await fetchPlatformAuditLogs({
				page: nextPage,
				page_size: nextPageSize,
				domain_id: numericDomainId,
			});
			setRows(result.list);
			setTotal(result.total);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载域内日志失败");
		}
		finally {
			setLoading(false);
		}
	}, [message, numericDomainId, page, pageSize]);

	useEffect(() => {
		void loadLogs(page, pageSize);
	}, [loadLogs, page, pageSize]);

	const columns: TableColumnsType<PlatformAuditLogView> = [
		{
			title: "时间",
			dataIndex: "occurredAt",
			width: 180,
			render: (_, row) => row.occurredAt ? dayjs(row.occurredAt).format("YYYY-MM-DD HH:mm:ss") : "—",
		},
		{
			title: "事件类型",
			dataIndex: "action",
			width: 180,
			render: (_, row) => row.action ?? "—",
		},
		{
			title: "操作主体",
			dataIndex: "operatorName",
			width: 150,
			render: (_, row) => row.operatorName ?? "—",
		},
		{
			title: "IP",
			dataIndex: "ip",
			width: 140,
			render: (_, row) => row.ip ?? "—",
		},
		{
			title: "动作内容",
			dataIndex: "detail",
			ellipsis: true,
			render: (_, row) => row.detail ?? row.target ?? "—",
		},
	];

	return (
		<AuthGuarded auth="domain.audit_log.read" fallback={<Empty description="无权限查看域内日志" />}>
			<div>
				<Title level={5} className="!mb-4">
					域内日志
				</Title>
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
				/>
			</div>
		</AuthGuarded>
	);
}
