import type { TableProps } from "antd";

import { BasicContent } from "#src/components/basic-content";

import { DownloadOutlined, ImportOutlined, InboxOutlined, SyncOutlined, UploadOutlined } from "@ant-design/icons";
import { Button, Card, Col, Progress, Row, Space, Table, Tag, Typography } from "antd";

interface ImportExportTaskRow {
	id: number;
	taskName: string;
	taskType: "import" | "export";
	objectName: string;
	status: "pending" | "running" | "success";
	progress: number;
	operator: string;
	updatedAt: string;
}

const taskRows: ImportExportTaskRow[] = [
	{
		id: 1,
		taskName: "用户导入",
		taskType: "import",
		objectName: "平台用户",
		status: "pending",
		progress: 0,
		operator: "admin",
		updatedAt: "待执行",
	},
	{
		id: 2,
		taskName: "组织导出",
		taskType: "export",
		objectName: "平台组织",
		status: "running",
		progress: 65,
		operator: "admin",
		updatedAt: "处理中",
	},
	{
		id: 3,
		taskName: "角色导出",
		taskType: "export",
		objectName: "平台角色",
		status: "success",
		progress: 100,
		operator: "admin",
		updatedAt: "已完成",
	},
];

const columns: TableProps<ImportExportTaskRow>["columns"] = [
	{
		title: "任务名称",
		dataIndex: "taskName",
		width: 160,
	},
	{
		title: "类型",
		dataIndex: "taskType",
		width: 100,
		render: (_, record) => (
			<Tag color={record.taskType === "import" ? "processing" : "success"}>
				{record.taskType === "import" ? "导入" : "导出"}
			</Tag>
		),
	},
	{
		title: "对象",
		dataIndex: "objectName",
		width: 140,
	},
	{
		title: "状态",
		dataIndex: "status",
		width: 120,
		render: (_, record) => {
			const color = record.status === "success" ? "success" : record.status === "running" ? "processing" : "default";
			const text = record.status === "success" ? "成功" : record.status === "running" ? "执行中" : "待执行";
			return <Tag color={color}>{text}</Tag>;
		},
	},
	{
		title: "进度",
		dataIndex: "progress",
		width: 180,
		render: (_, record) => <Progress percent={record.progress} size="small" />,
	},
	{
		title: "操作人",
		dataIndex: "operator",
		width: 120,
	},
	{
		title: "更新时间",
		dataIndex: "updatedAt",
		width: 160,
	},
];

export default function PlatformImportExport() {
	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4">
				<div className="flex flex-col gap-2">
					<Typography.Title level={3} className="!mb-0">
						导入导出中心
					</Typography.Title>
					<Typography.Paragraph type="secondary" className="!mb-0">
						先承接平台用户、组织、角色、平台域和工单配置的批量流转入口，首期只落骨架页与任务视图。
					</Typography.Paragraph>
				</div>

				<Row gutter={[16, 16]}>
					<Col xs={24} md={8}>
						<Card>
							<Space align="start">
								<InboxOutlined className="text-lg" />
								<div>
									<Typography.Text strong>模板下载</Typography.Text>
									<Typography.Paragraph type="secondary" className="!mb-0 !mt-2">
										统一提供用户、组织、角色等导入模板下载入口。
									</Typography.Paragraph>
								</div>
							</Space>
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card>
							<Space align="start">
								<UploadOutlined className="text-lg" />
								<div>
									<Typography.Text strong>用户导入</Typography.Text>
									<Typography.Paragraph type="secondary" className="!mb-0 !mt-2">
										预留字段映射、预检、确认执行和错误明细下载流程。
									</Typography.Paragraph>
								</div>
							</Space>
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card>
							<Space align="start">
								<DownloadOutlined className="text-lg" />
								<div>
									<Typography.Text strong>异步导出</Typography.Text>
									<Typography.Paragraph type="secondary" className="!mb-0 !mt-2">
										统一走任务化导出，避免大数据量阻塞页面和同步超时。
									</Typography.Paragraph>
								</div>
							</Space>
						</Card>
					</Col>
				</Row>

				<Card
					className="min-h-0 flex-1"
					title="最近任务"
					extra={(
						<Space>
							<Button icon={<SyncOutlined />} disabled>刷新状态</Button>
							<Button type="primary" icon={<ImportOutlined />} disabled>创建任务</Button>
						</Space>
					)}
				>
					<Table<ImportExportTaskRow>
						rowKey="id"
						columns={columns}
						dataSource={taskRows}
						pagination={false}
						scroll={{ x: 960 }}
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
