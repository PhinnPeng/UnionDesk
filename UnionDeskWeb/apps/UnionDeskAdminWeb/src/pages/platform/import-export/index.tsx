import type { TableProps, UploadProps } from "antd";
import { toErrorMessage } from "@uniondesk/shared";

import { exportStaffCsv, fetchImportTask, uploadStaffImport, type ImportTaskStatus, type ImportTaskView } from "#src/api/platform/import-export";
import { BasicContent } from "#src/components/basic-content";

import { DownloadOutlined, InboxOutlined, ProfileOutlined } from "@ant-design/icons";
import { Alert, App, Button, Card, Col, Row, Space, Spin, Table, Tag, Typography, Upload } from "antd";
import { useEffect, useState } from "react";

const POLL_INTERVAL_MS = 2000;
const MAX_POLL_ATTEMPTS = 150;
const MAX_FILE_SIZE = 10 * 1024 * 1024;

const TASK_STATUS_META: Record<ImportTaskStatus, { color: string; text: string }> = {
	pending: { color: "default", text: "排队中" },
	processing: { color: "processing", text: "处理中" },
	success: { color: "success", text: "成功" },
	failed: { color: "error", text: "失败" },
};

function renderTaskErrors(record: ImportTaskView) {
	const errors = record.error_summary ?? [];
	if (errors.length === 0) {
		return <Typography.Text type="secondary">无失败记录</Typography.Text>;
	}
	return (
		<ul className="!mb-0 max-h-48 list-disc overflow-auto pl-6">
			{errors.map(error => (
				<li key={`${error.row}-${error.message}`}>
					<Typography.Text>第 {error.row} 行：{error.message}</Typography.Text>
				</li>
			))}
			{record.fail_count > errors.length && (
				<li><Typography.Text type="secondary">（仅展示前 {errors.length} 条失败明细）</Typography.Text></li>
			)}
		</ul>
	);
}

const columns: TableProps<ImportTaskView>["columns"] = [
	{
		title: "文件名",
		dataIndex: "file_name",
		ellipsis: true,
		width: 220,
	},
	{
		title: "状态",
		dataIndex: "status",
		width: 100,
		render: (_, record) => (
			<Tag color={TASK_STATUS_META[record.status].color}>{TASK_STATUS_META[record.status].text}</Tag>
		),
	},
	{
		title: "总行数",
		dataIndex: "total_count",
		width: 90,
		align: "center",
	},
	{
		title: "成功",
		dataIndex: "success_count",
		width: 90,
		align: "center",
		render: value => <Typography.Text type="success">{value}</Typography.Text>,
	},
	{
		title: "失败",
		dataIndex: "fail_count",
		width: 90,
		align: "center",
		render: value => <Typography.Text type={value > 0 ? "danger" : "secondary"}>{value}</Typography.Text>,
	},
	{
		title: "创建时间",
		dataIndex: "created_at",
		width: 180,
		render: value => value ?? "-",
	},
	{
		title: "完成时间",
		dataIndex: "finished_at",
		width: 180,
		render: value => value ?? "-",
	},
];

export default function PlatformImportExport() {
	const { message } = App.useApp();
	const [tasks, setTasks] = useState<ImportTaskView[]>([]);
	const [pollingTaskId, setPollingTaskId] = useState<number | null>(null);
	const [resultTaskId, setResultTaskId] = useState<number | null>(null);

	useEffect(() => {
		if (pollingTaskId == null) {
			return;
		}
		let cancelled = false;
		let attempts = 0;
		let timer: number;
		const poll = async () => {
			if (cancelled) {
				return;
			}
			try {
				const task = await fetchImportTask(pollingTaskId);
				setTasks(prev => prev.map(item => (item.id === task.id ? task : item)));
				if (task.status === "success" || task.status === "failed") {
					return;
				}
			}
			catch {
				// 轮询失败静默处理，继续下一轮
			}
			attempts += 1;
			if (attempts >= MAX_POLL_ATTEMPTS) {
				message.warning("导入任务仍在执行，请稍后重新进入页面查看结果");
				return;
			}
			timer = window.setTimeout(poll, POLL_INTERVAL_MS);
		};
		void poll();
		return () => {
			cancelled = true;
			window.clearTimeout(timer);
		};
	}, [message, pollingTaskId]);

	const resultTask = resultTaskId == null ? null : tasks.find(task => task.id === resultTaskId) ?? null;

	const handleBeforeUpload: UploadProps["beforeUpload"] = async (file) => {
		const fileName = file.name.toLowerCase();
		if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
			message.error("仅支持 .xlsx / .xls 格式的 Excel 文件");
			return Upload.LIST_IGNORE;
		}
		if (file.size > MAX_FILE_SIZE) {
			message.error("文件大小不能超过 10MB");
			return Upload.LIST_IGNORE;
		}
		try {
			const task = await uploadStaffImport(file);
			setTasks(prev => [task, ...prev]);
			setResultTaskId(task.id);
			setPollingTaskId(task.id);
			message.success("导入任务已创建，正在处理");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		return Upload.LIST_IGNORE;
	};

	const handleExport = async () => {
		try {
			await exportStaffCsv();
			message.success("导出成功");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4">
				<div className="flex flex-col gap-2">
					<Typography.Title level={3} className="!mb-0">
						导入导出中心
					</Typography.Title>
					<Typography.Paragraph type="secondary" className="!mb-0">
						平台用户批量导入与导出，导入任务异步执行，完成后展示导入结果与失败明细。
					</Typography.Paragraph>
				</div>

				<Row gutter={[16, 16]}>
					<Col xs={24} md={12}>
						<Card title={(
							<Space>
								<InboxOutlined />
								<span>用户导入</span>
							</Space>
						)}>
							<Upload.Dragger
								accept=".xlsx,.xls"
								maxCount={1}
								showUploadList={false}
								beforeUpload={handleBeforeUpload}
							>
								<p className="ant-upload-drag-icon"><InboxOutlined /></p>
								<p className="ant-upload-text">点击或拖拽 Excel 文件到此区域上传</p>
								<p className="ant-upload-hint">
									表头需包含：登录账号、手机号（可选：姓名、邮箱、密码，密码为空默认 123456），单次最多 5000 行
								</p>
							</Upload.Dragger>
						</Card>
					</Col>
					<Col xs={24} md={12}>
						<Card title={(
							<Space>
								<DownloadOutlined />
								<span>用户导出</span>
							</Space>
						)}>
							<Space direction="vertical" size="middle">
								<Button type="primary" icon={<DownloadOutlined />} onClick={handleExport}>
									导出员工列表
								</Button>
								<Typography.Paragraph type="secondary" className="!mb-0">
									导出当前平台全部员工（含在职、停用、离职）为 CSV 文件，可直接用 Excel 打开。
								</Typography.Paragraph>
							</Space>
						</Card>
					</Col>
				</Row>

				{resultTask != null && (
					<Card title="本次导入结果">
						{resultTask.status === "pending" || resultTask.status === "processing" ? (
							<Alert
								type="info"
								showIcon
								message="正在导入，请稍候…"
								description={(
									<Space>
										<Spin size="small" />
										<Typography.Text>共 {resultTask.total_count} 行，成功 {resultTask.success_count} 行，失败 {resultTask.fail_count} 行</Typography.Text>
									</Space>
								)}
							/>
						) : resultTask.status === "success" ? (
							<Alert
								type="success"
								showIcon
								message="导入完成"
								description={(
									<Typography.Text>
										共 {resultTask.total_count} 行，成功 {resultTask.success_count} 行，
										失败 {resultTask.fail_count} 行
										{resultTask.fail_count > 0 ? "，失败明细见下方任务列表展开行" : ""}
									</Typography.Text>
								)}
							/>
						) : (
							<Alert
								type="error"
								showIcon
								message="导入失败"
								description={(
									<Typography.Text>
										{resultTask.error_summary != null && resultTask.error_summary.length === 1 && resultTask.error_summary[0].row === 0
											? resultTask.error_summary[0].message
											: `共 ${resultTask.total_count} 行，成功 ${resultTask.success_count} 行，失败 ${resultTask.fail_count} 行，失败明细见下方任务列表展开行`}
									</Typography.Text>
								)}
							/>
						)}
					</Card>
				)}

				<Card
					className="min-h-0 flex-1"
					title={(
						<Space>
							<ProfileOutlined />
							<span>最近导入任务</span>
						</Space>
					)}
				>
					<Table<ImportTaskView>
						rowKey="id"
						columns={columns}
						dataSource={tasks}
						pagination={false}
						scroll={{ x: 950 }}
						locale={{ emptyText: "暂无导入任务" }}
						expandable={{ expandedRowRender: renderTaskErrors }}
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
