import type { TicketAttributeSlot, TicketAttributeSlotConfig } from "@uniondesk/shared";

import { formatAttributeTypeLabel } from "#src/pages/platform/ticket-config/attributes/components/attribute-utils";

import { ConfirmPopover } from "#src/components/confirm-popover";

import { DeleteOutlined, EditOutlined, HolderOutlined } from "@ant-design/icons";
import type { DragEndEvent } from "@dnd-kit/core";
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Button, Switch, Table, Tag, Tooltip, Typography } from "antd";
import type { TableColumnsType } from "antd";
import React, { useMemo, useState } from "react";

import { formatSlotDefaultValueLabel } from "./attribute-default-value";
import { AttributeSlotEditModal } from "./attribute-slot-edit-modal";

import "./attribute-slot-table.less";

const { Text } = Typography;

export interface SlotRow extends TicketAttributeSlot {
	dragId: string;
}

type DividerRow = { type: "divider"; dragId: string };
export type DisplayRow = SlotRow | DividerRow;

export interface FixedSystemSlotOptions {
	category?: string;
}

/** 判断系统属性插槽是否对该类型固定（不可排序、不可删除、不可拔出） */
export function isFixedSystemSlot(record: SlotRow, options?: FixedSystemSlotOptions): boolean {
	if (!record.is_system || !record.system_field_key) {
		return false;
	}
	const category = options?.category ?? "transaction";
	const key = record.system_field_key;
	if ("feedback" === category) {
		return key === "description"; // feedback 类型只有描述固定
	}
	// transaction 及其他：标题和描述都固定
	return key === "title" || key === "description";
}

function slotDisplayName(record: SlotRow): string {
	return record.slot_config.display_name?.trim() || record.attribute?.name || "未命名属性";
}

export function isDividerRow(row: DisplayRow): row is DividerRow {
	return "type" in row && row.type === "divider";
}

export function buildDisplayRows(slots: SlotRow[], options?: FixedSystemSlotOptions): DisplayRow[] {
	const fixed = slots.filter(item => isFixedSystemSlot(item, options));
	const rest = slots.filter(item => !isFixedSystemSlot(item, options));
	if (fixed.length === 0) {
		return rest;
	}
	return [...fixed, { type: "divider", dragId: "__divider__" }, ...rest];
}

interface SlotRowProps extends React.HTMLAttributes<HTMLTableRowElement> {
	"data-row-key": string;
}

interface AttributeSlotTableProps {
	loading: boolean;
	dataSource: SlotRow[];
	canUpdate: boolean;
	showDragColumn?: boolean;
	labelVariant?: "default" | "platform";
	category?: string;
	onConfigChange: (slotId: string, patch: Partial<TicketAttributeSlotConfig>) => void;
	onRemove: (slotId: string) => void;
	onDragEnd: (event: DragEndEvent) => void;
}

function SortableSlotRow({ children, ...props }: SlotRowProps) {
	const rowKey = props["data-row-key"];
	const isDivider = rowKey === "__divider__";
	const isFixed = rowKey.startsWith("system_") && (rowKey.includes("_title_") || rowKey.includes("_description_"));
	const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
		id: rowKey,
		disabled: isDivider || isFixed,
	});
	const style: React.CSSProperties = {
		...props.style,
		transform: CSS.Transform.toString(transform),
		transition,
		...(isDragging ? { position: "relative", zIndex: 9999, background: "var(--ant-color-bg-container)" } : {}),
	};

	if (isDivider) {
		return (
			<tr {...props} className="attribute-slot-table__divider-row">
				<td colSpan={8}>
					<span className="attribute-slot-table__divider-text">以上属性不可排序</span>
				</td>
			</tr>
		);
	}

	return (
		<tr {...props} ref={setNodeRef} style={style} className={isFixed ? "system-row" : undefined}>
			{React.Children.map(children, (child) => {
				if (!React.isValidElement(child)) {
					return child;
				}
				const cell = child as React.ReactElement<{ className?: string }>;
				if (cell.props.className?.includes("drag-handle-cell")) {
					const handleClass = isFixed
						? "attribute-slot-table__drag-handle attribute-slot-table__drag-handle--fixed"
						: "attribute-slot-table__drag-handle";
					return React.cloneElement(cell, {}, isFixed ? (
						<span className={handleClass}>—</span>
					) : (
						<span {...attributes} {...listeners} className={handleClass} style={{ touchAction: "none" }}>
							<HolderOutlined />
						</span>
					));
				}
				return child;
			})}
		</tr>
	);
}

function PlainRow({ children, ...props }: SlotRowProps) {
	const rowKey = props["data-row-key"];
	if (rowKey === "__divider__") {
		return (
			<tr {...props} className="attribute-slot-table__divider-row">
				<td colSpan={8}>
					<span className="attribute-slot-table__divider-text">以上属性不可排序</span>
				</td>
			</tr>
		);
	}
	return <tr {...props}>{children}</tr>;
}

export function AttributeSlotTable({
	loading,
	dataSource,
	canUpdate,
	showDragColumn = true,
	labelVariant = "default",
	category,
	onConfigChange,
	onRemove,
	onDragEnd,
}: AttributeSlotTableProps) {
	const isPlatformLabels = labelVariant === "platform";
	const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));
	const [editingSlot, setEditingSlot] = useState<SlotRow | null>(null);

	const slotOptions = useMemo((): FixedSystemSlotOptions | undefined =>
		category != null ? { category } : undefined, [category]);

	const displayRows = useMemo(() => buildDisplayRows(dataSource, slotOptions), [dataSource, slotOptions]);
	const sortableIds = useMemo(
		() => dataSource.filter(item => !isFixedSystemSlot(item, slotOptions)).map(item => item.dragId),
		[dataSource, slotOptions],
	);

	const columns: TableColumnsType<DisplayRow> = useMemo(() => {
		const dragColumn: TableColumnsType<DisplayRow>[number] = {
			title: "",
			width: 40,
			align: "center",
			className: "drag-handle-cell",
			render: () => null,
		};

		return [
			...(showDragColumn ? [dragColumn] : []),
			{
				title: "属性名称",
				width: 200,
				ellipsis: true,
				render: (_, record) => {
					if (isDividerRow(record)) {
						return null;
					}
					const displayName = slotDisplayName(record);
					return (
						<span className="attribute-slot-table__name-cell">
							<Text strong>{displayName}</Text>
							{(record.is_system || record.attribute?.is_system) ? <Tag color="blue">系统</Tag> : null}
						</span>
					);
				},
			},
			{
				title: "属性类型",
				width: 100,
				align: "center",
				render: (_, record) => (isDividerRow(record) ? null : record.attribute ? formatAttributeTypeLabel(record.attribute) : "—"),
			},
			{
				title: "描述",
				width: 160,
				ellipsis: true,
				render: (_, record) => {
					if (isDividerRow(record)) {
						return null;
					}
					return record.attribute?.description || "—";
				},
			},
			{
				title: "默认值",
				width: 100,
				align: "center",
				ellipsis: true,
				render: (_, record) => {
					if (isDividerRow(record)) {
						return null;
					}
					return (
						<Text type="secondary" className="attribute-slot-table__default-value">
							{formatSlotDefaultValueLabel(record.slot_config.default_value, record.attribute)}
						</Text>
					);
				},
			},
			{
				title: isPlatformLabels ? "是否必填" : "必填",
				width: 80,
				align: "center",
				render: (_, record) => {
					if (isDividerRow(record)) {
						return null;
					}
					return (
						<Switch
							size="small"
							checked={record.is_system ? true : Boolean(record.slot_config.required)}
							disabled={record.is_system || !canUpdate}
							onChange={checked => onConfigChange(record.id, { required: checked })}
						/>
					);
				},
			},
			{
				title: isPlatformLabels ? "是否显示" : "是否显示",
				width: 88,
				align: "center",
				render: (_, record) => {
					if (isDividerRow(record)) {
						return null;
					}
					return (
						<Switch
							size="small"
							checked={record.is_system ? true : record.slot_config.visible_to_customer !== false}
							disabled={record.is_system || !canUpdate}
							onChange={checked => onConfigChange(record.id, { visible_to_customer: checked })}
						/>
					);
				},
			},
			{
				title: "操作",
				width: 140,
				align: "center",
				fixed: "right",
				render: (_, record) => {
					if (isDividerRow(record)) {
						return null;
					}
					return (
						<span className="attribute-slot-table__actions">
							{canUpdate ? (
								<Tooltip title="编辑">
									<Button
										type="link"
										size="small"
										icon={<EditOutlined />}
										onClick={() => setEditingSlot(record)}
									/>
								</Tooltip>
							) : null}
							{!record.is_system && canUpdate ? (
								<ConfirmPopover
									title="确认拔出该属性？"
									onConfirm={() => onRemove(record.id)}
								>
									<Tooltip title="删除">
										<Button
											type="link"
											size="small"
											danger
											icon={<DeleteOutlined />}
										/>
									</Tooltip>
								</ConfirmPopover>
							) : null}
						</span>
					);
				},
			},
		];
	}, [canUpdate, isPlatformLabels, onConfigChange, onRemove, showDragColumn]);

	const table = (
		<Table
			className="attribute-slot-table"
			rowKey="dragId"
			loading={loading}
			columns={columns}
			dataSource={displayRows}
			pagination={false}
			tableLayout="fixed"
			components={{ body: { row: showDragColumn ? SortableSlotRow : PlainRow } }}
		/>
	);

	return (
		<>
			{showDragColumn ? (
				<DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
					<SortableContext items={sortableIds} strategy={verticalListSortingStrategy}>
						{table}
					</SortableContext>
				</DndContext>
			) : table}

			<AttributeSlotEditModal
				open={Boolean(editingSlot)}
				slot={editingSlot}
				onCancel={() => setEditingSlot(null)}
				onSubmit={(patch) => {
					if (!editingSlot) {
						return;
					}
					onConfigChange(editingSlot.id, {
						display_name: patch.display_name,
						placeholder: patch.placeholder,
						default_value: patch.default_value,
					});
					setEditingSlot(null);
				}}
			/>
		</>
	);
}
