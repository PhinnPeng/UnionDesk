import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuthStore } from "#src/store/auth";

import { Alert, Button, Card, Col, Empty, Row, Statistic, Typography } from "antd";
import { useMemo } from "react";
import { Link } from "react-router";

const { Paragraph, Text } = Typography;

export default function DomainOverviewPage() {
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

	return (
		<BasicContent>
			<AuthGuarded
				auth="domain.overview.read"
				fallback={<Alert type="warning" showIcon message="无权限查看运营概览" />}
			>
				<div className="flex flex-col gap-4">
					<Card
						title="运营概览"
						extra={(
							<Link to="/home">
								<Button type="link">返回概览</Button>
							</Link>
						)}
					>
						{currentDomain
							? (
								<Paragraph className="!mb-4">
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
									className="!mb-4"
									type="info"
									showIcon
									message="未解析到当前业务域"
								/>
							)}

						<Row gutter={[16, 16]}>
							<Col xs={24} sm={12} lg={6}>
								<Card size="small"><Statistic title="待处理工单" value="—" /></Card>
							</Col>
							<Col xs={24} sm={12} lg={6}>
								<Card size="small"><Statistic title="在岗成员" value="—" /></Card>
							</Col>
							<Col xs={24} sm={12} lg={6}>
								<Card size="small"><Statistic title="域内客户" value="—" /></Card>
							</Col>
							<Col xs={24} sm={12} lg={6}>
								<Card size="small"><Statistic title="屏蔽词" value="—" /></Card>
							</Col>
						</Row>
					</Card>

					<Card title="趋势分析">
						<Empty description="趋势数据接入中；本页为运营概览，后续对接本域 KPI API" />
					</Card>
				</div>
			</AuthGuarded>
		</BasicContent>
	);
}
