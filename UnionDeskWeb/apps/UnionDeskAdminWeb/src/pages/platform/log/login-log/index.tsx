import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import { fetchLoginLogsPage } from "#src/api/platform/audit";
import { fetchBusinessDomains } from "#src/api/platform/domain";

import { App, Button, Card, DatePicker, Form, Input, Select, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { SearchOutlined, ReloadOutlined } from "@ant-design/icons";

import type { LoginLogView } from "#src/api/platform/audit";

const { RangePicker } = DatePicker;

const CLIENT_OPTIONS = [
	{ label: "管理端", value: "ud-admin-web" },
	{ label: "客户门户", value: "ud-customer-web" },
];

const PORTAL_OPTIONS = [
	{ label: "员工端", value: "staff" },
	{ label: "客户门户", value: "customer" },
];

const RESULT_OPTIONS = [
	{ label: "成功", value: "success" },
	{ label: "失败", value: "failure" },
];

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

function renderPortalTag(val?: string | null) {
	if (val === "staff") {
		return <Tag color="blue">员工端</Tag>;
	}
	if (val === "customer") {
		return <Tag color="cyan">客户门户</Tag>;
	}
	return <Tag color="default">{val ?? "-"}</Tag>;
}

function renderClientTag(val?: string | null) {
	if (val === "ud-admin-web") {
		return <Tag>管理端</Tag>;
	}
	if (val === "ud-customer-web") {
		return <Tag color="cyan">客户 Web</Tag>;
	}
	return <Tag color="default">{val ?? "-"}</Tag>;
}

export default function PlatformLoginLogs() {
	const { message } = App.useApp();
	const [form] = Form.useForm();
	const [loading, setLoading] = useState(false);
	const [dataSource, setDataSource] = useState<LoginLogView[]>([]);
	const [total, setTotal] = useState(0);
	const [current, setCurrent] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [domainOptions, setDomainOptions] = useState<{ label: string; value: number }[]>([]);

	const loadData = async (page = 1, size = pageSize) => {
		setLoading(true);
		try {
			const values = form.getFieldsValue();
			const dates = buildDateRange(values.timeRange);
			const response = await fetchLoginLogsPage({
				page,
				page_size: size,
				event_type: "LOGIN",
				keyword: values.keyword,
				username: values.username,
				nickname: values.nickname,
				ip: values.ip,
				portal_type: values.portalType,
				client_code: values.clientCode,
				business_domain_id: values.businessDomainId,
				result: values.result,
				...dates,
			});
			setDataSource(response.list);
			setTotal(response.total);
			setCurrent(page);
			setPageSize(size);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载登录日志失败");
		}
		finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		void fetchBusinessDomains()
			.then(domains => {
				setDomainOptions(domains.map(d => ({ label: d.name, value: d.id })));
			})
			.catch(() => {
				message.error("加载业务域列表失败");
			});
		void loadData(1, 20);
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	const handleSearch = () => {
		void loadData(1, pageSize);
	};

	const handleReset = () => {
		form.resetFields();
		void loadData(1, pageSize);
	};

	const columns: TableColumnsType<LoginLogView> = [
		{
			title: "序号",
			key: "index",
			width: 70,
			align: "center",
			render: (_, __, index) => (current - 1) * pageSize + index + 1,
		},
		{
			title: "登录主体ID",
			dataIndex: "subjectId",
			width: 110,
			align: "center",
			render: (val) => val ?? "-",
		},
		{
			title: "登录账号",
			dataIndex: "loginName",
			width: 140,
			render: (val) => <span className="font-medium text-slate-800">{val ?? "-"}</span>,
		},
		{
			title: "用户昵称",
			dataIndex: "operatorName",
			width: 120,
			render: (val) => val ?? "-",
		},
		{
			title: "业务域",
			dataIndex: "domainName",
			width: 120,
			ellipsis: true,
			render: (val, record) => val ?? (record.businessDomainId != null ? String(record.businessDomainId) : "-"),
		},
		{
			title: "登录门户",
			dataIndex: "portalType",
			width: 100,
			align: "center",
			render: (val) => renderPortalTag(val),
		},
		{
			title: "客户端",
			dataIndex: "clientCode",
			width: 110,
			align: "center",
			render: (val) => renderClientTag(val),
		},
		{
			title: "客户端IP",
			dataIndex: "ip",
			width: 130,
			render: (val) => (
				<span className="font-mono text-xs bg-slate-100 px-1.5 py-0.5 rounded text-slate-600">
					{val ?? "-"}
				</span>
			),
		},
		{
			title: "状态",
			dataIndex: "result",
			width: 90,
			align: "center",
			render: (val) => (
				<Tag color={val === "success" ? "success" : "error"}>
					{val === "success" ? "成功" : "失败"}
				</Tag>
			),
		},
		{
			title: "失败原因",
			dataIndex: "failReason",
			ellipsis: true,
			render: (val) => val ?? <span className="text-slate-400">-</span>,
		},
		{
			title: "客户端UA",
			dataIndex: "userAgent",
			ellipsis: true,
			width: 180,
			render: (val) => val ?? <span className="text-slate-400">-</span>,
		},
		{
			title: "登录时间",
			dataIndex: "createdAt",
			width: 170,
			align: "center",
			render: (val) => (val ? dayjs(val).format("YYYY-MM-DD HH:mm:ss") : "-"),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4">
				<Card bordered={false} className="shadow-sm">
					<TableSearchForm
						form={form}
						loading={loading}
						onFinish={handleSearch}
						onReset={handleReset}
						optionRender={(_searchConfig, _props, dom) => [
							...dom,
							<Button
								key="refresh"
								icon={<ReloadOutlined />}
								onClick={() => void loadData(current, pageSize)}
							>
								刷新
							</Button>,
						]}
					>
						<Form.Item name="keyword" label="关键字">
							<Input
								allowClear
								placeholder="用户名、手机号、邮箱"
								prefix={<SearchOutlined className="text-slate-400" />}
							/>
						</Form.Item>
						<Form.Item name="username" label="登录账号">
							<Input allowClear placeholder="请输入账号" />
						</Form.Item>
						<Form.Item name="nickname" label="用户昵称">
							<Input allowClear placeholder="请输入用户昵称" />
						</Form.Item>
						<Form.Item name="ip" label="客户端IP">
							<Input allowClear placeholder="请输入登录IP" />
						</Form.Item>
						<Form.Item name="portalType" label="登录门户">
							<Select allowClear placeholder="全部" options={PORTAL_OPTIONS} />
						</Form.Item>
						<Form.Item name="clientCode" label="客户端">
							<Select allowClear placeholder="全部" options={CLIENT_OPTIONS} />
						</Form.Item>
						<Form.Item name="businessDomainId" label="业务域">
							<Select allowClear placeholder="全部" options={domainOptions} />
						</Form.Item>
						<Form.Item name="result" label="登录结果">
							<Select allowClear placeholder="全部" options={RESULT_OPTIONS} />
						</Form.Item>
						<Form.Item name="timeRange" label="登录时间范围">
							<RangePicker showTime className="w-full" placeholder={["开始时间", "结束时间"]} />
						</Form.Item>
					</TableSearchForm>
				</Card>

				<Card bordered={false} title="登录日志列表" className="flex-1 shadow-sm overflow-auto">
					<Table<LoginLogView>
						rowKey="id"
						loading={loading}
						columns={columns}
						dataSource={dataSource}
						pagination={{
							current,
							pageSize,
							total,
							showSizeChanger: true,
							showQuickJumper: true,
							showTotal: (totalCount) => `共 ${totalCount} 条记录`,
							onChange: (page, size) => {
								void loadData(page, size);
							},
						}}
						scroll={{ x: 1500 }}
						size="middle"
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
