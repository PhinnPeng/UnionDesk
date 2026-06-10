import type { LoginLogView } from "#src/api/platform/audit";

import { fetchPlatformOverview } from "#src/api/platform/overview";
import { BasicContent } from "#src/components/basic-content";

import { Alert, Card, Col, List, Row, Space, Statistic, Tag, Typography } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router";

function resolveAuditTitle(log: LoginLogView): string {
	return log.result === "success" ? "登录成功" : "登录失败";
}

function resolveAuditDescription(log: LoginLogView): string {
	const parts = [
		log.loginName,
		log.failReason || "无附加原因",
		log.ip,
		log.createdAt,
	];
	return parts.filter(Boolean).join(" · ");
}

export default function PlatformHome() {
	const [overview, setOverview] = useState({
		domainCount: 0,
		activeUserCount: 0,
		disabledUserCount: 0,
		offboardUserCount: 0,
		pendingImportTaskCount: 0,
		announcementCount: 0,
		recentAuditCount: 0,
		loginLogs: [] as LoginLogView[],
	});
	const [loading, setLoading] = useState(false);
	const [loadError, setLoadError] = useState<string | null>(null);

	useEffect(() => {
		let ignore = false;

		setLoading(true);
		fetchPlatformOverview()
			.then((nextOverview) => {
				if (ignore) {
					return;
				}
				setOverview(nextOverview);
				setLoadError(null);
			})
			.catch(() => {
				if (ignore) {
					return;
				}
				setLoadError("平台首页统计加载失败，请稍后重试。");
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

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card
				className="h-full"
				styles={{ body: { height: "100%" } }}
			>
				<div className="flex h-full flex-col gap-6">
					<div className="flex flex-col gap-3">
						<Space size={8} wrap>
							<Tag color="blue">平台管理</Tag>
							<Tag color="processing">管理面板</Tag>
						</Space>
						<Typography.Title level={2} className="!mb-0">
							平台管理面板
						</Typography.Title>
						<Typography.Paragraph type="secondary" className="!mb-0 max-w-3xl">
							这里承接平台级治理摘要，只展示平台域、用户与审计等总览信息，不混入业务看板指标。
						</Typography.Paragraph>
					</div>

					<Row gutter={[16, 16]}>
						<Col xs={24} md={12} xl={6}>
							<Card loading={loading}>
								<Statistic title="平台域数量" value={overview.domainCount} />
							</Card>
						</Col>
						<Col xs={24} md={12} xl={6}>
							<Card loading={loading}>
								<Statistic title="在职用户" value={overview.activeUserCount} />
								<Typography.Text type="secondary">停用账号 {overview.disabledUserCount} 人</Typography.Text>
							</Card>
						</Col>
						<Col xs={24} md={12} xl={6}>
							<Card loading={loading}>
								<Statistic title="离职人员" value={overview.offboardUserCount} />
							</Card>
						</Col>
						<Col xs={24} md={12} xl={6}>
							<Card loading={loading}>
								<Statistic title="最近审计" value={overview.recentAuditCount} suffix="条" />
							</Card>
						</Col>
					</Row>
					{loadError
						? <Alert type="error" showIcon message={loadError} />
						: null}

					<Card title="P0 管理端入口" size="small">
						<Row gutter={[12, 12]}>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/domains">业务域管理</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									对齐 `GET/POST/PUT/DELETE /admin/domains` 与可见策略、注册策略
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/user">平台用户</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									IAM 用户列表与治理占位
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/dept">组织树</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									平台组织 / 部门只读树与后续导入
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/role">平台角色</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									角色与权限绑定（敏感授权需 step-up）
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/domain-onboarding">客户入域</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									邀请码与域客户列表（P0 契约路径）
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/ticket-pool">工单池</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									管理端工单列表与领取
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/inbox">站内信</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									`/inbox` 通知中心基础能力
								</Typography.Paragraph>
							</Col>
							<Col xs={24} sm={12} md={8}>
								<Link className="text-[var(--ant-color-primary)]" to="/platform/attachments">附件</Link>
								<Typography.Paragraph type="secondary" className="!mb-0 !mt-1 !text-xs">
									预签名与本地上传契约对齐
								</Typography.Paragraph>
							</Col>
						</Row>
					</Card>

					<Row gutter={[16, 16]} className="min-h-0 flex-1">
						<Col xs={24} xl={16} className="min-h-0">
							<Card
								className="h-full"
								title="最近审计"
								loading={loading}
							>
								<List
									dataSource={overview.loginLogs}
									locale={{ emptyText: "暂无审计记录" }}
									renderItem={log => (
										<List.Item>
											<List.Item.Meta
												title={(
													<Space size={8}>
														<span>{log.operatorName || log.loginName || "未知账号"}</span>
														<Tag color={log.result === "success" ? "success" : "error"}>
															{resolveAuditTitle(log)}
														</Tag>
													</Space>
												)}
												description={resolveAuditDescription(log)}
											/>
										</List.Item>
									)}
								/>
							</Card>
						</Col>
						<Col xs={24} xl={8} className="min-h-0">
							<Card
								className="h-full"
								title="预留模块"
								loading={loading}
							>
								<div className="flex flex-col gap-3">
									<div className="flex items-center justify-between">
										<Typography.Text>待处理导入任务</Typography.Text>
										<Tag>{overview.pendingImportTaskCount}</Tag>
									</div>
									<div className="flex items-center justify-between">
										<Typography.Text>公告数量</Typography.Text>
										<Tag>{overview.announcementCount}</Tag>
									</div>
									<Typography.Paragraph type="secondary" className="!mb-0">
										导入导出中心与公告中心尚未进入本轮开发，当前先保留首页统计位。
									</Typography.Paragraph>
								</div>
							</Card>
						</Col>
					</Row>
				</div>
			</Card>
		</BasicContent>
	);
}
