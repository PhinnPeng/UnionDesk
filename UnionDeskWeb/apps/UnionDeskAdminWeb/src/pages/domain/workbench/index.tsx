import { BasicContent } from "#src/components/basic-content";
import DomainConsultationsPage from "#src/pages/domain/consultations";
import DomainTicketQueuePage from "#src/pages/domain/ticket-queue";

import { Tabs, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router";

const { Title, Text } = Typography;

type WorkbenchTabKey = "ticket" | "consultation";

/** 解析 URL ?tab= 参数，非法值回退到「工单队列」 */
function parseWorkbenchTab(raw: string | null): WorkbenchTabKey {
	if (raw === "consultation") {
		return "consultation";
	}
	return "ticket";
}

/**
 * 「工作台」壳页：以 Tabs 聚合工单队列与在线咨询两个原子页面，
 * 内嵌页面保留各自的数据加载、权限守卫与布局；?tab= 参数同步激活 Tab，
 * 供「咨询转工单 → 前往工单」等跨页跳转直接定位。
 */
export default function DomainWorkbenchPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const [activeTab, setActiveTab] = useState<WorkbenchTabKey>(() => parseWorkbenchTab(searchParams.get("tab")));

	useEffect(() => {
		setActiveTab(parseWorkbenchTab(searchParams.get("tab")));
	}, [searchParams]);

	const handleTabChange = useCallback((nextTab: string) => {
		const tab = parseWorkbenchTab(nextTab);
		setActiveTab(tab);
		setSearchParams(prev => {
			const next = new URLSearchParams(prev);
			next.set("tab", tab);
			return next;
		}, { replace: true });
	}, [setSearchParams]);

	return (
		<BasicContent>
			<div className="flex flex-col gap-4">
				<div>
					<Title level={5} className="!mb-1">工作台</Title>
					<Text type="secondary">聚合「工单队列」与「在线咨询」，集中处理业务事项</Text>
				</div>
				<Tabs
					type="card"
					activeKey={activeTab}
					onChange={handleTabChange}
					items={[
						{
							key: "ticket",
							label: "工单队列",
							children: <DomainTicketQueuePage />,
						},
						{
							key: "consultation",
							label: "在线咨询",
							children: <DomainConsultationsPage />,
						},
					]}
				/>
			</div>
		</BasicContent>
	);
}
