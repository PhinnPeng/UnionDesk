import type {
	PlatformTicketTypeDetail,
	TicketAttribute,
	TicketAttributeSlot,
	TicketAttributeSlotConfig,
} from "@uniondesk/shared";
import {
	fetchPlatformTicketAttributes,
	fetchPlatformTicketAttributeSlots,
	fetchPlatformTicketType,
	insertPlatformTicketAttributeSlot,
	publishPlatformTicketFormRelease,
	removePlatformTicketAttributeSlot,
	reorderPlatformTicketAttributeSlots,
	savePlatformTicketFormReleaseDraft,
	toErrorMessage,
	updatePlatformTicketAttributeSlot,
	updatePlatformTicketType,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { AttributeTab, buildReorderPayload } from "#src/pages/platform/domains/ticket-type-config/components/attribute-tab";
import type { SlotRow } from "#src/pages/platform/domains/ticket-type-config/components/attribute-slot-table";
import { isFixedSystemSlot } from "#src/pages/platform/domains/ticket-type-config/components/attribute-slot-table";
import { TemplateTab } from "#src/pages/platform/domains/ticket-type-config/components/template-tab";
import { WorkflowTab } from "#src/pages/platform/domains/ticket-type-config/components/workflow-tab";
import {
	PLATFORM_TICKET_CONFIG_TYPE_READ,
	PLATFORM_TICKET_CONFIG_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";
import {
	buildTicketConfigPath,
	parsePlatformTicketTypeConfigTab,
	type PlatformTicketTypeConfigTab,
	TICKET_CONFIG_BASE,
} from "#src/pages/platform/ticket-config/ticket-config-path";
import { TicketTypeConfigPageHeader } from "#src/pages/platform/ticket-config/types/config/components/ticket-type-config-header";
import type { PlatformTicketTypeConfigLocationState } from "#src/pages/platform/ticket-config/types/open-config-tab";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { App, Button, Empty, Spin } from "antd";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useSearchParams } from "react-router";

function slotConfigChanged(
	baseline: TicketAttributeSlotConfig,
	next: TicketAttributeSlotConfig,
): boolean {
	return baseline.required !== next.required
		|| baseline.default_value !== next.default_value
		|| baseline.visible_to_customer !== next.visible_to_customer
		|| baseline.placeholder !== next.placeholder
		|| (baseline.display_name ?? "") !== (next.display_name ?? "");
}

function isPersistedSlotId(slotId: string): boolean {
	return /^\d+$/.test(slotId);
}

function orderChanged(baseline: SlotRow[], next: SlotRow[]): boolean {
	const baselineOrder = baseline.filter(item => !isFixedSystemSlot(item)).map(item => item.id);
	const nextOrder = next.filter(item => !isFixedSystemSlot(item)).map(item => item.id);
	if (baselineOrder.length !== nextOrder.length) {
		return true;
	}
	return baselineOrder.some((id, index) => id !== nextOrder[index]);
}

export interface PlatformTicketTypeConfigContentProps {
	typeId: string
	onBack: () => void
}

export function PlatformTicketTypeConfigContent({ typeId, onBack }: PlatformTicketTypeConfigContentProps) {
	const { message, modal } = App.useApp();
	const location = useLocation();
	const [searchParams, setSearchParams] = useSearchParams();
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const routeState = location.state as PlatformTicketTypeConfigLocationState | null;
	const [activeTab, setActiveTab] = useState<PlatformTicketTypeConfigTab>(() => parsePlatformTicketTypeConfigTab(searchParams.get("tab")));
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [publishing, setPublishing] = useState(false);
	const [ticketType, setTicketType] = useState<PlatformTicketTypeDetail | null>(() => {
		const seeded = routeState?.ticketType;
		if (seeded && seeded.id === typeId) {
			return seeded as PlatformTicketTypeDetail;
		}
		return null;
	});
	const [slots, setSlots] = useState<TicketAttributeSlot[]>([]);
	const [availableAttributes, setAvailableAttributes] = useState<TicketAttribute[]>([]);
	const attributeDirtyRef = useRef(false);

	const createAttributePath = buildTicketConfigPath({ section: "attributes" });

	useEffect(() => {
		const tab = parsePlatformTicketTypeConfigTab(searchParams.get("tab"));
		setActiveTab(tab);
	}, [searchParams]);

	const loadData = useCallback(async () => {
		if (!typeId) {
			setTicketType(null);
			setSlots([]);
			setAvailableAttributes([]);
			return;
		}
		setLoading(true);
		try {
			const typeDetail = await fetchPlatformTicketType(typeId);
			setTicketType(typeDetail);

			const [slotsResult, attributesResult] = await Promise.allSettled([
				fetchPlatformTicketAttributeSlots(typeId),
				fetchPlatformTicketAttributes(),
			]);

			if (slotsResult.status === "fulfilled") {
				const slotsData = Array.isArray(slotsResult.value) ? slotsResult.value : [];
				setSlots(slotsData);
			}
			else {
				message.warning(`属性插槽加载失败：${toErrorMessage(slotsResult.reason)}`);
				setSlots([]);
			}

			if (attributesResult.status === "fulfilled") {
				setAvailableAttributes(attributesResult.value.items.filter(item => item.status === "active"));
			}
			else {
				message.warning(`属性字典加载失败：${toErrorMessage(attributesResult.reason)}`);
				setAvailableAttributes([]);
			}
		}
		catch (error) {
			message.error(toErrorMessage(error));
			if (!routeState?.ticketType || routeState.ticketType.id !== typeId) {
				setTicketType(null);
			}
			setSlots([]);
			setAvailableAttributes([]);
		}
		finally {
			setLoading(false);
		}
	}, [message, routeState?.ticketType, typeId]);

	useEffect(() => {
		void loadData();
	}, [loadData]);

	useEffect(() => {
		if (!ticketType?.name) {
			return;
		}
		setTableTitle(appScopes.platform, TICKET_CONFIG_BASE, `事项类型配置 - ${ticketType.name}`);
		return () => {
			resetTableTitle(appScopes.platform, TICKET_CONFIG_BASE);
		};
	}, [resetTableTitle, setTableTitle, ticketType?.name]);

	const handleBack = useCallback(() => {
		if (attributeDirtyRef.current) {
			modal.confirm({
				title: "有未暂存的属性配置",
				content: "离开前请先暂存，或放弃未保存的更改。",
				okText: "离开",
				cancelText: "留在此页",
				onOk: onBack,
			});
			return;
		}
		onBack();
	}, [modal, onBack]);

	const handleTabChange = useCallback((key: string) => {
		const nextTab = parsePlatformTicketTypeConfigTab(key);
		const switchTab = () => {
			setActiveTab(nextTab);
			setSearchParams((prev) => {
				const next = new URLSearchParams(prev);
				next.set("section", "types");
				next.set("typeId", typeId);
				next.set("tab", nextTab);
				return next;
			}, { replace: true });
		};
		if (attributeDirtyRef.current && activeTab === "attributes" && nextTab !== "attributes") {
			modal.confirm({
				title: "有未暂存的属性配置",
				content: "切换页签将丢失未暂存的更改，是否继续？",
				okText: "继续切换",
				cancelText: "留在此页",
				onOk: switchTab,
			});
			return;
		}
		switchTab();
	}, [activeTab, modal, setSearchParams, typeId]);

	const handleInsert = useCallback(async (attributeId: string, required: boolean) => {
		try {
			await insertPlatformTicketAttributeSlot(typeId, attributeId, { required });
			message.success("属性已添加");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [loadData, message, typeId]);

	const handleRemove = useCallback(async (slotId: string) => {
		try {
			await removePlatformTicketAttributeSlot(typeId, slotId);
			message.success("属性已拔出");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [loadData, message, typeId]);

	const persistAttributeSlots = useCallback(async (draftSlots: SlotRow[]) => {
		const baselineRows = (slots || []).map(item => ({ ...item, dragId: item.id }));
		const configTasks = draftSlots
			.filter(item => isPersistedSlotId(item.id))
			.map(async (draft) => {
				const baseline = baselineRows.find(item => item.id === draft.id);
				if (!baseline || !slotConfigChanged(baseline.slot_config, draft.slot_config)) {
					return;
				}
				await updatePlatformTicketAttributeSlot(typeId, draft.id, draft.slot_config);
			});
		await Promise.all(configTasks);

		if (orderChanged(baselineRows, draftSlots)) {
			await reorderPlatformTicketAttributeSlots(typeId, buildReorderPayload(draftSlots));
		}

		const saved = await savePlatformTicketFormReleaseDraft(typeId);
		setTicketType(saved);
		await loadData();
		attributeDirtyRef.current = false;
		return saved;
	}, [loadData, slots, typeId]);

	const handleAttributesSaveDraft = useCallback(async (draftSlots: SlotRow[]) => {
		setSaving(true);
		try {
			await persistAttributeSlots(draftSlots);
			message.success("已暂存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
			await loadData();
		}
		finally {
			setSaving(false);
		}
	}, [loadData, message, persistAttributeSlots]);

	const handleAttributesPublish = useCallback(async (draftSlots: SlotRow[]) => {
		setPublishing(true);
		try {
			await persistAttributeSlots(draftSlots);
			const saved = await publishPlatformTicketFormRelease(typeId);
			setTicketType(saved);
			message.success("已发布");
		}
		catch (error) {
			message.error(toErrorMessage(error));
			await loadData();
		}
		finally {
			setPublishing(false);
		}
	}, [loadData, message, persistAttributeSlots, typeId]);

	const handleSaveDescriptionTemplate = useCallback(async (markdown: string) => {
		if (!typeId) {
			return;
		}
		setSaving(true);
		try {
			await updatePlatformTicketType(typeId, {
				description_template_md: markdown,
			});
			message.success("描述模板已保存");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
			throw error;
		}
		finally {
			setSaving(false);
		}
	}, [loadData, message, typeId]);

	const unpublished = ticketType?.form_schema_has_unpublished ?? false;

	const slotRows = useMemo(() =>
		(slots || []).map(item => ({ ...item, dragId: item.id })),
	[slots],
	);

	return (
		<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TYPE_READ} fallback={<Empty description="无权限" className="py-16" />}>
			<div className="ticket-type-config-page">
				<TicketTypeConfigPageHeader
					ticketType={ticketType}
					unpublished={unpublished}
					activeTab={activeTab}
					onTabChange={handleTabChange}
					onBack={handleBack}
				/>

				{!typeId ? (
					<div className="ticket-type-config-page__body">
						<Empty description="缺少事项类型 ID" />
					</div>
				) : loading && !ticketType ? (
					<div className="ticket-type-config-page__body flex flex-1 justify-center py-16">
						<Spin />
					</div>
				) : !ticketType ? (
					<div className="ticket-type-config-page__body">
						<Empty description="未找到事项类型">
							<Button type="primary" onClick={handleBack}>
								返回事项类型列表
							</Button>
						</Empty>
					</div>
				) : (
					<div className="ticket-type-config-page__body">
						{activeTab === "attributes" ? (
						<AttributeTab
							layout="platform"
							loading={loading}
							ticketTypeCategory={ticketType?.category}
							createAttributePath={createAttributePath}
								updatePermission={PLATFORM_TICKET_CONFIG_TYPE_UPDATE}
								slots={slotRows}
								availableAttributes={availableAttributes}
								canUpdate={true}
								onInsert={handleInsert}
								onRemove={handleRemove}
								onReorder={async () => {}}
								onConfigChange={async () => {}}
								onSaveDraft={handleAttributesSaveDraft}
								onPublish={handleAttributesPublish}
								saving={saving}
								publishing={publishing}
								onDirtyChange={dirty => { attributeDirtyRef.current = dirty; }}
							/>
						) : null}
					{activeTab === "workflow" ? (
						<WorkflowTab
							loading={loading}
							domainId="0"
							ticketType={ticketType}
							availableAttributes={availableAttributes}
							slots={slotRows}
							canUpdate={true}
							onRefresh={loadData}
						/>
					) : null}
						{activeTab === "template" ? (
							<TemplateTab
								loading={loading}
								ticketType={ticketType}
								updatePermission={PLATFORM_TICKET_CONFIG_TYPE_UPDATE}
								onSave={handleSaveDescriptionTemplate}
								saving={saving}
							/>
						) : null}
					</div>
				)}
			</div>
		</AuthGuarded>
	);
}
