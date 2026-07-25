import type {
	DomainTicketType,
	TicketAttribute,
	TicketAttributeSlot,
	TicketAttributeSlotConfig,
} from "@uniondesk/shared";
import {
	fetchDomainTicketAttributes,
	fetchDomainTicketTypes,
	fetchTicketAttributeSlots,
	insertTicketAttributeSlot,
	removeTicketAttributeSlot,
	reorderTicketAttributeSlots,
	toErrorMessage,
	updateDomainTicketType,
	updateTicketAttributeSlot,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { ArrowLeftOutlined } from "@ant-design/icons";
import { App, Button, Empty, Spin, Tabs, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { AttributeTab } from "./components/attribute-tab";
import { TemplateTab } from "./components/template-tab";
import { WorkflowTab } from "./components/workflow-tab";

const { Title } = Typography;

type TabKey = "attributes" | "workflow" | "template";

export default function TicketTypeConfigPage() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { domainId: domainIdParam, typeId: typeIdParam } = useParams();
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const domainId = domainIdParam?.trim() ?? "";
	const typeId = typeIdParam?.trim() ?? "";

	const [activeTab, setActiveTab] = useState<TabKey>("attributes");
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [ticketType, setTicketType] = useState<DomainTicketType | null>(null);
	const [slots, setSlots] = useState<TicketAttributeSlot[]>([]);
	const [availableAttributes, setAvailableAttributes] = useState<TicketAttribute[]>([]);

	const pagePath = useMemo(() => {
		if (!domainId || !typeId) {
			return "";
		}
		return `/platform/domains/ticket-type-config/${encodeURIComponent(domainId)}/${encodeURIComponent(typeId)}`;
	}, [domainId, typeId]);

	const backToTickets = useCallback(() => {
		if (!domainId) {
			navigate("/platform/domains");
			return;
		}
		navigate(`/platform/domains/detail/${encodeURIComponent(domainId)}?tab=tickets`);
	}, [domainId, navigate]);

	const loadData = useCallback(async () => {
		if (!domainId || !typeId) {
			setTicketType(null);
			setSlots([]);
			setAvailableAttributes([]);
			return;
		}
		setLoading(true);
		try {
			const [types, slotList, attributes] = await Promise.all([
				fetchDomainTicketTypes(domainId),
				fetchTicketAttributeSlots(domainId, typeId),
				fetchDomainTicketAttributes(domainId),
			]);
			const found = types.find(item => item.id === typeId) ?? null;
			setTicketType(found);
			setSlots(slotList);
			setAvailableAttributes(attributes.items.filter(item => item.status === "active"));
		}
		catch (error) {
			message.error(toErrorMessage(error));
			setTicketType(null);
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, typeId]);

	useEffect(() => {
		void loadData();
	}, [loadData]);

	useEffect(() => {
		if (!pagePath || !ticketType?.name) {
			return;
		}
		setTableTitle(appScopes.platform, pagePath, `事项类型配置 - ${ticketType.name}`);
		return () => {
			resetTableTitle(appScopes.platform, pagePath);
		};
	}, [pagePath, resetTableTitle, setTableTitle, ticketType?.name]);

	const handleInsert = useCallback(async (attributeId: string, required: boolean) => {
		try {
			await insertTicketAttributeSlot(domainId, typeId, attributeId);
			message.success("属性已添加");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, loadData, message, typeId]);

	const handleRemove = useCallback(async (slotId: string) => {
		try {
			await removeTicketAttributeSlot(domainId, typeId, slotId);
			message.success("属性已拔出");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, loadData, message, typeId]);

	const handleReorder = useCallback(async (orders: { id: number; sort_order: number }[]) => {
		try {
			await reorderTicketAttributeSlots(domainId, typeId, orders);
		}
		catch (error) {
			message.error(toErrorMessage(error));
			await loadData();
		}
	}, [domainId, loadData, message, typeId]);

	const handleConfigChange = useCallback(async (slotId: string, patch: Partial<TicketAttributeSlotConfig>) => {
		const target = slots.find(item => item.id === slotId);
		if (!target || target.is_system) {
			return;
		}
		const nextConfig: TicketAttributeSlotConfig = { ...target.slot_config, ...patch };
		try {
			await updateTicketAttributeSlot(domainId, typeId, slotId, nextConfig);
			setSlots(prev => prev.map(item => item.id === slotId ? { ...item, slot_config: nextConfig } : item));
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, message, slots, typeId]);

	const handleSaveDescriptionTemplate = useCallback(async (markdown: string) => {
		if (!domainId || !typeId) {
			return;
		}
		setSaving(true);
		try {
			const saved = await updateDomainTicketType(domainId, typeId, {
				description_template_md: markdown,
			});
			setTicketType(saved);
			message.success("描述模板已保存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
			throw error;
		}
		finally {
			setSaving(false);
		}
	}, [domainId, message, typeId]);

	const slotRows = useMemo(() =>
		slots.map(item => ({ ...item, dragId: item.id })),
	[slots],
	);

	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ} fallback={<Empty description="无权限" className="py-16" />}>
				<div className="flex h-full flex-col gap-4">
					<div className="flex items-center justify-between gap-4">
						<div className="flex items-center gap-3">
							<Button type="text" icon={<ArrowLeftOutlined />} onClick={backToTickets}>
								返回事项列表
							</Button>
							<Title level={5} className="!mb-0">
								{ticketType ? `事项类型配置：${ticketType.name}` : "事项类型配置"}
							</Title>
						</div>
					</div>

					{!domainId || !typeId ? (
						<Empty description="缺少业务域或事项类型 ID" />
					) : loading && !ticketType ? (
						<div className="flex flex-1 justify-center py-16">
							<Spin />
						</div>
					) : !ticketType ? (
						<Empty description="未找到事项类型">
							<Button type="primary" onClick={backToTickets}>
								返回事项列表
							</Button>
						</Empty>
					) : (
						<Tabs
							type="card"
							activeKey={activeTab}
							onChange={key => setActiveTab(key as TabKey)}
							items={[
								{
									key: "attributes",
									label: "属性",
									children: (
									<AttributeTab
										loading={loading}
										domainId={domainId}
										ticketTypeCategory={ticketType?.category}
										slots={slotRows}
											availableAttributes={availableAttributes}
											canUpdate={true}
											onRefresh={loadData}
											onInsert={handleInsert}
											onRemove={handleRemove}
											onReorder={handleReorder}
											onConfigChange={handleConfigChange}
										/>
									),
								},
							{
								key: "workflow",
								label: "工作流",
								children: (
									<WorkflowTab
										loading={loading}
										domainId={domainId}
										ticketType={ticketType}
										availableAttributes={availableAttributes}
										slots={slotRows}
										canUpdate={true}
										onRefresh={loadData}
									/>
								),
							},
								{
									key: "template",
									label: "描述模板",
									children: (
										<TemplateTab
											loading={loading}
											ticketType={ticketType}
											updatePermission={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE}
											onSave={handleSaveDescriptionTemplate}
											saving={saving}
										/>
									),
								},
							]}
							/>
						)}
				</div>
			</AuthGuarded>
		</BasicContent>
	);
}
