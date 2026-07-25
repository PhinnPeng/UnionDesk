import type { TicketAttribute, TicketAttributeSlotConfig } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";
import { CreationSortDrawer } from "#src/pages/platform/ticket-config/types/config/components/creation-sort-drawer";

import { OrderedListOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { Button, Card, Empty, Input, Space, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import { AddAttributeModal } from "./add-attribute-modal";
import { AttributeSlotTable, type SlotRow, isFixedSystemSlot, type FixedSystemSlotOptions } from "./attribute-slot-table";

import "./attribute-tab.less";

const { Text } = Typography;

export type AttributeTabLayout = "legacy" | "platform";

interface AttributeTabProps {
	loading: boolean;
	layout?: AttributeTabLayout;
	domainId?: string;
	createAttributePath?: string;
	updatePermission?: string;
	slots: SlotRow[];
	availableAttributes: TicketAttribute[];
	canUpdate: boolean;
	onRefresh?: () => void;
	onInsert: (attributeId: string, required: boolean) => void | Promise<void>;
	onRemove: (slotId: string) => void | Promise<void>;
	onReorder: (orders: { id: number; sort_order: number }[]) => void | Promise<void>;
	onConfigChange: (slotId: string, patch: Partial<TicketAttributeSlotConfig>) => void | Promise<void>;
	onSaveDraft?: (draftSlots: SlotRow[]) => void | Promise<void>;
	onPublish?: (draftSlots: SlotRow[]) => void | Promise<void>;
	onDirtyChange?: (dirty: boolean) => void;
	saving?: boolean;
	publishing?: boolean;
	/** @deprecated 使用 onSaveDraft */
	onApply?: (draftSlots: SlotRow[]) => void | Promise<void>;
	/** @deprecated */
	applying?: boolean;
	/** 事项类型 category，用于判断系统属性是否固定 */
	ticketTypeCategory?: string;
}

function slotsEqual(a: SlotRow[], b: SlotRow[]): boolean {
	if (a.length !== b.length) {
		return false;
	}
	return a.every((slot, index) => {
		const other = b[index];
		if (!other || slot.id !== other.id) {
			return false;
		}
		const configA = slot.slot_config;
		const configB = other.slot_config;
		return configA.required === configB.required
			&& configA.default_value === configB.default_value
			&& configA.visible_to_customer === configB.visible_to_customer
			&& configA.placeholder === configB.placeholder
			&& (configA.display_name ?? "") === (configB.display_name ?? "");
	});
}

export function buildReorderPayload(slots: SlotRow[], options?: FixedSystemSlotOptions): { id: number; sort_order: number }[] {
	return slots
		.filter(item => !isFixedSystemSlot(item, options))
		.map((item, index) => ({
			id: Number(item.id),
			sort_order: index,
		}));
}

export function AttributeTab({
	loading,
	layout = "legacy",
	domainId,
	createAttributePath,
	updatePermission = PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
	slots,
	availableAttributes,
	canUpdate,
	onRefresh,
	onInsert,
	onRemove,
	onReorder,
	onConfigChange,
	onSaveDraft,
	onPublish,
	onDirtyChange,
	saving = false,
	publishing = false,
	onApply,
	applying = false,
	ticketTypeCategory,
}: AttributeTabProps) {
	const isPlatformLayout = layout === "platform";
	const [modalOpen, setModalOpen] = useState(false);
	const [creationSortOpen, setCreationSortOpen] = useState(false);
	const [inserting, setInserting] = useState(false);
	const [keyword, setKeyword] = useState("");
	const [draftSlots, setDraftSlots] = useState<SlotRow[]>(slots);
	const [baselineSlots, setBaselineSlots] = useState<SlotRow[]>(slots);

	useEffect(() => {
		setDraftSlots(slots);
		setBaselineSlots(slots);
	}, [slots]);

	const dirty = useMemo(
		() => isPlatformLayout && !slotsEqual(draftSlots, baselineSlots),
		[draftSlots, baselineSlots, isPlatformLayout],
	);

	useEffect(() => {
		onDirtyChange?.(dirty);
	}, [dirty, onDirtyChange]);

	const activeSlots = isPlatformLayout ? draftSlots : slots;

	const slotOptions = useMemo((): FixedSystemSlotOptions | undefined =>
		ticketTypeCategory != null ? { category: ticketTypeCategory } : undefined,
	[ticketTypeCategory],
	);

	const insertedAttributeIds = useMemo(
		() => new Set(activeSlots.map(item => item.attribute_id)),
		[activeSlots],
	);

	const filteredSlots = useMemo(() => {
		if (!keyword.trim()) {
			return activeSlots;
		}
		const lower = keyword.trim().toLowerCase();
		return activeSlots.filter(item => {
			// 防御性检查：确保 attribute 存在
			if (!item.attribute) {
				return false;
			}
			return item.attribute.name.toLowerCase().includes(lower) ||
				(item.attribute.description ?? "").toLowerCase().includes(lower);
		});
	}, [activeSlots, keyword]);

	const patchSlot = useCallback((slotId: string, patch: Partial<TicketAttributeSlotConfig>) => {
		if (isPlatformLayout) {
			setDraftSlots(prev => prev.map((item) => {
				if (item.id !== slotId) {
					return item;
				}
				const nextConfig = { ...item.slot_config, ...patch };
				if (patch.default_value === undefined) {
					delete nextConfig.default_value;
				}
				return { ...item, slot_config: nextConfig };
			}));
			return;
		}
		void onConfigChange(slotId, patch);
	}, [isPlatformLayout, onConfigChange]);

	const handleInsert = useCallback(async (attributeId: string, required: boolean) => {
		setInserting(true);
		try {
			await onInsert(attributeId, required);
			setModalOpen(false);
		}
		finally {
			setInserting(false);
		}
	}, [onInsert]);

	const handleDragEnd = useCallback(({ active, over }: import("@dnd-kit/core").DragEndEvent) => {
		if (!over || active.id === over.id) {
			return;
		}
		const sortable = activeSlots.filter(item => !isFixedSystemSlot(item, slotOptions));
		const oldIndex = sortable.findIndex(item => item.dragId === active.id);
		const newIndex = sortable.findIndex(item => item.dragId === over.id);
		if (oldIndex < 0 || newIndex < 0) {
			return;
		}
		const nextSortable = arrayMove(sortable, oldIndex, newIndex);
		const fixed = activeSlots.filter(item => isFixedSystemSlot(item, slotOptions));
		const nextSlots = [...fixed, ...nextSortable];

		if (isPlatformLayout) {
			setDraftSlots(nextSlots);
			return;
		}
		void onReorder(buildReorderPayload(nextSlots, slotOptions));
	}, [activeSlots, isPlatformLayout, onReorder, slotOptions]);

	const handleSaveDraft = useCallback(async () => {
		const save = onSaveDraft ?? onApply;
		if (!save) {
			return;
		}
		await save(draftSlots);
		if (onSaveDraft || onApply) {
			setBaselineSlots(draftSlots);
		}
	}, [draftSlots, onApply, onSaveDraft]);

	const addAttributeModal = (
		<AddAttributeModal
			open={modalOpen}
			domainId={domainId}
			createAttributePath={createAttributePath}
			availableAttributes={availableAttributes}
			insertedAttributeIds={insertedAttributeIds}
			loading={inserting}
			onConfirm={handleInsert}
			onCancel={() => setModalOpen(false)}
		/>
	);

	if (isPlatformLayout) {
		return (
			<div className="attribute-tab attribute-tab--platform">
				<div className="attribute-tab__toolbar">
					<Input
						prefix={<SearchOutlined />}
						placeholder="搜索属性名称或描述"
						allowClear
						value={keyword}
						disabled={loading}
						onChange={e => setKeyword(e.target.value)}
						className="attribute-tab__search-input"
					/>
					<AuthGuarded auth={updatePermission} fallback={null}>
						<Space>
							<Button icon={<OrderedListOutlined />} onClick={() => setCreationSortOpen(true)}>
								属性排序
							</Button>
							<Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
								添加属性
							</Button>
						</Space>
					</AuthGuarded>
				</div>

				<div className="attribute-tab__table-wrap">
					{filteredSlots.length === 0 ? (
						<Empty description="暂无已添加的属性" />
					) : (
						<AttributeSlotTable
							loading={loading}
							dataSource={filteredSlots}
							canUpdate={canUpdate}
							showDragColumn={true}
							labelVariant="platform"
							category={ticketTypeCategory}
							onConfigChange={patchSlot}
							onRemove={slotId => void onRemove(slotId)}
							onDragEnd={handleDragEnd}
						/>
					)}
				</div>

				<AuthGuarded auth={updatePermission} fallback={null}>
					<div className="attribute-tab__footer">
						<Space size={12}>
							{onSaveDraft || onApply ? (
								<Button loading={saving || applying} onClick={() => void handleSaveDraft()}>
									暂存
								</Button>
							) : null}
							{onPublish ? (
								<Button type="primary" loading={publishing} onClick={() => void onPublish(draftSlots)}>
									发布
								</Button>
							) : null}
						</Space>
					</div>
				</AuthGuarded>

				{addAttributeModal}

				<CreationSortDrawer
					open={creationSortOpen}
					slots={draftSlots}
					onClose={() => setCreationSortOpen(false)}
					onConfirm={setDraftSlots}
				/>
			</div>
		);
	}

	return (
		<div className="flex flex-col gap-4">
			<Card
				bordered={false}
				title="已添加属性"
				extra={(
					<Space>
						{onRefresh ? (
							<Button icon={<ReloadOutlined />} onClick={onRefresh} loading={loading}>
								刷新
							</Button>
						) : null}
						<AuthGuarded auth={updatePermission} fallback={null}>
							<Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
								添加属性
							</Button>
						</AuthGuarded>
					</Space>
				)}
			>
				{filteredSlots.length === 0 ? (
					<Empty description="暂无已添加的属性" />
				) : (
					<>
						<AttributeSlotTable
							loading={loading}
							dataSource={filteredSlots}
							canUpdate={canUpdate}
							category={ticketTypeCategory}
							onConfigChange={patchSlot}
							onRemove={slotId => void onRemove(slotId)}
							onDragEnd={handleDragEnd}
						/>
						<Text type="secondary" className="mt-3 block">
							说明：系统字段（标题、描述）固定在最前且不可调整顺序；自定义属性支持拖拽排序。
						</Text>
					</>
				)}
			</Card>

			{addAttributeModal}
		</div>
	);
}

function arrayMove<T>(array: T[], from: number, to: number): T[] {
	const result = [...array];
	const [removed] = result.splice(from, 1);
	result.splice(to, 0, removed);
	return result;
}
