import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import { fetchLoginLogsPage } from "#src/api/platform/audit";
import type { LoginLogView } from "#src/api/platform/audit";

import { fetchAdminDomainsPage, toErrorMessage } from "@uniondesk/shared";

import { App, Button, Card, DatePicker, Form, Input, Select, Table, Tag, Tooltip, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { SearchOutlined, ReloadOutlined } from "@ant-design/icons";

const { RangePicker } = DatePicker;
const { Text } = Typography;

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

function renderLoginMethod(val?: string | null) {
	const key = (val ?? "").toUpperCase();
	if (key === "USERNAME") {
		return "用户名";
	}
	if (key === "MOBILE") {
		return "手机号";
	}
	if (key === "EMAIL") {
		return "邮箱";
	}
	return val?.trim() ? val : "-";
}

export default function PlatformLoginLogs() {
	const { message } = App.useApp();
	const [form] = Form.useForm();
	const [loading, setLoading] = useState(false);
	const [dataSource, setDataSource] = useState<LoginLogView[]>([]);
	const [total, setTotal] = useState(0);
	const [current, setCurrent] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [domainOptions, setDomainOptions] = useState<{ label: string; value: string }[]>([]);

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
				business_domain_id: values.businessDomainId,
				result: values.result,
				client_code: "ud-admin-web",
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
		void fetchAdminDomainsPage({ page: 1, page_size: 200 })
			.then((result) => {
				setDomainOptions(result.list
					.map((item) => {
						if (!item.id) {
							return null;
						}
						return { label: item.name, value: item.id };
					})
					.filter((item): item is { label: string; value: string } => item != null));
			})
			.catch((error) => {
				message.error(toErrorMessage(error) || "加载业务域列表失败");
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
			title: "账号",
			dataIndex: "loginName",
			width: 140,
			render: val => <span className="font-medium text-slate-800">{val ?? "-"}</span>,
		},
		{
			title: "昵称",
			dataIndex: "operatorName",
			width: 120,
			ellipsis: true,
			render: val => val ?? "-",
		},
		{
			title: "登录方式",
			dataIndex: "loginIdentifierType",
			width: 100,
			align: "center",
			render: val => renderLoginMethod(val),
		},
		{
			title: "状态",
			dataIndex: "result",
			width: 100,
			align: "center",
			render: (val, record) => (
				<Tooltip title={val === "failure" ? (record.failReason || "失败") : undefined}>
					<Tag color={val === "success" ? "success" : "error"}>
						{val === "success" ? "成功" : "失败"}
					</Tag>
				</Tooltip>
			),
		},
		{
			title: "UA",
			dataIndex: "userAgent",
			ellipsis: true,
			render: (val) => {
				if (!val) {
					return <Text type="secondary">-</Text>;
				}
				return (
					<Tooltip title={val}>
						<span className="text-slate-600">{val}</span>
					</Tooltip>
				);
			},
		},
		{
			title: "登录时间",
			dataIndex: "createdAt",
			width: 170,
			align: "center",
			render: val => (val ? dayjs(val).format("YYYY-MM-DD HH:mm:ss") : "-"),
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
						<Form.Item name="username" label="账号">
							<Input allowClear placeholder="请输入账号" />
						</Form.Item>
						<Form.Item name="nickname" label="昵称">
							<Input allowClear placeholder="请输入昵称" />
						</Form.Item>
						<Form.Item name="ip" label="客户端IP">
							<Input allowClear placeholder="请输入登录IP" />
						</Form.Item>
						<Form.Item name="businessDomainId" label="业务域">
							<Select allowClear placeholder="全部" options={domainOptions} showSearch optionFilterProp="label" />
						</Form.Item>
						<Form.Item name="result" label="状态">
							<Select allowClear placeholder="全部" options={RESULT_OPTIONS} />
						</Form.Item>
						<Form.Item name="timeRange" label="登录时间">
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
							showTotal: totalCount => `共 ${totalCount} 条记录`,
							onChange: (page, size) => {
								void loadData(page, size);
							},
						}}
						scroll={{ x: 960 }}
						size="middle"
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
