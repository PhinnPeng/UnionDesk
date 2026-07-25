import type { DomainTicketType, TicketStatusFlow } from "@uniondesk/shared";
import { fetchDomainTicketTypes, toErrorMessage, updateDomainTicketType } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { ArrowLeftOutlined } from "@ant-design/icons";
import { App, Button, Empty, Spin, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router";

import { DEFAULT_STATUS_FLOW } from "#src/pages/platform/domains/detail/components/ticket-type-form-defaults";
import { TicketTypeFlowDesigner } from "#src/pages/platform/domains/detail/components/ticket-type-flow-designer";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

const { Title } = Typography;

export default function PlatformTicketTypeFlowConfig() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { domainId: domainIdParam, typeId: typeIdParam } = useParams();
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const domainId = domainIdParam?.trim() ?? "";
	const typeId = typeIdParam?.trim() ?? "";

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [ticketType, setTicketType] = useState<DomainTicketType | null>(null);
	const [statusFlow, setStatusFlow] = useState<TicketStatusFlow>(DEFAULT_STATUS_FLOW);

	const pagePath = useMemo(() => {
		if (!domainId || !typeId) {
			return "";
		}
		return `/platform/domains/ticket-type-config/${encodeURIComponent(domainId)}/${encodeURIComponent(typeId)}/flow`;
	}, [domainId, typeId]);

	const backToTickets = useCallback(() => {
		if (!domainId) {
			navigate("/platform/domains");
			return;
		}
		navigate(`/platform/domains/detail/${encodeURIComponent(domainId)}?tab=tickets`);
	}, [domainId, navigate]);

	const loadTicketType = useCallback(async () => {
		if (!domainId || !typeId) {
			setTicketType(null);
			return;
		}
		setLoading(true);
		try {
			const list = await fetchDomainTicketTypes(domainId);
			const found = list.find(item => item.id === typeId) ?? null;
			setTicketType(found);
			if (found) {
				setStatusFlow((found.status_flow as TicketStatusFlow | null) ?? DEFAULT_STATUS_FLOW);
			}
			else {
				message.error("未找到该事项类型");
			}
		}
		catch (error) {
			setTicketType(null);
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, typeId]);

	useEffect(() => {
		void loadTicketType();
	}, [loadTicketType]);

	useEffect(() => {
		if (!pagePath || !ticketType?.name) {
			return;
		}
		setTableTitle(appScopes.platform, pagePath, `状态管理 - ${ticketType.name}`);
		return () => {
			resetTableTitle(appScopes.platform, pagePath);
		};
	}, [pagePath, resetTableTitle, setTableTitle, ticketType?.name]);

	const handleSave = async () => {
		if (!domainId || !typeId) {
			return;
		}
		setSubmitting(true);
		try {
			const saved = await updateDomainTicketType(domainId, typeId, { status_flow: statusFlow });
			setTicketType(saved);
			message.success("状态管理已保存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ}>
				<div className="flex flex-col gap-4">
					<div className="flex items-center gap-3">
						<Button type="text" icon={<ArrowLeftOutlined />} onClick={backToTickets}>
							返回事项列表
						</Button>
						<Title level={5} className="!mb-0">
							{ticketType ? `状态管理：${ticketType.name}` : "状态管理"}
						</Title>
					</div>

					{!domainId || !typeId ? (
						<Empty description="缺少业务域或事项类型 ID" />
					) : loading ? (
						<div className="flex justify-center py-16">
							<Spin />
						</div>
					) : !ticketType ? (
						<Empty description="未找到事项类型">
							<Button type="primary" onClick={backToTickets}>
								返回事项列表
							</Button>
						</Empty>
					) : (
						<>
							<TicketTypeFlowDesigner
								key={ticketType.id}
								value={statusFlow}
								onChange={setStatusFlow}
							/>
							<div className="flex justify-end border-t border-colorBorderSecondary pt-4">
								<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
									<Button type="primary" loading={submitting} onClick={() => void handleSave()}>
										保存
									</Button>
								</AuthGuarded>
							</div>
						</>
					)}
				</div>
			</AuthGuarded>
		</BasicContent>
	);
}
