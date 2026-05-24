import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import { fetchBusinessDomains } from "#src/api/platform/domain";
import { fetchLoginLogsPage, fetchPlatformAuditLogs } from "#src/api/platform/audit";

import { App, Card, DatePicker, Form, Input, Select, Space, Table, Tag, Tabs, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";

import type { BusinessDomainView } from "@uniondesk/shared";

import type { LoginLogView, PlatformAuditLogView } from "#src/api/platform/audit";

type AuditTabKey = "audit" | "login";

const { RangePicker } = DatePicker;

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

export default function PlatformAuditLogs() {
	const { message } = App.useApp();
	const [activeTab, setActiveTab] = useState<AuditTabKey>("audit");
	const [domains, setDomains] = useState<BusinessDomainView[]>([]);
	const [auditRows, setAuditRows] = useState<PlatformAuditLogView[]>([]);
	const [loginRows, setLoginRows] = useState<LoginLogView[]>([]);
	const [auditLoading, setAuditLoading] = useState(false);
	const [loginLoading, setLoginLoading] = useState(false);
	const [auditTotal, setAuditTotal] = useState(0);
	const [loginTotal, setLoginTotal] = useState(0);
	const [auditForm] = Form.useForm();
	const [loginForm] = Form.useForm();

	useEffect(() => {
		let ignore = false;
		void (async () => {
			try {
				const list = await fetchBusinessDomains();
				if (!ignore) {
					setDomains(list);
				}
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

	const loadAuditLogs = async (page = 1, pageSize = 20) => {
		setAuditLoading(true);
		try {
			const values = auditForm.getFieldsValue();
			const response = await fetchPlatformAuditLogs({
				page,
				page_size: pageSize,
				domain_id: values.domainId,
				operator: values.operator,
				action: values.action,
				...buildDateRange(values.timeRange),
			});
			setAuditRows(response.list);
			setAuditTotal(response.total);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载审计日志失败");
		}
		finally {
			setAuditLoading(false);
		}
	};

	const loadLoginLogs = async (page = 1, pageSize = 20) => {
		setLoginLoading(true);
		try {
			const values = loginForm.getFieldsValue();
			const response = await fetchLoginLogsPage({
				page,
				page_size: pageSize,
				subject_id: values.subjectId,
				portal_type: values.portalType,
				result: values.result,
				...buildDateRange(values.timeRange),
			});
			setLoginRows(response.list);
			setLoginTotal(response.total);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载登录日志失败");
		}
		finally {
			setLoginLoading(false);
		}
	};

	useEffect(() => {
		if (activeTab === "audit") {
			void loadAuditLogs();
		}
		else {
			void loadLoginLogs();
		}
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [activeTab]);

	const auditColumns: TableColumnsType<PlatformAuditLogView> = [
		{
			title: "业务域",
			dataIndex: "businessDomainId",
			width: 120,
			render: (_, row) => row.businessDomainId ?? "-",
		},
		{
			title: "操作者",
			dataIndex: "operatorName",
			width: 150,
			render: (_, row) => row.operatorName ?? "-",
		},
		{
			title: "动作",
			dataIndex: "action",
			width: 180,
		},
		{
			title: "结果",
			dataIndex: "result",
			width: 110,
			render: (_, row) => <Tag color={row.result === "success" ? "green" : "gold"}>{row.result ?? "-"}</Tag>,
		},
		{
			title: "目标",
			dataIndex: "target",
			width: 220,
		},
		{
			title: "明细",
			dataIndex: "detail",
			ellipsis: true,
		},
		{
			title: "发生时间",
			dataIndex: "occurredAt",
			width: 180,
			render: (_, row) => row.occurredAt ? dayjs(row.occurredAt).format("YYYY-MM-DD HH:mm:ss") : "-",
		},
	];

	const loginColumns: TableColumnsType<LoginLogView> = [
		{
			title: "业务域",
			dataIndex: "businessDomainId",
			width: 120,
			render: (_, row) => row.businessDomainId ?? "-",
		},
		{
			title: "账号",
			dataIndex: "operatorName",
			width: 150,
			render: (_, row) => row.operatorName ?? row.loginName ?? "-",
		},
		{
			title: "门户",
			dataIndex: "portalType",
			width: 120,
		},
		{
			title: "结果",
			dataIndex: "result",
			width: 110,
			render: (_, row) => <Tag color={row.result === "success" ? "green" : "red"}>{row.result ?? "-"}</Tag>,
		},
		{
			title: "IP",
			dataIndex: "ip",
			width: 150,
		},
		{
			title: "失败原因",
			dataIndex: "failReason",
			ellipsis: true,
		},
		{
			title: "登录时间",
			dataIndex: "createdAt",
			width: 180,
			render: (_, row) => row.createdAt ? dayjs(row.createdAt).format("YYYY-MM-DD HH:mm:ss") : "-",
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Space direction="vertical" size={16} className="w-full">
				<Card
					title="审计日志"
					bordered={false}
					extra={<Typography.Text type="secondary">平台与登录日志统一查看</Typography.Text>}
				>
					<Tabs
						activeKey={activeTab}
						onChange={(key) => setActiveTab(key as AuditTabKey)}
						items={[
							{ key: "audit", label: "平台审计" },
							{ key: "login", label: "登录日志" },
						]}
					/>

					{activeTab === "audit" ? (
						<>
							<TableSearchForm
								form={auditForm}
								className="mb-4"
								initialValues={{ domainId: undefined }}
								onFinish={() => void loadAuditLogs()}
								onReset={() => {
									void loadAuditLogs();
								}}
							>
								<Form.Item name="domainId" label="业务域">
									<Select allowClear options={domainOptions} placeholder="全部业务域" />
								</Form.Item>
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

							<Table<PlatformAuditLogView>
								rowKey="id"
								loading={auditLoading}
								columns={auditColumns}
								dataSource={auditRows}
								pagination={{
									total: auditTotal,
									showSizeChanger: true,
									onChange: (page, pageSize) => {
										void loadAuditLogs(page, pageSize);
									},
								}}
								scroll={{ x: 1200 }}
							/>
						</>
					) : (
						<>
							<TableSearchForm
								form={loginForm}
								className="mb-4"
								onFinish={() => void loadLoginLogs()}
								onReset={() => {
									void loadLoginLogs();
								}}
							>
								<Form.Item name="subjectId" label="主体ID">
									<Input allowClear placeholder="登录主体 ID" />
								</Form.Item>
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

							<Table<LoginLogView>
								rowKey="id"
								loading={loginLoading}
								columns={loginColumns}
								dataSource={loginRows}
								pagination={{
									total: loginTotal,
									showSizeChanger: true,
									onChange: (page, pageSize) => {
										void loadLoginLogs(page, pageSize);
									},
								}}
								scroll={{ x: 1200 }}
							/>
						</>
					)}
				</Card>
			</Space>
		</BasicContent>
	);
}
