import type { TicketAttributeTypeConfig } from "@uniondesk/shared";

import { DeleteOutlined, HolderOutlined } from "@ant-design/icons";
import type { DragEndEvent } from "@dnd-kit/core";
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { App, Button, ColorPicker, Input } from "antd";
import { useMemo, useState, type CSSProperties } from "react";

import "./attribute-menu-options-editor.less";

export type MenuOptionItem = NonNullable<TicketAttributeTypeConfig["options"]>[number];

const OPTION_COLOR_PRESETS = ["#1677ff", "#52c41a", "#faad14", "#eb2f96", "#722ed1", "#13c2c2"];
const MAX_OPTION_LABEL_LENGTH = 120;

interface AttributeMenuOptionsEditorProps {
	value?: MenuOptionItem[];
	onChange?: (value: MenuOptionItem[]) => void;
}

interface SortableOptionRowProps {
	item: MenuOptionItem;
	colorIndex: number;
	onColorChange: (value: string, color: string) => void;
	onRemove: (value: string) => void;
}

function generateOptionValue(label: string, existing: MenuOptionItem[]): string {
	const base = label.trim().replace(/\s+/g, "_").slice(0, 64) || "option";
	const used = new Set(existing.map(option => option.value));
	let candidate = base;
	let suffix = 2;
	while (used.has(candidate)) {
		candidate = `${base}_${suffix}`;
		suffix += 1;
	}
	return candidate;
}

function pickDefaultColor(index: number): string {
	return OPTION_COLOR_PRESETS[index % OPTION_COLOR_PRESETS.length] ?? OPTION_COLOR_PRESETS[0];
}

function SortableOptionRow({ item, colorIndex, onColorChange, onRemove }: SortableOptionRowProps) {
	const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
		id: item.value,
	});

	const style: CSSProperties = {
		transform: CSS.Transform.toString(transform),
		transition,
	};

	return (
		<div
			ref={setNodeRef}
			style={style}
			className={`attribute-menu-options-editor__row${isDragging ? " attribute-menu-options-editor__row--dragging" : ""}`}
		>
			<span className="attribute-menu-options-editor__handle" {...attributes} {...listeners}>
				<HolderOutlined />
			</span>
			<span className="attribute-menu-options-editor__label" title={item.label}>
				{item.label}
			</span>
			<ColorPicker
				value={item.color ?? pickDefaultColor(colorIndex)}
				size="small"
				className="attribute-menu-options-editor__color-trigger"
				onChangeComplete={(color) => {
					if (color && typeof color !== "string" && "toHex" in color) {
						onColorChange(item.value, `#${color.toHex()}`);
					}
				}}
			/>
			<Button
				type="text"
				size="small"
				icon={<DeleteOutlined />}
				className="attribute-menu-options-editor__delete"
				onClick={() => onRemove(item.value)}
			/>
		</div>
	);
}

export function AttributeMenuOptionsEditor({ value = [], onChange }: AttributeMenuOptionsEditorProps) {
	const { message } = App.useApp();
	const [draftLabel, setDraftLabel] = useState("");
	const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

	const optionIds = useMemo(() => value.map(item => item.value), [value]);

	const emitChange = (next: MenuOptionItem[]) => {
		onChange?.(next);
	};

	const handleAdd = () => {
		const label = draftLabel.trim();
		if (!label) {
			message.warning("请输入选项名称");
			return;
		}
		if (label.length > MAX_OPTION_LABEL_LENGTH) {
			message.warning(`选项名称最多 ${MAX_OPTION_LABEL_LENGTH} 个字符`);
			return;
		}
		if (value.some(item => item.label === label)) {
			message.warning("选项名称不能重复");
			return;
		}
		const nextItem: MenuOptionItem = {
			label,
			value: generateOptionValue(label, value),
			color: pickDefaultColor(value.length),
		};
		emitChange([...value, nextItem]);
		setDraftLabel("");
	};

	const handleDragEnd = (event: DragEndEvent) => {
		const { active, over } = event;
		if (!over || active.id === over.id) {
			return;
		}
		const oldIndex = value.findIndex(item => item.value === active.id);
		const newIndex = value.findIndex(item => item.value === over.id);
		if (oldIndex < 0 || newIndex < 0) {
			return;
		}
		emitChange(arrayMove(value, oldIndex, newIndex));
	};

	return (
		<div className="attribute-menu-options-editor">
			{value.length > 0 ? (
				<DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
					<SortableContext items={optionIds} strategy={verticalListSortingStrategy}>
						<div className="attribute-menu-options-editor__list">
							{value.map((item, index) => (
								<SortableOptionRow
									key={item.value}
									item={item}
									colorIndex={index}
									onColorChange={(optionValue, color) => {
										emitChange(value.map(option => (
											option.value === optionValue ? { ...option, color } : option
										)));
									}}
									onRemove={(optionValue) => {
										emitChange(value.filter(option => option.value !== optionValue));
									}}
								/>
							))}
						</div>
					</SortableContext>
				</DndContext>
			) : null}
			<div className="attribute-menu-options-editor__add">
				<Input
					className="attribute-menu-options-editor__add-input"
					value={draftLabel}
					maxLength={MAX_OPTION_LABEL_LENGTH}
					placeholder={`选项名称，最多 ${MAX_OPTION_LABEL_LENGTH} 个汉字`}
					onChange={event => setDraftLabel(event.target.value)}
					onPressEnter={handleAdd}
				/>
				<Button onClick={handleAdd}>添加</Button>
			</div>
		</div>
	);
}
