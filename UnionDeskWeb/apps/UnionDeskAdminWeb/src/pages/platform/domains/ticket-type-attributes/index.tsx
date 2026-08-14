import type { DomainTicketType, TicketAttribute, TicketAttributeSlot, TicketAttributeSlotConfig } from "@uniondesk/shared";
import {
	fetchDomainTicketAttributes,
	fetchDomainTicketTypes,
	fetchTicketAttributeSlots,
	insertTicketAttributeSlot,
	publishTicketFormRelease,
	removeTicketAttributeSlot,
	reorderTicketAttributeSlots,
	saveTicketFormReleaseDraft,
	toErrorMessage,
	updateTicketAttributeSlot,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "../platform-domain-permissions";

import { formatAttributeTypeLabel } from "#src/pages/platform/ticket-config/attributes/components/attribute-utils";

import { ArrowLeftOutlined, HolderOutlined, ReloadOutlined, RollbackOutlined } from "@ant-design/icons";
import type { DragEndEvent } from "@dnd-kit/core";
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
	App,
	Button,
	Card,
	Checkbox,
	Empty,
	Input,
	Select,
	Space,
	Table,
	Tag,
	Tooltip,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router";

const { Text, Title } = Typography;

interface SlotRow extends TicketAttributeSlot {
	dragId: string;
}

interface SlotRowProps extends React.HTMLAttributes<HTMLTableRowElement> {
	"data-row-key": string;
}

function SortableSlotRow({ children, ...props }: SlotRowProps) {
	const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
		id: props["data-row-key"],
		disabled: props["data-row-key"].startsWith("system_"),
	});
	const style: React.CSSProperties = {
		...props.style,
		transform: CSS.Transform.toString(transform),
		transition,
		...(isDragging ? { position: "relative", zIndex: 9999, background: "var(--ant-color-bg-container)" } : {}),
	};
	return (
		<tr {...props} ref={setNodeRef} style={style}>
			{React.Children.map(children, (child) => {
				if (!React.isValidElement(child)) {
					return child;
				}
				const cell = child as React.ReactElement<{ className?: string }>;
				if (cell.props.className?.includes("drag-handle-cell")) {
					if (props["data-row-key"].startsWith("system_")) {
						return React.cloneElement(cell, {}, "—");
					}
					return React.cloneElement(cell, {}, (
						<span {...attributes} {...listeners} style={{ cursor: "move", touchAction: "none" }}>
							<HolderOutlined />
						</span>
					));
				}
				return child;
			})}
		</tr>
	);
}

export default function TicketTypeAttributesPage() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { domainId = "", typeId = "" } = useParams();
	const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [ticketType, setTicketType] = useState<DomainTicketType | null>(null);
	const [slots, setSlots] = useState<SlotRow[]>([]);
	const [availableAttributes, setAvailableAttributes] = useState<TicketAttribute[]>([]);
	const [insertAttributeId, setInsertAttributeId] = useState<string>();

	const loadData = useCallback(async () => {
		if (!domainId || !typeId) {
			return;
		}
		setLoading(true);
		try {
			const [types, slotList, attributes] = await Promise.all([
				fetchDomainTicketTypes(domainId),
				fetchTicketAttributeSlots(domainId, typeId),
				fetchDomainTicketAttributes(domainId),
			]);
			setTicketType(types.find(item => item.id === typeId) ?? null);
			setSlots(slotList.map(item => ({ ...item, dragId: item.id })));
			setAvailableAttributes(attributes.items.filter(item => item.status === "active"));
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, typeId]);

	useEffect(() => {
		void loadData();
	}, [loadData]);

	const insertedAttributeIds = useMemo(
		() => new Set(slots.filter(item => !item.is_system).map(item => item.attribute_id)),
		[slots],
	);

	const insertOptions = useMemo(
		() => availableAttributes
			.filter(item => !insertedAttributeIds.has(item.id))
			.map(item => ({ value: item.id, label: `${item.name}（${formatAttributeTypeLabel(item)}）` })),
		[availableAttributes, insertedAttributeIds],
	);

	const customSlots = useMemo(
		() => slots.filter(item => !item.is_system),
		[slots],
	);

	const handleInsert = async () => {
		if (!insertAttributeId) {
			message.warning("请选择要插入的属性");
			return;
		}
		try {
			await insertTicketAttributeSlot(domainId, typeId, insertAttributeId);
			setInsertAttributeId(undefined);
			message.success("属性已插入");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const handleSlotConfigChange = async (slotId: string, patch: Partial<TicketAttributeSlotConfig>) => {
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
	};

	const handleRemove = async (slotId: string) => {
		try {
			await removeTicketAttributeSlot(domainId, typeId, slotId);
			message.success("属性已拔出");
			await loadData();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const handleDragEnd = async ({ active, over }: DragEndEvent) => {
		if (!over || active.id === over.id) {
			return;
		}
		const draggable = customSlots;
		const oldIndex = draggable.findIndex(item => item.dragId === active.id);
		const newIndex = draggable.findIndex(item => item.dragId === over.id);
		if (oldIndex < 0 || newIndex < 0) {
			return;
		}
		const nextCustom = arrayMove(draggable, oldIndex, newIndex);
		const systemRows = slots.filter(item => item.is_system);
		const nextSlots = [...systemRows, ...nextCustom.map((item, index) => ({ ...item, sort_order: systemRows.length + index }))];
		setSlots(nextSlots);
		try {
			await reorderTicketAttributeSlots(
				domainId,
				typeId,
				nextCustom.map((item, index) => ({ id: item.id, sort_order: index })),
			);
		}
		catch (error) {
			message.error(toErrorMessage(error));
			await loadData();
		}
	};

	const handleSaveDraft = async () => {
		setSaving(true);
		try {
			const updated = await saveTicketFormReleaseDraft(domainId, typeId);
			setTicketType(updated);
			message.success("草稿已保存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	const handlePublish = async () => {
		setSaving(true);
		try {
			const updated = await publishTicketFormRelease(domainId, typeId);
			setTicketType(updated);
			message.success("已发布");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	const columns: TableColumnsType<SlotRow> = [
		{ title: "", width: 40, className: "drag-handle-cell", render: () => null },
		{
			title: "字段",
			render: (_, record) => (
				<div>
					<Text strong>{record.attribute.name}</Text>
					<div><Text type="secondary">{formatAttributeTypeLabel(record.attribute)}</Text></div>
				</div>
			),
		},
		{
			title: "来源",
			width: 72,
			render: (_, record) => (record.is_system ? "系统" : "字典"),
		},
		{
			title: "必填",
			width: 88,
			render: (_, record) => (
				record.is_system
					? <Checkbox checked disabled />
					: (
						<Checkbox
							checked={Boolean(record.slot_config.required)}
							onChange={event => void handleSlotConfigChange(record.id, { required: event.target.checked })}
						/>
					)
			),
		},
		{
			title: "placeholder",
			render: (_, record) => (
				record.is_system || record.attribute.field_type === "switch"
					? "—"
					: (
						<Input
							size="small"
							value={record.slot_config.placeholder ?? ""}
							placeholder="请输入…"
							onChange={event => void handleSlotConfigChange(record.id, { placeholder: event.target.value })}
						/>
					)
			),
		},
		{
			title: "用户可见",
			width: 88,
			align: "center",
			render: (_, record) => (
				<Checkbox
					checked={record.is_system ? true : record.slot_config.visible_to_customer !== false}
					disabled={record.is_system}
					onChange={event => void handleSlotConfigChange(record.id, { visible_to_customer: event.target.checked })}
				/>
			),
		},
		{
			title: "操作",
			width: 72,
			render: (_, record) => (
				record.is_system
					? "—"
					: (
						<ConfirmPopover title="确认拔出该属性？" onConfirm={() => void handleRemove(record.id)}>
							<Tooltip title="拔出">
								<Button type="link" size="small" danger icon={<RollbackOutlined />} />
							</Tooltip>
						</ConfirmPopover>
					)
			),
		},
	];

	const unpublished = ticketType?.form_schema_has_unpublished ?? false;

	return (
		<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ} fallback={<Empty description="无权限" className="py-16" />}>
			<div className="flex flex-col gap-4 p-4">
				<div className="flex items-center justify-between gap-4">
					<Space>
						<Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>返回</Button>
						<Title level={5} className="!mb-0">
							事项类型「{ticketType?.name ?? typeId}」— 属性编排
						</Title>
						{unpublished ? <Tag color="warning">未发布</Tag> : null}
					</Space>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<Space>
							<Button loading={saving} onClick={() => void handleSaveDraft()}>保存草稿</Button>
							<Button type="primary" loading={saving} onClick={() => void handlePublish()}>发布</Button>
						</Space>
					</AuthGuarded>
				</div>

				<Card bordered={false} title="插入属性" extra={<Button icon={<ReloadOutlined />} onClick={() => void loadData()}>刷新</Button>}>
					<Space wrap>
						<Select
							style={{ minWidth: 280 }}
							placeholder="选择属性"
							options={insertOptions}
							value={insertAttributeId}
							onChange={setInsertAttributeId}
							allowClear
						/>
						<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
							<Button type="primary" onClick={() => void handleInsert()} disabled={!insertOptions.length}>插入</Button>
						</AuthGuarded>
					</Space>
				</Card>

				<Card bordered={false} title="页面字段顺序">
					<DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={event => void handleDragEnd(event)}>
						<SortableContext items={customSlots.map(item => item.dragId)} strategy={verticalListSortingStrategy}>
							<Table
								rowKey="dragId"
								loading={loading}
								columns={columns}
								dataSource={slots}
								pagination={false}
								components={{ body: { row: SortableSlotRow } }}
							/>
						</SortableContext>
					</DndContext>
					<Text type="secondary" className="mt-3 block">
						说明：每条属性独占一行，顺序由上表拖拽决定；系统字段固定在最前且不可移除。
					</Text>
				</Card>
			</div>
		</AuthGuarded>
	);
}
