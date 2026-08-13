import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import { fetchPlatformAuditLogs } from "#src/api/platform/audit";

import { App, Button, Card, DatePicker, Form, Input, Select, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { SearchOutlined, ReloadOutlined } from "@ant-design/icons";

import type { PlatformAuditLogView } from "#src/api/platform/audit";

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

const moduleOptions = [
	{ label: "身份与访问控制 (IAM)", value: "iam" },
	{ label: "业务域管理 (Domain)", value: "domain" },
	{ label: "工单管理 (Ticket)", value: "ticket" },
	{ label: "附件管理 (Attachment)", value: "attachment" },
	{ label: "站内信 (Inbox)", value: "inbox" },
	{ label: "咨询服务 (Consultation)", value: "consultation" },
	{ label: "系统配置 (System)", value: "system" },
];

export default function PlatformOperationLogs() {
	const { message } = App.useApp();
	const [form] = Form.useForm();
	const [loading, setLoading] = useState(false);
	const [dataSource, setDataSource] = useState<PlatformAuditLogView[]>([]);
	const [total, setTotal] = useState(0);
	const [current, setCurrent] = useState(1);
	const [pageSize, setPageSize] = useState(20);

	const loadData = async (page = 1, size = pageSize) => {
		setLoading(true);
		try {
			const values = form.getFieldsValue();
			const dates = buildDateRange(values.timeRange);
			const response = await fetchPlatformAuditLogs({
				page,
				page_size: size,
				keyword: values.keyword,
				username: values.username,
				nickname: values.nickname,
				ip: values.ip,
				module: values.module,
				...dates,
			});
			setDataSource(response.list);
			setTotal(response.total);
			setCurrent(page);
			setPageSize(size);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载操作日志失败");
		}
		finally {
			setLoading(false);
		}
	};

	useEffect(() => {
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

	const columns: TableColumnsType<PlatformAuditLogView> = [
		{
			title: "序号",
			key: "index",
			width: 70,
			align: "center",
			render: (_, __, index) => (current - 1) * pageSize + index + 1,
		},
		{
			title: "操作账号",
			dataIndex: "operatorName",
			width: 140,
			render: (val) => <span className="font-medium text-slate-800">{val ?? "-"}</span>,
		},
		{
			title: "操作者ID",
			dataIndex: "operatorSubjectId",
			width: 100,
			align: "center",
			render: (val) => val ?? "-",
		},
		{
			title: "主体类型",
			dataIndex: "operatorActorType",
			width: 100,
			align: "center",
			render: (val) => {
				if (val === "user") {
					return <Tag color="blue">用户</Tag>;
				}
				if (val === "system") {
					return <Tag color="purple">系统</Tag>;
				}
				return val ?? "-";
			},
		},
		{
			title: "模块/类型",
			dataIndex: "action",
			width: 150,
			render: (val) => {
				if (!val) {
					return "-";
				}
				const parts = val.split(".");
				const mod = parts[0];
				const found = moduleOptions.find(o => o.value === mod);
				return (
					<Space direction="vertical" size={0}>
						<span className="text-xs text-slate-400 font-mono">{val}</span>
						<span className="text-sm text-slate-700">{found ? found.label.split(" ")[0] : mod}</span>
					</Space>
				);
			},
		},
		{
			title: "操作目标",
			dataIndex: "target",
			width: 150,
			render: (val) => <span className="font-mono text-xs text-slate-600">{val ?? "-"}</span>,
		},
		{
			title: "客户端IP",
			dataIndex: "ip",
			width: 140,
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
				<Tag color={val === "success" ? "success" : "warning"}>
					{val === "success" ? "成功" : "警告"}
				</Tag>
			),
		},
		{
			title: "操作明细",
			dataIndex: "detail",
			ellipsis: true,
			render: (val) => val ?? <span className="text-slate-400">-</span>,
		},
		{
			title: "操作时间",
			dataIndex: "occurredAt",
			width: 180,
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
						<Form.Item name="username" label="操作账号">
							<Input allowClear placeholder="请输入账号" />
						</Form.Item>
						<Form.Item name="nickname" label="操作人昵称">
							<Input allowClear placeholder="请输入昵称" />
						</Form.Item>
						<Form.Item name="ip" label="操作IP">
							<Input allowClear placeholder="请输入IP地址" />
						</Form.Item>
						<Form.Item name="module" label="操作模块">
							<Select
								allowClear
								placeholder="选择操作模块"
								options={moduleOptions}
							/>
						</Form.Item>
						<Form.Item name="timeRange" label="操作时间范围">
							<RangePicker showTime className="w-full" placeholder={["开始时间", "结束时间"]} />
						</Form.Item>
					</TableSearchForm>
				</Card>

				<Card bordered={false} title="操作日志列表" className="flex-1 shadow-sm overflow-auto">
					<Table<PlatformAuditLogView>
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
						scroll={{ x: 1300 }}
						size="middle"
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
