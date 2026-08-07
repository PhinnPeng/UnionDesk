import type { TicketAttribute } from "@uniondesk/shared";

import { ConfirmPopover } from "#src/components/confirm-popover";

import { formatAttributeTypeLabel } from "./attribute-utils";

import "./attribute-sortable-table.less";

import { DeleteOutlined, EditOutlined, HolderOutlined } from "@ant-design/icons";
import type { DragEndEvent } from "@dnd-kit/core";
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Button, Space, Switch, Table, Tag, Tooltip } from "antd";
import type { TableColumnsType } from "antd";
import React, { useMemo } from "react";

interface AttributeSortableTableProps {
	loading: boolean;
	dataSource: TicketAttribute[];
	total: number;
	page: number;
	pageSize: number;
	canUpdate: boolean;
	canDelete: boolean;
	onPageChange: (page: number, pageSize: number) => void;
	onReorder: (nextRows: TicketAttribute[]) => void;
	onEdit: (record: TicketAttribute) => void;
	onDelete: (record: TicketAttribute) => void;
	onStatusToggle: (record: TicketAttribute) => void;
}

interface RowProps extends React.HTMLAttributes<HTMLTableRowElement> {
	"data-row-key": string;
}

function createSortableRow() {
	return function SortableRow({ children, ...props }: RowProps) {
		const rowKey = props["data-row-key"];
		const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
			id: rowKey,
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
	};
}

export function AttributeSortableTable({
	loading,
	dataSource,
	total,
	page,
	pageSize,
	canUpdate,
	canDelete,
	onPageChange,
	onReorder,
	onEdit,
	onDelete,
	onStatusToggle,
}: AttributeSortableTableProps) {
	const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));
	const SortableRow = useMemo(() => createSortableRow(), []);

	const columns: TableColumnsType<TicketAttribute> = useMemo(() => [
		{
			title: "",
			width: 48,
			align: "center",
			className: "drag-handle-cell",
			render: () => null,
		},
		{
			title: "名称",
			dataIndex: "name",
			width: 180,
			align: "center",
			ellipsis: true,
			render: (value, record) => (
				<span className="attribute-sortable-table__name-cell">
					<span>{value || "—"}</span>
					{record.is_system ? <Tag color="blue">系统</Tag> : null}
				</span>
			),
		},
		{
			title: "描述",
			dataIndex: "description",
			width: 220,
			align: "center",
			ellipsis: true,
			render: value => value?.trim() || "—",
		},
		{
			title: "属性类型",
			width: 140,
			align: "center",
			ellipsis: true,
			render: (_, record) => formatAttributeTypeLabel(record),
		},
		{
			title: "状态",
			width: 80,
			align: "center",
			render: (_, record) => (
				<Switch
					size="small"
					checked={record.status === "active"}
					disabled={!canUpdate}
					onChange={() => onStatusToggle(record)}
				/>
			),
		},
		{
			title: "操作",
			width: 120,
			align: "center",
			render: (_, record) => (
				<div className="flex justify-center">
					<Space size={4}>
						{canUpdate ? (
							<Tooltip title="编辑">
								<Button type="link" size="small" icon={<EditOutlined />} onClick={() => onEdit(record)} />
							</Tooltip>
						) : null}
						{canDelete ? (
							<ConfirmPopover title="确认删除该属性？" onConfirm={() => onDelete(record)}>
								<Tooltip title="删除">
									<Button type="link" size="small" danger icon={<DeleteOutlined />} />
								</Tooltip>
							</ConfirmPopover>
						) : null}
					</Space>
				</div>
			),
		},
	], [canDelete, canUpdate, onDelete, onEdit, onStatusToggle]);

	const onDragEnd = ({ active, over }: DragEndEvent) => {
		if (!over || active.id === over.id) {
			return;
		}
		const oldIndex = dataSource.findIndex(item => item.id === active.id);
		const newIndex = dataSource.findIndex(item => item.id === over.id);
		if (oldIndex < 0 || newIndex < 0) {
			return;
		}
		const next = arrayMove(dataSource, oldIndex, newIndex).map((item, index) => ({
			...item,
			sort_order: index,
		}));
		onReorder(next);
	};

	const usePagination = total > 100;

	return (
		<DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
			<SortableContext items={dataSource.map(item => item.id)} strategy={verticalListSortingStrategy}>
				<Table
					className="attribute-sortable-table"
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={dataSource}
					tableLayout="fixed"
					pagination={usePagination ? {
						current: page,
						pageSize,
						total,
						showSizeChanger: true,
						onChange: onPageChange,
					} : false}
					components={{ body: { row: SortableRow } }}
				/>
			</SortableContext>
		</DndContext>
	);
}
