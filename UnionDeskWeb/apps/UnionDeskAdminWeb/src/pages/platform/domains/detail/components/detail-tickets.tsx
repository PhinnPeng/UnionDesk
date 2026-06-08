import { Button, Empty, Typography } from "antd";
import { useNavigate } from "react-router";

const { Title, Paragraph } = Typography;

export interface DetailTicketsProps {
	domainId: string;
}

export function DetailTickets({ domainId }: DetailTicketsProps) {
	const navigate = useNavigate();

	return (
		<div>
			<Title level={5} className="!mb-4">
				工单配置
			</Title>
			<Empty description="工单拓扑与状态机配置开发中">
				<Paragraph type="secondary">
					当前业务域（ID: {domainId}）的工单流程画布与状态机尚未接入，请前往工单池查看工单数据。
				</Paragraph>
				<Button type="primary" onClick={() => navigate("/platform/ticket-pool")}>
					前往工单池
				</Button>
			</Empty>
		</div>
	);
}
