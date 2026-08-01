import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import { useAuthStore } from "#src/store/auth";

import {
	AuditOutlined,
	CustomerServiceOutlined,
	FileTextOutlined,
	SettingOutlined,
	TeamOutlined,
	UserOutlined,
} from "@ant-design/icons";
import { Alert, Card, Col, Row, Space, Tag, Typography } from "antd";
import { useMemo, type ReactNode } from "react";
import { Link } from "react-router";

import "./index.less";

const { Title, Paragraph, Text } = Typography;

type QuickEntry = {
	title: string
	description: string
	path: string
	auth: string
	icon: ReactNode
};

const QUICK_ENTRIES: QuickEntry[] = [
	{
		title: "运营概览",
		description: "查看本域运营指标与趋势",
		path: "/domain/overview",
		auth: "domain.overview.read",
		icon: <FileTextOutlined />,
	},
	{
		title: "事项配置",
		description: "事项类型、属性与状态",
		path: "/domain/ticket-config",
		auth: "domain.ticket_type.read",
		icon: <SettingOutlined />,
	},
	{
		title: "客户管理",
		description: "客户列表与入域配置",
		path: "/domain/customers/list",
		auth: "domain.customer.read",
		icon: <UserOutlined />,
	},
	{
		title: "员工管理",
		description: "成员启停、角色绑定",
		path: "/domain/settings/members",
		auth: "domain.member.read",
		icon: <TeamOutlined />,
	},
	{
		title: "通用设置",
		description: "域名称、LOGO 与描述",
		path: "/domain/settings/basic",
		auth: "domain.general.read",
		icon: <SettingOutlined />,
	},
	{
		title: "入域配置",
		description: "注册与邀请策略",
		path: "/domain/customers/onboarding",
		auth: "domain.invitation_code.read",
		icon: <CustomerServiceOutlined />,
	},
	{
		title: "操作日志",
		description: "本域操作审计",
		path: "/domain/settings/audit-logs",
		auth: "domain.audit_log.read",
		icon: <AuditOutlined />,
	},
];

export default function Home() {
	const { hasPermission } = useAuth();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const currentDomain = useMemo(() => {
		if (!accessibleDomains?.length) {
			return null;
		}
		return accessibleDomains.find(domain => domain.id === defaultBusinessDomainId)
			?? accessibleDomains[0]
			?? null;
	}, [accessibleDomains, defaultBusinessDomainId]);

	const visibleEntries = QUICK_ENTRIES.filter(entry => hasPermission(entry.auth));

	return (
		<BasicContent className="business-home">
			<AuthGuarded
				auth="domain.home.read"
				fallback={<Alert type="warning" showIcon message="无权限访问概览" />}
			>
				<div className="business-home__hero">
					<Space size={8} wrap>
						<Tag color="green">业务域端</Tag>
						<Tag color="processing">概览</Tag>
					</Space>
					<Title level={2} className="business-home__title">
						概览
					</Title>
					{currentDomain
						? (
							<Paragraph className="business-home__desc">
								当前业务域：
								<Text strong>
									{currentDomain.name}
								</Text>
								{" "}
								（
								{currentDomain.code}
								）
							</Paragraph>
						)
						: (
							<Alert
								type="info"
								showIcon
								message="未解析到当前业务域，请确认账号已加入域访问名单"
							/>
						)}
				</div>

				<Card title="快捷入口" className="business-home__entries">
					{visibleEntries.length === 0
						? <Alert type="info" showIcon message="暂无可用入口，请联系管理员授权" />
						: (
							<Row gutter={[16, 16]}>
								{visibleEntries.map(entry => (
									<Col key={entry.path} xs={24} sm={12} lg={8}>
										<Link to={entry.path} className="business-home__entry-link">
											<Card hoverable size="small" className="business-home__entry-card">
												<Space align="start">
													<span className="business-home__entry-icon">{entry.icon}</span>
													<div>
														<div className="business-home__entry-title">{entry.title}</div>
														<div className="business-home__entry-desc">{entry.description}</div>
													</div>
												</Space>
											</Card>
										</Link>
									</Col>
								))}
							</Row>
						)}
				</Card>

				<Card title="说明" className="business-home__note">
					<Paragraph className="!mb-0">
						本页为业务域端默认概览。更细的运营指标请进入「运营概览」；人员、客户与配置能力可通过上方快捷入口进入。
					</Paragraph>
				</Card>
			</AuthGuarded>
		</BasicContent>
	);
}
