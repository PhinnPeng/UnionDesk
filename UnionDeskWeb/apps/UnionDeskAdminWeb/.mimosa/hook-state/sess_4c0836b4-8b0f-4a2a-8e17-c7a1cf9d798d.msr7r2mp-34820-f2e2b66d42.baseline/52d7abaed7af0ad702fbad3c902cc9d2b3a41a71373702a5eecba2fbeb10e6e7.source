import type { DragEndEvent } from "@dnd-kit/core";
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Drawer, List, Typography, Button, Space } from "antd";
import React, { useEffect, useMemo, useState } from "react";

import { HolderOutlined } from "@ant-design/icons";

import type { SlotRow } from "#src/pages/platform/domains/ticket-type-config/components/attribute-slot-table";
import { isFixedSystemSlot } from "#src/pages/platform/domains/ticket-type-config/components/attribute-slot-table";

import "./creation-sort-drawer.less";

const { Text } = Typography;

interface CreationSortDrawerProps {
	open: boolean;
	slots: SlotRow[];
	onClose: () => void;
	onConfirm: (nextSlots: SlotRow[]) => void;
}

interface SortableItemProps {
	item: SlotRow;
	disabled?: boolean;
}

function SortableItem({ item, disabled }: SortableItemProps) {
	const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
		id: item.dragId,
		disabled,
	});
	const style: React.CSSProperties = {
		transform: CSS.Transform.toString(transform),
		transition,
		...(isDragging ? { position: "relative", zIndex: 9999, background: "var(--ant-color-bg-container)" } : {}),
	};

	return (
		<List.Item ref={setNodeRef} style={style} className="creation-sort-drawer__item">
			{disabled ? (
				<span className="creation-sort-drawer__handle creation-sort-drawer__handle--disabled">—</span>
			) : (
				<span {...attributes} {...listeners} className="creation-sort-drawer__handle">
					<HolderOutlined />
				</span>
			)}
			<Text>{item.attribute.name}</Text>
			{item.is_system || item.attribute.is_system ? <Text type="secondary">系统</Text> : null}
		</List.Item>
	);
}

export function CreationSortDrawer({ open, slots, onClose, onConfirm }: CreationSortDrawerProps) {
	const fixedSlots = useMemo(() => slots.filter(item => isFixedSystemSlot(item)), [slots]);
	const creationSlots = useMemo(
		() => slots.filter(item => !isFixedSystemSlot(item) && item.slot_config.visible_to_customer !== false),
		[slots],
	);

	const [orderedSlots, setOrderedSlots] = useState<SlotRow[]>(creationSlots);

	useEffect(() => {
		if (open) {
			setOrderedSlots(creationSlots);
		}
	}, [creationSlots, open]);

	const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

	const handleDragEnd = (event: DragEndEvent) => {
		const { active, over } = event;
		if (!over || active.id === over.id) {
			return;
		}
		const oldIndex = orderedSlots.findIndex(item => item.dragId === active.id);
		const newIndex = orderedSlots.findIndex(item => item.dragId === over.id);
		if (oldIndex < 0 || newIndex < 0) {
			return;
		}
		setOrderedSlots(arrayMove(orderedSlots, oldIndex, newIndex));
	};

	const handleConfirm = () => {
		const hidden = slots.filter(
			item => !isFixedSystemSlot(item) && item.slot_config.visible_to_customer === false,
		);
		onConfirm([...fixedSlots, ...orderedSlots, ...hidden]);
		onClose();
	};

	return (
		<Drawer
			title="属性排序"
			open={open}
			width={480}
			destroyOnHidden
			onClose={onClose}
			footer={(
				<Space style={{ justifyContent: "flex-end", width: "100%" }}>
					<Button onClick={onClose}>取消</Button>
					<Button type="primary" onClick={handleConfirm}>确定</Button>
				</Space>
			)}
		>
			<Text type="secondary" className="creation-sort-drawer__hint">
				仅展示在创建页显示的属性，拖拽调整顺序后点击确定写回（需点击「暂存」提交）。
			</Text>
			{fixedSlots.length > 0 ? (
				<List
					size="small"
					header={<Text type="secondary">固定字段（不可排序）</Text>}
					dataSource={fixedSlots}
					renderItem={item => <SortableItem key={item.dragId} item={item} disabled />}
				/>
			) : null}
			<DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
				<SortableContext items={orderedSlots.map(item => item.dragId)} strategy={verticalListSortingStrategy}>
					<List
						size="small"
						header={<Text type="secondary">创建页显示属性</Text>}
						dataSource={orderedSlots}
						renderItem={item => <SortableItem key={item.dragId} item={item} />}
					/>
				</SortableContext>
			</DndContext>
		</Drawer>
	);
}
