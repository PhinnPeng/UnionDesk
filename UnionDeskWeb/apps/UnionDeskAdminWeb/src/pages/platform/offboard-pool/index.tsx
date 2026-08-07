import type { TableProps } from "antd";
import type { IamUser } from "@uniondesk/shared";

import { fetchPlatformOffboardPoolUsers } from "#src/api/platform/iam";
import { BasicContent } from "#src/components/basic-content";

import { DeleteOutlined, DownloadOutlined, RollbackOutlined } from "@ant-design/icons";
import { Alert, Button, Card, Col, Row, Space, Table, Tag, Tooltip, Typography } from "antd";
import { useEffect, useState } from "react";

interface OffboardRow {
	id: number;
	username: string;
	mobile: string;
	scopeNames: string[];
	roles: string[];
	offboardAt: string;
	reason: string;
	operator: string;
}

function toOffboardRow(user: IamUser): OffboardRow {
	return {
		id: user.id,
		username: user.username,
		mobile: user.mobile || "-",
		scopeNames: user.businessDomainIds.length > 0
			? user.businessDomainIds.map(domainId => `域 #${domainId}`)
			: ["全局"],
		roles: user.roleCodes,
		offboardAt: user.offboardedAt || "-",
		reason: user.offboardReason || "-",
		operator: user.offboardedBy ? `用户 #${user.offboardedBy}` : "-",
	};
}

function isCurrentMonth(value: string): boolean {
	if (!value || value === "-") {
		return false;
	}
	const date = new Date(value.replace(" ", "T"));
	if (Number.isNaN(date.getTime())) {
		return false;
	}
	const now = new Date();
	return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
}

const columns: TableProps<OffboardRow>["columns"] = [
	{
		title: "账号",
		dataIndex: "username",
		width: 160,
	},
	{
		title: "手机号",
		dataIndex: "mobile",
		width: 140,
	},
	{
		title: "绑定范围",
		dataIndex: "scopeNames",
		width: 180,
		render: (_, record) => (
			<Space size={4} wrap>
				{record.scopeNames.map(scopeName => <Tag key={scopeName}>{scopeName}</Tag>)}
			</Space>
		),
	},
	{
		title: "原角色",
		dataIndex: "roles",
		render: (_, record) => (
			<Space size={4} wrap>
				{record.roles.map(role => <Tag key={role}>{role}</Tag>)}
			</Space>
		),
	},
	{
		title: "离职时间",
		dataIndex: "offboardAt",
		width: 180,
	},
	{
		title: "离职原因",
		dataIndex: "reason",
		width: 140,
	},
	{
		title: "操作人",
		dataIndex: "operator",
		width: 140,
	},
	{
		title: "操作",
		key: "action",
		width: 100,
		render: () => (
			<Space size={8}>
				<Tooltip title="恢复">
					<Button size="small" type="link" disabled icon={<RollbackOutlined />} />
				</Tooltip>
				<Tooltip title="删除">
					<Button size="small" type="link" danger disabled icon={<DeleteOutlined />} />
				</Tooltip>
			</Space>
		),
	},
];

export default function PlatformOffboardPool() {
	const [offboardRows, setOffboardRows] = useState<OffboardRow[]>([]);
	const [loading, setLoading] = useState(false);
	const [loadError, setLoadError] = useState<string | null>(null);

	useEffect(() => {
		let ignore = false;

		setLoading(true);
		fetchPlatformOffboardPoolUsers()
			.then((users) => {
				if (ignore) {
					return;
				}
				setOffboardRows(users.map(toOffboardRow));
				setLoadError(null);
			})
			.catch(() => {
				if (ignore) {
					return;
				}
				setLoadError("离职池加载失败，请稍后重试。");
			})
			.finally(() => {
				if (!ignore) {
					setLoading(false);
				}
			});

		return () => {
			ignore = true;
		};
	}, []);

	const offboardCount = offboardRows.length;
	const currentMonthCount = offboardRows.filter(row => isCurrentMonth(row.offboardAt)).length;

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4">
				<Row gutter={[16, 16]}>
					<Col xs={24} md={8}>
						<Card>
							<Typography.Text type="secondary">离职人员</Typography.Text>
							<Typography.Title level={3} className="!mb-0 !mt-2">{offboardCount}</Typography.Title>
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card>
							<Typography.Text type="secondary">可恢复账号</Typography.Text>
							<Typography.Title level={3} className="!mb-0 !mt-2">{offboardCount}</Typography.Title>
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card>
							<Typography.Text type="secondary">本月离职</Typography.Text>
							<Typography.Title level={3} className="!mb-0 !mt-2">{currentMonthCount}</Typography.Title>
						</Card>
					</Col>
				</Row>
				{loadError
					? <Alert type="error" showIcon message={loadError} />
					: null}

				<Card
					className="min-h-0 flex-1"
					title="离职池"
					extra={(
						<Space>
							<Button icon={<DownloadOutlined />} disabled>导出</Button>
							<Button icon={<RollbackOutlined />} disabled>批量恢复</Button>
						</Space>
					)}
				>
					<Table<OffboardRow>
						rowKey="id"
						columns={columns}
						dataSource={offboardRows}
						loading={loading}
						pagination={false}
						scroll={{ x: 1080 }}
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
