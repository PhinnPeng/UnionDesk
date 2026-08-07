import type { PlatformTicketType } from "@uniondesk/shared";

import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";

import "./ticket-type-sortable-table.less";

import { CopyOutlined, DeleteOutlined, EditOutlined, EllipsisOutlined, HolderOutlined, NodeIndexOutlined, SettingOutlined } from "@ant-design/icons";
import type { DragEndEvent } from "@dnd-kit/core";
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Button, Dropdown, Space, Switch, Table, Tag, Tooltip } from "antd";
import type { TableColumnsType } from "antd";
import React, { useMemo } from "react";

interface TicketTypeSortableTableProps {
	loading: boolean;
	dataSource: PlatformTicketType[];
	total: number;
	page: number;
	pageSize: number;
	canUpdate: boolean;
	canDelete: boolean;
	onPageChange: (page: number, pageSize: number) => void;
	onReorder: (nextRows: PlatformTicketType[]) => void;
	onEdit: (record: PlatformTicketType) => void;
	onDelete: (record: PlatformTicketType) => void;
	onStatusToggle: (record: PlatformTicketType) => void;
	onAttributeEdit: (record: PlatformTicketType) => void;
	onWorkflowEdit: (record: PlatformTicketType) => void;
	onCopy: (record: PlatformTicketType) => void;
}

interface RowProps extends React.HTMLAttributes<HTMLTableRowElement> {
	"data-row-key": string;
}

function SortableRow({ children, ...props }: RowProps) {
	const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
		id: props["data-row-key"],
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
}

export function TicketTypeSortableTable({
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
	onAttributeEdit,
	onWorkflowEdit,
	onCopy,
}: TicketTypeSortableTableProps) {
	const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

	const columns: TableColumnsType<PlatformTicketType> = useMemo(() => [
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
			render: (_, record) => (
				<span className="ticket-type-sortable-table__name-cell">
					{record.icon ? resolveMenuIcon(record.icon) : null}
					<span className="ticket-type-sortable-table__name-text">{record.name}</span>
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
			render: value => value || "—",
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
			width: 200,
			align: "center",
			render: (_, record) => (
				<Space size={4}>
					{canUpdate ? (
						<>
							<Tooltip title="编辑">
								<Button type="link" size="small" icon={<EditOutlined />} onClick={() => onEdit(record)} />
							</Tooltip>
							<Tooltip title="属性">
								<Button type="text" size="small" icon={<SettingOutlined />} onClick={() => onAttributeEdit(record)} />
							</Tooltip>
							<Tooltip title="工作流">
								<Button type="text" size="small" icon={<NodeIndexOutlined />} onClick={() => onWorkflowEdit(record)} />
							</Tooltip>
						</>
					) : null}
					<Dropdown
						trigger={["click"]}
						menu={{
							items: [
								{
									key: "copy",
									label: "复制为新类型",
									icon: <CopyOutlined />,
									onClick: () => onCopy(record),
								},
								...(canDelete && !record.is_system ? [{
									key: "delete",
									danger: true,
									label: "删除",
									icon: <DeleteOutlined />,
									onClick: () => onDelete(record),
								}] : []),
							],
						}}
					>
						<Tooltip title="更多">
							<Button type="text" size="small" icon={<EllipsisOutlined />} />
						</Tooltip>
					</Dropdown>
				</Space>
			),
		},
	], [canDelete, canUpdate, onAttributeEdit, onCopy, onDelete, onEdit, onStatusToggle, onWorkflowEdit]);

	const onDragEnd = (event: DragEndEvent) => {
		const { active, over } = event;
		if (!over || active.id === over.id) {
			return;
		}
		const oldIndex = dataSource.findIndex(item => item.id === active.id);
		const newIndex = dataSource.findIndex(item => item.id === over.id);
		if (oldIndex < 0 || newIndex < 0) {
			return;
		}
		onReorder(arrayMove(dataSource, oldIndex, newIndex));
	};

	const usePagination = total > 100;

	return (
		<DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
			<SortableContext items={dataSource.map(item => item.id)} strategy={verticalListSortingStrategy}>
				<Table
					className="ticket-type-sortable-table"
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
